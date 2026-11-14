package ir.hesabyar.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ir.hesabyar.app.ui.contacts.ContactsScreen
import ir.hesabyar.app.ui.dashboard.DashboardScreen
import ir.hesabyar.app.ui.products.ProductsScreen
import ir.hesabyar.app.ui.reports.ReportsScreen
import ir.hesabyar.app.ui.transactions.TransactionsScreen

enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("خانه", Icons.Rounded.Home),
    TRANSACTIONS("تراکنش‌ها", Icons.Rounded.ReceiptLong),
    PRODUCTS("کالاها", Icons.Rounded.Inventory2),
    CONTACTS("اشخاص", Icons.Rounded.People),
    REPORTS("گزارش‌ها", Icons.Rounded.BarChart)
}

@Composable
fun MainShell(
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenSale: () -> Unit,
    onOpenExpense: () -> Unit,
    onOpenPurchase: () -> Unit,
    onOpenReceive: () -> Unit,
    onOpenPay: () -> Unit,
    onNewContact: () -> Unit,
    onNewProduct: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenContact: (Long) -> Unit,
    onOpenProduct: (Long) -> Unit,
    onOpenReminders: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }

    BackHandler(enabled = tab != MainTab.HOME) { tab = MainTab.HOME }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                MainTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                MainTab.HOME -> DashboardScreen(
                    onOpenSettings = onOpenSettings,
                    onOpenNotifications = onOpenNotifications,
                    onOpenAccounts = onOpenAccounts,
                    onOpenSale = onOpenSale,
                    onOpenExpense = onOpenExpense,
                    onOpenReceive = onOpenReceive,
                    onOpenPay = onOpenPay,
                    onNewContact = onNewContact,
                    onNewProduct = onNewProduct,
                    onOpenTransactions = { tab = MainTab.TRANSACTIONS },
                    onOpenTransaction = onOpenTransaction,
                    onOpenDailyReport = { tab = MainTab.REPORTS },
                    onOpenReceivables = { tab = MainTab.REPORTS },
                    onOpenPayables = { tab = MainTab.REPORTS },
                    onOpenLowStock = { tab = MainTab.PRODUCTS },
                    onOpenReminders = onOpenReminders
                )
                MainTab.TRANSACTIONS -> TransactionsScreen(
                    onOpenTransaction = onOpenTransaction,
                    onNewSale = onOpenSale,
                    onNewExpense = onOpenExpense,
                    onNewPurchase = onOpenPurchase,
                    onReceive = onOpenReceive,
                    onPay = onOpenPay
                )
                MainTab.PRODUCTS -> ProductsScreen(
                    onOpenProduct = onOpenProduct,
                    onAddProduct = onNewProduct
                )
                MainTab.CONTACTS -> ContactsScreen(
                    onOpenContact = onOpenContact,
                    onAddContact = onNewContact
                )
                MainTab.REPORTS -> ReportsScreen(onOpenContact = onOpenContact)
            }
        }
    }
}
