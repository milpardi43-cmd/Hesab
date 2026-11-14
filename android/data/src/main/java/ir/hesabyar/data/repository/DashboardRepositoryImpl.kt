package ir.hesabyar.data.repository

import ir.hesabyar.core.database.dao.AccountDao
import ir.hesabyar.core.database.dao.BusinessDao
import ir.hesabyar.core.database.dao.ContactDao
import ir.hesabyar.core.database.dao.ProductDao
import ir.hesabyar.core.database.dao.ReminderDao
import ir.hesabyar.core.database.dao.ReportDao
import ir.hesabyar.core.database.dao.TransactionDao
import ir.hesabyar.data.mapper.toDomain
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.model.DashboardData
import ir.hesabyar.domain.repository.DashboardRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val businessDao: BusinessDao,
    private val accountDao: AccountDao,
    private val contactDao: ContactDao,
    private val reportDao: ReportDao,
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao,
    private val reminderDao: ReminderDao
) : DashboardRepository {

    override fun observeDashboard(): Flow<DashboardData> = flow {
        val today = JalaliDate.startOfDayMillis(System.currentTimeMillis())
        val business = businessDao.observeFirst()
        val accounts = accountDao.observeAll()
        val received = accountDao.observeTotalReceived()
        val paid = accountDao.observeTotalPaid()
        val receivable = contactDao.observeReceivable()
        val payable = contactDao.observePayable()
        val aggregate = reportDao.observeDashboardAggregate(today)
        val receivedToday = accountDao.observeReceivedOn(today)
        val recent = transactionDao.observeRecent(10)
        val lowStock = productDao.observeLowStockCount()
        val pendingReminders = reminderDao.observePendingCount()
        val transactionCount = transactionDao.observeCount()

        val balances = combine(business, accounts, received, paid) { biz, acc, r, p ->
            BalanceSnapshot(biz?.toDomain()?.name.orEmpty(), acc.map { it.toDomain() }, r, p)
        }
        val debts = combine(receivable, payable, aggregate, receivedToday) { rec, pay, agg, recToday ->
            DebtSnapshot(rec, pay, agg, recToday)
        }
        val activity = combine(recent, lowStock, pendingReminders, transactionCount) { rows, low, reminders, count ->
            ActivitySnapshot(rows.map { it.toDomain() }, low, reminders, count)
        }

        emitAll(combine(balances, debts, activity) { b, d, a ->
            val netSales = d.aggregate.sales - d.aggregate.returns
            val cogs = d.aggregate.cogs - d.aggregate.returnsCost
            DashboardData(
                businessName = b.businessName,
                cashBalance = b.accounts.sumOf { it.currentBalance },
                totalReceived = b.totalReceived,
                totalPaid = b.totalPaid,
                todaySales = netSales,
                todayReceived = d.receivedToday,
                todayExpenses = d.aggregate.expenses,
                todayProfit = netSales - cogs - d.aggregate.expenses,
                receivable = d.receivable,
                payable = -d.payable,
                accounts = b.accounts,
                recentTransactions = a.recent,
                hasAnyActivity = a.transactionCount > 0,
                lowStockCount = a.lowStock,
                pendingReminders = a.pendingReminders
            )
        })
    }

    private data class BalanceSnapshot(
        val businessName: String,
        val accounts: List<ir.hesabyar.domain.model.Account>,
        val totalReceived: Long,
        val totalPaid: Long
    )

    private data class DebtSnapshot(
        val receivable: Long,
        val payable: Long,
        val aggregate: ir.hesabyar.core.database.dao.DashboardAggregate,
        val receivedToday: Long
    )

    private data class ActivitySnapshot(
        val recent: List<ir.hesabyar.domain.model.Transaction>,
        val lowStock: Int,
        val pendingReminders: Int,
        val transactionCount: Int
    )
}
