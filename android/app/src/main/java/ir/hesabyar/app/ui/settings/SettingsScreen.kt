package ir.hesabyar.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.AccountDropdown
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.ChipSelector
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.LabeledSwitch
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.components.SegmentedControl
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.AppSettings
import ir.hesabyar.domain.model.Business
import ir.hesabyar.domain.model.BusinessType
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.Subscription
import ir.hesabyar.domain.model.ThemeMode
import ir.hesabyar.domain.usecase.GetAccountsUseCase
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.usecase.GetSettingsUseCase
import ir.hesabyar.domain.usecase.GetSubscriptionUseCase
import ir.hesabyar.domain.usecase.UpdateBusinessUseCase
import ir.hesabyar.domain.usecase.UpdateSettingsUseCase
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getSettings: GetSettingsUseCase,
    getBusiness: GetBusinessUseCase,
    getAccounts: GetAccountsUseCase,
    getSubscription: GetSubscriptionUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val updateBusiness: UpdateBusinessUseCase
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val business: StateFlow<Business?> = getBusiness()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val accounts: StateFlow<List<Account>> = getAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val subscription = getSubscription()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Subscription(businessId = 0))

    val planLabel: StateFlow<String> = subscription
        .map { it.plan.label }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "رایگان")

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { updateSettings(transform) }
    }

    fun saveBusiness(business: Business) {
        viewModelScope.launch {
            when (val result = updateBusiness(business)) {
                is OperationResult.Success -> _events.send(UiEvent("اطلاعات فروشگاه ذخیره شد"))
                is OperationResult.Failure -> _events.send(UiEvent(result.message, isError = true))
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDataManagement: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val business by viewModel.business.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val planLabel by viewModel.planLabel.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showBusinessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "تنظیمات", subtitle = "حسابیار را مطابق فروشگاه خود تنظیم کنید", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val current = settings
        if (current == null) return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(title = "فروشگاه")
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(business?.name ?: "فروشگاه من", style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOfNotNull(business?.type?.label, business?.city, business?.phone).joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = HesabyarTheme.colors.onSurfaceSecondary
                            )
                            Text(
                                "مالک: ${business?.ownerName ?: "—"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = HesabyarTheme.colors.onSurfaceSecondary
                            )
                        }
                        IconButton(onClick = { showBusinessDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "ویرایش فروشگاه")
                        }
                    }
                }
            }
            item {
                SectionHeader(title = "نمایش")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("پوسته", style = MaterialTheme.typography.labelMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                        SegmentedControl(
                            options = ThemeMode.entries.toList(),
                            selected = current.themeMode,
                            onSelect = { mode -> viewModel.update { it.copy(themeMode = mode) } },
                            label = { it.label }
                        )
                        Text(
                            "اندازه متن: ${(current.textScale * 100).toInt()}٪",
                            style = MaterialTheme.typography.labelMedium,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                        Slider(
                            value = current.textScale,
                            onValueChange = { scale -> viewModel.update { it.copy(textScale = scale) } },
                            valueRange = 0.85f..1.3f,
                            steps = 2
                        )
                        LabeledSwitch(
                            title = "اعداد فارسی",
                            subtitle = "نمایش مبالغ با ارقام فارسی",
                            checked = current.persianDigits,
                            onCheckedChange = { value -> viewModel.update { it.copy(persianDigits = value) } }
                        )
                    }
                }
            }
            item {
                SectionHeader(title = "اعلان‌ها")
                AppCard {
                    Column {
                        LabeledSwitch(
                            title = "اعلان یادآوری‌ها",
                            subtitle = "اطلاع از سررسید یادآوری‌ها",
                            checked = current.notificationsEnabled,
                            onCheckedChange = { value -> viewModel.update { it.copy(notificationsEnabled = value) } }
                        )
                        LabeledSwitch(
                            title = "هشدار کمبود موجودی",
                            subtitle = "اعلان هنگام رسیدن کالا به حداقل موجودی",
                            checked = current.lowStockAlerts,
                            onCheckedChange = { value -> viewModel.update { it.copy(lowStockAlerts = value) } }
                        )
                    }
                }
            }
            item {
                SectionHeader(title = "حساب‌های پیش‌فرض")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccountDropdown(
                            label = "حساب پیش‌فرض دریافت",
                            accounts = accounts,
                            selectedId = current.defaultReceiveAccountId,
                            onSelect = { account -> viewModel.update { it.copy(defaultReceiveAccountId = account.id) } }
                        )
                        AccountDropdown(
                            label = "حساب پیش‌فرض پرداخت",
                            accounts = accounts,
                            selectedId = current.defaultPayAccountId,
                            onSelect = { account -> viewModel.update { it.copy(defaultPayAccountId = account.id) } }
                        )
                    }
                }
            }
            item {
                SectionHeader(title = "پشتیبان‌گیری و داده‌ها")
                AppCard {
                    Column {
                        LabeledSwitch(
                            title = "پشتیبان‌گیری خودکار روزانه",
                            subtitle = "هر روز یک نسخه امن از داده‌ها در حافظه داخلی ذخیره می‌شود",
                            checked = current.autoBackupEnabled,
                            onCheckedChange = { value -> viewModel.update { it.copy(autoBackupEnabled = value) } }
                        )
                    }
                }
            }
            item {
                AppCard {
                    Column {
                        SettingsRow(Icons.Rounded.Backup, "پشتیبان‌گیری و بازیابی", "خروجی JSON، CSV، اکسل و بازیابی", onOpenDataManagement)
                        SettingsRow(Icons.Rounded.NotificationsNone, "یادآوری‌ها", "سررسیدها و کارهای روز", onOpenReminders)
                        SettingsRow(Icons.Rounded.WorkspacePremium, "اشتراک و نسخه حرفه‌ای", "امکانات بیشتر برای رشد فروشگاه", onOpenSubscription)
                        SettingsRow(Icons.Rounded.Info, "درباره حسابیار", "نسخه، حریم خصوصی و پشتیبانی", onOpenAbout, showDivider = false)
                    }
                }
            }
            item {
                Text(
                    "حسابیار $planLabel — داده‌ها روی همین دستگاه و به‌صورت آفلاین نگهداری می‌شوند.",
                    style = MaterialTheme.typography.labelSmall,
                    color = HesabyarTheme.colors.onSurfaceSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    if (showBusinessDialog && business != null) {
        BusinessEditDialog(
            initial = business!!,
            onSave = {
                viewModel.saveBusiness(it)
                showBusinessDialog = false
            },
            onDismiss = { showBusinessDialog = false }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
            }
        }
        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(color = HesabyarTheme.colors.hairline)
        }
    }
}

@Composable
private fun BusinessEditDialog(initial: Business, onSave: (Business) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var owner by remember { mutableStateOf(initial.ownerName.orEmpty()) }
    var phone by remember { mutableStateOf(initial.phone.orEmpty()) }
    var city by remember { mutableStateOf(initial.city.orEmpty()) }
    var type by remember { mutableStateOf(initial.type) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اطلاعات فروشگاه") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                AppTextField(value = name, onValueChange = { name = it }, label = "نام فروشگاه *")
                AppTextField(value = owner, onValueChange = { owner = it }, label = "نام مالک")
                AppTextField(value = phone, onValueChange = { phone = it }, label = "موبایل", keyboardType = KeyboardType.Phone)
                AppTextField(value = city, onValueChange = { city = it }, label = "شهر")
                ChipSelector(
                    options = BusinessType.entries.toList(),
                    selected = type,
                    onSelect = { type = it },
                    optionLabel = { it.label }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                ownerName = owner.trim().takeIf { it.isNotBlank() },
                                phone = phone.trim().takeIf { it.isNotBlank() },
                                city = city.trim().takeIf { it.isNotBlank() },
                                type = type
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
