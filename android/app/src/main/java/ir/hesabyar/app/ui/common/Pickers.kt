package ir.hesabyar.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.hesabyar.core.design.components.DropdownField
import ir.hesabyar.core.design.components.EmptyState
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.SearchField
import ir.hesabyar.core.design.components.StatusBadge
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPickerSheet(
    title: String,
    products: List<Product>,
    onSelect: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(products, query) {
        val q = query.trim()
        if (q.isBlank()) products
        else products.filter { it.name.contains(q, ignoreCase = true) || it.code?.contains(q, ignoreCase = true) == true }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp))
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "جست‌وجوی کالا",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Inventory2,
                    title = if (products.isEmpty()) "کالایی ثبت نشده" else "کالایی پیدا نشد",
                    message = if (products.isEmpty()) "ابتدا از بخش کالاها یک کالا ثبت کنید." else "عبارت دیگری را جست‌وجو کنید."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.id }) { product ->
                        Surface(
                            onClick = { onSelect(product) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(product.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "موجودی: ${PersianText.formatQuantity(product.stockQuantity)} · میانگین بهای تمام‌شده: ${PersianText.formatMoney(product.averageCost)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HesabyarTheme.colors.onSurfaceSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    MoneyText(product.sellingPrice, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    if (product.isLowStock) StatusBadge("کم‌موجود", HesabyarTheme.colors.warning)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerSheet(
    title: String,
    contacts: List<Contact>,
    onSelect: (Contact) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(contacts, query) {
        val q = query.trim()
        if (q.isBlank()) contacts else contacts.filter { it.name.contains(q, ignoreCase = true) || it.phone?.contains(q) == true }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp))
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "جست‌وجوی نام یا موبایل",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Person,
                    title = "شخصی پیدا نشد",
                    message = "می‌توانید با دکمه «جدید» در همان صفحه، شخص را اضافه کنید."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.id }) { contact ->
                        Surface(
                            onClick = { onSelect(contact) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(contact.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        contact.phone ?: contact.type.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HesabyarTheme.colors.onSurfaceSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    MoneyText(
                                        amount = kotlin.math.abs(contact.balance),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (contact.balance > 0) HesabyarTheme.colors.receivable else HesabyarTheme.colors.payable
                                    )
                                    Text(
                                        when {
                                            contact.balance > 0 -> "بدهکار"
                                            contact.balance < 0 -> "بستانکار"
                                            else -> "تسویه"
                                        },
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
    }
}

@Composable
fun AccountDropdown(
    label: String,
    accounts: List<Account>,
    selectedId: Long?,
    onSelect: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownField(
        label = label,
        options = accounts,
        selected = accounts.firstOrNull { it.id == selectedId },
        optionLabel = { "${it.title} — ${PersianText.formatMoney(it.currentBalance)}" },
        onSelect = onSelect,
        modifier = modifier,
        placeholder = if (accounts.isEmpty()) "حسابی ثبت نشده" else "انتخاب حساب"
    )
}
