package ir.hesabyar.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ir.hesabyar.core.database.entity.AccountEntity
import ir.hesabyar.core.database.entity.AccountMovementEntity
import ir.hesabyar.core.database.entity.BusinessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Insert
    suspend fun insert(business: BusinessEntity): Long

    @Update
    suspend fun update(business: BusinessEntity)

    @Query("SELECT * FROM businesses ORDER BY id LIMIT 1")
    suspend fun first(): BusinessEntity?

    @Query("SELECT * FROM businesses ORDER BY id LIMIT 1")
    fun observeFirst(): Flow<BusinessEntity?>

    @Query("SELECT COUNT(*) FROM businesses")
    suspend fun count(): Int
}

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Query("UPDATE accounts SET current_balance = :balance WHERE id = :id")
    suspend fun updateBalance(id: Long, balance: Long)

    @Query("UPDATE accounts SET title = :title, account_type = :type WHERE id = :id")
    suspend fun updateInfo(id: Long, title: String, type: String)

    @Query("SELECT * FROM accounts ORDER BY id")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY id")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun byId(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE account_type = :type ORDER BY id LIMIT 1")
    suspend fun firstOfType(type: String): AccountEntity?

    @Query("SELECT COALESCE(SUM(current_balance), 0) FROM accounts")
    suspend fun totalBalance(): Long

    @Query("SELECT COALESCE(SUM(current_balance), 0) FROM accounts")
    fun observeTotalBalance(): Flow<Long>

    @Insert
    suspend fun insertMovement(movement: AccountMovementEntity): Long

    @Query("SELECT * FROM account_movements WHERE account_id = :accountId ORDER BY created_at DESC, id DESC")
    fun observeMovements(accountId: Long): Flow<List<AccountMovementEntity>>

    @Query("SELECT * FROM account_movements WHERE account_id = :accountId ORDER BY created_at DESC, id DESC")
    suspend fun movements(accountId: Long): List<AccountMovementEntity>

    @Query("SELECT * FROM account_movements WHERE transaction_id = :transactionId ORDER BY id")
    suspend fun movementsForTransaction(transactionId: Long): List<AccountMovementEntity>

    @Query("SELECT COALESCE(SUM(delta), 0) FROM account_movements WHERE account_id = :accountId AND delta > 0")
    suspend fun totalIn(accountId: Long): Long

    @Query("SELECT COALESCE(SUM(delta), 0) FROM account_movements WHERE account_id = :accountId AND delta < 0")
    suspend fun totalOut(accountId: Long): Long

    @Query("SELECT COALESCE(SUM(delta), 0) FROM account_movements WHERE delta > 0 AND type <> 'TRANSFER_IN'")
    fun observeTotalReceived(): Flow<Long>

    @Query("SELECT COALESCE(SUM(delta), 0) FROM account_movements WHERE delta < 0 AND type <> 'TRANSFER_OUT'")
    fun observeTotalPaid(): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(m.delta), 0)
        FROM account_movements m
        JOIN transactions t ON m.transaction_id = t.id
        WHERE m.delta > 0 AND m.type IN ('SALE_PAYMENT', 'CUSTOMER_RECEIPT')
          AND t.status = 'COMPLETED' AND t.date = :date
        """
    )
    fun observeReceivedOn(date: Long): Flow<Long>
}
