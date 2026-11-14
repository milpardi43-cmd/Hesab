package ir.hesabyar.domain.usecase

import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.AccountMovement
import ir.hesabyar.domain.model.AccountReport
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactFilter
import ir.hesabyar.domain.model.ContactLedgerEntry
import ir.hesabyar.domain.model.CustomerReportRow
import ir.hesabyar.domain.model.DailyReport
import ir.hesabyar.domain.model.DashboardData
import ir.hesabyar.domain.model.MonthlyReport
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.model.ProductCategory
import ir.hesabyar.domain.model.ProductFilter
import ir.hesabyar.domain.model.ProductReportRow
import ir.hesabyar.domain.model.SalesTrend
import ir.hesabyar.domain.model.StockMovement
import ir.hesabyar.domain.model.SupplierReportRow
import ir.hesabyar.domain.model.Transaction
import ir.hesabyar.domain.model.TransactionDetail
import ir.hesabyar.domain.model.TransactionFilter
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.repository.ContactTotals
import ir.hesabyar.domain.repository.DashboardRepository
import ir.hesabyar.domain.repository.ProductRepository
import ir.hesabyar.domain.repository.ReportRepository
import ir.hesabyar.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDashboardUseCase @Inject constructor(private val repository: DashboardRepository) {
    operator fun invoke(): Flow<DashboardData> = repository.observeDashboard()
}

class GetTransactionsUseCase @Inject constructor(private val repository: TransactionRepository) {
    operator fun invoke(filter: TransactionFilter): Flow<List<Transaction>> =
        repository.observeTransactions(filter)
}

class GetRecentTransactionsUseCase @Inject constructor(private val repository: TransactionRepository) {
    operator fun invoke(limit: Int = 10): Flow<List<Transaction>> = repository.observeRecent(limit)
}

class GetTransactionDetailUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: Long): TransactionDetail? = repository.getDetail(id)
}

class GetContactsUseCase @Inject constructor(private val repository: ContactRepository) {
    operator fun invoke(filter: ContactFilter): Flow<List<Contact>> = repository.observeContacts(filter)
}

class GetContactUseCase @Inject constructor(private val repository: ContactRepository) {
    operator fun invoke(id: Long): Flow<Contact?> = repository.observeContact(id)
}

class GetContactLedgerUseCase @Inject constructor(private val repository: ContactRepository) {
    operator fun invoke(id: Long): Flow<List<ContactLedgerEntry>> = repository.observeLedger(id)
}

class GetContactTotalsUseCase @Inject constructor(private val repository: ContactRepository) {
    suspend operator fun invoke(id: Long): ContactTotals = repository.getTotals(id)
}

class GetProductsUseCase @Inject constructor(private val repository: ProductRepository) {
    operator fun invoke(filter: ProductFilter): Flow<List<Product>> = repository.observeProducts(filter)
}

class GetProductUseCase @Inject constructor(private val repository: ProductRepository) {
    suspend operator fun invoke(id: Long): Product? = repository.getProduct(id)
}

class ObserveProductUseCase @Inject constructor(private val repository: ProductRepository) {
    operator fun invoke(id: Long): Flow<Product?> = repository.observeProduct(id)
}

class GetCategoriesUseCase @Inject constructor(private val repository: ProductRepository) {
    operator fun invoke(): Flow<List<ProductCategory>> = repository.observeCategories()
}

class GetStockMovementsUseCase @Inject constructor(private val repository: ProductRepository) {
    operator fun invoke(productId: Long): Flow<List<StockMovement>> = repository.observeMovements(productId)
}

class GetLowStockCountUseCase @Inject constructor(private val repository: ProductRepository) {
    operator fun invoke(): Flow<Int> = repository.observeLowStockCount()
}

class GetAccountsUseCase @Inject constructor(private val repository: AccountRepository) {
    operator fun invoke(): Flow<List<Account>> = repository.observeAccounts()
}

class GetAccountMovementsUseCase @Inject constructor(private val repository: AccountRepository) {
    operator fun invoke(accountId: Long): Flow<List<AccountMovement>> = repository.observeMovements(accountId)
}

class GetAccountReportUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(accountId: Long): AccountReport? = repository.getAccountReport(accountId)
}

class GetDailyReportUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(date: Long): DailyReport = repository.daily(date)
}

class GetMonthlyReportUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(year: Int, month: Int): MonthlyReport = repository.monthly(year, month)
}

class GetCustomerReportUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(): List<CustomerReportRow> = repository.customers()
}

class GetSupplierReportUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(): List<SupplierReportRow> = repository.suppliers()
}

class GetProductReportUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(): List<ProductReportRow> = repository.products()
}

class GetSalesTrendUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(days: Int): SalesTrend = repository.salesTrend(days)
}
