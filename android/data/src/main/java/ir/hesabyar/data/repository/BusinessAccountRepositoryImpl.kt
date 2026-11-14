package ir.hesabyar.data.repository

import androidx.room.withTransaction
import ir.hesabyar.core.database.HesabyarDatabase
import ir.hesabyar.core.database.dao.AccountDao
import ir.hesabyar.core.database.dao.BusinessDao
import ir.hesabyar.core.database.dao.SubscriptionDao
import ir.hesabyar.core.database.entity.AccountEntity
import ir.hesabyar.core.database.entity.AccountMovementEntity
import ir.hesabyar.core.database.entity.SubscriptionEntity
import ir.hesabyar.data.mapper.toDomain
import ir.hesabyar.data.mapper.toEntity
import ir.hesabyar.data.posting.PostingExecutor
import ir.hesabyar.domain.accounting.AccountingEngine
import ir.hesabyar.domain.accounting.AdjustCashCommand
import ir.hesabyar.domain.accounting.TransferFundsCommand
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.AccountMovement
import ir.hesabyar.domain.model.AccountMovementType
import ir.hesabyar.domain.model.AccountReport
import ir.hesabyar.domain.model.AccountType
import ir.hesabyar.domain.model.Business
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.PlanType
import ir.hesabyar.domain.model.SubscriptionStatus
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.repository.BusinessRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BusinessRepositoryImpl @Inject constructor(
    private val db: HesabyarDatabase,
    private val businessDao: BusinessDao,
    private val accountDao: AccountDao,
    private val subscriptionDao: SubscriptionDao
) : BusinessRepository {

    override fun observeBusiness(): Flow<Business?> = businessDao.observeFirst().map { it?.toDomain() }

    override suspend fun getBusiness(): Business? = businessDao.first()?.toDomain()

    override suspend fun hasBusiness(): Boolean = businessDao.count() > 0

    override suspend fun createBusiness(business: Business, cashBalance: Long): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val businessId = businessDao.insert(
            business.copy(createdAt = if (business.createdAt == 0L) now else business.createdAt).toEntity()
        )
        val cashId = accountDao.insert(
            AccountEntity(
                businessId = businessId,
                title = "صندوق نقدی",
                accountType = AccountType.CASH.name,
                initialBalance = cashBalance,
                currentBalance = cashBalance,
                createdAt = now
            )
        )
        if (cashBalance > 0) {
            accountDao.insertMovement(
                AccountMovementEntity(
                    accountId = cashId,
                    transactionId = null,
                    type = AccountMovementType.OPENING.name,
                    delta = cashBalance,
                    balanceAfter = cashBalance,
                    note = "موجودی اولیه صندوق",
                    createdAt = now
                )
            )
        }
        accountDao.insert(
            AccountEntity(
                businessId = businessId,
                title = "حساب بانکی",
                accountType = AccountType.BANK.name,
                initialBalance = 0,
                currentBalance = 0,
                createdAt = now
            )
        )
        accountDao.insert(
            AccountEntity(
                businessId = businessId,
                title = "حساب کارتخوان",
                accountType = AccountType.POS.name,
                initialBalance = 0,
                currentBalance = 0,
                createdAt = now
            )
        )
        subscriptionDao.insert(
            SubscriptionEntity(
                businessId = businessId,
                planType = PlanType.FREE.name,
                startDate = now,
                endDate = null,
                status = SubscriptionStatus.ACTIVE.name,
                paymentReference = null
            )
        )
        businessId
    }

    override suspend fun updateBusiness(business: Business) {
        val existing = businessDao.first() ?: return
        businessDao.update(business.copy(id = existing.id).toEntity())
    }
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val db: HesabyarDatabase,
    private val accountDao: AccountDao,
    private val businessDao: BusinessDao,
    private val postingExecutor: PostingExecutor
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAccount(id: Long): Account? = accountDao.byId(id)?.toDomain()

    override suspend fun getAccounts(): List<Account> = accountDao.getAll().map { it.toDomain() }

    override suspend fun getFirstAccount(type: AccountType): Account? =
        accountDao.firstOfType(type.name)?.toDomain()

    override suspend fun createAccount(title: String, type: AccountType, initialBalance: Long): Long =
        db.withTransaction {
            val business = businessDao.first()?.id ?: error("اطلاعات فروشگاه ثبت نشده است")
            val now = System.currentTimeMillis()
            val id = accountDao.insert(
                AccountEntity(
                    businessId = business,
                    title = title,
                    accountType = type.name,
                    initialBalance = initialBalance,
                    currentBalance = initialBalance,
                    createdAt = now
                )
            )
            if (initialBalance > 0) {
                accountDao.insertMovement(
                    AccountMovementEntity(
                        accountId = id,
                        transactionId = null,
                        type = AccountMovementType.OPENING.name,
                        delta = initialBalance,
                        balanceAfter = initialBalance,
                        note = "موجودی اولیه حساب",
                        createdAt = now
                    )
                )
            }
            id
        }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateInfo(account.id, account.title, account.type.name)
    }

    override fun observeMovements(accountId: Long): Flow<List<AccountMovement>> =
        accountDao.observeMovements(accountId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAccountReport(id: Long): AccountReport? {
        val account = accountDao.byId(id) ?: return null
        return AccountReport(
            account = account.toDomain(),
            totalIn = accountDao.totalIn(id),
            totalOut = accountDao.totalOut(id),
            movements = accountDao.movements(id).map { it.toDomain() }
        )
    }

    override suspend fun adjustCash(cmd: AdjustCashCommand): OperationResult<Long> {
        val plan = AccountingEngine.cashAdjustment(cmd)
        return when (plan) {
            is OperationResult.Failure -> plan
            is OperationResult.Success -> postingExecutor.execute(plan.data)
        }
    }

    override suspend fun transfer(cmd: TransferFundsCommand): OperationResult<Long> {
        val plan = AccountingEngine.transfer(cmd)
        return when (plan) {
            is OperationResult.Failure -> plan
            is OperationResult.Success -> postingExecutor.execute(plan.data)
        }
    }
}
