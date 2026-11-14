package ir.hesabyar.app.ui.contacts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.AccountDropdown
import ir.hesabyar.app.ui.common.ContactPickerSheet
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.ChipSelector
import ir.hesabyar.core.design.components.ErrorBanner
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.JalaliDateField
import ir.hesabyar.core.design.components.MoneyField
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.accounting.PaySupplierCommand
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.accounting.ReceivePaymentCommand
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactFilter
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.PaymentMethod
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.repository.SettingsRepository
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.usecase.PaySupplierUseCase
import ir.hesabyar.domain.usecase.ReceivePaymentUseCase
import ir.hesabyar.domain.util.OperationIds
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentUiState(
    val mode: String = "receive",
    val contactId: Long? = null,
    val amount: Long? = null,
    val method: PaymentMethod = PaymentMethod.CASH,
    val accountId: Long? = null,
    val date: Long = JalaliDate.startOfDayMillis(System.currentTimeMillis()),
    val description: String = "",
    val contacts: List<Contact> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val saving: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null
) {
    val isReceive: Boolean get() = mode == "receive"
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val receivePayment: ReceivePaymentUseCase,
    private val paySupplier: PaySupplierUseCase,
    private val contactsRepository: ContactRepository,
    private val accountsRepository: AccountRepository,
    private val settings: SettingsRepository,
    private val getBusiness: GetBusinessUseCase
) : ViewModel() {

    private val operationId = OperationIds.new()
    private val _state = MutableStateFlow(
        PaymentUiState(
            mode = savedStateHandle.get<String>("mode") ?: "receive",
            contactId = (savedStateHandle.get<Long>("contactId") ?: -1L).takeIf { it > 0 },
            amount = (savedStateHandle.get<Long>("amount") ?: 0L).takeIf { it > 0 }
        )
    )
    val state = _state.asStateFlow()
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            contactsRepository.observeContacts(ContactFilter()).collect { list ->
                _state.update { it.copy(contacts = list) }
            }
        }
        viewModelScope.launch {
            accountsRepository.observeAccounts().collect { list ->
                _state.update { current ->
                    val selected = current.accountId?.takeIf { id -> list.any { it.id == id } } ?: list.firstOrNull()?.id
                    current.copy(accounts = list, accountId = selected)
                }
            }
        }
        viewModelScope.launch {
            val current = settings.current()
            val preferred = if (_state.value.isReceive) current.defaultReceiveAccountId else current.defaultPayAccountId
            if (preferred != null) _state.update { it.copy(accountId = it.accountId ?: preferred) }
        }
    }

    fun setContact(contact: Contact) = _state.update { it.copy(contactId = contact.id, error = null) }
    fun setAmount(value: Long?) = _state.update { it.copy(amount = value) }
    fun setMethod(value: PaymentMethod) = _state.update { it.copy(method = value) }
    fun setAccount(value: Account) = _state.update { it.copy(accountId = value.id) }
    fun setDate(value: Long) = _state.update { it.copy(date = value) }
    fun setDescription(value: String) = _state.update { it.copy(description = value) }

    fun submit() {
        val current = _state.value
        if (current.saving) return
        val error = when {
            current.contactId == null -> "شخص را انتخاب کنید"
            (current.amount ?: 0) <= 0 -> "مبلغ را وارد کنید"
            current.accountId == null -> "حساب را انتخاب کنید"
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            viewModelScope.launch { _events.send(UiEvent(error, isError = true)) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val business = getBusiness.current()
            val result = if (current.isReceive) {
                receivePayment(
                    ReceivePaymentCommand(
                        businessId = business?.id ?: 0L,
                        operationId = operationId,
                        date = current.date,
                        contactId = current.contactId!!,
                        amount = current.amount ?: 0L,
                        paymentMethod = current.method,
                        accountId = current.accountId!!,
                        description = current.description.takeIf { it.isNotBlank() },
                        now = System.currentTimeMillis()
                    )
                )
            } else {
                paySupplier(
                    PaySupplierCommand(
                        businessId = business?.id ?: 0L,
                        operationId = operationId,
                        date = current.date,
                        contactId = current.contactId!!,
                        amount = current.amount ?: 0L,
                        paymentMethod = current.method,
                        accountId = current.accountId!!,
                        description = current.description.takeIf { it.isNotBlank() },
                        now = System.currentTimeMillis()
                    )
                )
            }
            when (result) {
                is OperationResult.Success -> {
                    _state.update { it.copy(saving = false, savedId = result.data) }
                    _events.send(UiEvent(if (current.isReceive) "دریافت ثبت شد" else "پرداخت ثبت شد"))
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
fun PaymentScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showContactPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }

    val contact = state.contacts.firstOrNull { it.id == state.contactId }
    val eligible = if (state.isReceive) state.contacts.filter { it.type.isCustomer } else state.contacts.filter { it.type.isSupplier }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HesabyarTopBar(
                title = if (state.isReceive) "دریافت از مشتری" else "پرداخت به تأمین‌کننده",
                subtitle = "تسویه مانده حساب",
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) ErrorBanner(state.error!!)
            SectionHeader(title = "شخص")
            AppCard {
                Surface(
                    onClick = { showContactPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                contact?.name ?: "انتخاب شخص",
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (contact != null) {
                                Text(
                                    "مانده: ${PersianText.formatMoney(kotlin.math.abs(contact.balance))} ${if (contact.balance > 0) "بدهکار" else if (contact.balance < 0) "بستانکار" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HesabyarTheme.colors.onSurfaceSecondary
                                )
                            }
                        }
                    }
                }
            }
            SectionHeader(title = "مبلغ و روش")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneyField(value = state.amount, onValueChange = viewModel::setAmount, label = "مبلغ *")
                    if (contact != null) {
                        Text(
                            "مانده فعلی: ${PersianText.formatMoney(kotlin.math.abs(contact.balance))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                    Text("روش پرداخت", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                    ChipSelector(
                        options = PaymentMethod.entries.toList(),
                        selected = state.method,
                        onSelect = viewModel::setMethod,
                        optionLabel = { it.label }
                    )
                    AccountDropdown(
                        label = if (state.isReceive) "حساب دریافت" else "حساب پرداخت",
                        accounts = state.accounts,
                        selectedId = state.accountId,
                        onSelect = viewModel::setAccount
                    )
                }
            }
            SectionHeader(title = "جزئیات")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    JalaliDateField(label = "تاریخ", millis = state.date, onSelect = viewModel::setDate)
                    AppTextField(
                        value = state.description,
                        onValueChange = viewModel::setDescription,
                        label = "توضیحات",
                        placeholder = "اختیاری",
                        singleLine = false,
                        maxLines = 3
                    )
                    InfoRowPreview(state.amount ?: 0L, state.isReceive)
                }
            }
            PrimaryButton(
                text = if (state.isReceive) "ثبت دریافت" else "ثبت پرداخت",
                onClick = viewModel::submit,
                loading = state.saving
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showContactPicker) {
        ContactPickerSheet(
            title = if (state.isReceive) "انتخاب مشتری" else "انتخاب تأمین‌کننده",
            contacts = eligible,
            onSelect = {
                viewModel.setContact(it)
                showContactPicker = false
            },
            onDismiss = { showContactPicker = false }
        )
    }
}

@Composable
private fun InfoRowPreview(amount: Long, isReceive: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("اثر مالی", style = MaterialTheme.typography.bodySmall, color = HesabyarTheme.colors.onSurfaceSecondary)
        MoneyText(
            amount = if (isReceive) amount else -amount,
            style = MaterialTheme.typography.labelLarge,
            color = if (isReceive) HesabyarTheme.colors.income else HesabyarTheme.colors.expense,
            signed = true
        )
    }
}
