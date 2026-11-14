package ir.hesabyar.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class SalesAggregate(
    @ColumnInfo(name = "sales_count") val salesCount: Int,
    @ColumnInfo(name = "gross") val gross: Long,
    @ColumnInfo(name = "discount") val discount: Long,
    @ColumnInfo(name = "paid") val paid: Long,
    @ColumnInfo(name = "remaining") val remaining: Long
)

data class DashboardAggregate(
    @ColumnInfo(name = "sales") val sales: Long,
    @ColumnInfo(name = "returns") val returns: Long,
    @ColumnInfo(name = "expenses") val expenses: Long,
    @ColumnInfo(name = "cogs") val cogs: Long,
    @ColumnInfo(name = "returns_cost") val returnsCost: Long
)

data class DateAmount(
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "amount") val amount: Long
)

data class CustomerReportRecord(
    @ColumnInfo(name = "contact_id") val contactId: Long,
    val name: String,
    val phone: String?,
    val balance: Long,
    @ColumnInfo(name = "total_sales") val totalSales: Long,
    @ColumnInfo(name = "total_receipts") val totalReceipts: Long,
    @ColumnInfo(name = "last_activity") val lastActivity: Long?
)

data class SupplierReportRecord(
    @ColumnInfo(name = "contact_id") val contactId: Long,
    val name: String,
    val phone: String?,
    val balance: Long,
    @ColumnInfo(name = "total_purchases") val totalPurchases: Long,
    @ColumnInfo(name = "total_payments") val totalPayments: Long,
    @ColumnInfo(name = "last_activity") val lastActivity: Long?
)

data class SaleItemAggregateRow(
    @ColumnInfo(name = "product_id") val productId: Long,
    val quantity: Double,
    @ColumnInfo(name = "total_amount") val totalAmount: Long,
    @ColumnInfo(name = "unit_cost_snapshot") val unitCost: Long,
    @ColumnInfo(name = "returned_quantity") val returnedQuantity: Double
)

data class ReturnItemAggregateRow(
    @ColumnInfo(name = "product_id") val productId: Long,
    val quantity: Double,
    val amount: Long,
    @ColumnInfo(name = "unit_cost") val unitCost: Long
)

