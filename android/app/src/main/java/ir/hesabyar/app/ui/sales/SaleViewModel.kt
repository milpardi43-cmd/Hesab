package ir.hesabyar.app.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.domain.accounting.CreateSaleCommand
import ir.hesabyar.domain.accounting.SaleItemInput
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.AccountType
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactFilter
import ir.hesabyar.domain.model.ContactType
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.PaymentMethod
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.model.ProductFilter
import ir.hesabyar.domain.model.SalePaymentMode
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.repository.ProductRepository
import ir.hesabyar.domain.repository.SettingsRepository
import ir.hesabyar.domain.usecase.CreateSaleUseCase
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.usecase.SaveContactUseCase
import ir.hesabyar.domain.usecase.SaveProductUseCase
import ir.hesabyar.domain.util.OperationIds
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SaleLineUi(
    val productId: Long,
    val name: String,
    val quantity: Double,
    val unitPrice: Long,
    val discount: Long,
    val availableStock: Double,
    val averageCost: Long
) {
    val gross: Long get() = (quantity * unitPrice).toLong()
    val total: Long get() = (gross - discount).coerceAtLeast(0)
}

data class SaleUiState(
    val loading: Boolean = true,
    val items: List<SaleLineUi> = emptyList(),
    val invoiceDiscount: Long? = null,
    val paymentMode: SalePaymentMode = SalePaymentMode.CASH,
    val paidAmount: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val accountId: Long? = null,
    val contactId: Long? = null,
    val date: Long = JalaliDate.startOfDayMillis(System.currentTimeMillis()),
    val description: String = "",
    val products: List<Product> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val saving: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null
) {
    val subtotal: Long get() = items.sumOf { it.total }
    val netTotal: Long get() = (subtotal - (invoiceDiscount ?: 0L)).coerceAtLeast(0)
    val paid: Long
        get() = when (paymentMode) {
            SalePaymentMode.CASH -> netTotal
            SalePaymentMode.CREDIT -> 0L
            SalePaymentMode.MIXED -> (paidAmount ?: 0L).coerceIn(0L, netTotal)
        }
    val remaining: Long get() = netTotal - paid
    val needsContact: Boolean get() = paymentMode != SalePaymentMode.CASH
}

