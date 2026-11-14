package ir.hesabyar.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactFilter
import ir.hesabyar.domain.model.PaymentStatus
import ir.hesabyar.domain.model.Transaction
import ir.hesabyar.domain.model.TransactionFilter
import ir.hesabyar.domain.model.TransactionType
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.usecase.GetTransactionsUseCase
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
class TransactionsViewModel @Inject constructor(
    private val getTransactions: GetTransactionsUseCase,
    contacts: ContactRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = _filter
        .flatMapLatest { getTransactions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val contacts: StateFlow<List<Contact>> = contacts.observeContacts(ContactFilter())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) = _filter.update { it.copy(query = value) }

    fun apply(
        type: TransactionType?,
        paymentStatus: PaymentStatus?,
        contactId: Long?,
        from: Long?,
        to: Long?,
        newestFirst: Boolean
    ) = _filter.update {
        it.copy(
            type = type,
            paymentStatus = paymentStatus,
            contactId = contactId,
            from = from,
            to = to?.let { end -> end + 86_399_999L },
            newestFirst = newestFirst
        )
    }

    fun reset() {
        _filter.value = TransactionFilter()
    }
}
