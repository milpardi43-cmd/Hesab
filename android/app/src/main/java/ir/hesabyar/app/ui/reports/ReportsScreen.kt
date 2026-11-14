package ir.hesabyar.app.ui.reports

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.EmptyState
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.InfoRow
import ir.hesabyar.core.design.components.JalaliDateField
import ir.hesabyar.core.design.components.LoadingScreen
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.components.SimpleBarChart
import ir.hesabyar.core.design.components.StatCard
import ir.hesabyar.core.design.components.StatusBadge
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.CustomerReportRow
import ir.hesabyar.domain.model.DailyReport
import ir.hesabyar.domain.model.MonthlyReport
import ir.hesabyar.domain.model.ProductReportRow
import ir.hesabyar.domain.model.SupplierReportRow

@Composable
fun ReportsScreen(
    onOpenContact: (Long) -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HesabyarTopBar(
                title = "گزارش‌ها",
                subtitle = "تصویر دقیق از پول و سود فروشگاه",
                actions = {
                    state.daily?.let { daily ->
                        IconButton(
                            onClick = {
                                val text = buildString {
                                    appendLine("گزارش روزانه — ${JalaliDate.formatLongDate(daily.date)}")
                                    appendLine("فروش خالص: ${PersianText.formatMoney(daily.netSales)}")
                                    appendLine("دریافتی نقدی: ${PersianText.formatMoney(daily.cashReceived)}")
                                    appendLine("فروش نسیه: ${PersianText.formatMoney(daily.creditSales)}")
                                    appendLine("هزینه‌ها: ${PersianText.formatMoney(daily.operatingExpenses)}")
                                    appendLine("سود ناخالص: ${PersianText.formatMoney(daily.grossProfit)}")
                                    appendLine("سود خالص: ${PersianText.formatMoney(daily.netProfit)}")
                                    append("ساخته‌شده با حسابیار")
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "اشتراک گزارش"))
                            }
                        ) { Icon(Icons.Rounded.Share, contentDescription = "اشتراک گزارش") }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = state.tab.ordinal,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.background,
                divider = {}
            ) {
                ReportTab.entries.forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = { Text(tab.label) }
                    )
                }
            }
            if (state.loading && state.daily == null) {
                LoadingScreen()
            } else {
                when (state.tab) {
                    ReportTab.DAILY -> DailyTab(state.daily, state.date, viewModel::setDate)
                    ReportTab.MONTHLY -> MonthlyTab(state.monthly, viewModel::previousMonth, viewModel::nextMonth)
                    ReportTab.CUSTOMERS -> CustomersTab(state.customers, onOpenContact)
                    ReportTab.SUPPLIERS -> SuppliersTab(state.suppliers, onOpenContact)
                    ReportTab.PRODUCTS -> ProductsTab(state.products)
                    ReportTab.ACCOUNTS -> AccountsTab(state.accounts, state.trend?.sales.orEmpty())
                }
            }
        }
    }
}

