package ir.hesabyar.app.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import ir.hesabyar.core.design.components.DropdownField
import ir.hesabyar.core.design.components.EmptyState
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.JalaliDateField
import ir.hesabyar.core.design.components.LabeledSwitch
import ir.hesabyar.core.design.components.MoneyField
import ir.hesabyar.core.design.components.StatusBadge
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactFilter
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.Reminder
import ir.hesabyar.domain.model.ReminderStatus
import ir.hesabyar.domain.model.ReminderType
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.usecase.CompleteReminderUseCase
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.usecase.GetRemindersUseCase
import ir.hesabyar.domain.usecase.SaveReminderUseCase
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RemindersViewModel @Inject constructor(
    getReminders: GetRemindersUseCase,
    private val saveReminder: SaveReminderUseCase,
    private val completeReminder: CompleteReminderUseCase,
    private val getBusiness: GetBusinessUseCase,
    contacts: ContactRepository
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = getReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val contacts: StateFlow<List<Contact>> = contacts.observeContacts(ContactFilter())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun toggle(reminder: Reminder) {
        viewModelScope.launch {
            completeReminder(reminder.id, reminder.status != ReminderStatus.DONE)
        }
    }

    fun add(type: ReminderType, contactId: Long?, amount: Long, dueDate: Long, description: String) {
        viewModelScope.launch {
            val businessId = getBusiness.current()?.id ?: 0L
            val reminder = Reminder(
                businessId = businessId,
                contactId = contactId,
                type = type,
                amount = amount,
                dueDate = dueDate,
                description = description.takeIf { it.isNotBlank() }
            )
            when (val result = saveReminder(reminder)) {
                is OperationResult.Success -> _events.send(UiEvent("یادآوری ثبت شد"))
                is OperationResult.Failure -> _events.send(UiEvent(result.message, isError = true))
            }
        }
    }
}

@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }

    val sorted = reminders.sortedWith(compareBy({ it.status == ReminderStatus.DONE }, { it.dueDate }))
    val pendingCount = reminders.count { it.status == ReminderStatus.PENDING }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "یادآوری‌ها", subtitle = if (pendingCount > 0) "$pendingCount یادآوری در انتظار" else "همه کارها انجام شده", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("یادآوری جدید") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    icon = Icons.Rounded.NotificationsNone,
                    title = "یادآوری‌ای ثبت نشده",
                    message = "برای سررسید چک، بدهی یا سفارش مشتری یادآوری بسازید تا هیچ‌چیز فراموش نشود.",
                    actionLabel = "یادآوری جدید",
                    onAction = { showAdd = true }
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sorted, key = { it.id }) { reminder ->
                    ReminderCard(reminder, onToggle = { viewModel.toggle(reminder) })
                }
            }
        }
    }

    if (showAdd) {
        AddReminderDialog(
            contacts = contacts,
            onConfirm = { type, contactId, amount, due, description ->
                viewModel.add(type, contactId, amount, due, description)
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }
}

@Composable
private fun ReminderCard(reminder: Reminder, onToggle: () -> Unit) {
    val done = reminder.status == ReminderStatus.DONE
    val overdue = !done && reminder.dueDate < JalaliDate.startOfDayMillis(System.currentTimeMillis())
    AppCard(contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle) {
                Icon(
                    if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (done) "انجام شد" else "انجام نشده",
                    tint = if (done) HesabyarTheme.colors.income else MaterialTheme.colorScheme.primary
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = reminder.description ?: reminder.contactName ?: reminder.type.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (done) HesabyarTheme.colors.onSurfaceSecondary else MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        JalaliDate.formatDate(reminder.dueDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = HesabyarTheme.colors.onSurfaceSecondary
                    )
                    Text("·", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                    Text(reminder.type.label, style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                    if (reminder.amount > 0) {
                        Text("·", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                        Text(ir.hesabyar.domain.format.PersianText.formatMoney(reminder.amount), style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                    }
                }
            }
            if (overdue) StatusBadge("سررسید گذشته", HesabyarTheme.colors.expense)
            else if (done) StatusBadge("انجام شد", HesabyarTheme.colors.income)
        }
    }
}

@Composable
private fun AddReminderDialog(
    contacts: List<Contact>,
    onConfirm: (ReminderType, Long?, Long, Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(ReminderType.GENERAL) }
    var contact by remember { mutableStateOf<Contact?>(null) }
    var amount by remember { mutableStateOf<Long?>(null) }
    var due by remember { mutableStateOf(JalaliDate.startOfDayMillis(System.currentTimeMillis())) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("یادآوری جدید") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                ChipSelector(
                    options = ReminderType.entries.toList(),
                    selected = type,
                    onSelect = { type = it },
                    optionLabel = { it.label }
                )
                DropdownField(
                    label = "شخص مرتبط",
                    options = contacts,
                    selected = contact,
                    optionLabel = { it.name },
                    onSelect = { contact = it },
                    placeholder = "اختیاری"
                )
                MoneyField(value = amount, onValueChange = { amount = it }, label = "مبلغ")
                JalaliDateField(label = "تاریخ سررسید", millis = due, onSelect = { due = it })
                AppTextField(value = description, onValueChange = { description = it }, label = "توضیحات")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(type, contact?.id, amount ?: 0L, due, description) }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
