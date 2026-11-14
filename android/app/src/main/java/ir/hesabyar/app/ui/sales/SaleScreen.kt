package ir.hesabyar.app.ui.sales

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
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
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
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.ErrorBanner
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.InfoRow
import ir.hesabyar.core.design.components.JalaliDateField
import ir.hesabyar.core.design.components.MoneyField
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.components.SegmentedControl
import ir.hesabyar.core.design.components.ChipSelector
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.PaymentMethod
import ir.hesabyar.domain.model.SalePaymentMode

@Composable
fun SaleScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: SaleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showProductPicker by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var showQuickContact by remember { mutableStateOf(false) }
    var showQuickProduct by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbar.showSnackbar(it.message) }
    }
    LaunchedEffect(state.savedId) {
        state.savedId?.let(onSaved)
    }

    val selectedContact = state.contacts.firstOrNull { it.id == state.contactId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "ثبت فروش", subtitle = "سریع، دقیق و متصل به حساب‌ها", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("مبلغ نهایی", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                        MoneyText(amount = state.netTotal, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(Modifier.width(170.dp)) {
                        PrimaryButton(
                            text = "ثبت فروش",
                            onClick = viewModel::submit,
                            loading = state.saving,
                            enabled = state.items.isNotEmpty()
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) {
                ErrorBanner(message = state.error!!)
            }
            SectionHeader(
                title = "کالاهای فروش",
                actionLabel = "افزودن کالا",
                onAction = { showProductPicker = true }
            )
            if (state.items.isEmpty()) {
                AppCard(border = BorderStroke(1.dp, HesabyarTheme.colors.hairline)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                        Text("کالایی به فروش اضافه نشده", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { showProductPicker = true }) { Text("انتخاب از لیست کالاها") }
                        TextButton(onClick = { showQuickProduct = true }) { Text("ثبت کالای جدید") }
                    }
                }
            } else {
                state.items.forEach { line ->
                    SaleLineCard(
                        line = line,
                        onQuantity = { viewModel.updateQuantity(line.productId, it) },
                        onPrice = { viewModel.updatePrice(line.productId, it) },
                        onDiscount = { viewModel.updateDiscount(line.productId, it) },
                        onRemove = { viewModel.removeItem(line.productId) }
                    )
                }
                TextButton(onClick = { showProductPicker = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("افزودن کالای دیگر")
                }
            }

            SectionHeader(title = "پرداخت")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SegmentedControl(
                        options = SalePaymentMode.entries.toList(),
                        selected = state.paymentMode,
                        onSelect = viewModel::setPaymentMode,
                        label = { it.label }
                    )
                    if (state.paymentMode == SalePaymentMode.MIXED) {
                        MoneyField(
                            value = state.paidAmount,
                            onValueChange = viewModel::setPaidAmount,
                            label = "مبلغ پرداختی نقدی"
                        )
                    }
                    Text("روش پرداخت", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                    ChipSelector(
                        options = PaymentMethod.entries.toList(),
                        selected = state.paymentMethod,
                        onSelect = viewModel::setPaymentMethod,
                        optionLabel = { it.label }
                    )
                    if (state.paymentMode != SalePaymentMode.CREDIT) {
                        AccountDropdown(
                            label = "حساب دریافت وجه",
                            accounts = state.accounts,
                            selectedId = state.accountId,
                            onSelect = viewModel::setAccount
                        )
                    }
                    Text("مشتری", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = { showContactPicker = true },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = selectedContact?.name ?: "انتخاب مشتری (برای نسیه الزامی است)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                    color = if (selectedContact == null) HesabyarTheme.colors.onSurfaceSecondary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Surface(
                            onClick = { showQuickContact = true },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "جدید",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            SectionHeader(title = "تخفیف و توضیحات")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoneyField(
                        value = state.invoiceDiscount,
                        onValueChange = viewModel::setInvoiceDiscount,
                        label = "تخفیف کل فاکتور"
                    )
                    InfoRow(label = "جمع کالاها", valueContent = {
                        MoneyText(amount = state.subtotal, style = MaterialTheme.typography.bodyMedium)
                    })
                    InfoRow(label = "تخفیف کل", valueContent = {
                        MoneyText(amount = -(state.invoiceDiscount ?: 0L), style = MaterialTheme.typography.bodyMedium, color = HesabyarTheme.colors.expense)
                    })
                    InfoRow(label = "مانده (نسیه)", valueContent = {
                        MoneyText(
                            amount = state.remaining,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (state.remaining > 0) HesabyarTheme.colors.warning else HesabyarTheme.colors.income
                        )
                    })
                    JalaliDateField(label = "تاریخ فروش", millis = state.date, onSelect = viewModel::setDate)
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
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showProductPicker) {
        ProductPickerSheet(
            title = "انتخاب کالا برای فروش",
            products = state.products,
            onSelect = {
                viewModel.addProduct(it)
                showProductPicker = false
            },
            onDismiss = { showProductPicker = false }
        )
    }
    if (showContactPicker) {
        ContactPickerSheet(
            title = "انتخاب مشتری",
            contacts = state.contacts.filter { it.type.isCustomer },
            onSelect = {
                viewModel.setContact(it)
                showContactPicker = false
            },
            onDismiss = { showContactPicker = false }
        )
    }
    if (showQuickContact) {
        QuickContactDialog(
            onConfirm = { name, phone ->
                viewModel.quickAddContact(name, phone)
                showQuickContact = false
            },
            onDismiss = { showQuickContact = false }
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
private fun SaleLineCard(
    line: SaleLineUi,
    onQuantity: (Double) -> Unit,
    onPrice: (Long?) -> Unit,
    onDiscount: (Long?) -> Unit,
    onRemove: () -> Unit
) {
    AppCard(contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(line.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "موجودی: ${PersianText.formatQuantity(line.availableStock)}",
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
                    QtyStepper(value = line.quantity, onChange = onQuantity)
                }
                MoneyField(
                    value = line.unitPrice,
                    onValueChange = onPrice,
                    label = "قیمت واحد",
                    modifier = Modifier.weight(1.35f)
                )
            }
            MoneyField(
                value = line.discount,
                onValueChange = onDiscount,
                label = "تخفیف این ردیف"
            )
        }
    }
}

@Composable
private fun QtyStepper(value: Double, onChange: (Double) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange((value - 1).coerceAtLeast(0.0)) }) {
                Icon(Icons.Rounded.Remove, contentDescription = "کاهش", modifier = Modifier.size(18.dp))
            }
            Text(
                text = PersianText.formatQuantity(value),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = { onChange(value + 1) }) {
                Icon(Icons.Rounded.Add, contentDescription = "افزایش", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun QuickContactDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مشتری جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(value = name, onValueChange = { name = it }, label = "نام *")
                AppTextField(value = phone, onValueChange = { phone = it }, label = "موبایل", placeholder = "اختیاری")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), phone.trim()) },
                enabled = name.isNotBlank()
            ) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun QuickProductDialog(
    onConfirm: (String, Long, Long, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selling by remember { mutableStateOf<Long?>(null) }
    var purchase by remember { mutableStateOf<Long?>(null) }
    var stock by remember { mutableStateOf<Double?>(1.0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("کالای جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(value = name, onValueChange = { name = it }, label = "نام کالا *")
                MoneyField(value = selling, onValueChange = { selling = it }, label = "قیمت فروش")
                MoneyField(value = purchase, onValueChange = { purchase = it }, label = "قیمت خرید")
                AppTextField(
                    value = stock?.let { PersianText.formatQuantity(it, false) } ?: "",
                    onValueChange = { stock = PersianText.parseQuantity(it) },
                    label = "موجودی اولیه",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && (selling ?: 0) > 0) {
                        onConfirm(name.trim(), selling ?: 0L, purchase ?: 0L, stock ?: 0.0)
                    }
                },
                enabled = name.isNotBlank() && (selling ?: 0) > 0
            ) { Text("ثبت و افزودن") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
