package ir.hesabyar.data.repository

import ir.hesabyar.core.database.dao.AccountDao
import ir.hesabyar.core.database.dao.ContactDao
import ir.hesabyar.core.database.dao.ProductDao
import ir.hesabyar.core.database.dao.PurchaseDao
import ir.hesabyar.core.database.dao.SaleDao
import ir.hesabyar.core.database.dao.ExpenseDao
import ir.hesabyar.core.database.dao.TransactionDao
import ir.hesabyar.data.mapper.toDomain
import ir.hesabyar.data.posting.PostingExecutor
import ir.hesabyar.domain.accounting.AccountingEngine
import ir.hesabyar.domain.accounting.ReverseTransactionCommand
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.Transaction
import ir.hesabyar.domain.model.TransactionDetail
import ir.hesabyar.domain.model.TransactionFilter
import ir.hesabyar.domain.repository.TransactionRepository
import ir.hesabyar.domain.util.OperationIds
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val saleDao: SaleDao,
    private val purchaseDao: PurchaseDao,
    private val expenseDao: ExpenseDao,
    private val contactDao: ContactDao,
    private val accountDao: AccountDao,
    private val productDao: ProductDao,
    private val postingExecutor: PostingExecutor
) : TransactionRepository {

    override fun observeTransactions(filter: TransactionFilter): Flow<List<Transaction>> =
        transactionDao.observeAll().map { rows ->
            var list = rows.map { it.toDomain() }
            val query = filter.query.trim()
            if (query.isNotEmpty()) {
                list = list.filter {
                    (it.description?.contains(query, ignoreCase = true) == true) ||
                        it.code.contains(query, ignoreCase = true) ||
                        (it.contactName?.contains(query, ignoreCase = true) == true) ||
                        it.type.label.contains(query, ignoreCase = true)
                }
            }
            filter.from?.let { from -> list = list.filter { it.date >= from } }
            filter.to?.let { to -> list = list.filter { it.date <= to } }
            filter.type?.let { type -> list = list.filter { it.type == type } }
            filter.contactId?.let { contactId -> list = list.filter { it.contactId == contactId } }
            filter.paymentStatus?.let { status -> list = list.filter { it.paymentStatus == status } }
            if (!filter.newestFirst) list = list.reversed()
            list
        }

    override fun observeRecent(limit: Int): Flow<List<Transaction>> =
        transactionDao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getDetail(id: Long): TransactionDetail? {
        val entity = transactionDao.byId(id) ?: return null
        val sale = saleDao.saleByTransaction(id)?.toDomain()
        val saleItems = sale?.let { saleDao.items(it.id).map { item -> item.toDomain() } }.orEmpty()
        val purchase = purchaseDao.byTransaction(id)?.toDomain()
        val purchaseItems = purchase?.let { purchaseDao.items(it.id).map { item -> item.toDomain() } }.orEmpty()
        val expense = expenseDao.byTransaction(id)?.toDomain()
        val saleReturn = saleDao.returnByTransaction(id)?.toDomain()
        val returnItems = saleReturn?.let { saleDao.returnItems(it.id).map { item -> item.toDomain() } }.orEmpty()
        val contact = entity.contactId?.let { contactDao.byId(it)?.toDomain() }
        val account = entity.accountId?.let { accountDao.byId(it)?.toDomain() }
        val referenceCode = entity.referenceId?.let { transactionDao.byId(it)?.code }
        val reversedByCode = transactionDao.reversalOf(id)?.code
        return TransactionDetail(
            transaction = entity.toDomain(contact?.name, account?.title),
            sale = sale,
            saleItems = saleItems,
            purchase = purchase,
            purchaseItems = purchaseItems,
            expense = expense,
            saleReturn = saleReturn,
            returnItems = returnItems,
            contact = contact,
            account = account,
            accountMovements = accountDao.movementsForTransaction(id).map { it.toDomain() },
            ledgerEntries = contactDao.ledgerForTransaction(id).map { it.toDomain() },
            stockMovements = productDao.movementsForTransaction(id).map { it.toDomain() },
            referenceCode = referenceCode,
            reversedByCode = reversedByCode
        )
    }

    override suspend fun reverse(transactionId: Long, reason: String?): OperationResult<Long> {
        val detail = getDetail(transactionId)
            ?: return OperationResult.Failure("تراکنش یافت نشد")
        if (!detail.isReversible) {
            return OperationResult.Failure(
                if (detail.hasReturns) "برای این فروش برگشت ثبت شده است؛ ابتدا برگشت‌ها را اصلاح کنید"
                else "این تراکنش قابل برگشت نیست"
            )
        }
        val productIds = detail.stockMovements.map { it.productId }.distinct()
        val stocks = mutableMapOf<Long, Double>()
        val costs = mutableMapOf<Long, Long>()
        productIds.forEach { productId ->
            productDao.byId(productId)?.let { product ->
                stocks[productId] = product.stockQuantity
                costs[productId] = product.averageCost
            }
        }
        val plan = AccountingEngine.reversal(
            ReverseTransactionCommand(
                businessId = detail.transaction.businessId,
                operationId = OperationIds.new(),
                date = JalaliDate.startOfDayMillis(System.currentTimeMillis()),
                detail = detail,
                currentStocks = stocks,
                currentAverageCosts = costs,
                reason = reason,
                now = System.currentTimeMillis()
            )
        )
        return when (plan) {
            is OperationResult.Failure -> plan
            is OperationResult.Success -> postingExecutor.execute(plan.data)
        }
    }

    override suspend fun countTransactions(): Int = transactionDao.count()
}
