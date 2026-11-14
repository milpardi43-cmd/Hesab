package ir.hesabyar.app.ui.purchases

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabyar.app.ui.common.AccountDropdown
import ir.hesabyar.app.ui.common.ContactPickerSheet
import ir.hesabyar.app.ui.common.ProductPickerSheet
import ir.hesabyar.app.ui.sales.QuickProductDialog
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.ChipSelector
import ir.hesabyar.core.design.components.ErrorBanner
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.InfoRow
import ir.hesabyar.core.design.components.JalaliDateField
import ir.hesabyar.core.design.components.LabeledSwitch
import ir.hesabyar.core.design.components.MoneyField
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.PaymentMethod

@Composable
fun PurchaseScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: PurchaseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showProductPicker by remember { mutableStateOf(false) }
    var showSupplierPicker by remember { mutableStateOf(false) }
    var showQuickProduct by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }

    val supplier = state.suppliers.firstOrNull { it.id == state.supplierId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "ثبت خرید", subtitle = "افزایش موجودی و بدهی تأمین‌کننده", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("مبلغ خرید", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                        MoneyText(amount = state.net, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(Modifier.width(170.dp)) {
                        PrimaryButton("ثبت خرید", viewModel::submit, loading = state.saving, enabled = state.items.isNotEmpty())
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) ErrorBanner(state.error!!)
            SectionHeader(title = "کالاهای خرید", actionLabel = "افزودن کالا", onAction = { showProductPicker = true })
            if (state.items.isEmpty()) {
                AppCard {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                        Text("کالایی برای خرید اضافه نشده", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { showProductPicker = true }) { Text("انتخاب از لیست کالاها") }
                        TextButton(onClick = { showQuickProduct = true }) { Text("ثبت کالای جدید") }
                    }
                }
            } else {
                state.items.forEach { line ->
                    PurchaseLineCard(
                        line = line,
                        onQuantity = { viewModel.updateQuantity(line.productId, it) },
                        onPrice = { viewModel.updatePrice(line.productId, it) },
                        onRemove = { viewModel.removeItem(line.productId) }
                    )
                }
                TextButton(onClick = { showProductPicker = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("افزودن کالای دیگر")
                }
            }

            SectionHeader("تأمین‌کننده و پرداخت")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تأمین‌کننده", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                    Surface(
                        onClick = { showSupplierPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                supplier?.name ?: "انتخاب تأمین‌کننده (اختیاری)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (supplier == null) HesabyarTheme.colors.onSurfaceSecondary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    MoneyField(value = state.paidAmount, onValueChange = viewModel::setPaid, label = "مبلغ پرداختی")
                    TextButton(onClick = { viewModel.setPaid(state.net) }) { Text("پرداخت کامل خرید") }
                    Text("روش پرداخت", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                    ChipSelector(
                        options = PaymentMethod.entries.toList(),
                        selected = state.method,
                        onSelect = viewModel::setMethod,
                        optionLabel = { it.label }
                    )
                    AccountDropdown(
                        label = "حساب پرداخت",
                        accounts = state.accounts,
                        selectedId = state.accountId,
                        onSelect = viewModel::setAccount
                    )
                }
            }

            SectionHeader("کرایه و تخفیف")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoneyField(value = state.discount, onValueChange = viewModel::setDiscount, label = "تخفیف خرید")
                    MoneyField(value = state.freight, onValueChange = viewModel::setFreight, label = "کرایه حمل")
                    LabeledSwitch(
                        title = "افزودن کرایه به بهای تمام‌شده",
                        subtitle = "کرایه بین کالاها سرشکن و به میانگین بهای خرید اضافه می‌شود",
                        checked = state.freightToCost,
                        onCheckedChange = viewModel::setFreightToCost
                    )
                    InfoRow(label = "جمع کالاها", valueContent = { MoneyText(amount = state.gross, style = MaterialTheme.typography.bodyMedium) })
                    InfoRow(label = "مانده بدهی خرید", valueContent = {
                        MoneyText(
                            amount = state.remaining,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (state.remaining > 0) HesabyarTheme.colors.warning else HesabyarTheme.colors.income
                        )
                    })
                }
            }

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    JalaliDateField(label = "تاریخ خرید", millis = state.date, onSelect = viewModel::setDate)
                    AppTextField(
                        value = state.description,
                        onValueChange = viewModel::setDescription,
                        label = "توضیحات",
                        placeholder = "اختیاری",
                        singleLine = false,
                        maxLines = 3
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showProductPicker) {
        ProductPickerSheet(
            title = "انتخاب کالا برای خرید",
            products = state.products,
            onSelect = {
                viewModel.addProduct(it)
                showProductPicker = false
            },
            onDismiss = { showProductPicker = false }
        )
    }
    if (showSupplierPicker) {
        ContactPickerSheet(
            title = "انتخاب تأمین‌کننده",
            contacts = state.suppliers,
            onSelect = {
                viewModel.setSupplier(it)
                showSupplierPicker = false
            },
            onDismiss = { showSupplierPicker = false }
        )
    }
    if (showQuickProduct) {
        QuickProductDialog(
            onConfirm = { name, selling, purchase, stock ->
                viewModel.quickAddProduct(name, selling, purchase, stock)
                showQuickProduct = false
            },
            onDismiss = { showQuickProduct = false }
        )
    }
}

@Composable
private fun PurchaseLineCard(
    line: PurchaseLineUi,
    onQuantity: (Double) -> Unit,
    onPrice: (Long?) -> Unit,
    onRemove: () -> Unit
) {
    AppCard(contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(line.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "موجودی فعلی: ${PersianText.formatQuantity(line.currentStock)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = HesabyarTheme.colors.onSurfaceSecondary
                    )
                }
                MoneyText(amount = line.total, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = HesabyarTheme.colors.expense)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("تعداد", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(Modifier.height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onQuantity((line.quantity - 1).coerceAtLeast(0.0)) }) {
                                Icon(Icons.Rounded.Remove, contentDescription = "کاهش", modifier = Modifier.size(18.dp))
                            }
                            Text(
                                PersianText.formatQuantity(line.quantity),
                                Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            IconButton(onClick = { onQuantity(line.quantity + 1) }) {
                                Icon(Icons.Rounded.Add, contentDescription = "افزایش", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                MoneyField(
                    value = line.unitPrice,
                    onValueChange = onPrice,
                    label = "قیمت خرید",
                    modifier = Modifier.weight(1.35f)
                )
            }
        }
    }
}
