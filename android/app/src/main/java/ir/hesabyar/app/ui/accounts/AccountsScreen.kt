package ir.hesabyar.app.ui.accounts

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.ChipSelector
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.MoneyField
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.AccountType
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.usecase.CreateAccountUseCase
import ir.hesabyar.domain.usecase.GetAccountsUseCase
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AccountsViewModel @Inject constructor(
    getAccounts: GetAccountsUseCase,
    private val createAccount: CreateAccountUseCase
) : ViewModel() {
    val accounts: StateFlow<List<Account>> = getAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun create(title: String, type: AccountType, initial: Long) {
        viewModelScope.launch {
            when (val result = createAccount(title, type, initial)) {
                is OperationResult.Success -> _events.send(UiEvent("حساب جدید ایجاد شد"))
                is OperationResult.Failure -> _events.send(UiEvent(result.message, isError = true))
            }
        }
    }
}

@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    onTransfer: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    val total = accounts.sumOf { it.currentBalance }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "حساب‌ها", subtitle = "صندوق، بانک و کارتخوان", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("حساب جدید") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                AppCard(containerColor = MaterialTheme.colorScheme.primary, border = null) {
                    Column {
                        Text("موجودی کل حساب‌ها", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                        MoneyText(amount = total, style = MaterialTheme.typography.headlineMedium, color = Color.White, showCurrency = true)
                    }
                }
            }
            item {
                SectionHeader(
                    title = "انتقال بین حساب‌ها",
                    actionLabel = "انتقال وجه",
                    onAction = onTransfer
                )
            }
            items(accounts, key = { it.id }) { account ->
                AccountRow(account, onClick = { onOpenAccount(account.id) })
            }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            onConfirm = { title, type, initial ->
                viewModel.create(title, type, initial)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

private fun accountIcon(type: AccountType): ImageVector = when (type) {
    AccountType.CASH -> Icons.Rounded.AccountBalanceWallet
    AccountType.BANK -> Icons.Rounded.AccountBalance
    AccountType.POS -> Icons.Rounded.CreditCard
    AccountType.OTHER -> Icons.Rounded.SwapHoriz
}

@Composable
private fun AccountRow(account: Account, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).then(Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(accountIcon(account.type), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(account.title, style = MaterialTheme.typography.bodyLarge)
                Text(account.type.label, style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
            }
            MoneyText(amount = account.currentBalance, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AddAccountDialog(
    onConfirm: (String, AccountType, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.BANK) }
    var initial by remember { mutableStateOf<Long?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حساب جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(value = title, onValueChange = { title = it }, label = "عنوان حساب *")
                ChipSelector(
                    options = AccountType.entries.toList(),
                    selected = type,
                    onSelect = { type = it },
                    optionLabel = { it.label }
                )
                MoneyField(value = initial, onValueChange = { initial = it }, label = "موجودی اولیه")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onConfirm(title, type, initial ?: 0L) },
                enabled = title.isNotBlank()
            ) { Text("ایجاد") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
