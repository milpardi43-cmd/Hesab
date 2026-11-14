package ir.hesabyar.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.EmptyState
import ir.hesabyar.core.design.components.LoadingScreen
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.components.StatCard
import ir.hesabyar.core.design.components.TransactionListItem
import ir.hesabyar.core.design.theme.DeepGreenDark
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.DashboardData

@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenSale: () -> Unit,
    onOpenExpense: () -> Unit,
    onOpenReceive: () -> Unit,
    onOpenPay: () -> Unit,
    onNewContact: () -> Unit,
    onNewProduct: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenDailyReport: () -> Unit,
    onOpenReceivables: () -> Unit,
    onOpenPayables: () -> Unit,
    onOpenLowStock: () -> Unit,
    onOpenReminders: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val data by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        val dashboard = data
        if (dashboard == null) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingScreen() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Header(dashboard, onOpenNotifications, onOpenSettings) }
                item { CashCard(dashboard, onOpenAccounts) }
                item { StatsGrid(dashboard, onOpenDailyReport) }
                item { DebtsRow(dashboard, onOpenReceivables, onOpenPayables) }
                if (dashboard.lowStockCount > 0 || dashboard.pendingReminders > 0) {
                    item {
                        AlertsRow(
                            lowStockCount = dashboard.lowStockCount,
                            reminders = dashboard.pendingReminders,
                            onOpenLowStock = onOpenLowStock,
                            onOpenReminders = onOpenReminders
                        )
                    }
                }
                item { SectionHeader(title = "عملیات سریع") }
                item {
                    QuickActions(
                        onOpenSale = onOpenSale,
                        onOpenExpense = onOpenExpense,
                        onOpenReceive = onOpenReceive,
                        onOpenPay = onOpenPay,
                        onNewContact = onNewContact,
                        onNewProduct = onNewProduct
                    )
                }
                item {
                    SectionHeader(
                        title = "آخرین فعالیت‌ها",
                        actionLabel = "همه",
                        onAction = onOpenTransactions
                    )
                }
                if (!dashboard.hasAnyActivity) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.ReceiptLong,
                            title = "هنوز تراکنشی ثبت نشده",
                            message = "با ثبت اولین فروش، داشبورد شما زنده می‌شود و گزارش‌ها به‌صورت خودکار ساخته می‌شوند.",
                            actionLabel = "ثبت فروش",
                            onAction = onOpenSale
                        )
                    }
                } else {
                    items(dashboard.recentTransactions.take(6), key = { it.id }) { transaction ->
                        TransactionListItem(transaction = transaction, onClick = { onOpenTransaction(transaction.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(data: DashboardData, onOpenNotifications: () -> Unit, onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "سلام، خوش آمدید",
                style = MaterialTheme.typography.labelMedium,
                color = HesabyarTheme.colors.onSurfaceSecondary
            )
            Text(
                text = data.businessName.ifBlank { "حسابیار" },
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = JalaliDate.today().long(),
                style = MaterialTheme.typography.labelSmall,
                color = HesabyarTheme.colors.onSurfaceSecondary
            )
        }
        Box {
            IconButton(onClick = onOpenNotifications) {
                Icon(Icons.Rounded.NotificationsNone, contentDescription = "یادآوری‌ها")
            }
            if (data.pendingReminders > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(HesabyarTheme.colors.expense),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = PersianText.toPersianDigits(data.pendingReminders.toString()),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "تنظیمات")
        }
    }
}

@Composable
private fun CashCard(data: DashboardData, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, DeepGreenDark)))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    "موجودی صندوق و حساب‌ها",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(Modifier.weight(1f))
                Text("مدیریت", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
            }
            Spacer(Modifier.height(14.dp))
            MoneyText(
                amount = data.cashBalance,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                showCurrency = true
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("کل دریافت‌ها", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    MoneyText(amount = data.totalReceived, style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
                Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.2f)))
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text("کل پرداخت‌ها", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    MoneyText(amount = data.totalPaid, style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(data: DashboardData, onOpenDailyReport: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "فروش امروز",
                amount = data.todaySales,
                icon = Icons.Rounded.ShoppingCart,
                tint = HesabyarTheme.colors.income,
                modifier = Modifier.weight(1f),
                onClick = onOpenDailyReport
            )
            StatCard(
                title = "دریافتی امروز",
                amount = data.todayReceived,
                icon = Icons.Rounded.CallReceived,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = onOpenDailyReport
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "هزینه امروز",
                amount = data.todayExpenses,
                icon = Icons.Rounded.TrendingDown,
                tint = HesabyarTheme.colors.expense,
                modifier = Modifier.weight(1f),
                onClick = onOpenDailyReport
            )
            StatCard(
                title = "سود امروز",
                amount = data.todayProfit,
                icon = Icons.Rounded.TrendingUp,
                tint = if (data.todayProfit >= 0) HesabyarTheme.colors.income else HesabyarTheme.colors.expense,
                modifier = Modifier.weight(1f),
                onClick = onOpenDailyReport
            )
        }
    }
}

