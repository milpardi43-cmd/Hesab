package ir.hesabyar.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentReturn
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.model.PaymentStatus
import ir.hesabyar.domain.model.Transaction
import ir.hesabyar.domain.model.TransactionStatus
import ir.hesabyar.domain.model.TransactionType

fun transactionTypeIcon(type: TransactionType): ImageVector = when (type) {
    TransactionType.CASH_SALE, TransactionType.CREDIT_SALE, TransactionType.MIXED_SALE -> Icons.Rounded.ShoppingCart
    TransactionType.CUSTOMER_RECEIPT -> Icons.Rounded.CallReceived
    TransactionType.EXPENSE -> Icons.Rounded.TrendingDown
    TransactionType.PURCHASE -> Icons.Rounded.Inventory2
    TransactionType.SUPPLIER_PAYMENT -> Icons.Rounded.NorthEast
    TransactionType.SALE_RETURN -> Icons.Rounded.AssignmentReturn
    TransactionType.CASH_ADJUSTMENT -> Icons.Rounded.Tune
    TransactionType.STOCK_ADJUSTMENT -> Icons.Rounded.ReceiptLong
    TransactionType.ACCOUNT_TRANSFER -> Icons.Rounded.SwapHoriz
    TransactionType.REVERSAL -> Icons.Rounded.Undo
}

@Composable
fun transactionTypeColor(type: TransactionType): Color = when (type) {
    TransactionType.CASH_SALE, TransactionType.CREDIT_SALE, TransactionType.MIXED_SALE,
    TransactionType.CUSTOMER_RECEIPT -> HesabyarTheme.colors.income
    TransactionType.EXPENSE, TransactionType.PURCHASE, TransactionType.SUPPLIER_PAYMENT,
    TransactionType.SALE_RETURN -> HesabyarTheme.colors.expense
    else -> HesabyarTheme.colors.neutral
}

fun transactionSign(type: TransactionType): String = when (type) {
    TransactionType.CASH_SALE, TransactionType.CREDIT_SALE, TransactionType.MIXED_SALE,
    TransactionType.CUSTOMER_RECEIPT -> "+"
    TransactionType.EXPENSE, TransactionType.PURCHASE, TransactionType.SUPPLIER_PAYMENT,
    TransactionType.SALE_RETURN -> "−"
    else -> ""
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true
) {
    val tint = transactionTypeColor(transaction.type)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(transactionTypeIcon(transaction.type), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = transaction.description?.takeIf { it.isNotBlank() } ?: transaction.type.label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (showDate) {
                        Text(
                            text = JalaliDate.formatDate(transaction.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                    val contact = transaction.contactName
                    if (!contact.isNullOrBlank()) {
                        Text("·", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                        Text(
                            text = contact,
                            style = MaterialTheme.typography.labelSmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val signed = transactionSign(transaction.type)
                MoneyText(
                    amount = if (signed == "−") -transaction.amount else transaction.amount,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (transaction.status == TransactionStatus.REVERSED) HesabyarTheme.colors.neutral else tint,
                    signed = signed.isNotEmpty()
                )
                when {
                    transaction.status == TransactionStatus.REVERSED -> StatusBadge(
                        text = "برگشت‌خورده",
                        color = HesabyarTheme.colors.neutral
                    )
                    transaction.paymentStatus != null -> {
                        val status = transaction.paymentStatus!!
                        val color = when (status) {
                            PaymentStatus.PAID -> HesabyarTheme.colors.income
                            PaymentStatus.PARTIAL -> HesabyarTheme.colors.warning
                            PaymentStatus.UNPAID -> HesabyarTheme.colors.expense
                        }
                        StatusBadge(text = status.label, color = color)
                    }
                }
            }
        }
    }
}
