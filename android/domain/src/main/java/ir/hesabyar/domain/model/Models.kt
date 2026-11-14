package ir.hesabyar.domain.model

/** Core domain models. Money values are always Long (Toman). Quantities may be fractional. */

data class Business(
    val id: Long = 0,
    val name: String,
    val ownerName: String? = null,
    val phone: String? = null,
    val type: BusinessType = BusinessType.OTHER,
    val city: String? = null,
    val createdAt: Long = 0
)

data class Account(
    val id: Long = 0,
    val businessId: Long,
    val title: String,
    val type: AccountType,
    val initialBalance: Long = 0,
    val currentBalance: Long = 0,
    val createdAt: Long = 0
)

data class Contact(
    val id: Long = 0,
    val businessId: Long,
    val name: String,
    val phone: String? = null,
    val type: ContactType = ContactType.CUSTOMER,
    val initialBalance: Long = 0,
    val balance: Long = 0,
    val description: String? = null,
    val createdAt: Long = 0
)

data class ProductCategory(
    val id: Long = 0,
    val businessId: Long,
    val name: String
)

data class Product(
    val id: Long = 0,
    val businessId: Long,
    val name: String,
    val code: String? = null,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val purchasePrice: Long = 0,
    val sellingPrice: Long = 0,
    val averageCost: Long = 0,
    val stockQuantity: Double = 0.0,
    val minimumStock: Double = 0.0,
    val description: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) {
    val isLowStock: Boolean get() = minimumStock > 0 && stockQuantity <= minimumStock
    val stockValue: Long get() = (stockQuantity * averageCost).toLong()
}

data class Transaction(
    val id: Long = 0,
    val businessId: Long,
    val code: String,
    val type: TransactionType,
    val amount: Long,
    val paidAmount: Long,
    val remainingAmount: Long,
    val contactId: Long? = null,
    val contactName: String? = null,
    val accountId: Long? = null,
    val accountTitle: String? = null,
    val date: Long,
    val description: String? = null,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val paymentStatus: PaymentStatus? = null,
    val operationId: String,
    val referenceId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class Sale(
    val id: Long = 0,
    val transactionId: Long,
    val contactId: Long? = null,
    val grossAmount: Long,
    val discountAmount: Long,
    val netAmount: Long,
    val paidAmount: Long,
    val remainingAmount: Long,
    val costAmount: Long,
    val createdAt: Long
)

data class SaleItem(
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Long,
    val unitCostSnapshot: Long,
    val discountAmount: Long,
    val totalAmount: Long,
    val returnedQuantity: Double = 0.0
)

data class Purchase(
    val id: Long = 0,
    val transactionId: Long,
    val supplierId: Long? = null,
    val grossAmount: Long,
    val discountAmount: Long,
    val netAmount: Long,
    val paidAmount: Long,
    val remainingAmount: Long,
    val freightAmount: Long = 0,
    val freightToCost: Boolean = true,
    val createdAt: Long
)

data class PurchaseItem(
    val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Long,
    val totalAmount: Long
)

data class Expense(
    val id: Long = 0,
    val transactionId: Long,
    val title: String,
    val category: ExpenseCategory,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    val contactId: Long? = null,
    val description: String? = null,
    val imagePath: String? = null,
    val isPaid: Boolean = true,
    val createdAt: Long
)

data class SaleReturn(
    val id: Long = 0,
    val transactionId: Long,
    val saleId: Long,
    val cashRefund: Long,
    val creditReduction: Long,
    val costReversed: Long,
    val reason: String? = null,
    val sellable: Boolean = true,
    val createdAt: Long
)

data class SaleReturnItem(
    val id: Long = 0,
    val saleReturnId: Long,
    val saleItemId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Long,
    val unitCost: Long,
    val amount: Long
)

data class StockMovement(
    val id: Long = 0,
    val productId: Long,
    val type: StockMovementType,
    val quantityChange: Double,
    val unitCost: Long,
    val referenceTransactionId: Long? = null,
    val note: String? = null,
    val createdAt: Long
)

data class ContactLedgerEntry(
    val id: Long = 0,
    val contactId: Long,
    val transactionId: Long? = null,
    val transactionCode: String? = null,
    val transactionType: TransactionType? = null,
    val debit: Long,
    val credit: Long,
    val balanceAfter: Long,
    val note: String? = null,
    val createdAt: Long
)

data class AccountMovement(
    val id: Long = 0,
    val accountId: Long,
    val transactionId: Long? = null,
    val transactionCode: String? = null,
    val type: AccountMovementType,
    val delta: Long,
    val balanceAfter: Long,
    val note: String? = null,
    val createdAt: Long
)

data class Reminder(
    val id: Long = 0,
    val businessId: Long,
    val contactId: Long? = null,
    val contactName: String? = null,
    val type: ReminderType,
    val amount: Long = 0,
    val dueDate: Long,
    val status: ReminderStatus = ReminderStatus.PENDING,
    val description: String? = null,
    val createdAt: Long = 0
)

data class BackupRecord(
    val id: Long = 0,
    val businessId: Long,
    val date: Long,
    val fileLocation: String,
    val status: String,
    val sizeBytes: Long = 0
)

data class Subscription(
    val id: Long = 0,
    val businessId: Long,
    val plan: PlanType = PlanType.FREE,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val status: SubscriptionStatus = SubscriptionStatus.NONE,
    val paymentReference: String? = null
)

data class TransactionDetail(
    val transaction: Transaction,
    val sale: Sale? = null,
    val saleItems: List<SaleItem> = emptyList(),
    val purchase: Purchase? = null,
    val purchaseItems: List<PurchaseItem> = emptyList(),
    val expense: Expense? = null,
    val saleReturn: SaleReturn? = null,
    val returnItems: List<SaleReturnItem> = emptyList(),
    val contact: Contact? = null,
    val account: Account? = null,
    val accountMovements: List<AccountMovement> = emptyList(),
    val ledgerEntries: List<ContactLedgerEntry> = emptyList(),
    val stockMovements: List<StockMovement> = emptyList(),
    val referenceCode: String? = null,
    val reversedByCode: String? = null
) {
    val hasReturns: Boolean get() = saleItems.any { it.returnedQuantity > 0 }
    val isReversible: Boolean
        get() = transaction.status == TransactionStatus.COMPLETED &&
            transaction.type != TransactionType.REVERSAL &&
            !hasReturns
}
