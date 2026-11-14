package ir.hesabyar.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.hesabyar.data.repository.AccountRepositoryImpl
import ir.hesabyar.data.repository.BackupRepositoryImpl
import ir.hesabyar.data.repository.BusinessRepositoryImpl
import ir.hesabyar.data.repository.ContactRepositoryImpl
import ir.hesabyar.data.repository.DashboardRepositoryImpl
import ir.hesabyar.data.repository.ExpenseRepositoryImpl
import ir.hesabyar.data.repository.ProductRepositoryImpl
import ir.hesabyar.data.repository.PurchaseRepositoryImpl
import ir.hesabyar.data.repository.ReminderRepositoryImpl
import ir.hesabyar.data.repository.ReportRepositoryImpl
import ir.hesabyar.data.repository.SaleRepositoryImpl
import ir.hesabyar.data.repository.SubscriptionRepositoryImpl
import ir.hesabyar.data.repository.TransactionRepositoryImpl
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.repository.BackupRepository
import ir.hesabyar.domain.repository.BusinessRepository
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.repository.DashboardRepository
import ir.hesabyar.domain.repository.ExpenseRepository
import ir.hesabyar.domain.repository.ProductRepository
import ir.hesabyar.domain.repository.PurchaseRepository
import ir.hesabyar.domain.repository.ReminderRepository
import ir.hesabyar.domain.repository.ReportRepository
import ir.hesabyar.domain.repository.SaleRepository
import ir.hesabyar.domain.repository.SubscriptionRepository
import ir.hesabyar.domain.repository.TransactionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBusinessRepository(impl: BusinessRepositoryImpl): BusinessRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindSaleRepository(impl: SaleRepositoryImpl): SaleRepository

    @Binds
    @Singleton
    abstract fun bindPurchaseRepository(impl: PurchaseRepositoryImpl): PurchaseRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
