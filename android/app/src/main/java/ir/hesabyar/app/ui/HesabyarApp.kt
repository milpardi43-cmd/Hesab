package ir.hesabyar.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.hesabyar.app.ui.accounts.AccountAdjustScreen
import ir.hesabyar.app.ui.accounts.AccountDetailScreen
import ir.hesabyar.app.ui.accounts.AccountsScreen
import ir.hesabyar.app.ui.accounts.TransferScreen
import ir.hesabyar.app.ui.contacts.ContactDetailScreen
import ir.hesabyar.app.ui.contacts.ContactFormScreen
import ir.hesabyar.app.ui.contacts.ContactsScreen
import ir.hesabyar.app.ui.contacts.OpeningBalanceScreen
import ir.hesabyar.app.ui.contacts.PaymentScreen
import ir.hesabyar.app.ui.expenses.ExpenseScreen
import ir.hesabyar.app.ui.onboarding.OnboardingScreen
import ir.hesabyar.app.ui.onboarding.SetupScreen
import ir.hesabyar.app.ui.onboarding.SplashScreen
import ir.hesabyar.app.ui.products.ProductDetailScreen
import ir.hesabyar.app.ui.products.ProductFormScreen
import ir.hesabyar.app.ui.products.StockAdjustScreen
import ir.hesabyar.app.ui.purchases.PurchaseScreen
import ir.hesabyar.app.ui.reminders.RemindersScreen
import ir.hesabyar.app.ui.sales.ReturnScreen
import ir.hesabyar.app.ui.sales.SaleScreen
import ir.hesabyar.app.ui.settings.AboutScreen
import ir.hesabyar.app.ui.settings.DataManagementScreen
import ir.hesabyar.app.ui.settings.SettingsScreen
import ir.hesabyar.app.ui.settings.SubscriptionScreen
import ir.hesabyar.app.ui.transactions.TransactionDetailScreen
import ir.hesabyar.core.design.theme.HesabyarTheme

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val SETUP = "setup"
    const val MAIN = "main"
    const val SALE = "sale"
    const val PURCHASE = "purchase"
    const val EXPENSE = "expense"
    const val ACCOUNTS = "accounts"
    const val REMINDERS = "reminders"
    const val SETTINGS = "settings"
    const val DATA_MANAGEMENT = "dataManagement"
    const val SUBSCRIPTION = "subscription"
    const val ABOUT = "about"

    const val TRANSACTION = "transaction/{transactionId}"
    fun transaction(id: Long) = "transaction/$id"

    const val RETURN = "return/{transactionId}"
    fun returnSale(id: Long) = "return/$id"

    const val CONTACT = "contact/{contactId}"
    fun contact(id: Long) = "contact/$id"

    const val CONTACT_FORM = "contactForm?contactId={contactId}"
    fun contactForm(id: Long = 0) = "contactForm?contactId=$id"

    const val PRODUCT = "product/{productId}"
    fun product(id: Long) = "product/$id"

    const val PRODUCT_FORM = "productForm?productId={productId}"
    fun productForm(id: Long = 0) = "productForm?productId=$id"

    const val STOCK_ADJUST = "stockAdjust/{productId}"
    fun stockAdjust(id: Long) = "stockAdjust/$id"

    const val ACCOUNT = "account/{accountId}"
    fun account(id: Long) = "account/$id"

    const val ACCOUNT_ADJUST = "accountAdjust/{accountId}"
    fun accountAdjust(id: Long) = "accountAdjust/$id"

    const val OPENING_BALANCE = "openingBalance/{contactId}"
    fun openingBalance(id: Long) = "openingBalance/$id"

    const val TRANSFER = "transfer?fromId={fromId}"
    fun transfer(fromId: Long = -1) = "transfer?fromId=$fromId"

    const val PAYMENT = "payment?mode={mode}&contactId={contactId}&amount={amount}"
    fun payment(mode: String, contactId: Long? = null, amount: Long = 0) =
        "payment?mode=$mode&contactId=${contactId ?: -1}&amount=$amount"
}

