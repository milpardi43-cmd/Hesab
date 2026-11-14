package ir.hesabyar.domain.model

data class DashboardData(
    val businessName: String = "",
    val cashBalance: Long = 0,
    val totalReceived: Long = 0,
    val totalPaid: Long = 0,
    val todaySales: Long = 0,
    val todayReceived: Long = 0,
    val todayExpenses: Long = 0,
    val todayProfit: Long = 0,
    val receivable: Long = 0,
    val payable: Long = 0,
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val hasAnyActivity: Boolean = false,
    val lowStockCount: Int = 0,
    val pendingReminders: Int = 0
)

data class DailyReport(
    val date: Long,
    val salesCount: Int = 0,
    val grossSales: Long = 0,
    val discount: Long = 0,
    val returns: Long = 0,
    val netSales: Long = 0,
    val cashReceived: Long = 0,
    val creditSales: Long = 0,
    val receiptsFromDebts: Long = 0,
    val operatingExpenses: Long = 0,
    val costOfGoodsSold: Long = 0,
    val grossProfit: Long = 0,
    val netProfit: Long = 0
)

data class MonthlyReport(
    val year: Int,
    val month: Int,
    val grossSales: Long = 0,
    val returns: Long = 0,
    val netSales: Long = 0,
    val cashReceived: Long = 0,
    val creditSales: Long = 0,
    val operatingExpenses: Long = 0,
    val costOfGoodsSold: Long = 0,
    val grossProfit: Long = 0,
    val netProfit: Long = 0,
    val salesCount: Int = 0,
    val dailySales: List<ChartPoint> = emptyList(),
    val dailyExpenses: List<ChartPoint> = emptyList(),
    val dailyProfit: List<ChartPoint> = emptyList()
)

data class ChartPoint(val label: String, val value: Long, val key: Long = 0)

data class CustomerReportRow(
    val contactId: Long,
    val name: String,
    val phone: String?,
    val balance: Long,
    val totalSales: Long,
    val totalReceipts: Long,
    val lastActivity: Long?
)

data class SupplierReportRow(
    val contactId: Long,
    val name: String,
    val phone: String?,
    val balance: Long,
    val totalPurchases: Long,
    val totalPayments: Long,
    val lastActivity: Long?
)

data class ProductReportRow(
    val productId: Long,
    val name: String,
    val soldQuantity: Double,
    val salesAmount: Long,
    val profit: Long,
    val stockQuantity: Double,
    val averageCost: Long,
    val stockValue: Long,
    val minimumStock: Double
) {
    val isLowStock: Boolean get() = minimumStock > 0 && stockQuantity <= minimumStock
}

data class AccountReport(
    val account: Account,
    val totalIn: Long,
    val totalOut: Long,
    val movements: List<AccountMovement>
)

data class SalesTrend(
    val sales: List<ChartPoint>,
    val expenses: List<ChartPoint>,
    val profit: List<ChartPoint>
)

data class TransactionFilter(
    val query: String = "",
    val from: Long? = null,
    val to: Long? = null,
    val type: TransactionType? = null,
    val contactId: Long? = null,
    val paymentStatus: PaymentStatus? = null,
    val newestFirst: Boolean = true
)

data class ContactFilter(
    val query: String = "",
    val type: ContactType? = null,
    val onlyWithBalance: Boolean = false,
    val sortByBalance: Boolean = false
)

data class ProductFilter(
    val query: String = "",
    val categoryId: Long? = null,
    val onlyLowStock: Boolean = false
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val textScale: Float = 1f,
    val persianDigits: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val lowStockAlerts: Boolean = true,
    val autoBackupEnabled: Boolean = true,
    val defaultReceiveAccountId: Long? = null,
    val defaultPayAccountId: Long? = null,
    val lowStockThreshold: Double = 0.0,
    val lastAutoBackupAt: Long? = null
)

enum class ThemeMode(val label: String) {
    LIGHT("روشن"),
    DARK("تاریک"),
    SYSTEM("هماهنگ با سیستم")
}

data class PlanLimits(
    val maxContacts: Int?,
    val maxProducts: Int?,
    val advancedReports: Boolean,
    val excelExport: Boolean,
    val reminders: Boolean,
    val cloudSync: Boolean
) {
    companion object {
        fun of(plan: PlanType): PlanLimits = when (plan) {
            PlanType.FREE -> PlanLimits(
                maxContacts = 50,
                maxProducts = 100,
                advancedReports = false,
                excelExport = false,
                reminders = true,
                cloudSync = false
            )
            PlanType.PRO -> PlanLimits(
                maxContacts = null,
                maxProducts = null,
                advancedReports = true,
                excelExport = true,
                reminders = true,
                cloudSync = false
            )
            PlanType.BUSINESS -> PlanLimits(
                maxContacts = null,
                maxProducts = null,
                advancedReports = true,
                excelExport = true,
                reminders = true,
                cloudSync = true
            )
        }
    }
}
