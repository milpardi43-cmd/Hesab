package ir.hesabyar.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.components.SecondaryButton
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.components.StatusBadge
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.PlanLimits
import ir.hesabyar.domain.model.PlanType
import ir.hesabyar.domain.model.Subscription
import ir.hesabyar.domain.model.SubscriptionStatus
import ir.hesabyar.domain.usecase.ActivateSubscriptionUseCase
import ir.hesabyar.domain.usecase.GetSubscriptionUseCase
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    getSubscription: GetSubscriptionUseCase,
    private val activate: ActivateSubscriptionUseCase
) : ViewModel() {

    val subscription: StateFlow<Subscription> = getSubscription()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Subscription(businessId = 0))

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun activate(plan: PlanType) {
        viewModelScope.launch {
            activate(plan)
            _events.send(UiEvent(if (plan == PlanType.FREE) "نسخه رایگان فعال شد" else "نسخه ${plan.label} فعال شد"))
        }
    }
}

@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val subscription by viewModel.subscription.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "اشتراک حسابیار", subtitle = "ابزار بیشتر برای فروشگاه در حال رشد", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard(containerColor = MaterialTheme.colorScheme.primary, border = null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(34.dp))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text("نسخه فعلی: ${subscription.plan.label}", style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.White)
                            Text(
                                when {
                                    subscription.status == SubscriptionStatus.ACTIVE && subscription.endDate != null ->
                                        "فعال تا ${JalaliDate.formatLongDate(subscription.endDate!!)}"
                                    else -> "همیشه رایگان برای شروع"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "حسابیار نسخه پایه برای همیشه رایگان است. با ارتقا، محدودیت تعداد اشخاص و کالا برداشته می‌شود و گزارش‌های پیشرفته، خروجی اکسل و همگام‌سازی در اختیار شما قرار می‌گیرد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HesabyarTheme.colors.onSurfaceSecondary
                )
            }
            items(PlanType.entries.size) { index ->
                val plan = PlanType.entries[index]
                PlanCard(
                    plan = plan,
                    current = subscription.plan == plan,
                    onActivate = { viewModel.activate(plan) }
                )
            }
            item {
                Text(
                    "درگاه پرداخت در نسخه منتشرشده به اپ متصل می‌شود. در حال حاضر فعال‌سازی به‌صورت آزمایشی و بدون پرداخت انجام می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = HesabyarTheme.colors.onSurfaceSecondary
                )
            }
        }
    }
}

@Composable
private fun PlanCard(plan: PlanType, current: Boolean, onActivate: () -> Unit) {
    val limits = PlanLimits.of(plan)
    AppCard(border = BorderStroke(1.dp, if (current) MaterialTheme.colorScheme.primary else HesabyarTheme.colors.hairline)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (current) StatusBadge("فعال", MaterialTheme.colorScheme.primary)
            }
            Text(planDescription(plan), style = MaterialTheme.typography.bodySmall, color = HesabyarTheme.colors.onSurfaceSecondary)
            LimitRow("تعداد اشخاص", limits.maxContacts?.let { "تا ${PersianText.formatNumber(it.toLong())} نفر" } ?: "نامحدود")
            LimitRow("تعداد کالا", limits.maxProducts?.let { "تا ${PersianText.formatNumber(it.toLong())} کالا" } ?: "نامحدود")
            LimitRow("گزارش‌های پیشرفته", if (limits.advancedReports) "دارد" else "—")
            LimitRow("خروجی اکسل", if (limits.excelExport) "دارد" else "—")
            LimitRow("همگام‌سازی ابری", if (limits.cloudSync) "دارد" else "—")
            Spacer(Modifier.height(4.dp))
            if (current) {
                SecondaryButton(text = "نسخه فعال", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            } else {
                PrimaryButton(text = "فعال‌سازی آزمایشی", onClick = onActivate)
            }
        }
    }
}

@Composable
private fun LimitRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = HesabyarTheme.colors.income, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(start = 6.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
    }
}

private fun planDescription(plan: PlanType): String = when (plan) {
    PlanType.FREE -> "برای شروع و فروشگاه‌های کوچک"
    PlanType.PRO -> "برای فروشگاه‌هایی که هر روز رشد می‌کنند"
    PlanType.BUSINESS -> "برای کسب‌وکارهای چندشعبه‌ای و تیم‌ها"
}
