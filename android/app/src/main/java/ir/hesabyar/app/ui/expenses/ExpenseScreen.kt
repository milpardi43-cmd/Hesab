package ir.hesabyar.app.ui.expenses

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.hesabyar.app.ui.common.AccountDropdown
import ir.hesabyar.app.ui.common.ContactPickerSheet
import ir.hesabyar.app.ui.common.FileUtils
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.ChipSelector
import ir.hesabyar.core.design.components.ErrorBanner
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.JalaliDateField
import ir.hesabyar.core.design.components.LabeledSwitch
import ir.hesabyar.core.design.components.MoneyField
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.model.ExpenseCategory
import ir.hesabyar.domain.model.PaymentMethod

@Composable
fun ExpenseScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showContactPicker by remember { mutableStateOf(false) }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.setImagePath(FileUtils.copyUriToInternal(context, uri, "expense"))
        }
    }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "ثبت هزینه", subtitle = "هزینه‌ها را از فروش تفکیک کنید", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) ErrorBanner(state.error!!)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        value = state.title,
                        onValueChange = viewModel::setTitle,
                        label = "عنوان هزینه *",
                        placeholder = "مثلاً اجاره ماهانه"
                    )
                    MoneyField(value = state.amount, onValueChange = viewModel::setAmount, label = "مبلغ *")
                    Text("دسته هزینه", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                    ChipSelector(
                        options = ExpenseCategory.entries.toList(),
                        selected = state.category,
                        onSelect = viewModel::setCategory,
                        optionLabel = { it.label }
                    )
                }
            }
            SectionHeader(title = "نحوه پرداخت")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledSwitch(
                        title = "پرداخت‌شده",
                        subtitle = if (state.isPaid) "مبلغ از حساب انتخابی کسر می‌شود" else "به‌صورت بدهی به شخص طلبکار ثبت می‌شود",
                        checked = state.isPaid,
                        onCheckedChange = viewModel::setPaid
                    )
                    Text("روش پرداخت", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                    ChipSelector(
                        options = PaymentMethod.entries.toList(),
                        selected = state.method,
                        onSelect = viewModel::setMethod,
                        optionLabel = { it.label }
                    )
                    if (state.isPaid) {
                        AccountDropdown(
                            label = "حساب پرداخت",
                            accounts = state.accounts,
                            selectedId = state.accountId,
                            onSelect = viewModel::setAccount
                        )
                    } else {
                        Text("شخص طلبکار", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                        val contact = state.contacts.firstOrNull { it.id == state.contactId }
                        Surface(
                            onClick = { showContactPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    contact?.name ?: "انتخاب شخص طلبکار",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (contact == null) HesabyarTheme.colors.onSurfaceSecondary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            SectionHeader(title = "جزئیات")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    JalaliDateField(label = "تاریخ هزینه", millis = state.date, onSelect = viewModel::setDate)
                    AppTextField(
                        value = state.description,
                        onValueChange = viewModel::setDescription,
                        label = "توضیحات",
                        placeholder = "اختیاری",
                        singleLine = false,
                        maxLines = 3
                    )
                    Text("تصویر فاکتور (اختیاری)", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                    if (state.imagePath != null) {
                        Box(Modifier.fillMaxWidth().height(150.dp)) {
                            AsyncImage(
                                model = state.imagePath,
                                contentDescription = "فاکتور",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().padding(end = 36.dp).clip(RoundedCornerShape(14.dp))
                            )
                            IconButton(onClick = { viewModel.setImagePath(null) }, modifier = Modifier.align(Alignment.TopEnd)) {
                                Icon(Icons.Rounded.Close, contentDescription = "حذف تصویر")
                            }
                        }
                    } else {
                        Surface(
                            onClick = { imageLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, HesabyarTheme.colors.hairline)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.size(10.dp))
                                Text("افزودن تصویر فاکتور", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            PrimaryButton(text = "ثبت هزینه", onClick = viewModel::submit, loading = state.saving)
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showContactPicker) {
        ContactPickerSheet(
            title = "انتخاب شخص طلبکار",
            contacts = state.contacts,
            onSelect = {
                viewModel.setContact(it)
                showContactPicker = false
            },
            onDismiss = { showContactPicker = false }
        )
    }
}