@Composable
private fun DailyTab(report: DailyReport?, date: Long, onDate: (Long) -> Unit) {
    if (report == null) {
        EmptyState(title = "گزارشی موجود نیست", message = "پس از ثبت تراکنش، گزارش روزانه ساخته می‌شود.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { JalaliDateField(label = "تاریخ گزارش", millis = date, onSelect = onDate) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("فروش خالص", report.netSales, Icons.Rounded.ChevronRight, HesabyarTheme.colors.income, Modifier.weight(1f))
                StatCard("سود خالص", report.netProfit, Icons.Rounded.ChevronRight, if (report.netProfit >= 0) HesabyarTheme.colors.income else HesabyarTheme.colors.expense, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("دریافتی نقدی", report.cashReceived, Icons.Rounded.ChevronRight, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatCard("هزینه‌ها", report.operatingExpenses, Icons.Rounded.ChevronRight, HesabyarTheme.colors.expense, Modifier.weight(1f))
            }
        }
        item {
            SectionHeader(title = "جزئیات محاسبه")
            AppCard {
                Column {
                    InfoRow("تعداد فروش", value = PersianText.formatNumber(report.salesCount.toLong()))
                    InfoRow("فروش ناخالص", value = PersianText.formatMoney(report.grossSales))
                    InfoRow("تخفیف فروش", value = PersianText.formatMoney(report.discount))
                    InfoRow("برگشت از فروش", value = PersianText.formatMoney(report.returns))
                    InfoRow("بهای تمام‌شده کالای فروش‌رفته", value = PersianText.formatMoney(report.costOfGoodsSold))
                    InfoRow("سود ناخالص", value = PersianText.formatMoney(report.grossProfit), valueColor = HesabyarTheme.colors.income)
                    InfoRow("هزینه‌های عملیاتی", value = PersianText.formatMoney(report.operatingExpenses), valueColor = HesabyarTheme.colors.expense)
                    InfoRow("سود خالص", value = PersianText.formatMoney(report.netProfit), valueColor = if (report.netProfit >= 0) HesabyarTheme.colors.income else HesabyarTheme.colors.expense)
                }
            }
        }
        item {
            AppCard {
                Column {
                    InfoRow("فروش نسیه امروز", value = PersianText.formatMoney(report.creditSales), valueColor = HesabyarTheme.colors.warning)
                    InfoRow("وصول طلب‌های قبلی", value = PersianText.formatMoney(report.receiptsFromDebts))
                    Text(
                        "دریافت طلب قبلی درآمد امروز محسوب نمی‌شود؛ فقط صندوق را افزایش می‌دهد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = HesabyarTheme.colors.onSurfaceSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyTab(report: MonthlyReport?, onPrevious: () -> Unit, onNext: () -> Unit) {
    if (report == null) {
        EmptyState(title = "گزارشی موجود نیست", message = "داده‌ای برای این ماه ثبت نشده است.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevious) { Icon(Icons.Rounded.ChevronRight, contentDescription = "ماه قبل") }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(JalaliDate.monthName(report.month), style = MaterialTheme.typography.titleMedium)
                        Text(PersianText.toPersianDigits(report.year.toString()), style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                    }
                    IconButton(onClick = onNext) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "ماه بعد") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("فروش خالص", report.netSales, Icons.Rounded.ChevronRight, HesabyarTheme.colors.income, Modifier.weight(1f))
                StatCard("سود خالص", report.netProfit, Icons.Rounded.ChevronRight, if (report.netProfit >= 0) HesabyarTheme.colors.income else HesabyarTheme.colors.expense, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("دریافتی نقدی", report.cashReceived, Icons.Rounded.ChevronRight, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatCard("هزینه‌ها", report.operatingExpenses, Icons.Rounded.ChevronRight, HesabyarTheme.colors.expense, Modifier.weight(1f))
            }
        }
        item { SectionHeader(title = "نمودار فروش روزانه") }
        item {
            AppCard {
                SimpleBarChart(points = report.dailySales, barColor = HesabyarTheme.colors.income)
            }
        }
        item { SectionHeader(title = "نمودار هزینه روزانه") }
        item {
            AppCard {
                SimpleBarChart(points = report.dailyExpenses, barColor = HesabyarTheme.colors.expense)
            }
        }
        item { SectionHeader(title = "نمودار سود روزانه") }
        item {
            AppCard {
                SimpleBarChart(points = report.dailyProfit, barColor = MaterialTheme.colorScheme.primary)
            }
        }
        item {
            AppCard {
                Column {
                    InfoRow("فروش ناخالص", value = PersianText.formatMoney(report.grossSales))
                    InfoRow("برگشت از فروش", value = PersianText.formatMoney(report.returns))
                    InfoRow("بهای تمام‌شده", value = PersianText.formatMoney(report.costOfGoodsSold))
                    InfoRow("سود ناخالص", value = PersianText.formatMoney(report.grossProfit))
                    InfoRow("فروش نسیه", value = PersianText.formatMoney(report.creditSales))
                }
            }
        }
    }
}

@Composable
private fun CustomersTab(rows: List<CustomerReportRow>, onOpenContact: (Long) -> Unit) {
    if (rows.isEmpty()) {
        EmptyState(title = "مشتری ثبت نشده", message = "با ثبت مشتری و فروش نسیه، گزارش بدهی مشتریان ساخته می‌شود.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rows, key = { it.contactId }) { row ->
            AppCard(onClick = { onOpenContact(row.contactId) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "فروش: ${PersianText.formatMoney(row.totalSales)} · دریافت: ${PersianText.formatMoney(row.totalReceipts)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        MoneyText(
                            amount = kotlin.math.abs(row.balance),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (row.balance > 0) HesabyarTheme.colors.receivable else HesabyarTheme.colors.neutral
                        )
                        Text(
                            if (row.balance > 0) "بدهکار" else "تسویه",
                            style = MaterialTheme.typography.labelSmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuppliersTab(rows: List<SupplierReportRow>, onOpenContact: (Long) -> Unit) {
    if (rows.isEmpty()) {
        EmptyState(title = "تأمین‌کننده‌ای ثبت نشده", message = "با ثبت تأمین‌کننده و خرید، گزارش بدهی به تأمین‌کنندگان ساخته می‌شود.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rows, key = { it.contactId }) { row ->
            AppCard(onClick = { onOpenContact(row.contactId) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "خرید: ${PersianText.formatMoney(row.totalPurchases)} · پرداخت: ${PersianText.formatMoney(row.totalPayments)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        MoneyText(
                            amount = kotlin.math.abs(row.balance),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (row.balance < 0) HesabyarTheme.colors.payable else HesabyarTheme.colors.neutral
                        )
                        Text(
                            if (row.balance < 0) "بستانکار" else "تسویه",
                            style = MaterialTheme.typography.labelSmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductsTab(rows: List<ProductReportRow>) {
    if (rows.isEmpty()) {
        EmptyState(title = "کالایی ثبت نشده", message = "پس از ثبت کالا و فروش، سود هر کالا نمایش داده می‌شود.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rows, key = { it.productId }) { row ->
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(row.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (row.isLowStock) StatusBadge("کم‌موجود", HesabyarTheme.colors.warning)
                    }
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("فروش", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                            MoneyText(row.salesAmount, style = MaterialTheme.typography.labelLarge)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("سود", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                            MoneyText(row.profit, style = MaterialTheme.typography.labelLarge, color = if (row.profit >= 0) HesabyarTheme.colors.income else HesabyarTheme.colors.expense)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("تعداد فروش", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                            Text(PersianText.formatQuantity(row.soldQuantity), style = MaterialTheme.typography.labelLarge)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("موجودی", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                            Text(
                                PersianText.formatQuantity(row.stockQuantity),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (row.isLowStock) HesabyarTheme.colors.warning else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountsTab(accounts: List<Account>, salesTrend: List<ir.hesabyar.domain.model.ChartPoint>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader(title = "روند فروش ۷ روز اخیر") }
        item {
            AppCard { SimpleBarChart(points = salesTrend, barColor = HesabyarTheme.colors.income, showValue = true) }
        }
        item { SectionHeader(title = "حساب‌ها") }
        if (accounts.isEmpty()) {
            item { EmptyState(title = "حسابی ثبت نشده", message = "حساب صندوق در راه‌اندازی ایجاد می‌شود.") }
        } else {
            items(accounts, key = { it.id }) { account ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(account.title, style = MaterialTheme.typography.bodyLarge)
                            Text(account.type.label, style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                        }
                        MoneyText(account.currentBalance, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
