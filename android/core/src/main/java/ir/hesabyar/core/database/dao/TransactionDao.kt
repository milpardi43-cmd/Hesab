package ir.hesabyar.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.hesabyar.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class TransactionRow(
    @Embedded val transaction: TransactionEntity,
    @ColumnInfo(name = "contact_name") val contactName: String?,
    @ColumnInfo(name = "account_title") val accountTitle: String?
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE operation_id = :operationId LIMIT 1")
    suspend fun byOperationId(operationId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE reference_id = :transactionId AND type = 'REVERSAL' LIMIT 1")
    suspend fun reversalOf(transactionId: Long): TransactionEntity?

    @Query("UPDATE transactions SET status = :status, updated_at = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, now: Long)

    @Query("SELECT COALESCE(MAX(code), '') FROM transactions WHERE code LIKE :prefix || '%'")
    suspend fun maxCodeForPrefix(prefix: String): String

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeCount(): Flow<Int>

    @Query(
        """
        SELECT t.*, c.name AS contact_name, a.title AS account_title
        FROM transactions t
        LEFT JOIN contacts c ON t.contact_id = c.id
        LEFT JOIN accounts a ON t.account_id = a.id
        ORDER BY t.date DESC, t.created_at DESC, t.id DESC
        """
    )
    fun observeAll(): Flow<List<TransactionRow>>

    @Query(
        """
        SELECT t.*, c.name AS contact_name, a.title AS account_title
        FROM transactions t
        LEFT JOIN contacts c ON t.contact_id = c.id
        LEFT JOIN accounts a ON t.account_id = a.id
        ORDER BY t.created_at DESC, t.id DESC
        LIMIT :limit
        """
    )
    fun observeRecent(limit: Int): Flow<List<TransactionRow>>
}
