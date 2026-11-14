package ir.hesabyar.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.domain.accounting.CreateExpenseCommand
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactFilter
import ir.hesabyar.domain.model.ExpenseCategory
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.PaymentMethod
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.repository.SettingsRepository
import ir.hesabyar.domain.usecase.CreateExpenseUseCase
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.util.OperationIds
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseUiState(
    val title: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val amount: Long? = null,
    val method: PaymentMethod = PaymentMethod.CASH,
    val accountId: Long? = null,
    val contactId: Long? = null,
    val date: Long = JalaliDate.startOfDayMillis(System.currentTimeMillis()),
    val description: String = "",
    val imagePath: String? = null,
    val isPaid: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val saving: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val createExpense: CreateExpenseUseCase,
    private val getBusiness: GetBusinessUseCase,
    private val accounts: AccountRepository,
    contacts: ContactRepository,
    settings: SettingsRepository
) : ViewModel() {

    private val operationId = OperationIds.new()
    private val _state = MutableStateFlow(ExpenseUiState())
    val state = _state.asStateFlow()
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            accounts.observeAccounts().collect { list ->
                _state.update { current ->
                    val selected = current.accountId?.takeIf { id -> list.any { it.id == id } } ?: list.firstOrNull()?.id
                    current.copy(accounts = list, accountId = selected)
                }
            }
        }
        viewModelScope.launch {
            contacts.observeContacts(ContactFilter()).collect { list ->
                _state.update { it.copy(contacts = list) }
            }
        }
        viewModelScope.launch {
            val preferred = settings.current().defaultPayAccountId
            if (preferred != null) _state.update { it.copy(accountId = it.accountId ?: preferred) }
        }
    }

    fun setTitle(value: String) = _state.update { it.copy(title = value, error = null) }
    fun setCategory(value: ExpenseCategory) = _state.update { it.copy(category = value) }
    fun setAmount(value: Long?) = _state.update { it.copy(amount = value) }
    fun setMethod(value: PaymentMethod) = _state.update { it.copy(method = value) }
    fun setAccount(value: Account) = _state.update { it.copy(accountId = value.id) }
    fun setContact(value: Contact) = _state.update { it.copy(contactId = value.id) }
    fun setDate(value: Long) = _state.update { it.copy(date = value) }
    fun setDescription(value: String) = _state.update { it.copy(description = value) }
    fun setImagePath(value: String?) = _state.update { it.copy(imagePath = value) }
    fun setPaid(value: Boolean) = _state.update { it.copy(isPaid = value) }

    fun submit() {
        val current = _state.value
        if (current.saving) return
        val error = when {
            current.title.isBlank() -> "عنوان هزینه را وارد کنید"
            (current.amount ?: 0) <= 0 -> "مبلغ هزینه را وارد کنید"
            current.isPaid && current.accountId == null -> "حساب پرداخت را انتخاب کنید"
            !current.isPaid && current.contactId == null -> "برای هزینه پرداخت‌نشده، شخص طلبکار را انتخاب کنید"
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
            val command = CreateExpenseCommand(
                businessId = business?.id ?: 0L,
                operationId = operationId,
                date = current.date,
                title = current.title.trim(),
                category = current.category,
                amount = current.amount ?: 0L,
                paymentMethod = current.method,
                accountId = if (current.isPaid) current.accountId else null,
                contactId = current.contactId,
                description = current.description.takeIf { it.isNotBlank() },
                imagePath = current.imagePath,
                isPaid = current.isPaid,
                now = System.currentTimeMillis()
            )
            when (val result = createExpense(command)) {
                is OperationResult.Success -> {
                    _state.update { it.copy(saving = false, savedId = result.data) }
                    _events.send(UiEvent("هزینه با موفقیت ثبت شد"))
                }
                is OperationResult.Failure -> {
                    _state.update { it.copy(saving = false, error = result.message) }
                    _events.send(UiEvent(result.message, isError = true))
                }
            }
        }
    }
}
