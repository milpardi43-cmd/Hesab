package ir.hesabyar.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.JalaliDateField
import ir.hesabyar.core.design.components.MoneyField
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.components.SecondaryButton
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.AccountType
import ir.hesabyar.domain.model.Business
import ir.hesabyar.domain.model.BusinessType
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactType
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.usecase.CreateBusinessUseCase
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.usecase.SaveContactUseCase
import ir.hesabyar.domain.usecase.SaveProductUseCase
import ir.hesabyar.domain.usecase.UpdateSettingsUseCase
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val step: Int = 1,
    val name: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val type: BusinessType = BusinessType.SUPERMARKET,
    val city: String = "",
    val cash: Long? = null,
    val openingDate: Long = JalaliDate.startOfDayMillis(System.currentTimeMillis()),
    val inventoryValue: Long? = null,
    val receivable: Long? = null,
    val payable: Long? = null,
    val nameError: String? = null,
    val phoneError: String? = null,
    val saving: Boolean = false,
    val done: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val createBusiness: CreateBusinessUseCase,
    private val getBusiness: GetBusinessUseCase,
    private val saveContact: SaveContactUseCase,
    private val saveProduct: SaveProductUseCase,
    private val accountRepository: AccountRepository,
    private val updateSettings: UpdateSettingsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setName(value: String) = _state.update { it.copy(name = value, nameError = null) }
    fun setOwner(value: String) = _state.update { it.copy(ownerName = value) }
    fun setPhone(value: String) = _state.update { it.copy(phone = value, phoneError = null) }
    fun setType(value: BusinessType) = _state.update { it.copy(type = value) }
    fun setCity(value: String) = _state.update { it.copy(city = value) }
    fun setCash(value: Long?) = _state.update { it.copy(cash = value) }
    fun setDate(value: Long) = _state.update { it.copy(openingDate = value) }
    fun setInventory(value: Long?) = _state.update { it.copy(inventoryValue = value) }
    fun setReceivable(value: Long?) = _state.update { it.copy(receivable = value) }
    fun setPayable(value: Long?) = _state.update { it.copy(payable = value) }

    fun next() {
        val current = _state.value
        val nameError = if (current.name.isBlank()) "نام فروشگاه را وارد کنید" else null
        val phoneNormalized = PersianText.toEnglishDigits(current.phone).trim()
        val phoneError = if (phoneNormalized.isNotEmpty() && !phoneNormalized.matches(Regex("^09\\d{9}$"))) {
            "شماره موبایل باید ۱۱ رقم و با ۰۹ شروع شود"
        } else null
        if (nameError != null || phoneError != null) {
            _state.update { it.copy(nameError = nameError, phoneError = phoneError) }
            return
        }
        _state.update { it.copy(step = 2, phone = phoneNormalized) }
    }

    fun back() = _state.update { it.copy(step = 1) }

    fun save() {
        val current = _state.value
        if (current.saving || current.done) return
        if (current.name.isBlank()) {
            _state.update { it.copy(step = 1, nameError = "نام فروشگاه را وارد کنید") }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val business = Business(
                name = current.name.trim(),
                ownerName = current.ownerName.trim().takeIf { it.isNotBlank() },
                phone = current.phone.trim().takeIf { it.isNotBlank() },
                type = current.type,
                city = current.city.trim().takeIf { it.isNotBlank() }
            )
            when (val result = createBusiness(business, current.cash ?: 0L)) {
                is OperationResult.Failure -> {
                    _state.update { it.copy(saving = false) }
                    _events.send(UiEvent(result.message, isError = true))
                }
                is OperationResult.Success -> {
                    val created = getBusiness.current()
                    if (created == null) {
                        _state.update { it.copy(saving = false) }
                        _events.send(UiEvent("خطا در خواندن اطلاعات فروشگاه", isError = true))
                        return@launch
                    }
                    val receivable = current.receivable ?: 0L
                    if (receivable > 0) {
                        saveContact(
                            Contact(
                                businessId = created.id,
                                name = "مشتریان متفرقه (مانده اولیه)",
                                type = ContactType.CUSTOMER,
                                initialBalance = receivable,
                                balance = receivable,
                                description = "ثبت‌شده در راه‌اندازی اولیه"
                            )
                        )
                    }
                    val payable = current.payable ?: 0L
                    if (payable > 0) {
                        saveContact(
                            Contact(
                                businessId = created.id,
                                name = "تأمین‌کنندگان متفرقه (مانده اولیه)",
                                type = ContactType.SUPPLIER,
                                initialBalance = -payable,
                                balance = -payable,
                                description = "ثبت‌شده در راه‌اندازی اولیه"
                            )
                        )
                    }
                    val inventory = current.inventoryValue ?: 0L
                    if (inventory > 0) {
                        saveProduct(
                            Product(
                                businessId = created.id,
                                name = "موجودی اولیه انبار",
                                purchasePrice = inventory,
                                sellingPrice = inventory,
                                averageCost = inventory,
                                stockQuantity = 1.0,
                                minimumStock = 0.0,
                                description = "ثبت‌شده در راه‌اندازی اولیه"
                            )
                        )
                    }
                    val cashAccount = accountRepository.getFirstAccount(AccountType.CASH)
                    updateSettings {
                        it.copy(
                            defaultReceiveAccountId = cashAccount?.id,
                            defaultPayAccountId = cashAccount?.id
                        )
                    }
                    _state.update { it.copy(saving = false, done = true) }
                    _events.send(UiEvent("فروشگاه با موفقیت راه‌اندازی شد"))
                }
            }
        }
    }
}

