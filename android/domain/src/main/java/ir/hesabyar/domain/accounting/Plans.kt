package ir.hesabyar.domain.accounting

import ir.hesabyar.domain.model.AccountMovementType
import ir.hesabyar.domain.model.ExpenseCategory
import ir.hesabyar.domain.model.PaymentMethod
import ir.hesabyar.domain.model.PaymentStatus
import ir.hesabyar.domain.model.StockMovementType
import ir.hesabyar.domain.model.TransactionStatus
import ir.hesabyar.domain.model.TransactionType

/**
 * A PostingPlan is the complete set of database effects of one financial operation.
 * The data layer persists every plan inside a single Room transaction, so a sale can never
 * leave the database half-written.
 */

data class NewTransaction(
    val code: String,
    val type: TransactionType,
    val amount: Long,
    val paidAmount: Long,
    val remainingAmount: Long,
    val contactId: Long?,
    val accountId: Long?,
    val date: Long,
    val description: String?,
    val status: TransactionStatus,
    val paymentStatus: PaymentStatus?,
    val operationId: String,
    val referenceId: Long?,
    val createdAt: Long
)

data class AccountMovementDraft(
    val accountId: Long,
    val delta: Long,
    val type: AccountMovementType,
    val note: String?
)

data class LedgerDraft(
    val contactId: Long,
    val debit: Long,
    val credit: Long,
    val note: String?
)

data class StockMovementDraft(
    val productId: Long,
    val type: StockMovementType,
    val quantityChange: Double,
    val unitCost: Long,
    val note: String?
)

data class StockLevelUpdate(
    val productId: Long,
    val newQuantity: Double,
    val newAverageCost: Long,
    val newPurchasePrice: Long? = null
)

data class SaleDraft(
    val contactId: Long?,
    val grossAmount: Long,
    val discountAmount: Long,
    val netAmount: Long,
    val paidAmount: Long,
    val remainingAmount: Long,
    val costAmount: Long
)

data class SaleItemDraft(
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Long,
    val unitCostSnapshot: Long,
    val discountAmount: Long,
    val totalAmount: Long
)

data class PurchaseDraft(
    val supplierId: Long?,
    val grossAmount: Long,
    val discountAmount: Long,
    val netAmount: Long,
    val paidAmount: Long,
    val remainingAmount: Long,
    val freightAmount: Long,
    val freightToCost: Boolean
)

data class PurchaseItemDraft(
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Long,
    val totalAmount: Long
)

data class ExpenseDraft(
    val title: String,
    val category: ExpenseCategory,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    val contactId: Long?,
    val description: String?,
    val imagePath: String?,
    val isPaid: Boolean
)

data class ReturnItemDraft(
    val saleItemId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Long,
    val unitCost: Long,
    val amount: Long
)

data class SaleReturnDraft(
    val saleId: Long,
    val cashRefund: Long,
    val creditReduction: Long,
    val costReversed: Long,
    val reason: String?,
    val sellable: Boolean,
    val items: List<ReturnItemDraft>
)

data class ReturnedQuantityUpdate(
    val saleItemId: Long,
    val delta: Double
)

data class PostingPlan(
    val transaction: NewTransaction,
    val accountMovements: List<AccountMovementDraft> = emptyList(),
    val ledger: List<LedgerDraft> = emptyList(),
    val stockMovements: List<StockMovementDraft> = emptyList(),
    val stockUpdates: List<StockLevelUpdate> = emptyList(),
    val sale: SaleDraft? = null,
    val saleItems: List<SaleItemDraft> = emptyList(),
    val purchase: PurchaseDraft? = null,
    val purchaseItems: List<PurchaseItemDraft> = emptyList(),
    val expense: ExpenseDraft? = null,
    val saleReturn: SaleReturnDraft? = null,
    val returnedQuantityUpdates: List<ReturnedQuantityUpdate> = emptyList(),
    val reversedTransactionId: Long? = null
)
