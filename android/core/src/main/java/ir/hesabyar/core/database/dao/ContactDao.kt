package ir.hesabyar.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import ir.hesabyar.core.database.entity.ContactEntity
import ir.hesabyar.core.database.entity.ContactLedgerEntity
import kotlinx.coroutines.flow.Flow

data class LedgerRow(
    @Embedded val entry: ContactLedgerEntity,
    @ColumnInfo(name = "transaction_code") val transactionCode: String?,
    @ColumnInfo(name = "transaction_type") val transactionType: String?
)

@Dao
interface ContactDao {
    @Insert
    suspend fun insert(contact: ContactEntity): Long

    @Query(
        """
        UPDATE contacts SET name = :name, phone = :phone, contact_type = :type, description = :description
        WHERE id = :id
        """
    )
    suspend fun updateInfo(id: Long, name: String, phone: String?, type: String, description: String?)

    @Query("UPDATE contacts SET balance = :balance WHERE id = :id")
    suspend fun updateBalance(id: Long, balance: Long)

    @Query("SELECT * FROM contacts ORDER BY name")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY name")
    suspend fun getAll(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun observeById(id: Long): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun byId(id: Long): ContactEntity?

    @Query("SELECT COALESCE(SUM(balance), 0) FROM contacts WHERE balance > 0")
    fun observeReceivable(): Flow<Long>

    @Query("SELECT COALESCE(SUM(balance), 0) FROM contacts WHERE balance < 0")
    fun observePayable(): Flow<Long>

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun count(): Int

    @Insert
    suspend fun insertLedger(entry: ContactLedgerEntity): Long

    @Query(
        """
        SELECT l.*, t.code AS transaction_code, t.type AS transaction_type
        FROM contact_ledger l
        LEFT JOIN transactions t ON l.transaction_id = t.id
        WHERE l.contact_id = :contactId
        ORDER BY l.created_at DESC, l.id DESC
        """
    )
    fun observeLedger(contactId: Long): Flow<List<LedgerRow>>

    @Query("SELECT * FROM contact_ledger WHERE transaction_id = :transactionId ORDER BY id")
    suspend fun ledgerForTransaction(transactionId: Long): List<ContactLedgerEntity>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE contact_id = :contactId AND type IN ('CASH_SALE', 'CREDIT_SALE', 'MIXED_SALE')
          AND status = 'COMPLETED'
        """
    )
    suspend fun totalSales(contactId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE contact_id = :contactId AND type = 'CUSTOMER_RECEIPT' AND status = 'COMPLETED'
        """
    )
    suspend fun totalReceipts(contactId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE contact_id = :contactId AND type = 'PURCHASE' AND status = 'COMPLETED'
        """
    )
    suspend fun totalPurchases(contactId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE contact_id = :contactId AND type = 'SUPPLIER_PAYMENT' AND status = 'COMPLETED'
        """
    )
    suspend fun totalPayments(contactId: Long): Long

    @Query("SELECT MAX(created_at) FROM transactions WHERE contact_id = :contactId")
    suspend fun lastActivity(contactId: Long): Long?
}
