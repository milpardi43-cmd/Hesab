package ir.hesabyar.data.repository

import androidx.room.withTransaction
import ir.hesabyar.core.database.HesabyarDatabase
import ir.hesabyar.core.database.dao.ContactDao
import ir.hesabyar.core.database.entity.ContactEntity
import ir.hesabyar.core.database.entity.ContactLedgerEntity
import ir.hesabyar.data.mapper.toDomain
import ir.hesabyar.data.posting.PostingExecutor
import ir.hesabyar.domain.accounting.AccountingEngine
import ir.hesabyar.domain.accounting.PaySupplierCommand
import ir.hesabyar.domain.accounting.ReceivePaymentCommand
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactFilter
import ir.hesabyar.domain.model.ContactLedgerEntry
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.repository.ContactTotals
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val db: HesabyarDatabase,
    private val contactDao: ContactDao,
    private val postingExecutor: PostingExecutor
) : ContactRepository {

    override fun observeContacts(filter: ContactFilter): Flow<List<Contact>> =
        contactDao.observeAll().map { rows ->
            var list = rows.map { it.toDomain() }
            val query = filter.query.trim()
            if (query.isNotEmpty()) {
                list = list.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.phone?.contains(query) == true ||
                        it.description?.contains(query, ignoreCase = true) == true
                }
            }
            filter.type?.let { type ->
                list = list.filter { contact ->
                    when (type) {
                        ir.hesabyar.domain.model.ContactType.CUSTOMER -> contact.type.isCustomer
                        ir.hesabyar.domain.model.ContactType.SUPPLIER -> contact.type.isSupplier
                        ir.hesabyar.domain.model.ContactType.BOTH -> contact.type == ir.hesabyar.domain.model.ContactType.BOTH
                    }
                }
            }
            if (filter.onlyWithBalance) list = list.filter { it.balance != 0L }
            if (filter.sortByBalance) {
                list = list.sortedByDescending { kotlin.math.abs(it.balance) }
            }
            list
        }

    override fun observeContact(id: Long): Flow<Contact?> =
        contactDao.observeById(id).map { it?.toDomain() }

    override suspend fun getContact(id: Long): Contact? = contactDao.byId(id)?.toDomain()

    override suspend fun getContacts(): List<Contact> = contactDao.getAll().map { it.toDomain() }

    override suspend fun createContact(contact: Contact): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val id = contactDao.insert(
            ContactEntity(
                businessId = contact.businessId,
                name = contact.name.trim(),
                phone = contact.phone?.takeIf { it.isNotBlank() },
                contactType = contact.type.name,
                initialBalance = contact.initialBalance,
                balance = contact.initialBalance,
                description = contact.description?.takeIf { it.isNotBlank() },
                createdAt = now
            )
        )
        if (contact.initialBalance != 0L) {
            contactDao.insertLedger(
                ContactLedgerEntity(
                    contactId = id,
                    transactionId = null,
                    debit = if (contact.initialBalance > 0) contact.initialBalance else 0,
                    credit = if (contact.initialBalance < 0) -contact.initialBalance else 0,
                    balanceAfter = contact.initialBalance,
                    note = "مانده اولیه حساب",
                    createdAt = now
                )
            )
        }
        id
    }

    override suspend fun updateContact(contact: Contact) {
        contactDao.updateInfo(
            id = contact.id,
            name = contact.name.trim(),
            phone = contact.phone?.takeIf { it.isNotBlank() },
            type = contact.type.name,
            description = contact.description?.takeIf { it.isNotBlank() }
        )
    }

    override suspend fun addOpeningBalance(contactId: Long, amount: Long, note: String?): OperationResult<Unit> {
        return try {
            db.withTransaction {
                val contact = contactDao.byId(contactId)
                    ?: throw IllegalStateException("شخص مورد نظر یافت نشد")
                val newBalance = contact.balance + amount
                contactDao.updateBalance(contact.id, newBalance)
                contactDao.insertLedger(
                    ContactLedgerEntity(
                        contactId = contact.id,
                        transactionId = null,
                        debit = if (amount > 0) amount else 0,
                        credit = if (amount < 0) -amount else 0,
                        balanceAfter = newBalance,
                        note = note?.takeIf { it.isNotBlank() } ?: "ثبت مانده اولیه",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            OperationResult.Success(Unit)
        } catch (error: Exception) {
            OperationResult.Failure(error.message ?: "خطا در ثبت مانده اولیه")
        }
    }

    override fun observeLedger(contactId: Long): Flow<List<ContactLedgerEntry>> =
        contactDao.observeLedger(contactId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getTotals(contactId: Long): ContactTotals = ContactTotals(
        totalSales = contactDao.totalSales(contactId),
        totalReceipts = contactDao.totalReceipts(contactId),
        totalPurchases = contactDao.totalPurchases(contactId),
        totalPayments = contactDao.totalPayments(contactId),
        lastActivity = contactDao.lastActivity(contactId)
    )

    override suspend fun receive(cmd: ReceivePaymentCommand): OperationResult<Long> {
        val plan = AccountingEngine.receipt(cmd)
        return when (plan) {
            is OperationResult.Failure -> plan
            is OperationResult.Success -> postingExecutor.execute(plan.data)
        }
    }

    override suspend fun pay(cmd: PaySupplierCommand): OperationResult<Long> {
        val plan = AccountingEngine.supplierPayment(cmd)
        return when (plan) {
            is OperationResult.Failure -> plan
            is OperationResult.Success -> postingExecutor.execute(plan.data)
        }
    }

    override suspend fun countContacts(): Int = contactDao.count()
}
