package ir.hesabyar.app.ui.sales

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.domain.accounting.ReturnItemInput
import ir.hesabyar.domain.accounting.ReturnSaleCommand
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.Sale
import ir.hesabyar.domain.model.Transaction
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.repository.ProductRepository
import ir.hesabyar.domain.repository.SettingsRepository
import ir.hesabyar.domain.usecase.GetTransactionDetailUseCase
import ir.hesabyar.domain.usecase.ReturnSaleUseCase
import ir.hesabyar.domain.util.OperationIds
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReturnLineUi(
    val saleItemId: Long,
    val productId: Long,
    val name: String,
    val soldQuantity: Double,
    val returnedQuantity: Double,
    val unitPrice: Long,
    val unitCost: Long,
    val lineDiscount: Long,
    val currentStock: Double,
    val currentAverageCost: Long,
    val quantity: Double = 0.0
) {
    val remaining: Double get() = (soldQuantity - returnedQuantity).coerceAtLeast(0.0)
    val amount: Long
        get() {
            if (quantity <= 0 || soldQuantity <= 0) return 0
            val share = quantity / soldQuantity
            return (quantity * unitPrice - lineDiscount * share).toLong().coerceAtLeast(0)
        }
}

data class ReturnUiState(
    val loading: Boolean = true,
    val transaction: Transaction? = null,
    val sale: Sale? = null,
    val lines: List<ReturnLineUi> = emptyList(),
    val reason: String = "",
    val sellable: Boolean = true,
    val refundCash: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val accountId: Long? = null,
    val saving: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null
) {
    val total: Long get() = lines.sumOf { it.amount }
}

@HiltViewModel
class ReturnViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDetail: GetTransactionDetailUseCase,
    private val returnSale: ReturnSaleUseCase,
    private val products: ProductRepository,
    accounts: AccountRepository,
    settings: SettingsRepository
) : ViewModel() {

    private val transactionId: Long = savedStateHandle.get<Long>("transactionId") ?: 0L
    private val operationId = OperationIds.new()

    private val _state = MutableStateFlow(ReturnUiState())
    val state = _state.asStateFlow()
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
        viewModelScope.launch {
            accounts.observeAccounts().collect { list ->
                _state.update { current ->
                    val selected = current.accountId?.takeIf { id -> list.any { it.id == id } } ?: list.firstOrNull()?.id
                    current.copy(accounts = list, accountId = selected)
                }
            }
        }
        viewModelScope.launch {
            val preferred = settings.current().defaultReceiveAccountId
            if (preferred != null) _state.update { it.copy(accountId = it.accountId ?: preferred) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val detail = getDetail(transactionId)
            val sale = detail?.sale
            if (detail == null || sale == null) {
                _state.update { it.copy(loading = false, error = "این تراکنش فروش نیست یا پیدا نشد") }
                return@launch
            }
            val lines = detail.saleItems.map { item ->
                val liveProduct = products.getProduct(item.productId)
                ReturnLineUi(
                    saleItemId = item.id,
                    productId = item.productId,
                    name = item.productName,
                    soldQuantity = item.quantity,
                    returnedQuantity = item.returnedQuantity,
                    unitPrice = item.unitPrice,
                    unitCost = item.unitCostSnapshot,
                    lineDiscount = item.discountAmount,
                    currentStock = liveProduct?.stockQuantity ?: 0.0,
                    currentAverageCost = liveProduct?.averageCost ?: item.unitCostSnapshot
                )
            }
            _state.update {
                it.copy(
                    loading = false,
                    transaction = detail.transaction,
                    sale = detail.sale,
                    lines = lines,
                    refundCash = sale.remainingAmount == 0L
                )
            }
        }
    }

    fun setQuantity(saleItemId: Long, quantity: Double) = _state.update { current ->
        current.copy(
            lines = current.lines.map { line ->
                if (line.saleItemId == saleItemId) line.copy(quantity = quantity.coerceIn(0.0, line.remaining)) else line
            }
        )
    }

    fun setReason(value: String) = _state.update { it.copy(reason = value) }
    fun setSellable(value: Boolean) = _state.update { it.copy(sellable = value) }
    fun setRefundCash(value: Boolean) = _state.update { it.copy(refundCash = value) }
    fun setAccount(account: Account) = _state.update { it.copy(accountId = account.id) }

    fun submit() {
        val current = _state.value
        val sale = current.sale ?: return
        val transaction = current.transaction ?: return
        if (current.saving) return
        val selected = current.lines.filter { it.quantity > 0 }
        val error = when {
            selected.isEmpty() -> "تعداد برگشتی حداقل یک کالا را وارد کنید"
            current.reason.isBlank() -> "دلیل برگشت را وارد کنید"
            current.refundCash && current.accountId == null -> "حساب بازپرداخت را انتخاب کنید"
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            viewModelScope.launch { _events.send(UiEvent(error, isError = true)) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val command = ReturnSaleCommand(
                businessId = transaction.businessId,
                operationId = operationId,
                date = ir.hesabyar.domain.calendar.JalaliDate.startOfDayMillis(System.currentTimeMillis()),
                saleTransactionId = transaction.id,
                saleId = sale.id,
                saleRemaining = sale.remainingAmount,
                salePaid = sale.paidAmount,
                items = selected.map { line ->
                    ReturnItemInput(
                        saleItemId = line.saleItemId,
                        productId = line.productId,
                        productName = line.name,
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        unitCost = line.unitCost,
                        lineDiscount = line.lineDiscount,
                        soldQuantity = line.soldQuantity,
                        alreadyReturned = line.returnedQuantity,
                        currentStock = line.currentStock,
                        currentAverageCost = line.currentAverageCost
                    )
                },
                refundCash = current.refundCash,
                sellable = current.sellable,
                reason = current.reason.trim(),
                accountId = if (current.refundCash) current.accountId else null,
                contactId = sale.contactId,
                now = System.currentTimeMillis()
            )
            when (val result = returnSale(command)) {
                is OperationResult.Success -> {
                    _state.update { it.copy(saving = false, savedId = result.data) }
                    _events.send(UiEvent("برگشت از فروش ثبت شد"))
                }
                is OperationResult.Failure -> {
                    _state.update { it.copy(saving = false, error = result.message) }
                    _events.send(UiEvent(result.message, isError = true))
                }
            }
        }
    }
}
