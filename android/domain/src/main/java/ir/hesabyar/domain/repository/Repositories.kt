package ir.hesabyar.domain.repository

import ir.hesabyar.domain.accounting.AdjustCashCommand
import ir.hesabyar.domain.accounting.AdjustStockCommand
import ir.hesabyar.domain.accounting.CreateExpenseCommand
import ir.hesabyar.domain.accounting.CreatePurchaseCommand
import ir.hesabyar.domain.accounting.CreateSaleCommand
import ir.hesabyar.domain.accounting.PaySupplierCommand
import ir.hesabyar.domain.accounting.ReceivePaymentCommand
import ir.hesabyar.domain.accounting.ReturnSaleCommand
import ir.hesabyar.domain.accounting.TransferFundsCommand
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.AccountMovement
import ir.hesabyar.domain.model.AccountReport
import ir.hesabyar.domain.model.AccountType
import ir.hesabyar.domain.model.AppSettings
import ir.hesabyar.domain.model.Business
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactFilter
import ir.hesabyar.domain.model.ContactLedgerEntry
import ir.hesabyar.domain.model.CustomerReportRow
import ir.hesabyar.domain.model.DailyReport
import ir.hesabyar.domain.model.DashboardData
import ir.hesabyar.domain.model.MonthlyReport
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.PlanType
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.model.ProductCategory
import ir.hesabyar.domain.model.ProductFilter
import ir.hesabyar.domain.model.ProductReportRow
import ir.hesabyar.domain.model.Reminder
import ir.hesabyar.domain.model.ReminderStatus
import ir.hesabyar.domain.model.SalesTrend
import ir.hesabyar.domain.model.StockMovement
import ir.hesabyar.domain.model.Subscription
import ir.hesabyar.domain.model.SupplierReportRow
import ir.hesabyar.domain.model.Transaction
import ir.hesabyar.domain.model.TransactionDetail
import ir.hesabyar.domain.model.TransactionFilter
import kotlinx.coroutines.flow.Flow

interface BusinessRepository {
    fun observeBusiness(): Flow<Business?>
    suspend fun getBusiness(): Business?
    suspend fun hasBusiness(): Boolean
    suspend fun createBusiness(business: Business, cashBalance: Long): Long
    suspend fun updateBusiness(business: Business)
}

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    suspend fun getAccount(id: Long): Account?
    suspend fun getAccounts(): List<Account>
    suspend fun getFirstAccount(type: AccountType): Account?
    suspend fun createAccount(title: String, type: AccountType, initialBalance: Long): Long
    suspend fun updateAccount(account: Account)
    fun observeMovements(accountId: Long): Flow<List<AccountMovement>>
    suspend fun getAccountReport(id: Long): AccountReport?
    suspend fun adjustCash(cmd: AdjustCashCommand): OperationResult<Long>
    suspend fun transfer(cmd: TransferFundsCommand): OperationResult<Long>
}

interface ContactRepository {
    fun observeContacts(filter: ContactFilter): Flow<List<Contact>>
    fun observeContact(id: Long): Flow<Contact?>
    suspend fun getContact(id: Long): Contact?
    suspend fun getContacts(): List<Contact>
    suspend fun createContact(contact: Contact): Long
    suspend fun updateContact(contact: Contact)
    suspend fun addOpeningBalance(contactId: Long, amount: Long, note: String?): OperationResult<Unit>
    fun observeLedger(contactId: Long): Flow<List<ContactLedgerEntry>>
    suspend fun getTotals(contactId: Long): ContactTotals
    suspend fun receive(cmd: ReceivePaymentCommand): OperationResult<Long>
    suspend fun pay(cmd: PaySupplierCommand): OperationResult<Long>
    suspend fun countContacts(): Int
}

data class ContactTotals(
    val totalSales: Long = 0,
    val totalReceipts: Long = 0,
    val totalPurchases: Long = 0,
    val totalPayments: Long = 0,
    val lastActivity: Long? = null
)

interface ProductRepository {
    fun observeProducts(filter: ProductFilter): Flow<List<Product>>
    fun observeProduct(id: Long): Flow<Product?>
    suspend fun getProduct(id: Long): Product?
    suspend fun getProducts(): List<Product>
    suspend fun saveProduct(product: Product): Long
    fun observeCategories(): Flow<List<ProductCategory>>
    suspend fun getCategories(): List<ProductCategory>
    suspend fun createCategory(name: String): Long
    fun observeMovements(productId: Long): Flow<List<StockMovement>>
    suspend fun adjustStock(cmd: AdjustStockCommand): OperationResult<Long>
    suspend fun countProducts(): Int
    fun observeLowStockCount(): Flow<Int>
}

interface TransactionRepository {
    fun observeTransactions(filter: TransactionFilter): Flow<List<Transaction>>
    fun observeRecent(limit: Int): Flow<List<Transaction>>
    suspend fun getDetail(id: Long): TransactionDetail?
    suspend fun reverse(transactionId: Long, reason: String?): OperationResult<Long>
    suspend fun countTransactions(): Int
}

interface SaleRepository {
    suspend fun create(cmd: CreateSaleCommand): OperationResult<Long>
    suspend fun returnSale(cmd: ReturnSaleCommand): OperationResult<Long>
}

interface PurchaseRepository {
    suspend fun create(cmd: CreatePurchaseCommand): OperationResult<Long>
}

interface ExpenseRepository {
    suspend fun create(cmd: CreateExpenseCommand): OperationResult<Long>
    fun observeRecentExpenses(limit: Int): Flow<List<Transaction>>
}

interface DashboardRepository {
    fun observeDashboard(): Flow<DashboardData>
}

interface ReportRepository {
    suspend fun daily(date: Long): DailyReport
    suspend fun monthly(year: Int, month: Int): MonthlyReport
    suspend fun customers(): List<CustomerReportRow>
    suspend fun suppliers(): List<SupplierReportRow>
    suspend fun products(): List<ProductReportRow>
    suspend fun salesTrend(days: Int): SalesTrend
}

interface ReminderRepository {
    fun observeAll(): Flow<List<Reminder>>
    suspend fun save(reminder: Reminder): Long
    suspend fun setStatus(id: Long, status: ReminderStatus)
    suspend fun pendingDue(now: Long): List<Reminder>
    suspend fun countPending(): Int
}

data class BackupPayloadInfo(val sizeBytes: Long, val createdAt: Long)

interface BackupRepository {
    suspend fun exportJson(): String
    suspend fun importJson(json: String): Result<Unit>
    suspend fun exportCsvZip(): ByteArray
    suspend fun exportXlsx(): ByteArray
}

interface SubscriptionRepository {
    fun observeSubscription(): Flow<Subscription>
    suspend fun currentPlan(): PlanType
    suspend fun save(subscription: Subscription)
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
