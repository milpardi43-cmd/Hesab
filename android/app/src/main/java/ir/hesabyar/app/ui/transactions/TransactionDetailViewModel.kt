package ir.hesabyar.app.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.TransactionDetail
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.usecase.GetTransactionDetailUseCase
import ir.hesabyar.domain.usecase.ReverseTransactionUseCase
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionDetailUiState(
    val loading: Boolean = true,
    val detail: TransactionDetail? = null,
    val businessName: String = "",
    val reversing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDetail: GetTransactionDetailUseCase,
    private val reverseTransaction: ReverseTransactionUseCase,
    private val getBusiness: GetBusinessUseCase
) : ViewModel() {

    private val transactionId: Long = savedStateHandle.get<Long>("transactionId") ?: 0L

    private val _state = MutableStateFlow(TransactionDetailUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val detail = getDetail(transactionId)
            val business = getBusiness.current()
            _state.update {
                it.copy(
                    loading = false,
                    detail = detail,
                    businessName = business?.name.orEmpty(),
                    error = if (detail == null) "تراکنش پیدا نشد" else null
                )
            }
        }
    }

    fun reverse(reason: String?) {
        if (_state.value.reversing) return
        viewModelScope.launch {
            _state.update { it.copy(reversing = true) }
            when (val result = reverseTransaction(transactionId, reason?.takeIf { it.isNotBlank() })) {
                is OperationResult.Success -> {
                    _state.update { it.copy(reversing = false) }
                    _events.send(UiEvent("تراکنش با موفقیت برگشت داده شد"))
                    load()
                }
                is OperationResult.Failure -> {
                    _state.update { it.copy(reversing = false) }
                    _events.send(UiEvent(result.message, isError = true))
                }
            }
        }
    }
}
