package ir.hesabyar.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.hesabyar.core.database.HesabyarDatabase
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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HesabyarDatabase =
        Room.databaseBuilder(context, HesabyarDatabase::class.java, HesabyarDatabase.NAME)
            .addMigrations(*HesabyarDatabase.MIGRATIONS)
            .build()

    @Provides
    fun businessDao(db: HesabyarDatabase): BusinessDao = db.businessDao()

    @Provides
    fun accountDao(db: HesabyarDatabase): AccountDao = db.accountDao()

    @Provides
    fun contactDao(db: HesabyarDatabase): ContactDao = db.contactDao()

    @Provides
    fun productDao(db: HesabyarDatabase): ProductDao = db.productDao()

    @Provides
    fun transactionDao(db: HesabyarDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun saleDao(db: HesabyarDatabase): SaleDao = db.saleDao()

    @Provides
    fun purchaseDao(db: HesabyarDatabase): PurchaseDao = db.purchaseDao()

    @Provides
    fun expenseDao(db: HesabyarDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun reportDao(db: HesabyarDatabase): ReportDao = db.reportDao()

    @Provides
    fun reminderDao(db: HesabyarDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun backupDao(db: HesabyarDatabase): BackupDao = db.backupDao()

    @Provides
    fun backupExportDao(db: HesabyarDatabase): BackupExportDao = db.backupExportDao()

    @Provides
    fun subscriptionDao(db: HesabyarDatabase): SubscriptionDao = db.subscriptionDao()
}
