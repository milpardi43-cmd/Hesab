package ir.hesabyar.data.posting

import androidx.room.withTransaction
import ir.hesabyar.core.database.HesabyarDatabase
import ir.hesabyar.core.database.dao.AccountDao
import ir.hesabyar.core.database.dao.BusinessDao
import ir.hesabyar.core.database.dao.ContactDao
import ir.hesabyar.core.database.dao.ExpenseDao
import ir.hesabyar.core.database.dao.ProductDao
import ir.hesabyar.core.database.dao.PurchaseDao
import ir.hesabyar.core.database.dao.SaleDao
import ir.hesabyar.core.database.dao.TransactionDao
import ir.hesabyar.core.database.entity.AccountMovementEntity
import ir.hesabyar.core.database.entity.ContactLedgerEntity
import ir.hesabyar.core.database.entity.ExpenseEntity
import ir.hesabyar.core.database.entity.PurchaseEntity
import ir.hesabyar.core.database.entity.PurchaseItemEntity
import ir.hesabyar.core.database.entity.SaleEntity
import ir.hesabyar.core.database.entity.SaleItemEntity
import ir.hesabyar.core.database.entity.SaleReturnEntity
import ir.hesabyar.core.database.entity.SaleReturnItemEntity
import ir.hesabyar.core.database.entity.StockMovementEntity
import ir.hesabyar.core.database.entity.TransactionEntity
import ir.hesabyar.domain.accounting.PostingPlan
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.TransactionStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists a PostingPlan atomically.
 *
 * All balance updates, ledger entries, stock movements and document rows happen inside a single
 * Room transaction: if any insert fails, SQLite rolls back the whole operation. A unique index on
 * transactions.operation_id guarantees idempotency even if a submit is retried.
 */
