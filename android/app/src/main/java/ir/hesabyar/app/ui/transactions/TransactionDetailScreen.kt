package ir.hesabyar.app.ui.transactions

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentReturn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabyar.app.ui.common.Receipts
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.EmptyState
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.InfoRow
import ir.hesabyar.core.design.components.LoadingScreen
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.components.SecondaryButton
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.components.StatusBadge
import ir.hesabyar.core.design.components.transactionSign
import ir.hesabyar.core.design.components.transactionTypeColor
import ir.hesabyar.core.design.components.transactionTypeIcon
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.PaymentStatus
import ir.hesabyar.domain.model.TransactionStatus
import ir.hesabyar.domain.model.TransactionType

@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    onOpenContact: (Long) -> Unit,
    onReturnSale: (Long) -> Unit,
    onSettle: (Long?, Long) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showReverseDialog by remember { mutableStateOf(false) }
    var reverseReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HesabyarTopBar(
                title = "جزئیات تراکنش",
                subtitle = state.detail?.transaction?.code,
                onBack = onBack,
                actions = {
                    state.detail?.let { detail ->
                        IconButton(
                            onClick = {
                                val text = Receipts.transactionReceipt(detail, state.businessName)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "اشتراک رسید"))
                            }
                        ) { Icon(Icons.Rounded.Share, contentDescription = "اشتراک رسید") }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val detail = state.detail
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingScreen() }
        } else if (detail == null) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(title = "تراکنش پیدا نشد", message = "ممکن است حذف شده باشد.", actionLabel = "بازگشت", onAction = onBack)
            }
        } else {
            val tx = detail.transaction
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AppCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                Modifier.size(54.dp).clip(CircleShape).background(transactionTypeColor(tx.type).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(transactionTypeIcon(tx.type), contentDescription = null, tint = transactionTypeColor(tx.type), modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(tx.type.label, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            MoneyText(
                                amount = if (transactionSign(tx.type) == "−") -tx.amount else tx.amount,
                                style = MaterialTheme.typography.headlineMedium,
                                color = transactionTypeColor(tx.type),
                                showCurrency = true,
                                signed = transactionSign(tx.type).isNotEmpty()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (tx.status == TransactionStatus.REVERSED) {
                                    StatusBadge("برگشت‌خورده", HesabyarTheme.colors.neutral)
                                }
                                tx.paymentStatus?.let { status ->
                                    val color = when (status) {
                                        PaymentStatus.PAID -> HesabyarTheme.colors.income
                                        PaymentStatus.PARTIAL -> HesabyarTheme.colors.warning
                                        PaymentStatus.UNPAID -> HesabyarTheme.colors.expense
                                    }
                                    StatusBadge(status.label, color)
                                }
                            }
                        }
                    }
                }
                item {
                    AppCard {
                        Column {
                            InfoRow("تاریخ", value = JalaliDate.formatLongDate(tx.date))
                            InfoRow("شماره تراکنش", value = PersianText.toPersianDigits(tx.code))
                            InfoRow("مبلغ کل", value = PersianText.formatMoney(tx.amount))
                            InfoRow("پرداخت‌شده", value = PersianText.formatMoney(tx.paidAmount))
                            if (tx.remainingAmount > 0) {
                                InfoRow("مانده", value = PersianText.formatMoney(tx.remainingAmount), valueColor = HesabyarTheme.colors.warning)
                            }
                            detail.account?.let { InfoRow("حساب", value = it.title) }
                            tx.description?.takeIf { it.isNotBlank() }?.let { InfoRow("توضیحات", value = it) }
                            detail.referenceCode?.let { InfoRow("مرجع تراکنش", value = PersianText.toPersianDigits(it)) }
                            detail.reversedByCode?.let { InfoRow("برگشت با", value = PersianText.toPersianDigits(it)) }
                        }
                    }
                }
                detail.contact?.let { contact ->
                    item {
                        AppCard(onClick = { onOpenContact(contact.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(contact.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "مانده: ${PersianText.formatMoney(kotlin.math.abs(contact.balance))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HesabyarTheme.colors.onSurfaceSecondary
                                    )
                                }
                                Text("مشاهده حساب", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                if (detail.saleItems.isNotEmpty()) {
                    item { SectionHeader(title = "اقلام فروش") }
                    item {
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                detail.saleItems.forEach { item ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(item.productName, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${PersianText.formatQuantity(item.quantity)} × ${PersianText.formatMoney(item.unitPrice)}" +
                                                    if (item.returnedQuantity > 0) " · برگشتی: ${PersianText.formatQuantity(item.returnedQuantity)}" else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = HesabyarTheme.colors.onSurfaceSecondary
                                            )
                                        }
                                        MoneyText(amount = item.totalAmount, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                                detail.sale?.let { sale ->
                                    androidx.compose.material3.HorizontalDivider()
                                    InfoRow("جمع کل", value = PersianText.formatMoney(sale.grossAmount))
                                    InfoRow("تخفیف", value = PersianText.formatMoney(sale.discountAmount))
                                    InfoRow("مبلغ نهایی", value = PersianText.formatMoney(sale.netAmount))
                                    InfoRow("سود این فروش", value = PersianText.formatMoney(sale.netAmount - sale.costAmount), valueColor = HesabyarTheme.colors.income)
                                }
                            }
                        }
                    }
                }
                if (detail.purchaseItems.isNotEmpty()) {
                    item { SectionHeader(title = "اقلام خرید") }
                    item {
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                detail.purchaseItems.forEach { item ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(item.productName, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${PersianText.formatQuantity(item.quantity)} × ${PersianText.formatMoney(item.unitPrice)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = HesabyarTheme.colors.onSurfaceSecondary
                                            )
                                        }
                                        MoneyText(amount = item.totalAmount, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                                detail.purchase?.let { purchase ->
                                    androidx.compose.material3.HorizontalDivider()
                                    InfoRow("مبلغ خرید", value = PersianText.formatMoney(purchase.netAmount))
                                    InfoRow("کرایه حمل", value = PersianText.formatMoney(purchase.freightAmount))
                                }
                            }
                        }
                    }
                }
                detail.expense?.let { expense ->
                    item {
                        AppCard {
                            Column {
                                InfoRow("عنوان هزینه", value = expense.title)
                                InfoRow("دسته", value = expense.category.label)
                                InfoRow("روش پرداخت", value = expense.paymentMethod.label)
                            }
                        }
                    }
                }
                detail.saleReturn?.let { saleReturn ->
                    item {
                        AppCard {
                            Column {
                                InfoRow("بازپرداخت نقدی", value = PersianText.formatMoney(saleReturn.cashRefund))
                                InfoRow("کاهش طلب", value = PersianText.formatMoney(saleReturn.creditReduction))
                                saleReturn.reason?.let { InfoRow("دلیل", value = it) }
                            }
                        }
                    }
                }
                if (detail.ledgerEntries.isNotEmpty()) {
                    item { SectionHeader(title = "گردش حساب شخص") }
                    item {
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                detail.ledgerEntries.forEach { entry ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(entry.note ?: "ثبت خودکار", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                JalaliDate.formatDate(entry.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = HesabyarTheme.colors.onSurfaceSecondary
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            if (entry.debit > 0) MoneyText(entry.debit, style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.receivable)
                                            if (entry.credit > 0) MoneyText(entry.credit, style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.payable)
                                            Text(
                                                "مانده: ${PersianText.formatNumber(entry.balanceAfter)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = HesabyarTheme.colors.onSurfaceSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (detail.accountMovements.isNotEmpty()) {
                    item { SectionHeader(title = "اثر روی حساب‌ها") }
                    item {
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                detail.accountMovements.forEach { movement ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(movement.type.label, style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "مانده پس از ثبت: ${PersianText.formatNumber(movement.balanceAfter)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = HesabyarTheme.colors.onSurfaceSecondary
                                            )
                                        }
                                        MoneyText(
                                            amount = movement.delta,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (movement.delta >= 0) HesabyarTheme.colors.income else HesabyarTheme.colors.expense,
                                            signed = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (tx.type.isSale && tx.remainingAmount > 0) {
                            PrimaryButton(
                                text = "تسویه مانده مشتری",
                                onClick = { onSettle(detail.contact?.id, tx.remainingAmount) }
                            )
                        }
                        if (tx.type.isSale && !detail.hasReturns && tx.status == TransactionStatus.COMPLETED) {
                            SecondaryButton(
                                text = "ثبت برگشت از فروش",
                                onClick = { onReturnSale(tx.id) },
                                icon = Icons.Rounded.AssignmentReturn,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (detail.isReversible) {
                            SecondaryButton(
                                text = "برگشت دادن این تراکنش",
                                onClick = { showReverseDialog = true },
                                icon = Icons.Rounded.Undo,
                                tint = HesabyarTheme.colors.expense,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showReverseDialog) {
        AlertDialog(
            onDismissRequest = { showReverseDialog = false },
            title = { Text("برگشت تراکنش") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "تمام اثرات مالی این تراکنش (حساب‌ها، موجودی و مانده اشخاص) معکوس می‌شود. سابقه تراکنش حفظ خواهد ماند.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AppTextField(
                        value = reverseReason,
                        onValueChange = { reverseReason = it },
                        label = "دلیل برگشت",
                        placeholder = "اختیاری"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.reverse(reverseReason)
                        showReverseDialog = false
                        reverseReason = ""
                    },
                    enabled = !state.reversing
                ) { Text("تأیید برگشت") }
            },
            dismissButton = { TextButton(onClick = { showReverseDialog = false }) { Text("انصراف") } }
        )
    }
}
