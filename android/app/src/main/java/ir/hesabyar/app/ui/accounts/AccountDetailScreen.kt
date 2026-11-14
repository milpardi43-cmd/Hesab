package ir.hesabyar.app.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.EmptyState
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.InfoRow
import ir.hesabyar.core.design.components.LoadingScreen
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.SecondaryButton
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.AccountMovement
import ir.hesabyar.domain.repository.AccountRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AccountDetailUiState(
    val account: Account? = null,
    val movements: List<AccountMovement> = emptyList(),
    val totalIn: Long = 0,
    val totalOut: Long = 0
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: AccountRepository
) : ViewModel() {
    private val accountId: Long = savedStateHandle.get<Long>("accountId") ?: 0L

    val state: StateFlow<AccountDetailUiState> = combine(
        repository.observeAccounts(),
        repository.observeMovements(accountId)
    ) { accounts, movements ->
        AccountDetailUiState(
            account = accounts.firstOrNull { it.id == accountId },
            movements = movements,
            totalIn = movements.filter { it.delta > 0 }.sumOf { it.delta },
            totalOut = -movements.filter { it.delta < 0 }.sumOf { it.delta }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountDetailUiState())
}

@Composable
fun AccountDetailScreen(
    onBack: () -> Unit,
    onAdjust: (Long) -> Unit,
    onTransfer: (Long) -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val account = state.account

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = account?.title ?: "جزئیات حساب", onBack = onBack) }
    ) { padding ->
        if (account == null) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingScreen() }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AppCard(containerColor = MaterialTheme.colorScheme.primary, border = null) {
                        Column {
                            Text(account.type.label, style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f))
                            MoneyText(account.currentBalance, style = MaterialTheme.typography.headlineMedium, color = androidx.compose.ui.graphics.Color.White, showCurrency = true)
                            InfoRow("موجودی اولیه", value = PersianText.formatMoney(account.initialBalance), valueColor = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton(
                            text = "تنظیم موجودی",
                            onClick = { onAdjust(account.id) },
                            icon = Icons.Rounded.Tune,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "انتقال وجه",
                            onClick = { onTransfer(account.id) },
                            icon = Icons.Rounded.SwapHoriz,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    AppCard {
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text("کل ورودی", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                                MoneyText(state.totalIn, style = MaterialTheme.typography.titleMedium, color = HesabyarTheme.colors.income)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("کل خروجی", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                                MoneyText(state.totalOut, style = MaterialTheme.typography.titleMedium, color = HesabyarTheme.colors.expense)
                            }
                        }
                    }
                }
                item { Text("گردش حساب", style = MaterialTheme.typography.titleMedium) }
                if (state.movements.isEmpty()) {
                    item { EmptyState(title = "گردشی ثبت نشده", message = "با فروش نقدی، دریافت یا پرداخت، گردش این حساب نمایش داده می‌شود.") }
                } else {
                    items(state.movements, key = { it.id }) { movement ->
                        AppCard(contentPadding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(movement.type.label, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        JalaliDate.formatLongDate(movement.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HesabyarTheme.colors.onSurfaceSecondary
                                    )
                                    movement.note?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    MoneyText(
                                        movement.delta,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (movement.delta >= 0) HesabyarTheme.colors.income else HesabyarTheme.colors.expense,
                                        signed = true
                                    )
                                    Text(
                                        "مانده: ${PersianText.formatNumber(movement.balanceAfter)}",
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
