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
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.ErrorBanner
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.MoneyField
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.accounting.AdjustCashCommand
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.usecase.AdjustCashUseCase
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.util.OperationIds
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountAdjustUiState(
    val account: Account? = null,
    val actualBalance: Long? = null,
    val reason: String = "",
    val saving: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class AccountAdjustViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: AccountRepository,
    private val adjustCash: AdjustCashUseCase,
    private val getBusiness: GetBusinessUseCase
) : ViewModel() {

    private val accountId: Long = savedStateHandle.get<Long>("accountId") ?: 0L
    private val operationId = OperationIds.new()
    private val _state = MutableStateFlow(AccountAdjustUiState())
    val state = _state.asStateFlow()
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeAccounts().collect { list ->
                _state.update { it.copy(account = list.firstOrNull { account -> account.id == accountId }) }
            }
        }
    }

    fun setActualBalance(value: Long?) = _state.update { it.copy(actualBalance = value) }
    fun setReason(value: String) = _state.update { it.copy(reason = value) }

    fun submit() {
        val current = _state.value
        val account = current.account ?: return
        val actual = current.actualBalance ?: return
        if (current.saving) return
        val error = when {
            actual == account.currentBalance -> "موجودی واقعی با موجودی ثبت‌شده یکسان است"
            actual < 0 -> "موجودی واقعی نمی‌تواند منفی باشد"
            current.reason.isBlank() -> "دلیل تنظیم موجودی را وارد کنید"
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
            val command = AdjustCashCommand(
                businessId = business?.id ?: 0L,
                operationId = operationId,
                date = JalaliDate.startOfDayMillis(System.currentTimeMillis()),
                accountId = account.id,
                accountTitle = account.title,
                currentBalance = account.currentBalance,
                actualBalance = actual,
                reason = current.reason.trim(),
                now = System.currentTimeMillis()
            )
            when (val result = adjustCash(command)) {
                is OperationResult.Success -> {
                    _state.update { it.copy(saving = false, savedId = result.data) }
                    _events.send(UiEvent("موجودی حساب تنظیم شد"))
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
fun AccountAdjustScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: AccountAdjustViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }

    val account = state.account
    val difference = (state.actualBalance ?: account?.currentBalance ?: 0L) - (account?.currentBalance ?: 0L)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "تنظیم موجودی", subtitle = account?.title, onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) ErrorBanner(state.error!!)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "موجودی ثبت‌شده: ${PersianText.formatMoney(account?.currentBalance ?: 0L)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    MoneyField(value = state.actualBalance, onValueChange = viewModel::setActualBalance, label = "موجودی واقعی *")
                    if (state.actualBalance != null) {
                        Text(
                            "اختلاف: ${PersianText.formatMoney(difference)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (difference >= 0) HesabyarTheme.colors.income else HesabyarTheme.colors.expense
                        )
                    }
                    AppTextField(
                        value = state.reason,
                        onValueChange = viewModel::setReason,
                        label = "دلیل تنظیم *",
                        placeholder = "مثلاً شمارش صندوق"
                    )
                    Text(
                        "این عملیات اختلاف را به‌عنوان اصلاح صندوق ثبت می‌کند و سابقه آن حفظ می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = HesabyarTheme.colors.onSurfaceSecondary
                    )
                }
            }
            PrimaryButton(text = "ثبت تنظیم", onClick = viewModel::submit, loading = state.saving)
            Spacer(Modifier.height(16.dp))
        }
    }
}
