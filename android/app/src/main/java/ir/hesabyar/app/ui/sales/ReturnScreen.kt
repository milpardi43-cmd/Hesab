package ir.hesabyar.app.ui.sales

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabyar.app.ui.common.AccountDropdown
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.ErrorBanner
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.InfoRow
import ir.hesabyar.core.design.components.LabeledSwitch
import ir.hesabyar.core.design.components.LoadingScreen
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.components.QuantityField
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText

@Composable
fun ReturnScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: ReturnViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "برگشت از فروش", subtitle = state.transaction?.code, onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingScreen() }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.error != null) ErrorBanner(state.error!!)
                state.transaction?.let { tx ->
                    AppCard {
                        Column {
                            InfoRow("شماره فروش", value = PersianText.toPersianDigits(tx.code))
                            InfoRow("تاریخ", value = JalaliDate.formatLongDate(tx.date))
                            InfoRow("مبلغ فروش", value = PersianText.formatMoney(tx.amount))
                            InfoRow("مانده نسیه", value = PersianText.formatMoney(tx.remainingAmount))
                        }
                    }
                }
                SectionHeader(title = "کالاهای برگشتی")
                state.lines.forEach { line ->
                    AppCard(contentPadding = PaddingValues(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(line.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "فروش: ${PersianText.formatQuantity(line.soldQuantity)} · قبلاً برگشتی: ${PersianText.formatQuantity(line.returnedQuantity)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HesabyarTheme.colors.onSurfaceSecondary
                                    )
                                }
                                MoneyText(amount = line.amount, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            QuantityField(
                                value = line.quantity,
                                onValueChange = { viewModel.setQuantity(line.saleItemId, it ?: 0.0) },
                                label = "تعداد برگشتی (حداکثر ${PersianText.formatQuantity(line.remaining)})"
                            )
                        }
                    }
                }
                SectionHeader(title = "نحوه تسویه")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LabeledSwitch(
                            title = "بازپرداخت نقدی",
                            subtitle = "در صورت خاموش بودن، ابتدا مانده نسیه مشتری کاهش می‌یابد",
                            checked = state.refundCash,
                            onCheckedChange = viewModel::setRefundCash
                        )
                        if (state.refundCash) {
                            AccountDropdown(
                                label = "حساب بازپرداخت",
                                accounts = state.accounts,
                                selectedId = state.accountId,
                                onSelect = viewModel::setAccount
                            )
                        }
                        LabeledSwitch(
                            title = "کالا سالم است و به موجودی برمی‌گردد",
                            subtitle = "در صورت خاموش بودن، فقط بهای کالا برگشت می‌خورد",
                            checked = state.sellable,
                            onCheckedChange = viewModel::setSellable
                        )
                        AppTextField(
                            value = state.reason,
                            onValueChange = viewModel::setReason,
                            label = "دلیل برگشت *",
                            placeholder = "مثلاً کالای معیوب"
                        )
                        InfoRow(label = "مبلغ کل برگشت", valueContent = {
                            MoneyText(amount = state.total, style = MaterialTheme.typography.titleSmall, color = HesabyarTheme.colors.expense)
                        })
                    }
                }
                PrimaryButton(text = "ثبت برگشت از فروش", onClick = viewModel::submit, loading = state.saving)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