@Composable
fun HesabyarApp(viewModel: AppViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    HesabyarTheme(
        themeMode = settings.themeMode,
        textScale = settings.textScale,
        persianDigits = settings.persianDigits
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = Routes.SPLASH) {
                composable(Routes.SPLASH) {
                    SplashScreen(
                        onReady = { hasBusiness ->
                            navController.navigate(if (hasBusiness) Routes.MAIN else Routes.ONBOARDING) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onFinish = {
                            navController.navigate(Routes.SETUP) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.SETUP) {
                    SetupScreen(
                        onDone = {
                            navController.navigate(Routes.MAIN) {
                                popUpTo(Routes.SETUP) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.MAIN) {
                    MainShell(
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenNotifications = { navController.navigate(Routes.REMINDERS) },
                        onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                        onOpenSale = { navController.navigate(Routes.SALE) },
                        onOpenExpense = { navController.navigate(Routes.EXPENSE) },
                        onOpenPurchase = { navController.navigate(Routes.PURCHASE) },
                        onOpenReceive = { navController.navigate(Routes.payment("receive")) },
                        onOpenPay = { navController.navigate(Routes.payment("pay")) },
                        onNewContact = { navController.navigate(Routes.contactForm()) },
                        onNewProduct = { navController.navigate(Routes.productForm()) },
                        onOpenTransaction = { navController.openTransaction(it) },
                        onOpenContact = { navController.navigate(Routes.contact(it)) },
                        onOpenProduct = { navController.navigate(Routes.product(it)) },
                        onOpenReminders = { navController.navigate(Routes.REMINDERS) }
                    )
                }
                composable(Routes.SALE) {
                    SaleScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.openTransaction(it) }
                    )
                }
                composable(Routes.PURCHASE) {
                    PurchaseScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.openTransaction(it) }
                    )
                }
                composable(Routes.EXPENSE) {
                    ExpenseScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.openTransaction(it) }
                    )
                }
                composable(
                    route = Routes.PAYMENT,
                    arguments = listOf(
                        navArgument("mode") { type = NavType.StringType; defaultValue = "receive" },
                        navArgument("contactId") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("amount") { type = NavType.LongType; defaultValue = 0L }
                    )
                ) {
                    PaymentScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.openTransaction(it) }
                    )
                }
                composable(
                    route = Routes.TRANSACTION,
                    arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
                ) {
                    TransactionDetailScreen(
                        onBack = { navController.popBackStack() },
                        onOpenContact = { navController.navigate(Routes.contact(it)) },
                        onReturnSale = { navController.navigate(Routes.returnSale(it)) },
                        onSettle = { contactId, amount ->
                            navController.navigate(Routes.payment("receive", contactId, amount))
                        }
                    )
                }
                composable(
                    route = Routes.RETURN,
                    arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
                ) {
                    ReturnScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.openTransaction(it) }
                    )
                }
                composable(
                    route = Routes.CONTACT,
                    arguments = listOf(navArgument("contactId") { type = NavType.LongType })
                ) {
                    ContactDetailScreen(
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(Routes.contactForm(it)) },
                        onReceive = { navController.navigate(Routes.payment("receive", it)) },
                        onPay = { navController.navigate(Routes.payment("pay", it)) },
                        onOpeningBalance = { navController.navigate(Routes.openingBalance(it)) }
                    )
                }
                composable(
                    route = Routes.CONTACT_FORM,
                    arguments = listOf(navArgument("contactId") { type = NavType.LongType; defaultValue = 0L })
                ) {
                    ContactFormScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { id -> navController.navigate(Routes.contact(id)) { popUpTo(Routes.MAIN) } }
                    )
                }
                composable(
                    route = Routes.PRODUCT,
                    arguments = listOf(navArgument("productId") { type = NavType.LongType })
                ) {
                    ProductDetailScreen(
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(Routes.productForm(it)) },
                        onAdjustStock = { navController.navigate(Routes.stockAdjust(it)) }
                    )
                }
                composable(
                    route = Routes.PRODUCT_FORM,
                    arguments = listOf(navArgument("productId") { type = NavType.LongType; defaultValue = 0L })
                ) {
                    ProductFormScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { id -> navController.navigate(Routes.product(id)) { popUpTo(Routes.MAIN) } }
                    )
                }
                composable(
                    route = Routes.STOCK_ADJUST,
                    arguments = listOf(navArgument("productId") { type = NavType.LongType })
                ) {
                    StockAdjustScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }
                composable(Routes.ACCOUNTS) {
                    AccountsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAccount = { navController.navigate(Routes.account(it)) },
                        onTransfer = { navController.navigate(Routes.transfer()) }
                    )
                }
                composable(
                    route = Routes.ACCOUNT,
                    arguments = listOf(navArgument("accountId") { type = NavType.LongType })
                ) {
                    AccountDetailScreen(
                        onBack = { navController.popBackStack() },
                        onAdjust = { navController.navigate(Routes.accountAdjust(it)) },
                        onTransfer = { navController.navigate(Routes.transfer(it)) }
                    )
                }
                composable(
                    route = Routes.ACCOUNT_ADJUST,
                    arguments = listOf(navArgument("accountId") { type = NavType.LongType })
                ) {
                    AccountAdjustScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.openTransaction(it) }
                    )
                }
                composable(
                    route = Routes.TRANSFER,
                    arguments = listOf(navArgument("fromId") { type = NavType.LongType; defaultValue = -1L })
                ) {
                    TransferScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.openTransaction(it) }
                    )
                }
                composable(
                    route = Routes.OPENING_BALANCE,
                    arguments = listOf(navArgument("contactId") { type = NavType.LongType })
                ) {
                    OpeningBalanceScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }
                composable(Routes.REMINDERS) {
                    RemindersScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDataManagement = { navController.navigate(Routes.DATA_MANAGEMENT) },
                        onOpenReminders = { navController.navigate(Routes.REMINDERS) },
                        onOpenSubscription = { navController.navigate(Routes.SUBSCRIPTION) },
                        onOpenAbout = { navController.navigate(Routes.ABOUT) }
                    )
                }
                composable(Routes.DATA_MANAGEMENT) {
                    DataManagementScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.SUBSCRIPTION) {
                    SubscriptionScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

private fun NavHostController.openTransaction(id: Long) {
    navigate(Routes.transaction(id)) {
        popUpTo(Routes.MAIN) { inclusive = false }
        launchSingleTop = true
    }
}