@Composable
fun SetupScreen(
    onDone: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event -> snackbar.showSnackbar(event.message) }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepDot(number = "۱", active = true, done = state.step > 1)
                Box(Modifier.height(2.dp).weight(1f).background(if (state.step > 1) MaterialTheme.colorScheme.primary else HesabyarTheme.colors.hairline))
                StepDot(number = "۲", active = state.step == 2, done = false)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                if (state.step == 1) "اطلاعات فروشگاه" else "مانده‌های اولیه",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (state.step == 1) "این اطلاعات در فاکتورها و پشتیبان‌گیری استفاده می‌شود."
                else "اگر فروشگاه را از قبل دارید، مانده‌های اولیه را وارد کنید. می‌توانید خالی بگذارید.",
                style = MaterialTheme.typography.bodyMedium,
                color = HesabyarTheme.colors.onSurfaceSecondary
            )
            Spacer(Modifier.height(22.dp))

            if (state.step == 1) {
                AppTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = "نام فروشگاه *",
                    error = state.nameError,
                    placeholder = "مثلاً فروشگاه آفتاب"
                )
                Spacer(Modifier.height(14.dp))
                AppTextField(
                    value = state.ownerName,
                    onValueChange = viewModel::setOwner,
                    label = "نام مالک / مدیر",
                    placeholder = "اختیاری"
                )
                Spacer(Modifier.height(14.dp))
                AppTextField(
                    value = state.phone,
                    onValueChange = viewModel::setPhone,
                    label = "شماره موبایل",
                    error = state.phoneError,
                    keyboardType = KeyboardType.Phone,
                    placeholder = "09xxxxxxxxx"
                )
                Spacer(Modifier.height(14.dp))
                Text("نوع فعالیت", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                Spacer(Modifier.height(8.dp))
                ir.hesabyar.core.design.components.ChipSelector(
                    options = BusinessType.entries.toList(),
                    selected = state.type,
                    onSelect = viewModel::setType,
                    optionLabel = { it.label }
                )
                Spacer(Modifier.height(14.dp))
                AppTextField(
                    value = state.city,
                    onValueChange = viewModel::setCity,
                    label = "شهر",
                    placeholder = "اختیاری"
                )
                Spacer(Modifier.height(28.dp))
                PrimaryButton(text = "ادامه", onClick = viewModel::next)
            } else {
                MoneyField(
                    value = state.cash,
                    onValueChange = viewModel::setCash,
                    label = "موجودی صندوق",
                )
                Spacer(Modifier.height(14.dp))
                JalaliDateField(
                    label = "تاریخ شروع",
                    millis = state.openingDate,
                    onSelect = viewModel::setDate
                )
                Spacer(Modifier.height(14.dp))
                MoneyField(
                    value = state.inventoryValue,
                    onValueChange = viewModel::setInventory,
                    label = "ارزش موجودی انبار"
                )
                Spacer(Modifier.height(14.dp))
                MoneyField(
                    value = state.receivable,
                    onValueChange = viewModel::setReceivable,
                    label = "طلب از مشتریان (بدهی مشتریان به شما)"
                )
                Spacer(Modifier.height(14.dp))
                MoneyField(
                    value = state.payable,
                    onValueChange = viewModel::setPayable,
                    label = "بدهی به تأمین‌کنندگان"
                )
                Spacer(Modifier.height(24.dp))
                PrimaryButton(
                    text = "ساخت فروشگاه و شروع",
                    onClick = viewModel::save,
                    loading = state.saving
                )
                Spacer(Modifier.height(10.dp))
                SecondaryButton(text = "بازگشت", onClick = viewModel::back, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StepDot(number: String, active: Boolean, done: Boolean) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(
                if (active || done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = if (active || done) MaterialTheme.colorScheme.onPrimary else HesabyarTheme.colors.onSurfaceSecondary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
