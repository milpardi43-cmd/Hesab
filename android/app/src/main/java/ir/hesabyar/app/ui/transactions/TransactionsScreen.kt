package ir.hesabyar.app.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabyar.core.design.components.ChipSelector
import ir.hesabyar.core.design.components.DropdownField
import ir.hesabyar.core.design.components.EmptyState
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.JalaliDateField
import ir.hesabyar.core.design.components.LabeledSwitch
import ir.hesabyar.core.design.components.SearchField
import ir.hesabyar.core.design.components.StatusBadge
import ir.hesabyar.core.design.components.TransactionListItem
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.model.PaymentStatus
import ir.hesabyar.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onOpenTransaction: (Long) -> Unit,
    onNewSale: () -> Unit,
    onNewExpense: () -> Unit,
    onNewPurchase: () -> Unit,
    onReceive: () -> Unit,
    onPay: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    val hasActiveFilter = filter.type != null || filter.paymentStatus != null || filter.contactId != null ||
        filter.from != null || filter.to != null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HesabyarTopBar(
                title = "تراکنش‌ها",
                subtitle = "همه گردش‌های مالی فروشگاه",
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Rounded.FilterList, contentDescription = "فیلتر")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showActions = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("ثبت جدید") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(
                    value = filter.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "جست‌وجو در شرح، شماره یا شخص"
                )
                if (hasActiveFilter) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val activeContact = contacts.firstOrNull { it.id == filter.contactId }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            filter.type?.let { StatusBadge(it.label, MaterialTheme.colorScheme.primary) }
                            filter.paymentStatus?.let { StatusBadge(it.label, HesabyarTheme.colors.warning) }
                            filter.contactId?.let { StatusBadge(activeContact?.name ?: "شخص", HesabyarTheme.colors.receivable) }
                        }
                        TextButton(onClick = viewModel::reset) { Text("حذف فیلترها") }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (transactions.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "تراکنشی پیدا نشد",
                    message = if (hasActiveFilter || filter.query.isNotBlank()) "فیلترها را تغییر دهید یا جست‌وجو را پاک کنید." else "اولین فروش یا هزینه را ثبت کنید.",
                    actionLabel = "ثبت فروش",
                    onAction = onNewSale
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionListItem(transaction = transaction, onClick = { onOpenTransaction(transaction.id) })
                    }
                }
            }
        }
    }

    if (showFilters) {
        TransactionFilterSheet(
            currentType = filter.type,
            currentPaymentStatus = filter.paymentStatus,
            currentContactId = filter.contactId,
            currentFrom = filter.from,
            currentTo = filter.to,
            newestFirst = filter.newestFirst,
            contacts = contacts.map { it.id to it.name },
            onApply = { type, status, contactId, from, to, newest ->
                viewModel.apply(type, status, contactId, from, to?.let { it } ?: to, newest)
                showFilters = false
            },
            onReset = {
                viewModel.reset()
                showFilters = false
            },
            onDismiss = { showFilters = false }
        )
    }

    if (showActions) {
        ModalBottomSheet(
            onDismissRequest = { showActions = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
                Text("ثبت تراکنش جدید", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                QuickActionRow(Icons.Rounded.ShoppingCart, "فروش", "ثبت فروش نقدی یا نسیه") { showActions = false; onNewSale() }
                QuickActionRow(Icons.Rounded.TrendingDown, "هزینه", "ثبت هزینه‌های فروشگاه") { showActions = false; onNewExpense() }
                QuickActionRow(Icons.Rounded.Inventory2, "خرید کالا", "افزایش موجودی انبار") { showActions = false; onNewPurchase() }
                QuickActionRow(Icons.Rounded.CallReceived, "دریافت از مشتری", "تسویه طلب") { showActions = false; onReceive() }
                QuickActionRow(Icons.Rounded.Payments, "پرداخت به تأمین‌کننده", "تسویه بدهی") { showActions = false; onPay() }
            }
        }
    }
}

@Composable
private fun QuickActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionFilterSheet(
    currentType: TransactionType?,
    currentPaymentStatus: PaymentStatus?,
    currentContactId: Long?,
    currentFrom: Long?,
    currentTo: Long?,
    newestFirst: Boolean,
    contacts: List<Pair<Long, String>>,
    onApply: (TransactionType?, PaymentStatus?, Long?, Long?, Long?, Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(currentType) }
    var status by remember { mutableStateOf(currentPaymentStatus) }
    var contactId by remember { mutableStateOf(currentContactId) }
    var from by remember { mutableStateOf(currentFrom) }
    var to by remember { mutableStateOf(currentTo) }
    var newest by remember { mutableStateOf(newestFirst) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("فیلتر تراکنش‌ها", style = MaterialTheme.typography.titleMedium)
            Text("نوع تراکنش", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
            ChipSelector(
                options = listOf<TransactionType?>(null) + TransactionType.entries.toList(),
                selected = type,
                onSelect = { type = it },
                optionLabel = { it?.label ?: "همه" }
            )
            Text("وضعیت پرداخت", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
            ChipSelector(
                options = listOf<PaymentStatus?>(null) + PaymentStatus.entries.toList(),
                selected = status,
                onSelect = { status = it },
                optionLabel = { it?.label ?: "همه" }
            )
            DropdownField(
                label = "شخص",
                options = contacts,
                selected = contacts.firstOrNull { it.first == contactId },
                optionLabel = { it.second },
                onSelect = { contactId = it.first },
                placeholder = "همه اشخاص"
            )
            if (contactId != null) TextButton(onClick = { contactId = null }) { Text("حذف فیلتر شخص") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JalaliDateField(label = "از تاریخ", millis = from, onSelect = { from = it }, modifier = Modifier.weight(1f))
                JalaliDateField(label = "تا تاریخ", millis = to, onSelect = { to = it }, modifier = Modifier.weight(1f))
            }
            if (from != null || to != null) {
                TextButton(onClick = { from = null; to = null }) { Text("حذف فیلتر تاریخ") }
            }
            LabeledSwitch(title = "جدیدترین تراکنش‌ها اول", checked = newest, onCheckedChange = { newest = it })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                TextButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("پاک کردن همه") }
                androidx.compose.material3.Button(
                    onClick = { onApply(type, status, contactId, from, to, newest) },
                    modifier = Modifier.weight(1f)
                ) { Text("اعمال فیلتر") }
            }
        }
    }
}
