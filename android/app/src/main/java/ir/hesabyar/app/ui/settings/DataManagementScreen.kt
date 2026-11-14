package ir.hesabyar.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.ConfirmDialog
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.LabeledSwitch
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.model.AppSettings
import ir.hesabyar.domain.usecase.ExportBackupUseCase
import ir.hesabyar.domain.usecase.ExportCsvUseCase
import ir.hesabyar.domain.usecase.ExportXlsxUseCase
import ir.hesabyar.domain.usecase.GetSettingsUseCase
import ir.hesabyar.domain.usecase.ImportBackupUseCase
import ir.hesabyar.domain.usecase.UpdateSettingsUseCase
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExportFile(val name: String, val mime: String, val bytes: ByteArray)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val exportBackup: ExportBackupUseCase,
    private val exportCsv: ExportCsvUseCase,
    private val exportXlsx: ExportXlsxUseCase,
    private val importBackup: ImportBackupUseCase,
    getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase
) : ViewModel() {

    val autoBackup: StateFlow<Boolean> = getSettings()
        .map { it.autoBackupEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _files = Channel<ExportFile>(Channel.BUFFERED)
    val files = _files.receiveAsFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private fun stamp(): String {
        val today = JalaliDate.today()
        return "${today.year}-${today.month.toString().padStart(2, '0')}-${today.day.toString().padStart(2, '0')}"
    }

    fun exportBackupFile() {
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                val json = exportBackup()
                ExportFile("hesabyar-backup-${stamp()}.json", "application/json", json.toByteArray(Charsets.UTF_8))
            }.onSuccess { file ->
                _files.send(file)
            }.onFailure {
                _events.send(UiEvent("ساخت پشتیبان ناموفق بود: ${it.message}", isError = true))
            }
            _busy.value = false
        }
    }

    fun exportCsvFile() {
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                ExportFile("hesabyar-csv-${stamp()}.zip", "application/zip", exportCsv())
            }.onSuccess { _files.send(it) }
                .onFailure { _events.send(UiEvent("خروجی CSV ناموفق بود: ${it.message}", isError = true)) }
            _busy.value = false
        }
    }

    fun exportXlsxFile() {
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                ExportFile(
                    "hesabyar-report-${stamp()}.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    exportXlsx()
                )
            }.onSuccess { _files.send(it) }
                .onFailure { _events.send(UiEvent("خروجی اکسل ناموفق بود: ${it.message}", isError = true)) }
            _busy.value = false
        }
    }

    fun importJson(json: String) {
        viewModelScope.launch {
            _busy.value = true
            importBackup(json)
                .onSuccess { _events.send(UiEvent("اطلاعات با موفقیت بازیابی شد")) }
                .onFailure { _events.send(UiEvent("بازیابی ناموفق بود: ${it.message}", isError = true)) }
            _busy.value = false
        }
    }

    fun setAutoBackup(value: Boolean) {
        viewModelScope.launch { updateSettings { it.copy(autoBackupEnabled = value) } }
    }
}

@Composable
fun DataManagementScreen(
    onBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val autoBackup by viewModel.autoBackup.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var pendingFile by remember { mutableStateOf<ExportFile?>(null) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
        val file = pendingFile
        if (uri != null && file != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(file.bytes) }
            }.onSuccess {
                scope.launch { snackbar.showSnackbar("فایل ذخیره شد") }
            }.onFailure {
                scope.launch { snackbar.showSnackbar("ذخیره فایل ناموفق بود") }
            }
        }
        pendingFile = null
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.onSuccess { pendingImport = it }
                .onFailure { scope.launch { snackbar.showSnackbar("خواندن فایل ناموفق بود") } }
        }
    }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }
    LaunchedEffect(Unit) {
        viewModel.files.collect { file ->
            pendingFile = file
            createDocument.launch(file.name)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "پشتیبان‌گیری و داده‌ها", subtitle = "داده‌های شما، مال شماست", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), border = null) {
                    Column {
                        Text("داده‌ها کاملاً آفلاین ذخیره می‌شوند", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "هیچ اطلاعاتی به سرور ارسال نمی‌شود. برای جلوگیری از از دست رفتن داده‌ها، به‌صورت دوره‌ای پشتیبان کامل بگیرید و فایل را در جای امن نگه دارید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                }
            }
            item {
                SectionHeader(title = "پشتیبان کامل")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("فایل JSON شامل همه فروشگاه، تراکنش‌ها و تنظیمات", style = MaterialTheme.typography.bodyMedium)
                        }
                        PrimaryButton(text = "ساخت فایل پشتیبان", onClick = viewModel::exportBackupFile, loading = busy)
                    }
                }
            }
            item {
                SectionHeader(title = "خروجی برای حسابداری")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportRow(
                            icon = Icons.Rounded.TableView,
                            title = "خروجی اکسل",
                            subtitle = "تراکنش‌ها، کالاها، اشخاص و حساب‌ها در یک فایل XLSX",
                            onClick = viewModel::exportXlsxFile,
                            enabled = !busy
                        )
                        ExportRow(
                            icon = Icons.Rounded.Description,
                            title = "خروجی CSV",
                            subtitle = "چند فایل CSV در یک بسته ZIP",
                            onClick = viewModel::exportCsvFile,
                            enabled = !busy
                        )
                    }
                }
            }
            item {
                SectionHeader(title = "بازیابی")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, tint = HesabyarTheme.colors.warning)
                            Text(
                                "بازیابی، داده‌های فعلی را با محتوای فایل پشتیبان جایگزین می‌کند.",
                                style = MaterialTheme.typography.bodySmall,
                                color = HesabyarTheme.colors.onSurfaceSecondary
                            )
                        }
                        PrimaryButton(
                            text = "انتخاب فایل پشتیبان و بازیابی",
                            onClick = { openDocument.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                            enabled = !busy
                        )
                    }
                }
            }
            item {
                SectionHeader(title = "پشتیبان‌گیری خودکار")
                AppCard {
                    LabeledSwitch(
                        title = "هر روز به‌صورت خودکار پشتیبان بگیر",
                        subtitle = "فایل پشتیبان در حافظه داخلی اپ ذخیره می‌شود",
                        checked = autoBackup,
                        onCheckedChange = viewModel::setAutoBackup
                    )
                }
            }
            item {
                Text(
                    "پیشنهاد: هر هفته یک نسخه پشتیبان روی فضای ابری یا حافظه خارجی ذخیره کنید.",
                    style = MaterialTheme.typography.labelSmall,
                    color = HesabyarTheme.colors.onSurfaceSecondary
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (pendingImport != null) {
        ConfirmDialog(
            title = "بازیابی اطلاعات",
            message = "تمام داده‌های فعلی با محتوای فایل انتخاب‌شده جایگزین می‌شود. این عمل قابل بازگشت نیست. ادامه می‌دهید؟",
            confirmLabel = "بله، بازیابی کن",
            destructive = true,
            onConfirm = {
                pendingImport?.let(viewModel::importJson)
                pendingImport = null
            },
            onDismiss = { pendingImport = null }
        )
    }
}

@Composable
private fun ExportRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, enabled: Boolean) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HesabyarTheme.colors.hairline)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
            }
        }
    }
}