@Composable
private fun DebtsRow(data: DashboardData, onReceivables: () -> Unit, onPayables: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DebtCard(
            title = "طلب از مشتریان",
            amount = data.receivable,
            icon = Icons.Rounded.ArrowDownward,
            tint = HesabyarTheme.colors.receivable,
            modifier = Modifier.weight(1f),
            onClick = onReceivables
        )
        DebtCard(
            title = "بدهی به تأمین‌کنندگان",
            amount = data.payable,
            icon = Icons.Rounded.ArrowUpward,
            tint = HesabyarTheme.colors.payable,
            modifier = Modifier.weight(1f),
            onClick = onPayables
        )
    }
}

@Composable
private fun DebtCard(
    title: String,
    amount: Long,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AppCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary, maxLines = 1)
            }
            MoneyText(amount = amount, style = MaterialTheme.typography.titleSmall, color = tint)
        }
    }
}

@Composable
private fun AlertsRow(lowStockCount: Int, reminders: Int, onOpenLowStock: () -> Unit, onOpenReminders: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (reminders > 0) {
            AlertBanner(
                icon = Icons.Rounded.NotificationsNone,
                text = "${PersianText.formatNumber(reminders.toLong())} یادآوری سررسید در انتظار است",
                onClick = onOpenReminders
            )
        }
        if (lowStockCount > 0) {
            AlertBanner(
                icon = Icons.Rounded.WarningAmber,
                text = "${PersianText.formatNumber(lowStockCount.toLong())} کالا به حداقل موجودی رسیده است",
                onClick = onOpenLowStock
            )
        }
    }
}

@Composable
private fun AlertBanner(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(HesabyarTheme.colors.warningContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = HesabyarTheme.colors.warning, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text("مشاهده", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun QuickActions(
    onOpenSale: () -> Unit,
    onOpenExpense: () -> Unit,
    onOpenReceive: () -> Unit,
    onOpenPay: () -> Unit,
    onNewContact: () -> Unit,
    onNewProduct: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction(Icons.Rounded.ShoppingCart, "فروش", MaterialTheme.colorScheme.primary, Modifier.weight(1f), onOpenSale)
            QuickAction(Icons.Rounded.TrendingDown, "هزینه", HesabyarTheme.colors.expense, Modifier.weight(1f), onOpenExpense)
            QuickAction(Icons.Rounded.CallReceived, "دریافت", HesabyarTheme.colors.income, Modifier.weight(1f), onOpenReceive)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction(Icons.Rounded.Payments, "پرداخت", HesabyarTheme.colors.payable, Modifier.weight(1f), onOpenPay)
            QuickAction(Icons.Rounded.PersonAdd, "مشتری جدید", Color(0xFF6D5BD0), Modifier.weight(1f), onNewContact)
            QuickAction(Icons.Rounded.Inventory2, "کالای جدید", Color(0xFFB4690E), Modifier.weight(1f), onNewProduct)
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, HesabyarTheme.colors.hairline)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
