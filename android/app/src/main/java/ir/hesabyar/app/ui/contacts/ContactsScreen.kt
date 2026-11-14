package ir.hesabyar.app.ui.contacts

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.core.design.components.EmptyState
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.SearchField
import ir.hesabyar.core.design.components.StatusBadge
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactFilter
import ir.hesabyar.domain.model.ContactType
import ir.hesabyar.domain.usecase.GetContactsUseCase
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactsViewModel @Inject constructor(
    getContacts: GetContactsUseCase
) : ViewModel() {

    private val _filter = MutableStateFlow(ContactFilter())
    val filter: StateFlow<ContactFilter> = _filter.asStateFlow()

    val contacts: StateFlow<List<Contact>> = _filter
        .flatMapLatest { getContacts(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) = _filter.update { it.copy(query = value) }
    fun setType(value: ContactType?) = _filter.update { it.copy(type = value) }
    fun toggleOnlyWithBalance() = _filter.update { it.copy(onlyWithBalance = !it.onlyWithBalance) }
    fun toggleSortByBalance() = _filter.update { it.copy(sortByBalance = !it.sortByBalance) }
}

@Composable
fun ContactsScreen(
    onOpenContact: (Long) -> Unit,
    onAddContact: () -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "اشخاص", subtitle = "مشتریان و تأمین‌کنندگان") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddContact,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("شخص جدید") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(
                    value = filter.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "جست‌وجوی نام یا موبایل"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContactTypeChip("همه", filter.type == null) { viewModel.setType(null) }
                    ContactTypeChip("مشتری", filter.type == ContactType.CUSTOMER) { viewModel.setType(ContactType.CUSTOMER) }
                    ContactTypeChip("تأمین‌کننده", filter.type == ContactType.SUPPLIER) { viewModel.setType(ContactType.SUPPLIER) }
                    ContactTypeChip("بدهکار", filter.onlyWithBalance) { viewModel.toggleOnlyWithBalance() }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (contacts.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.People,
                    title = "شخصی ثبت نشده",
                    message = "مشتریان و تأمین‌کنندگان را اضافه کنید تا بدهی‌ها و دریافت‌ها را دقیق پیگیری کنید.",
                    actionLabel = "افزودن شخص",
                    onAction = onAddContact
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        ContactRow(contact, onClick = { onOpenContact(contact.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, HesabyarTheme.colors.hairline)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else HesabyarTheme.colors.onSurfaceSecondary
        )
    }
}

@Composable
private fun ContactRow(contact: Contact, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape)
                    .background(if (contact.type.isSupplier && !contact.type.isCustomer) MaterialTheme.colorScheme.primaryContainer else HesabyarTheme.colors.positiveContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.trim().take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        contact.phone ?: contact.type.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = HesabyarTheme.colors.onSurfaceSecondary
                    )
                    if (contact.type == ContactType.BOTH) {
                        StatusBadge("مشتری و تأمین‌کننده", MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    amount = kotlin.math.abs(contact.balance),
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        contact.balance > 0 -> HesabyarTheme.colors.receivable
                        contact.balance < 0 -> HesabyarTheme.colors.payable
                        else -> HesabyarTheme.colors.neutral
                    }
                )
                Text(
                    text = when {
                        contact.balance > 0 -> "بدهکار"
                        contact.balance < 0 -> "بستانکار"
                        else -> "تسویه"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = HesabyarTheme.colors.onSurfaceSecondary
                )
            }
        }
    }
}
