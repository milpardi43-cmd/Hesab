package ir.hesabyar.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/*
 * Room schema. Every financial table keeps its own primary key, indexes on the columns used by
 * reports and a foreign key to its parent. Cascading deletes are intentionally avoided: confirmed
 * financial records must never disappear silently.
 */

@Serializable
@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "owner_name") val ownerName: String?,
    val phone: String?,
    @ColumnInfo(name = "business_type") val businessType: String,
    val city: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Serializable
@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["business_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("business_id")]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long,
    val title: String,
    @ColumnInfo(name = "account_type") val accountType: String,
    @ColumnInfo(name = "initial_balance") val initialBalance: Long,
    @ColumnInfo(name = "current_balance") val currentBalance: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Serializable
@Entity(
    tableName = "contacts",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["business_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("business_id"), Index("name"), Index("balance")]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long,
    val name: String,
    val phone: String?,
    @ColumnInfo(name = "contact_type") val contactType: String,
    @ColumnInfo(name = "initial_balance") val initialBalance: Long,
    val balance: Long,
    val description: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Serializable
@Entity(
    tableName = "product_categories",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["business_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["business_id", "name"], unique = true)]
)
data class ProductCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long,
    val name: String
)

@Serializable
@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["business_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ProductCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("business_id"),
        Index("category_id"),
        Index("name"),
        Index(value = ["business_id", "code"], unique = true)
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long,
    val name: String,
    val code: String?,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "purchase_price") val purchasePrice: Long,
    @ColumnInfo(name = "selling_price") val sellingPrice: Long,
    @ColumnInfo(name = "average_cost") val averageCost: Long,
    @ColumnInfo(name = "stock_quantity") val stockQuantity: Double,
    @ColumnInfo(name = "minimum_stock") val minimumStock: Double,
    val description: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Serializable
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["business_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["operation_id"], unique = true),
        Index(value = ["code"], unique = true),
        Index("business_id"),
        Index("contact_id"),
        Index("account_id"),
        Index("date"),
        Index("type"),
        Index("status"),
        Index("reference_id")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long,
    val code: String,
    val type: String,
    val amount: Long,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long,
    @ColumnInfo(name = "remaining_amount") val remainingAmount: Long,
    @ColumnInfo(name = "contact_id") val contactId: Long?,
    @ColumnInfo(name = "account_id") val accountId: Long?,
    val date: Long,
    val description: String?,
    val status: String,
    @ColumnInfo(name = "payment_status") val paymentStatus: String?,
    @ColumnInfo(name = "operation_id") val operationId: String,
    @ColumnInfo(name = "reference_id") val referenceId: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Serializable
@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["transaction_id"], unique = true), Index("contact_id")]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "contact_id") val contactId: Long?,
    @ColumnInfo(name = "gross_amount") val grossAmount: Long,
    @ColumnInfo(name = "discount_amount") val discountAmount: Long,
    @ColumnInfo(name = "net_amount") val netAmount: Long,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long,
    @ColumnInfo(name = "remaining_amount") val remainingAmount: Long,
    @ColumnInfo(name = "cost_amount") val costAmount: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Serializable
@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("sale_id"), Index("product_id")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sale_id") val saleId: Long,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    val quantity: Double,
    @ColumnInfo(name = "unit_price") val unitPrice: Long,
    @ColumnInfo(name = "unit_cost_snapshot") val unitCostSnapshot: Long,
    @ColumnInfo(name = "discount_amount") val discountAmount: Long,
    @ColumnInfo(name = "total_amount") val totalAmount: Long,
    @ColumnInfo(name = "returned_quantity") val returnedQuantity: Double
)

@Serializable
@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplier_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["transaction_id"], unique = true), Index("supplier_id")]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "supplier_id") val supplierId: Long?,
    @ColumnInfo(name = "gross_amount") val grossAmount: Long,
    @ColumnInfo(name = "discount_amount") val discountAmount: Long,
    @ColumnInfo(name = "net_amount") val netAmount: Long,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long,
    @ColumnInfo(name = "remaining_amount") val remainingAmount: Long,
    @ColumnInfo(name = "freight_amount") val freightAmount: Long,
    @ColumnInfo(name = "freight_to_cost") val freightToCost: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Serializable
