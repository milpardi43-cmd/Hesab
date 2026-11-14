package ir.hesabyar.data.backup

import ir.hesabyar.core.database.entity.AccountEntity
import ir.hesabyar.core.database.entity.AccountMovementEntity
import ir.hesabyar.core.database.entity.BusinessEntity
import ir.hesabyar.core.database.entity.ContactEntity
import ir.hesabyar.core.database.entity.ContactLedgerEntity
import ir.hesabyar.core.database.entity.ExpenseEntity
import ir.hesabyar.core.database.entity.ProductCategoryEntity
import ir.hesabyar.core.database.entity.ProductEntity
import ir.hesabyar.core.database.entity.PurchaseEntity
import ir.hesabyar.core.database.entity.PurchaseItemEntity
import ir.hesabyar.core.database.entity.ReminderEntity
import ir.hesabyar.core.database.entity.SaleEntity
import ir.hesabyar.core.database.entity.SaleItemEntity
import ir.hesabyar.core.database.entity.SaleReturnEntity
import ir.hesabyar.core.database.entity.SaleReturnItemEntity
import ir.hesabyar.core.database.entity.StockMovementEntity
import ir.hesabyar.core.database.entity.SubscriptionEntity
import ir.hesabyar.core.database.entity.TransactionEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val businesses: List<BusinessEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val contacts: List<ContactEntity> = emptyList(),
    val categories: List<ProductCategoryEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val saleItems: List<SaleItemEntity> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val purchaseItems: List<PurchaseItemEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val saleReturns: List<SaleReturnEntity> = emptyList(),
    val saleReturnItems: List<SaleReturnItemEntity> = emptyList(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val contactLedger: List<ContactLedgerEntity> = emptyList(),
    val accountMovements: List<AccountMovementEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList()
)

@Serializable
data class BackupFile(
    val format: String = "hesabyar-backup",
    val version: Int = 1,
    val createdAt: Long,
    val appVersion: String,
    val checksum: String,
    val payload: BackupPayload
)
