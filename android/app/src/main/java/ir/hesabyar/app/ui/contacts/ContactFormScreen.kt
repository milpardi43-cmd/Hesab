package ir.hesabyar.app.ui.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.ChipSelector
import ir.hesabyar.core.design.components.ErrorBanner
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.LoadingScreen
import ir.hesabyar.core.design.components.MoneyField
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactType
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.usecase.CheckContactLimitUseCase
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.usecase.SaveContactUseCase
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactFormUiState(
    val loading: Boolean = true,
    val name: String = "",
    val phone: String = "",
    val type: ContactType = ContactType.CUSTOMER,
    val description: String = "",
    val initialBalance: Long? = null,
    val existing: Contact? = null,
    val nameError: String? = null,
    val phoneError: String? = null,
    val saving: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null
) {
    val isEdit: Boolean get() = existing != null
}

@HiltViewModel
class ContactFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ContactRepository,
    private val saveContact: SaveContactUseCase,
    private val getBusiness: GetBusinessUseCase,
    private val checkLimit: CheckContactLimitUseCase
) : ViewModel() {

    private val contactId: Long = savedStateHandle.get<Long>("contactId") ?: 0L

    private val _state = MutableStateFlow(ContactFormUiState())
    val state = _state.asStateFlow()
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val contact = if (contactId > 0) repository.getContact(contactId) else null
            _state.update {
                if (contact != null) {
                    it.copy(
                        loading = false,
                        existing = contact,
                        name = contact.name,
                        phone = contact.phone.orEmpty(),
                        type = contact.type,
                        description = contact.description.orEmpty(),
                        initialBalance = contact.initialBalance
                    )
                } else {
                    it.copy(loading = false)
                }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value, nameError = null) }
    fun setPhone(value: String) = _state.update { it.copy(phone = value, phoneError = null) }
    fun setType(value: ContactType) = _state.update { it.copy(type = value) }
    fun setDescription(value: String) = _state.update { it.copy(description = value) }
    fun setInitialBalance(value: Long?) = _state.update { it.copy(initialBalance = value) }

    fun save() {
        val current = _state.value
        if (current.saving) return
        val phone = PersianText.toEnglishDigits(current.phone).trim()
        val nameError = if (current.name.isBlank()) "نام شخص را وارد کنید" else null
        val phoneError = if (phone.isNotEmpty() && !phone.matches(Regex("^09\\d{9}$"))) "شماره موبایل معتبر نیست" else null
        if (nameError != null || phoneError != null) {
            _state.update { it.copy(nameError = nameError, phoneError = phoneError) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            if (contactId == 0L) {
                checkLimit()?.let { message ->
                    _state.update { it.copy(saving = false, error = message) }
                    _events.send(UiEvent(message, isError = true))
                    return@launch
                }
            }
            val businessId = current.existing?.businessId ?: getBusiness.current()?.id ?: 0L
            val contact = Contact(
                id = contactId,
                businessId = businessId,
                name = current.name,
                phone = phone.takeIf { it.isNotBlank() },
                type = current.type,
                initialBalance = if (contactId == 0L) (current.initialBalance ?: 0L) else current.existing?.initialBalance ?: 0L,
                balance = current.existing?.balance ?: 0L,
                description = current.description.takeIf { it.isNotBlank() }
            )
            when (val result = saveContact(contact)) {
                is OperationResult.Success -> {
                    _state.update { it.copy(saving = false, savedId = result.data) }
                    _events.send(UiEvent(if (contactId == 0L) "شخص جدید ثبت شد" else "اطلاعات به‌روزرسانی شد"))
                }
                is OperationResult.Failure -> {
                    _state.update { it.copy(saving = false, error = result.message) }
                    _events.send(UiEvent(result.message, isError = true))
                }
            }
        }
    }
}

@Composable
fun ContactFormScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: ContactFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = if (state.isEdit) "ویرایش شخص" else "شخص جدید", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (state.loading) {
            LoadingScreen(Modifier.padding(padding))
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.error != null) ErrorBanner(state.error!!)
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppTextField(
                            value = state.name,
                            onValueChange = viewModel::setName,
                            label = "نام شخص *",
                            error = state.nameError,
                            placeholder = "مثلاً علی رضایی"
                        )
                        AppTextField(
                            value = state.phone,
                            onValueChange = viewModel::setPhone,
                            label = "شماره موبایل",
                            error = state.phoneError,
                            keyboardType = KeyboardType.Phone,
                            placeholder = "09xxxxxxxxx"
                        )
                        Text("نوع شخص", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                        ChipSelector(
                            options = ContactType.entries.toList(),
                            selected = state.type,
                            onSelect = viewModel::setType,
                            optionLabel = { it.label }
                        )
                        AppTextField(
                            value = state.description,
                            onValueChange = viewModel::setDescription,
                            label = "توضیحات",
                            placeholder = "اختیاری",
                            singleLine = false,
                            maxLines = 3
                        )
                    }
                }
                if (!state.isEdit) {
                    SectionHeader(title = "مانده اولیه")
                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MoneyField(
                                value = state.initialBalance,
                                onValueChange = viewModel::setInitialBalance,
                                label = "مانده اولیه حساب"
                            )
                            Text(
                                "برای مشتری، مبلغی که از او طلب دارید (بدهکار) و برای تأمین‌کننده، مبلغی که به او بدهکارید را با علامت منفی وارد کنید. در صورت نیاز بعداً هم قابل ثبت است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = HesabyarTheme.colors.onSurfaceSecondary
                            )
                        }
                    }
                } else {
                    SectionHeader(title = "مانده فعلی")
                    AppCard {
                        Text(
                            "مانده حساب این شخص با تراکنش‌ها تغییر می‌کند و از این فرم قابل ویرایش نیست.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                }
                PrimaryButton(
                    text = if (state.isEdit) "ذخیره تغییرات" else "ثبت شخص",
                    onClick = viewModel::save,
                    loading = state.saving
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