@Singleton
class PostingExecutor @Inject constructor(
    private val db: HesabyarDatabase,
    private val businessDao: BusinessDao,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val contactDao: ContactDao,
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val purchaseDao: PurchaseDao,
    private val expenseDao: ExpenseDao
) {

    suspend fun execute(plan: PostingPlan): OperationResult<Long> {
        return try {
            val id = db.withTransaction {
                val existing = transactionDao.byOperationId(plan.transaction.operationId)
                if (existing != null) return@withTransaction existing.id
                val business = businessDao.first()
                    ?: throw IllegalStateException("اطلاعات فروشگاه ثبت نشده است")
                val code = nextCode(plan.transaction.date)
                val now = plan.transaction.createdAt

                val txId = transactionDao.insert(
                    TransactionEntity(
                        businessId = business.id,
                        code = code,
                        type = plan.transaction.type.name,
                        amount = plan.transaction.amount,
                        paidAmount = plan.transaction.paidAmount,
                        remainingAmount = plan.transaction.remainingAmount,
                        contactId = plan.transaction.contactId,
                        accountId = plan.transaction.accountId,
                        date = plan.transaction.date,
                        description = plan.transaction.description,
                        status = plan.transaction.status.name,
                        paymentStatus = plan.transaction.paymentStatus?.name,
                        operationId = plan.transaction.operationId,
                        referenceId = plan.transaction.referenceId,
                        createdAt = now,
                        updatedAt = now
                    )
                )

                plan.accountMovements.forEach { movement ->
                    val account = accountDao.byId(movement.accountId)
                        ?: throw IllegalStateException("حساب مورد نظر یافت نشد")
                    val newBalance = account.currentBalance + movement.delta
                    accountDao.updateBalance(account.id, newBalance)
                    accountDao.insertMovement(
                        AccountMovementEntity(
                            accountId = account.id,
                            transactionId = txId,
                            type = movement.type.name,
                            delta = movement.delta,
                            balanceAfter = newBalance,
                            note = movement.note,
                            createdAt = now
                        )
                    )
                }

                plan.ledger.forEach { entry ->
                    val contact = contactDao.byId(entry.contactId)
                        ?: throw IllegalStateException("شخص مورد نظر یافت نشد")
                    val newBalance = contact.balance + entry.debit - entry.credit
                    contactDao.updateBalance(contact.id, newBalance)
                    contactDao.insertLedger(
                        ContactLedgerEntity(
                            contactId = contact.id,
                            transactionId = txId,
                            debit = entry.debit,
                            credit = entry.credit,
                            balanceAfter = newBalance,
                            note = entry.note,
                            createdAt = now
                        )
                    )
                }

                plan.stockMovements.forEach { movement ->
                    productDao.insertMovement(
                        StockMovementEntity(
                            productId = movement.productId,
                            type = movement.type.name,
                            quantityChange = movement.quantityChange,
                            unitCost = movement.unitCost,
                            referenceTransactionId = txId,
                            note = movement.note,
                            createdAt = now
                        )
                    )
                }

                plan.stockUpdates.forEach { update ->
                    productDao.updateStock(
                        id = update.productId,
                        quantity = update.newQuantity,
                        averageCost = update.newAverageCost,
                        purchasePrice = update.newPurchasePrice,
                        now = now
                    )
                }

                plan.sale?.let { sale ->
                    val saleId = saleDao.insertSale(
                        SaleEntity(
                            transactionId = txId,
                            contactId = sale.contactId,
                            grossAmount = sale.grossAmount,
                            discountAmount = sale.discountAmount,
                            netAmount = sale.netAmount,
                            paidAmount = sale.paidAmount,
                            remainingAmount = sale.remainingAmount,
                            costAmount = sale.costAmount,
                            createdAt = now
                        )
                    )
                    saleDao.insertSaleItems(
                        plan.saleItems.map { item ->
                            SaleItemEntity(
                                saleId = saleId,
                                productId = item.productId,
                                productName = item.productName,
                                quantity = item.quantity,
                                unitPrice = item.unitPrice,
                                unitCostSnapshot = item.unitCostSnapshot,
                                discountAmount = item.discountAmount,
                                totalAmount = item.totalAmount,
                                returnedQuantity = 0.0
                            )
                        }
                    )
                }

                plan.purchase?.let { purchase ->
                    val purchaseId = purchaseDao.insertPurchase(
                        PurchaseEntity(
                            transactionId = txId,
                            supplierId = purchase.supplierId,
                            grossAmount = purchase.grossAmount,
                            discountAmount = purchase.discountAmount,
                            netAmount = purchase.netAmount,
                            paidAmount = purchase.paidAmount,
                            remainingAmount = purchase.remainingAmount,
                            freightAmount = purchase.freightAmount,
                            freightToCost = purchase.freightToCost,
                            createdAt = now
                        )
                    )
                    purchaseDao.insertItems(
                        plan.purchaseItems.map { item ->
                            PurchaseItemEntity(
                                purchaseId = purchaseId,
                                productId = item.productId,
                                productName = item.productName,
                                quantity = item.quantity,
                                unitPrice = item.unitPrice,
                                totalAmount = item.totalAmount
                            )
                        }
                    )
                }

                plan.expense?.let { expense ->
                    expenseDao.insert(
                        ExpenseEntity(
                            transactionId = txId,
                            title = expense.title,
                            category = expense.category.name,
                            amount = expense.amount,
                            paymentMethod = expense.paymentMethod.name,
                            contactId = expense.contactId,
                            description = expense.description,
                            imagePath = expense.imagePath,
                            isPaid = expense.isPaid,
                            createdAt = now
                        )
                    )
                }

                plan.saleReturn?.let { saleReturn ->
                    val returnId = saleDao.insertReturn(
                        SaleReturnEntity(
                            transactionId = txId,
                            saleId = saleReturn.saleId,
                            cashRefund = saleReturn.cashRefund,
                            creditReduction = saleReturn.creditReduction,
                            costReversed = saleReturn.costReversed,
                            reason = saleReturn.reason,
                            sellable = saleReturn.sellable,
                            createdAt = now
                        )
                    )
                    saleDao.insertReturnItems(
                        saleReturn.items.map { item ->
                            SaleReturnItemEntity(
                                saleReturnId = returnId,
                                saleItemId = item.saleItemId,
                                productId = item.productId,
                                productName = item.productName,
                                quantity = item.quantity,
                                unitPrice = item.unitPrice,
                                unitCost = item.unitCost,
                                amount = item.amount
                            )
                        }
                    )
                }

                plan.returnedQuantityUpdates.forEach { update ->
                    saleDao.addReturnedQuantity(update.saleItemId, update.delta)
                }

                plan.reversedTransactionId?.let { originalId ->
                    transactionDao.updateStatus(originalId, TransactionStatus.REVERSED.name, now)
                }

                txId
            }
            OperationResult.Success(id)
        } catch (error: Exception) {
            OperationResult.Failure(error.message ?: "خطا در ثبت عملیات مالی")
        }
    }

    private suspend fun nextCode(date: Long): String {
        val jalali = JalaliDate.fromEpochMillis(date)
        val prefix = "T-${jalali.year}${"%02d".format(jalali.month)}${"%02d".format(jalali.day)}-"
        val max = transactionDao.maxCodeForPrefix(prefix)
        val sequence = max.removePrefix(prefix).toIntOrNull()?.plus(1) ?: 1
        return prefix + "%04d".format(sequence)
    }
}