@HiltViewModel
class SaleViewModel @Inject constructor(
    private val createSale: CreateSaleUseCase,
    private val getBusiness: GetBusinessUseCase,
    private val productRepository: ProductRepository,
    private val contactRepository: ContactRepository,
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository,
    private val saveContact: SaveContactUseCase,
    private val saveProduct: SaveProductUseCase
) : ViewModel() {

    private val operationId = OperationIds.new()

    private val _state = MutableStateFlow(SaleUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            productRepository.observeProducts(ProductFilter()).collect { products ->
                _state.update { it.copy(products = products, loading = false) }
            }
        }
        viewModelScope.launch {
            contactRepository.observeContacts(ContactFilter()).collect { contacts ->
                _state.update { it.copy(contacts = contacts) }
            }
        }
        viewModelScope.launch {
            accountRepository.observeAccounts().collect { accounts ->
                _state.update { current ->
                    val selected = current.accountId?.takeIf { id -> accounts.any { it.id == id } }
                        ?: accounts.firstOrNull { it.type == AccountType.CASH }?.id
                        ?: accounts.firstOrNull()?.id
                    current.copy(accounts = accounts, accountId = selected)
                }
            }
        }
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val preferred = settings.defaultReceiveAccountId
            if (preferred != null) {
                _state.update { current -> current.copy(accountId = current.accountId ?: preferred) }
            }
        }
    }

    fun addProduct(product: Product) {
        _state.update { current ->
            val existing = current.items.firstOrNull { it.productId == product.id }
            val items = if (existing != null) {
                current.items.map {
                    if (it.productId == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                current.items + SaleLineUi(
                    productId = product.id,
                    name = product.name,
                    quantity = 1.0,
                    unitPrice = product.sellingPrice,
                    discount = 0,
                    availableStock = product.stockQuantity,
                    averageCost = product.averageCost
                )
            }
            current.copy(items = items, error = null)
        }
    }

    fun updateQuantity(productId: Long, quantity: Double) = _state.update { current ->
        current.copy(items = current.items.map { if (it.productId == productId) it.copy(quantity = quantity) else it })
    }

    fun updatePrice(productId: Long, price: Long?) = _state.update { current ->
        current.copy(items = current.items.map { if (it.productId == productId) it.copy(unitPrice = price ?: 0L) else it })
    }

    fun updateDiscount(productId: Long, discount: Long?) = _state.update { current ->
        current.copy(items = current.items.map { if (it.productId == productId) it.copy(discount = discount ?: 0L) else it })
    }

    fun removeItem(productId: Long) = _state.update { current ->
        current.copy(items = current.items.filterNot { it.productId == productId })
    }

    fun setInvoiceDiscount(value: Long?) = _state.update { it.copy(invoiceDiscount = value) }
    fun setDescription(value: String) = _state.update { it.copy(description = value) }
    fun setDate(value: Long) = _state.update { it.copy(date = value) }
    fun setContact(contact: Contact) = _state.update { it.copy(contactId = contact.id, error = null) }
    fun setAccount(account: Account) = _state.update { it.copy(accountId = account.id) }
    fun setPaymentMethod(method: PaymentMethod) = _state.update { it.copy(paymentMethod = method) }

    fun setPaymentMode(mode: SalePaymentMode) = _state.update { current ->
        current.copy(
            paymentMode = mode,
            paidAmount = when (mode) {
                SalePaymentMode.CASH -> current.netTotal
                SalePaymentMode.CREDIT -> 0L
                SalePaymentMode.MIXED -> current.paidAmount ?: 0L
            }
        )
    }

    fun setPaidAmount(value: Long?) = _state.update { it.copy(paidAmount = value) }

    fun quickAddContact(name: String, phone: String) {
        viewModelScope.launch {
            val businessId = getBusiness.current()?.id ?: 0L
            when (
                val result = saveContact(
                    Contact(
                        businessId = businessId,
                        name = name,
                        phone = phone.takeIf { it.isNotBlank() },
                        type = ContactType.CUSTOMER
                    )
                )
            ) {
                is OperationResult.Success -> {
                    _state.update { it.copy(contactId = result.data) }
                    _events.send(UiEvent("مشتری جدید ثبت شد"))
                }
                is OperationResult.Failure -> _events.send(UiEvent(result.message, isError = true))
            }
        }
    }

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
                    addProduct(product.copy(id = result.data, averageCost = purchasePrice, stockQuantity = stock))
                    _events.send(UiEvent("کالای جدید ثبت و به فروش اضافه شد"))
                }
                is OperationResult.Failure -> _events.send(UiEvent(result.message, isError = true))
            }
        }
    }

    fun submit() {
        val current = _state.value
        if (current.saving) return
        val validation = validate(current)
        if (validation != null) {
            _state.update { it.copy(error = validation) }
            viewModelScope.launch { _events.send(UiEvent(validation, isError = true)) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val business = getBusiness.current()
            val command = CreateSaleCommand(
                businessId = business?.id ?: 0L,
                operationId = operationId,
                date = current.date,
                items = current.items.map { line ->
                    val product = current.products.firstOrNull { it.id == line.productId }
                    SaleItemInput(
                        productId = line.productId,
                        productName = line.name,
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        discount = line.discount,
                        averageCost = product?.averageCost ?: line.averageCost,
                        currentStock = product?.stockQuantity ?: line.availableStock
                    )
                },
                invoiceDiscount = current.invoiceDiscount ?: 0L,
                paymentMode = current.paymentMode,
                paidAmount = current.paid,
                paymentMethod = current.paymentMethod,
                accountId = current.accountId,
                contactId = current.contactId,
                description = current.description.takeIf { it.isNotBlank() },
                now = System.currentTimeMillis()
            )
            when (val result = createSale(command)) {
                is OperationResult.Success -> {
                    _state.update { it.copy(saving = false, savedId = result.data) }
                    _events.send(UiEvent("فروش با موفقیت ثبت شد"))
                }
                is OperationResult.Failure -> {
                    _state.update { it.copy(saving = false, error = result.message) }
                    _events.send(UiEvent(result.message, isError = true))
                }
            }
        }
    }

    private fun validate(state: SaleUiState): String? {
        if (state.items.isEmpty()) return "حداقل یک کالا به فروش اضافه کنید"
        if (state.items.any { it.quantity <= 0 }) return "تعداد هر کالا باید بیشتر از صفر باشد"
        if (state.items.any { it.unitPrice <= 0 }) return "قیمت فروش هر کالا را وارد کنید"
        if (state.items.any { it.discount > it.gross }) return "تخفیف هر ردیف بیشتر از مبلغ آن است"
        if ((state.invoiceDiscount ?: 0) > state.subtotal) return "تخفیف کل بیشتر از جمع کالاهاست"
        if (state.paymentMode == SalePaymentMode.CASH && state.accountId == null) return "حساب دریافت وجه را انتخاب کنید"
        if (state.paymentMode == SalePaymentMode.MIXED) {
            if (state.accountId == null) return "حساب دریافت وجه را انتخاب کنید"
            if (state.paid <= 0) return "مبلغ پرداختی نقدی را وارد کنید"
            if (state.paid >= state.netTotal) return "برای فروش کاملاً نقدی، نوع پرداخت را «نقدی» انتخاب کنید"
        }
        if (state.needsContact && state.contactId == null) return "برای فروش نسیه، مشتری را انتخاب کنید"
        return null
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
