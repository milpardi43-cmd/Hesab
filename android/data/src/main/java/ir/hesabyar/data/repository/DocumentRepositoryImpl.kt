package ir.hesabyar.data.repository

import ir.hesabyar.core.database.dao.TransactionDao
import ir.hesabyar.core.database.dao.ExpenseDao
import ir.hesabyar.data.mapper.toDomain
import ir.hesabyar.data.posting.PostingExecutor
import ir.hesabyar.domain.accounting.AccountingEngine
import ir.hesabyar.domain.accounting.CreateExpenseCommand
import ir.hesabyar.domain.accounting.CreatePurchaseCommand
import ir.hesabyar.domain.accounting.CreateSaleCommand
import ir.hesabyar.domain.accounting.ReturnSaleCommand
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.Transaction
import ir.hesabyar.domain.repository.ExpenseRepository
import ir.hesabyar.domain.repository.PurchaseRepository
import ir.hesabyar.domain.repository.SaleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SaleRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val postingExecutor: PostingExecutor
) : SaleRepository {

    override suspend fun create(cmd: CreateSaleCommand): OperationResult<Long> {
        transactionDao.byOperationId(cmd.operationId)?.let { return OperationResult.Success(it.id) }
        val plan = AccountingEngine.sale(cmd)
        return when (plan) {
            is OperationResult.Failure -> plan
            is OperationResult.Success -> postingExecutor.execute(plan.data)
        }
    }

    override suspend fun returnSale(cmd: ReturnSaleCommand): OperationResult<Long> {
        transactionDao.byOperationId(cmd.operationId)?.let { return OperationResult.Success(it.id) }
        val plan = AccountingEngine.saleReturn(cmd)
        return when (plan) {
            is OperationResult.Failure -> plan
            is OperationResult.Success -> postingExecutor.execute(plan.data)
        }
    }
}

@Singleton
class PurchaseRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val postingExecutor: PostingExecutor
) : PurchaseRepository {

    override suspend fun create(cmd: CreatePurchaseCommand): OperationResult<Long> {
        transactionDao.byOperationId(cmd.operationId)?.let { return OperationResult.Success(it.id) }
        val plan = AccountingEngine.purchase(cmd)
        return when (plan) {
            is OperationResult.Failure -> plan
            is OperationResult.Success -> postingExecutor.execute(plan.data)
        }
    }
}

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val expenseDao: ExpenseDao,
    private val postingExecutor: PostingExecutor
) : ExpenseRepository {

    override suspend fun create(cmd: CreateExpenseCommand): OperationResult<Long> {
        transactionDao.byOperationId(cmd.operationId)?.let { return OperationResult.Success(it.id) }
        val plan = AccountingEngine.expense(cmd)
        return when (plan) {
            is OperationResult.Failure -> plan
            is OperationResult.Success -> postingExecutor.execute(plan.data)
        }
    }

    override fun observeRecentExpenses(limit: Int): Flow<List<Transaction>> =
        expenseDao.observeRecentExpenses(limit).map { rows -> rows.map { it.toDomain() } }
}
