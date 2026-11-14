package ir.hesabyar.app.ui.contacts

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.Receipts
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.EmptyState
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.LoadingScreen
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.StatCard
import ir.hesabyar.core.design.components.StatusBadge
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactLedgerEntry
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.repository.ContactTotals
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactDetailUiState(
    val loading: Boolean = true,
    val contact: Contact? = null,
    val ledger: List<ContactLedgerEntry> = emptyList(),
    val totals: ContactTotals = ContactTotals(),
    val businessName: String = ""
)

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ContactRepository,
    private val getBusiness: GetBusinessUseCase
) : ViewModel() {

    private val contactId: Long = savedStateHandle.get<Long>("contactId") ?: 0L
    private val _state = MutableStateFlow(ContactDetailUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeContact(contactId).collect { contact ->
                _state.update { it.copy(contact = contact, loading = false) }
            }
        }
        viewModelScope.launch {
            repository.observeLedger(contactId).collect { ledger ->
                val totals = repository.getTotals(contactId)
                _state.update { it.copy(ledger = ledger, totals = totals) }
            }
        }
        viewModelScope.launch {
            val business = getBusiness.current()
            _state.update { it.copy(businessName = business?.name.orEmpty()) }
        }
    }
}

@Composable
fun ContactDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onReceive: (Long) -> Unit,
    onPay: (Long) -> Unit,
    onOpeningBalance: (Long) -> Unit,
    viewModel: ContactDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val contact = state.contact

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HesabyarTopBar(
                title = contact?.name ?: "جزئیات شخص",
                onBack = onBack,
                actions = {
                    contact?.let {
                        IconButton(onClick = { onEdit(it.id) }) { Icon(Icons.Rounded.Edit, contentDescription = "ویرایش") }
                        IconButton(
                            onClick = {
                                val text = Receipts.contactStatement(it, state.ledger, state.totals, state.businessName)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "اشتراک صورت‌حساب"))
                            }
                        ) { Icon(Icons.Rounded.Share, contentDescription = "صورت‌حساب") }
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingScreen() }
        } else if (contact == null) {
            EmptyState(title = "شخص پیدا نشد", message = "ممکن است حذف شده باشد.", actionLabel = "بازگشت", onAction = onBack)
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AppCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(contact.name, style = MaterialTheme.typography.titleLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusBadge(contact.type.label, MaterialTheme.colorScheme.primary)
                                contact.phone?.let { StatusBadge(PersianText.toPersianDigits(it), HesabyarTheme.colors.neutral) }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                when {
                                    contact.balance > 0 -> "بدهکار به شما"
                                    contact.balance < 0 -> "طلبکار از شما"
                                    else -> "حساب تسویه"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = HesabyarTheme.colors.onSurfaceSecondary
                            )
                            MoneyText(
                                amount = kotlin.math.abs(contact.balance),
                                style = MaterialTheme.typography.headlineMedium,
                                color = when {
                                    contact.balance > 0 -> HesabyarTheme.colors.receivable
                                    contact.balance < 0 -> HesabyarTheme.colors.payable
                                    else -> HesabyarTheme.colors.neutral
                                },
                                showCurrency = true
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContactAction("دریافت", Icons.Rounded.CallReceived, Modifier.weight(1f)) { onReceive(contact.id) }
                        ContactAction("پرداخت", Icons.Rounded.Payments, Modifier.weight(1f)) { onPay(contact.id) }
                        ContactAction("مانده اولیه", Icons.Rounded.Tune, Modifier.weight(1f)) { onOpeningBalance(contact.id) }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (contact.type.isCustomer) {
                            StatCard(
                                title = "مجموع فروش",
                                amount = state.totals.totalSales,
                                icon = Icons.Rounded.Person,
                                tint = HesabyarTheme.colors.income,
                                modifier = Modifier.weight(1f),
                                caption = "دریافتی: ${PersianText.formatMoney(state.totals.totalReceipts)}"
                            )
                        }
                        if (contact.type.isSupplier) {
                            StatCard(
                                title = "مجموع خرید",
                                amount = state.totals.totalPurchases,
                                icon = Icons.Rounded.Payments,
                                tint = HesabyarTheme.colors.payable,
                                modifier = Modifier.weight(1f),
                                caption = "پرداختی: ${PersianText.formatMoney(state.totals.totalPayments)}"
                            )
                        }
                    }
                }
                item { Text("گردش حساب", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
                if (state.ledger.isEmpty()) {
                    item { EmptyState(title = "گردشی ثبت نشده", message = "با ثبت فروش نسیه یا دریافت، گردش حساب این شخص نمایش داده می‌شود.") }
                } else {
                    items(state.ledger, key = { it.id }) { entry ->
                        AppCard(contentPadding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.note ?: entry.transactionCode ?: "ثبت خودکار", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        JalaliDate.formatLongDate(entry.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HesabyarTheme.colors.onSurfaceSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (entry.debit > 0) {
                                        MoneyText(entry.debit, style = MaterialTheme.typography.labelLarge, color = HesabyarTheme.colors.receivable, signed = true)
                                    } else if (entry.credit > 0) {
                                        MoneyText(-entry.credit, style = MaterialTheme.typography.labelLarge, color = HesabyarTheme.colors.payable, signed = true)
                                    }
                                    Text(
                                        "مانده: ${PersianText.formatNumber(entry.balanceAfter)}",
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
private fun ContactAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HesabyarTheme.colors.hairline)
    ) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
