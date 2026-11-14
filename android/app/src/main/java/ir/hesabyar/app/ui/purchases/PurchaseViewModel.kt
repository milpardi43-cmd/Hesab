package ir.hesabyar.app.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.domain.accounting.CreatePurchaseCommand
import ir.hesabyar.domain.accounting.PurchaseItemInput
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactFilter
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.PaymentMethod
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.model.ProductFilter
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.repository.ProductRepository
import ir.hesabyar.domain.repository.SettingsRepository
import ir.hesabyar.domain.usecase.CreatePurchaseUseCase
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.usecase.SaveProductUseCase
import ir.hesabyar.domain.util.OperationIds
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PurchaseLineUi(
    val productId: Long,
    val name: String,
    val quantity: Double,
    val unitPrice: Long,
    val currentStock: Double,
    val averageCost: Long
) {
    val total: Long get() = (quantity * unitPrice).toLong()
}

data class PurchaseUiState(
    val items: List<PurchaseLineUi> = emptyList(),
    val discount: Long? = null,
    val freight: Long? = null,
    val freightToCost: Boolean = true,
    val paidAmount: Long? = null,
    val method: PaymentMethod = PaymentMethod.CASH,
    val accountId: Long? = null,
    val supplierId: Long? = null,
    val date: Long = JalaliDate.startOfDayMillis(System.currentTimeMillis()),
    val description: String = "",
    val products: List<Product> = emptyList(),
    val suppliers: List<Contact> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val saving: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null
) {
    val gross: Long get() = items.sumOf { it.total }
    val net: Long get() = (gross - (discount ?: 0L)).coerceAtLeast(0)
    val paid: Long get() = (paidAmount ?: 0L).coerceIn(0L, net)
    val remaining: Long get() = net - paid
}

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val createPurchase: CreatePurchaseUseCase,
    private val getBusiness: GetBusinessUseCase,
    private val products: ProductRepository,
    private val contacts: ContactRepository,
    private val accounts: AccountRepository,
    private val settings: SettingsRepository,
    private val saveProduct: SaveProductUseCase
) : ViewModel() {

    private val operationId = OperationIds.new()
    private val _state = MutableStateFlow(PurchaseUiState())
    val state = _state.asStateFlow()
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            products.observeProducts(ProductFilter()).collect { list ->
                _state.update { it.copy(products = list) }
            }
        }
        viewModelScope.launch {
            contacts.observeContacts(ContactFilter()).collect { list ->
                _state.update { it.copy(suppliers = list.filter { c -> c.type.isSupplier }) }
            }
        }
        viewModelScope.launch {
            accounts.observeAccounts().collect { list ->
                _state.update { current ->
                    val selected = current.accountId?.takeIf { id -> list.any { it.id == id } } ?: list.firstOrNull()?.id
                    current.copy(accounts = list, accountId = selected)
                }
            }
        }
        viewModelScope.launch {
            val preferred = settings.current().defaultPayAccountId
            if (preferred != null) _state.update { it.copy(accountId = it.accountId ?: preferred) }
        }
    }

    fun addProduct(product: Product) = _state.update { current ->
        val existing = current.items.firstOrNull { it.productId == product.id }
        val items = if (existing != null) {
            current.items.map { if (it.productId == product.id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            current.items + PurchaseLineUi(
                productId = product.id,
                name = product.name,
                quantity = 1.0,
                unitPrice = if (product.purchasePrice > 0) product.purchasePrice else product.averageCost,
                currentStock = product.stockQuantity,
                averageCost = product.averageCost
            )
        }
        current.copy(items = items, error = null)
    }

    fun updateQuantity(productId: Long, value: Double) = _state.update { current ->
        current.copy(items = current.items.map { if (it.productId == productId) it.copy(quantity = value) else it })
    }

    fun updatePrice(productId: Long, value: Long?) = _state.update { current ->
        current.copy(items = current.items.map { if (it.productId == productId) it.copy(unitPrice = value ?: 0L) else it })
    }

    fun removeItem(productId: Long) = _state.update { current ->
        current.copy(items = current.items.filterNot { it.productId == productId })
    }

    fun setDiscount(value: Long?) = _state.update { it.copy(discount = value) }
    fun setFreight(value: Long?) = _state.update { it.copy(freight = value) }
    fun setFreightToCost(value: Boolean) = _state.update { it.copy(freightToCost = value) }
    fun setPaid(value: Long?) = _state.update { it.copy(paidAmount = value) }
    fun setMethod(value: PaymentMethod) = _state.update { it.copy(method = value) }
    fun setAccount(value: Account) = _state.update { it.copy(accountId = value.id) }
    fun setSupplier(value: Contact) = _state.update { it.copy(supplierId = value.id) }
    fun setDate(value: Long) = _state.update { it.copy(date = value) }
    fun setDescription(value: String) = _state.update { it.copy(description = value) }

    fun quickAddProduct(name: String, sellingPrice: Long, purchasePrice: Long, stock: Double) {
        viewModelScope.launch {
            val businessId = getBusiness.current()?.id ?: 0L
            val product = Product(
                businessId = businessId,
                name = name,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                averageCost = purchasePrice,
                stockQuantity = stock
            )
            when (val result = saveProduct(product)) {
                is OperationResult.Success -> {
                    addProduct(product.copy(id = result.data))
                    _events.send(UiEvent("کالای جدید ثبت شد"))
                }
                is OperationResult.Failure -> _events.send(UiEvent(result.message, isError = true))
            }
        }
    }

    fun submit() {
        val current = _state.value
        if (current.saving) return
        val error = when {
            current.items.isEmpty() -> "حداقل یک کالا به خرید اضافه کنید"
            current.items.any { it.quantity <= 0 } -> "تعداد هر کالا باید بیشتر از صفر باشد"
            current.items.any { it.unitPrice <= 0 } -> "قیمت خرید هر کالا را وارد کنید"
            (current.discount ?: 0) > current.gross -> "تخفیف بیشتر از جمع کالاهاست"
            current.accountId == null -> "حساب پرداخت را انتخاب کنید"
            current.paid > current.net -> "مبلغ پرداختی بیشتر از مبلغ خرید است"
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
            val command = CreatePurchaseCommand(
                businessId = business?.id ?: 0L,
                operationId = operationId,
                date = current.date,
                items = current.items.map { line ->
                    val product = current.products.firstOrNull { it.id == line.productId }
                    PurchaseItemInput(
                        productId = line.productId,
                        productName = line.name,
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        currentStock = product?.stockQuantity ?: line.currentStock,
                        averageCost = product?.averageCost ?: line.averageCost
                    )
                },
                discountAmount = current.discount ?: 0L,
                freightAmount = current.freight ?: 0L,
                freightToCost = current.freightToCost,
                paidAmount = current.paid,
                paymentMethod = current.method,
                accountId = current.accountId,
                supplierId = current.supplierId,
                description = current.description.takeIf { it.isNotBlank() },
                now = System.currentTimeMillis()
            )
            when (val result = createPurchase(command)) {
                is OperationResult.Success -> {
                    _state.update { it.copy(saving = false, savedId = result.data) }
                    _events.send(UiEvent("خرید با موفقیت ثبت شد"))
                }
                is OperationResult.Failure -> {
                    _state.update { it.copy(saving = false, error = result.message) }
                    _events.send(UiEvent(result.message, isError = true))
                }
            }
        }
    }
}