@Dao
interface ReportDao {
    @Query(
        """
        SELECT COUNT(*) AS sales_count, COALESCE(SUM(s.gross_amount), 0) AS gross,
               COALESCE(SUM(s.discount_amount), 0) AS discount,
               COALESCE(SUM(s.paid_amount), 0) AS paid,
               COALESCE(SUM(s.remaining_amount), 0) AS remaining
        FROM sales s JOIN transactions t ON s.transaction_id = t.id
        WHERE t.status = 'COMPLETED' AND t.date >= :from AND t.date < :to
        """
    )
    suspend fun salesAggregate(from: Long, to: Long): SalesAggregate

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'SALE_RETURN' AND status = 'COMPLETED' AND date >= :from AND date < :to
        """
    )
    suspend fun returns(from: Long, to: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'CUSTOMER_RECEIPT' AND status = 'COMPLETED' AND date >= :from AND date < :to
        """
    )
    suspend fun receipts(from: Long, to: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(e.amount), 0) FROM expenses e JOIN transactions t ON e.transaction_id = t.id
        WHERE t.status = 'COMPLETED' AND t.date >= :from AND t.date < :to
        """
    )
    suspend fun expenses(from: Long, to: Long): Long

    @Query(
        """
        SELECT CAST(COALESCE(SUM(si.quantity * si.unit_cost_snapshot), 0) AS INTEGER)
        FROM sale_items si JOIN sales s ON si.sale_id = s.id JOIN transactions t ON s.transaction_id = t.id
        WHERE t.status = 'COMPLETED' AND t.date >= :from AND t.date < :to
        """
    )
    suspend fun cogs(from: Long, to: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(sr.cost_reversed), 0) FROM sale_returns sr
        JOIN transactions t ON sr.transaction_id = t.id
        WHERE t.status = 'COMPLETED' AND t.date >= :from AND t.date < :to
        """
    )
    suspend fun returnsCost(from: Long, to: Long): Long

    @Query(
        """
        SELECT t.date AS date, COALESCE(SUM(s.net_amount), 0) AS amount
        FROM sales s JOIN transactions t ON s.transaction_id = t.id
        WHERE t.status = 'COMPLETED' AND t.date >= :from AND t.date < :to
        GROUP BY t.date ORDER BY t.date
        """
    )
    suspend fun salesByDate(from: Long, to: Long): List<DateAmount>

    @Query(
        """
        SELECT date, COALESCE(SUM(amount), 0) AS amount FROM transactions
        WHERE type = 'SALE_RETURN' AND status = 'COMPLETED' AND date >= :from AND date < :to
        GROUP BY date ORDER BY date
        """
    )
    suspend fun returnsByDate(from: Long, to: Long): List<DateAmount>

    @Query(
        """
        SELECT t.date AS date, COALESCE(SUM(e.amount), 0) AS amount
        FROM expenses e JOIN transactions t ON e.transaction_id = t.id
        WHERE t.status = 'COMPLETED' AND t.date >= :from AND t.date < :to
        GROUP BY t.date ORDER BY t.date
        """
    )
    suspend fun expensesByDate(from: Long, to: Long): List<DateAmount>

    @Query(
        """
        SELECT t.date AS date, CAST(COALESCE(SUM(si.quantity * si.unit_cost_snapshot), 0) AS INTEGER) AS amount
        FROM sale_items si JOIN sales s ON si.sale_id = s.id JOIN transactions t ON s.transaction_id = t.id
        WHERE t.status = 'COMPLETED' AND t.date >= :from AND t.date < :to
        GROUP BY t.date ORDER BY t.date
        """
    )
    suspend fun cogsByDate(from: Long, to: Long): List<DateAmount>

    @Query(
        """
        SELECT t.date AS date, COALESCE(SUM(sr.cost_reversed), 0) AS amount
        FROM sale_returns sr JOIN transactions t ON sr.transaction_id = t.id
        WHERE t.status = 'COMPLETED' AND t.date >= :from AND t.date < :to
        GROUP BY t.date ORDER BY t.date
        """
    )
    suspend fun returnsCostByDate(from: Long, to: Long): List<DateAmount>

    @Query(
        """
        SELECT
            (SELECT COALESCE(SUM(s.net_amount), 0) FROM sales s JOIN transactions t ON s.transaction_id = t.id
                WHERE t.status = 'COMPLETED' AND t.date = :date) AS sales,
            (SELECT COALESCE(SUM(amount), 0) FROM transactions
                WHERE type = 'SALE_RETURN' AND status = 'COMPLETED' AND date = :date) AS returns,
            (SELECT COALESCE(SUM(e.amount), 0) FROM expenses e JOIN transactions t ON e.transaction_id = t.id
                WHERE t.status = 'COMPLETED' AND t.date = :date) AS expenses,
            (SELECT CAST(COALESCE(SUM(si.quantity * si.unit_cost_snapshot), 0) AS INTEGER)
                FROM sale_items si JOIN sales s ON si.sale_id = s.id JOIN transactions t ON s.transaction_id = t.id
                WHERE t.status = 'COMPLETED' AND t.date = :date) AS cogs,
            (SELECT COALESCE(SUM(sr.cost_reversed), 0) FROM sale_returns sr JOIN transactions t ON sr.transaction_id = t.id
                WHERE t.status = 'COMPLETED' AND t.date = :date) AS returns_cost
        """
    )
    fun observeDashboardAggregate(date: Long): Flow<DashboardAggregate>

    @Query(
        """
        SELECT c.id AS contact_id, c.name AS name, c.phone AS phone, c.balance AS balance,
            (SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
                WHERE t.contact_id = c.id AND t.type IN ('CASH_SALE', 'CREDIT_SALE', 'MIXED_SALE') AND t.status = 'COMPLETED')
            - (SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
                WHERE t.contact_id = c.id AND t.type = 'SALE_RETURN' AND t.status = 'COMPLETED') AS total_sales,
            (SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
                WHERE t.contact_id = c.id AND t.type = 'CUSTOMER_RECEIPT' AND t.status = 'COMPLETED') AS total_receipts,
            (SELECT MAX(t.created_at) FROM transactions t WHERE t.contact_id = c.id) AS last_activity
        FROM contacts c
        WHERE c.contact_type IN ('CUSTOMER', 'BOTH')
        ORDER BY c.balance DESC, c.name
        """
    )
    suspend fun customerReport(): List<CustomerReportRecord>

    @Query(
        """
        SELECT c.id AS contact_id, c.name AS name, c.phone AS phone, c.balance AS balance,
            (SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
                WHERE t.contact_id = c.id AND t.type = 'PURCHASE' AND t.status = 'COMPLETED') AS total_purchases,
            (SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
                WHERE t.contact_id = c.id AND t.type = 'SUPPLIER_PAYMENT' AND t.status = 'COMPLETED') AS total_payments,
            (SELECT MAX(t.created_at) FROM transactions t WHERE t.contact_id = c.id) AS last_activity
        FROM contacts c
        WHERE c.contact_type IN ('SUPPLIER', 'BOTH')
        ORDER BY c.balance ASC, c.name
        """
    )
    suspend fun supplierReport(): List<SupplierReportRecord>

    @Query(
        """
        SELECT si.product_id AS product_id, si.quantity AS quantity, si.total_amount AS total_amount,
               si.unit_cost_snapshot AS unit_cost_snapshot, si.returned_quantity AS returned_quantity
        FROM sale_items si
        JOIN sales s ON si.sale_id = s.id
        JOIN transactions t ON s.transaction_id = t.id
        WHERE t.status = 'COMPLETED'
        """
    )
    suspend fun allSaleItems(): List<SaleItemAggregateRow>

    @Query(
        """
        SELECT sri.product_id AS product_id, sri.quantity AS quantity, sri.amount AS amount, sri.unit_cost AS unit_cost
        FROM sale_return_items sri
        JOIN sale_returns sr ON sri.sale_return_id = sr.id
        JOIN transactions t ON sr.transaction_id = t.id
        WHERE t.status = 'COMPLETED'
        """
    )
    suspend fun allReturnItems(): List<ReturnItemAggregateRow>
}