@Entity(
    tableName = "purchase_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchase_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("purchase_id"), Index("product_id")]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "purchase_id") val purchaseId: Long,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    val quantity: Double,
    @ColumnInfo(name = "unit_price") val unitPrice: Long,
    @ColumnInfo(name = "total_amount") val totalAmount: Long
)

@Serializable
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["transaction_id"], unique = true), Index("contact_id"), Index("category")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    val title: String,
    val category: String,
    val amount: Long,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "contact_id") val contactId: Long?,
    val description: String?,
    @ColumnInfo(name = "image_path") val imagePath: String?,
    @ColumnInfo(name = "is_paid") val isPaid: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Serializable
@Entity(
    tableName = "sale_returns",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["transaction_id"], unique = true), Index("sale_id")]
)
data class SaleReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "sale_id") val saleId: Long,
    @ColumnInfo(name = "cash_refund") val cashRefund: Long,
    @ColumnInfo(name = "credit_reduction") val creditReduction: Long,
    @ColumnInfo(name = "cost_reversed") val costReversed: Long,
    val reason: String?,
    val sellable: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Serializable
@Entity(
    tableName = "sale_return_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleReturnEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_return_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = SaleItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_item_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("sale_return_id"), Index("sale_item_id"), Index("product_id")]
)
data class SaleReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sale_return_id") val saleReturnId: Long,
    @ColumnInfo(name = "sale_item_id") val saleItemId: Long,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    val quantity: Double,
    @ColumnInfo(name = "unit_price") val unitPrice: Long,
    @ColumnInfo(name = "unit_cost") val unitCost: Long,
    val amount: Long
)

@Serializable
@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["reference_transaction_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("product_id"), Index("reference_transaction_id"), Index("created_at")]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "product_id") val productId: Long,
    val type: String,
    @ColumnInfo(name = "quantity_change") val quantityChange: Double,
    @ColumnInfo(name = "unit_cost") val unitCost: Long,
    @ColumnInfo(name = "reference_transaction_id") val referenceTransactionId: Long?,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Serializable
@Entity(
    tableName = "contact_ledger",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("contact_id"), Index("transaction_id"), Index("created_at")]
)
data class ContactLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "contact_id") val contactId: Long,
    @ColumnInfo(name = "transaction_id") val transactionId: Long?,
    val debit: Long,
    val credit: Long,
    @ColumnInfo(name = "balance_after") val balanceAfter: Long,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Serializable
@Entity(
    tableName = "account_movements",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("account_id"), Index("transaction_id"), Index("created_at")]
)
data class AccountMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "account_id") val accountId: Long,
    @ColumnInfo(name = "transaction_id") val transactionId: Long?,
    val type: String,
    val delta: Long,
    @ColumnInfo(name = "balance_after") val balanceAfter: Long,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Serializable
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["business_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("business_id"), Index("contact_id"), Index("due_date"), Index("status")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long,
    @ColumnInfo(name = "contact_id") val contactId: Long?,
    val type: String,
    val amount: Long,
    @ColumnInfo(name = "due_date") val dueDate: Long,
    val status: String,
    val description: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Serializable
@Entity(
    tableName = "backups",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["business_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("business_id"), Index("backup_date")]
)
data class BackupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long,
    @ColumnInfo(name = "backup_date") val backupDate: Long,
    @ColumnInfo(name = "file_location") val fileLocation: String,
    val status: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long
)

@Serializable
@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["business_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("business_id")]
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "business_id") val businessId: Long,
    @ColumnInfo(name = "plan_type") val planType: String,
    @ColumnInfo(name = "start_date") val startDate: Long?,
    @ColumnInfo(name = "end_date") val endDate: Long?,
    val status: String,
    @ColumnInfo(name = "payment_reference") val paymentReference: String?
)
