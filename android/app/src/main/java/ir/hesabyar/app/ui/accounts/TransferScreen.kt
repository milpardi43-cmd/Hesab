package ir.hesabyar.app.ui.accounts

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.AccountDropdown
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.ErrorBanner
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.MoneyField
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.accounting.TransferFundsCommand
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.usecase.TransferFundsUseCase
import ir.hesabyar.domain.util.OperationIds
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransferUiState(
    val fromId: Long? = null,
    val toId: Long? = null,
    val amount: Long? = null,
    val note: String = "",
    val accounts: List<Account> = emptyList(),
    val saving: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class TransferViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transferFunds: TransferFundsUseCase,
    repository: AccountRepository,
    private val getBusiness: GetBusinessUseCase
) : ViewModel() {

    private val operationId = OperationIds.new()
    private val initialFrom = savedStateHandle.get<Long>("fromId") ?: -1L
    private val _state = MutableStateFlow(TransferUiState(fromId = initialFrom.takeIf { it > 0 }))
    val state = _state.asStateFlow()
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeAccounts().collect { list ->
                _state.update { current ->
                    val from = current.fromId?.takeIf { id -> list.any { it.id == id } } ?: list.firstOrNull()?.id
                    val to = current.toId?.takeIf { id -> id != from && list.any { it.id == id } } ?: list.firstOrNull { it.id != from }?.id
                    current.copy(accounts = list, fromId = from, toId = to)
                }
            }
        }
    }

    fun setFrom(account: Account) = _state.update { current ->
        current.copy(
            fromId = account.id,
            toId = if (current.toId == account.id) current.accounts.firstOrNull { it.id != account.id }?.id else current.toId
        )
    }

    fun setTo(account: Account) = _state.update { it.copy(toId = account.id) }
    fun setAmount(value: Long?) = _state.update { it.copy(amount = value) }
    fun setNote(value: String) = _state.update { it.copy(note = value) }

    fun submit() {
        val current = _state.value
        val from = current.accounts.firstOrNull { it.id == current.fromId }
        val to = current.accounts.firstOrNull { it.id == current.toId }
        if (current.saving) return
        val error = when {
            from == null || to == null -> "حساب مبدأ و مقصد را انتخاب کنید"
            from.id == to.id -> "حساب مبدأ و مقصد نباید یکی باشند"
            (current.amount ?: 0) <= 0 -> "مبلغ انتقال را وارد کنید"
            (current.amount ?: 0) > from.currentBalance -> "موجودی حساب مبدأ کافی نیست"
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
            val command = TransferFundsCommand(
                businessId = business?.id ?: 0L,
                operationId = operationId,
                date = JalaliDate.startOfDayMillis(System.currentTimeMillis()),
                fromAccountId = from!!.id,
                fromAccountTitle = from.title,
                fromBalance = from.currentBalance,
                toAccountId = to!!.id,
                toAccountTitle = to.title,
                amount = current.amount ?: 0L,
                note = current.note.takeIf { it.isNotBlank() },
                now = System.currentTimeMillis()
            )
            when (val result = transferFunds(command)) {
                is OperationResult.Success -> {
                    _state.update { it.copy(saving = false, savedId = result.data) }
                    _events.send(UiEvent("انتقال وجه انجام شد"))
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
fun TransferScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }

    val from = state.accounts.firstOrNull { it.id == state.fromId }
    val to = state.accounts.firstOrNull { it.id == state.toId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "انتقال وجه", subtitle = "جابه‌جایی پول بین حساب‌ها", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) ErrorBanner(state.error!!)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccountDropdown(label = "از حساب", accounts = state.accounts, selectedId = state.fromId, onSelect = viewModel::setFrom)
                    AccountDropdown(label = "به حساب", accounts = state.accounts.filter { it.id != state.fromId }, selectedId = state.toId, onSelect = viewModel::setTo)
                    if (from != null) {
                        Text(
                            "موجودی ${from.title}: ${PersianText.formatMoney(from.currentBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                    MoneyField(value = state.amount, onValueChange = viewModel::setAmount, label = "مبلغ انتقال *")
                    AppTextField(value = state.note, onValueChange = viewModel::setNote, label = "توضیحات", placeholder = "اختیاری")
                    if (from != null && to != null) {
                        Text(
                            "${from.title} ← ${to.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            PrimaryButton(text = "ثبت انتقال", onClick = viewModel::submit, loading = state.saving)
            Spacer(Modifier.height(16.dp))
        }
    }
}
