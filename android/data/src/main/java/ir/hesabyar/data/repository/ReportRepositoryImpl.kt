package ir.hesabyar.data.repository

import ir.hesabyar.core.database.dao.ProductDao
import ir.hesabyar.core.database.dao.ReportDao
import ir.hesabyar.data.mapper.toDomain
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.ChartPoint
import ir.hesabyar.domain.model.CustomerReportRow
import ir.hesabyar.domain.model.DailyReport
import ir.hesabyar.domain.model.MonthlyReport
import ir.hesabyar.domain.model.ProductReportRow
import ir.hesabyar.domain.model.SalesTrend
import ir.hesabyar.domain.model.SupplierReportRow
import ir.hesabyar.domain.repository.ReportRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val reportDao: ReportDao,
    private val productDao: ProductDao
) : ReportRepository {

    override suspend fun daily(date: Long): DailyReport {
        val from = date
        val to = JalaliDate.fromEpochMillis(date).plusDays(1).toEpochMillis()
        val sales = reportDao.salesAggregate(from, to)
        val returns = reportDao.returns(from, to)
        val receipts = reportDao.receipts(from, to)
        val expenses = reportDao.expenses(from, to)
        val cogs = reportDao.cogs(from, to) - reportDao.returnsCost(from, to)
        val netSales = sales.gross - sales.discount - returns
        val grossProfit = netSales - cogs
        return DailyReport(
            date = date,
            salesCount = sales.salesCount,
            grossSales = sales.gross,
            discount = sales.discount,
            returns = returns,
            netSales = netSales,
            cashReceived = sales.paid,
            creditSales = sales.remaining,
            receiptsFromDebts = receipts,
            operatingExpenses = expenses,
            costOfGoodsSold = cogs,
            grossProfit = grossProfit,
            netProfit = grossProfit - expenses
        )
    }

    override suspend fun monthly(year: Int, month: Int): MonthlyReport {
        val from = JalaliDate.startOfMonthMillis(year, month)
        val to = JalaliDate.startOfNextMonthMillis(year, month)
        val sales = reportDao.salesAggregate(from, to)
        val returns = reportDao.returns(from, to)
        val expenses = reportDao.expenses(from, to)
        val cogs = reportDao.cogs(from, to) - reportDao.returnsCost(from, to)
        val netSales = sales.gross - sales.discount - returns
        val grossProfit = netSales - cogs

        val salesByDate = reportDao.salesByDate(from, to).associate { it.date to it.amount }
        val returnsByDate = reportDao.returnsByDate(from, to).associate { it.date to it.amount }
        val expensesByDate = reportDao.expensesByDate(from, to).associate { it.date to it.amount }
        val cogsByDate = reportDao.cogsByDate(from, to).associate { it.date to it.amount }
        val returnsCostByDate = reportDao.returnsCostByDate(from, to).associate { it.date to it.amount }

        val days = JalaliDate.monthDays(year, month)
        val dailySales = mutableListOf<ChartPoint>()
        val dailyExpenses = mutableListOf<ChartPoint>()
        val dailyProfit = mutableListOf<ChartPoint>()
        days.forEach { day ->
            val key = day.toEpochMillis()
            val daySales = (salesByDate[key] ?: 0L) - (returnsByDate[key] ?: 0L)
            val dayExpenses = expensesByDate[key] ?: 0L
            val dayCogs = (cogsByDate[key] ?: 0L) - (returnsCostByDate[key] ?: 0L)
            val label = PersianText.toPersianDigits(day.day.toString())
            dailySales += ChartPoint(label = label, value = daySales, key = day.epochDay)
            dailyExpenses += ChartPoint(label = label, value = dayExpenses, key = day.epochDay)
            dailyProfit += ChartPoint(label = label, value = daySales - dayCogs - dayExpenses, key = day.epochDay)
        }

        return MonthlyReport(
            year = year,
            month = month,
            grossSales = sales.gross,
            returns = returns,
            netSales = netSales,
            cashReceived = sales.paid,
            creditSales = sales.remaining,
            operatingExpenses = expenses,
            costOfGoodsSold = cogs,
            grossProfit = grossProfit,
            netProfit = grossProfit - expenses,
            salesCount = sales.salesCount,
            dailySales = dailySales,
            dailyExpenses = dailyExpenses,
            dailyProfit = dailyProfit
        )
    }

    override suspend fun customers(): List<CustomerReportRow> =
        reportDao.customerReport().map {
            CustomerReportRow(
                contactId = it.contactId,
                name = it.name,
                phone = it.phone,
                balance = it.balance,
                totalSales = it.totalSales,
                totalReceipts = it.totalReceipts,
                lastActivity = it.lastActivity
            )
        }

    override suspend fun suppliers(): List<SupplierReportRow> =
        reportDao.supplierReport().map {
            SupplierReportRow(
                contactId = it.contactId,
                name = it.name,
                phone = it.phone,
                balance = it.balance,
                totalPurchases = it.totalPurchases,
                totalPayments = it.totalPayments,
                lastActivity = it.lastActivity
            )
        }

    override suspend fun products(): List<ProductReportRow> {
        val products = productDao.getAll().map { it.toDomain() }
        val saleItems = reportDao.allSaleItems()
        val returnItems = reportDao.allReturnItems()

        data class Acc(var qty: Double = 0.0, var sales: Long = 0, var cost: Long = 0)
        val acc = mutableMapOf<Long, Acc>()
        saleItems.forEach { item ->
            val a = acc.getOrPut(item.productId) { Acc() }
            a.qty += item.quantity
            a.sales += item.totalAmount
            a.cost += (item.quantity * item.unitCost).toLong()
        }
        returnItems.forEach { item ->
            val a = acc.getOrPut(item.productId) { Acc() }
            a.qty -= item.quantity
            a.sales -= item.amount
            a.cost -= (item.quantity * item.unitCost).toLong()
        }

        return products.map { product ->
            val a = acc[product.id] ?: Acc()
            ProductReportRow(
                productId = product.id,
                name = product.name,
                soldQuantity = a.qty,
                salesAmount = a.sales,
                profit = a.sales - a.cost,
                stockQuantity = product.stockQuantity,
                averageCost = product.averageCost,
                stockValue = product.stockValue,
                minimumStock = product.minimumStock
            )
        }.sortedWith(compareByDescending<ProductReportRow> { it.salesAmount }.thenBy { it.name })
    }

    override suspend fun salesTrend(days: Int): SalesTrend {
        val safeDays = days.coerceIn(2, 90)
        val today = JalaliDate.today()
        val start = today.plusDays(-(safeDays - 1).toLong())
        val from = start.toEpochMillis()
        val to = today.plusDays(1).toEpochMillis()
        val salesByDate = reportDao.salesByDate(from, to).associate { it.date to it.amount }
        val returnsByDate = reportDao.returnsByDate(from, to).associate { it.date to it.amount }
        val expensesByDate = reportDao.expensesByDate(from, to).associate { it.date to it.amount }
        val cogsByDate = reportDao.cogsByDate(from, to).associate { it.date to it.amount }
        val returnsCostByDate = reportDao.returnsCostByDate(from, to).associate { it.date to it.amount }

        val sales = mutableListOf<ChartPoint>()
        val expenses = mutableListOf<ChartPoint>()
        val profit = mutableListOf<ChartPoint>()
        repeat(safeDays) { index ->
            val day = start.plusDays(index.toLong())
            val key = day.toEpochMillis()
            val daySales = (salesByDate[key] ?: 0L) - (returnsByDate[key] ?: 0L)
            val dayExpenses = expensesByDate[key] ?: 0L
            val dayCogs = (cogsByDate[key] ?: 0L) - (returnsCostByDate[key] ?: 0L)
            val label = PersianText.toPersianDigits(day.day.toString())
            sales += ChartPoint(label = label, value = daySales, key = day.epochDay)
            expenses += ChartPoint(label = label, value = dayExpenses, key = day.epochDay)
            profit += ChartPoint(label = label, value = daySales - dayCogs - dayExpenses, key = day.epochDay)
        }
        return SalesTrend(sales = sales, expenses = expenses, profit = profit)
    }
}
