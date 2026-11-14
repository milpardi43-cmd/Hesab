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
import ir.hesabyar.core.design.components.SegmentedControl
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.usecase.AddOpeningBalanceUseCase
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OpeningBalanceUiState(
    val contactName: String = "",
    val isDebit: Boolean = true,
    val amount: Long? = null,
    val note: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OpeningBalanceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addOpeningBalance: AddOpeningBalanceUseCase,
    contacts: ContactRepository
) : ViewModel() {

    private val contactId: Long = savedStateHandle.get<Long>("contactId") ?: 0L
    private val _state = MutableStateFlow(OpeningBalanceUiState())
    val state = _state.asStateFlow()
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            contacts.getContact(contactId)?.let { contact ->
                _state.update {
                    it.copy(
                        contactName = contact.name,
                        isDebit = contact.type.isCustomer
                    )
                }
            }
        }
    }

    fun setDebit(value: Boolean) = _state.update { it.copy(isDebit = value) }
    fun setAmount(value: Long?) = _state.update { it.copy(amount = value) }
    fun setNote(value: String) = _state.update { it.copy(note = value) }

    fun submit() {
        val current = _state.value
        if (current.saving) return
        val amount = current.amount ?: 0L
        if (amount <= 0) {
            _state.update { it.copy(error = "مبلغ را وارد کنید") }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val signed = if (current.isDebit) amount else -amount
            when (val result = addOpeningBalance(contactId, signed, current.note)) {
                is OperationResult.Success -> {
                    _state.update { it.copy(saving = false, saved = true) }
                    _events.send(UiEvent("مانده اولیه ثبت شد"))
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
fun OpeningBalanceScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: OpeningBalanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "ثبت مانده اولیه", subtitle = state.contactName, onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) ErrorBanner(state.error!!)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("نوع مانده", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                    SegmentedControl(
                        options = listOf(true, false),
                        selected = state.isDebit,
                        onSelect = viewModel::setDebit,
                        label = { if (it) "بدهکار (طلب ما)" else "بستانکار (بدهی ما)" }
                    )
                    MoneyField(value = state.amount, onValueChange = viewModel::setAmount, label = "مبلغ *")
                    AppTextField(
                        value = state.note,
                        onValueChange = viewModel::setNote,
                        label = "توضیح",
                        placeholder = "مثلاً مانده از سال قبل"
                    )
                    Text(
                        "این مبلغ به‌صورت یک گردش افتتاحیه در حساب شخص ثبت می‌شود و روی سود یا صندوق اثری ندارد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = HesabyarTheme.colors.onSurfaceSecondary
                    )
                }
            }
            PrimaryButton(text = "ثبت مانده اولیه", onClick = viewModel::submit, loading = state.saving)
            Spacer(Modifier.height(16.dp))
        }
    }
}
