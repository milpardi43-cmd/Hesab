package ir.hesabyar.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ir.hesabyar.core.database.dao.AccountDao
import ir.hesabyar.core.database.dao.BackupDao
import ir.hesabyar.core.database.dao.BackupExportDao
import ir.hesabyar.core.database.dao.BusinessDao
import ir.hesabyar.core.database.dao.ContactDao
import ir.hesabyar.core.database.dao.ExpenseDao
import ir.hesabyar.core.database.dao.ProductDao
import ir.hesabyar.core.database.dao.PurchaseDao
import ir.hesabyar.core.database.dao.ReminderDao
import ir.hesabyar.core.database.dao.ReportDao
import ir.hesabyar.core.database.dao.SaleDao
import ir.hesabyar.core.database.dao.SubscriptionDao
import ir.hesabyar.core.database.dao.TransactionDao
import ir.hesabyar.core.database.entity.AccountEntity
import ir.hesabyar.core.database.entity.AccountMovementEntity
import ir.hesabyar.core.database.entity.BackupEntity
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
import androidx.room.migration.Migration

/**
 * Version 1 schema. Future versions add Migration objects here; destructive migration is never
 * enabled so user data survives app updates.
 */
@Database(
    entities = [
        BusinessEntity::class,
        AccountEntity::class,
        ContactEntity::class,
        ProductCategoryEntity::class,
        ProductEntity::class,
        TransactionEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        ExpenseEntity::class,
        SaleReturnEntity::class,
        SaleReturnItemEntity::class,
        StockMovementEntity::class,
        ContactLedgerEntity::class,
        AccountMovementEntity::class,
        ReminderEntity::class,
        BackupEntity::class,
        SubscriptionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HesabyarDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun accountDao(): AccountDao
    abstract fun contactDao(): ContactDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun saleDao(): SaleDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun reportDao(): ReportDao
    abstract fun reminderDao(): ReminderDao
    abstract fun backupDao(): BackupDao
    abstract fun backupExportDao(): BackupExportDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        const val NAME = "hesabyar.db"
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
