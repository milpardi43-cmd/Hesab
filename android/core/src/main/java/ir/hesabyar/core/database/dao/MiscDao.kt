package ir.hesabyar.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import ir.hesabyar.core.database.entity.BackupEntity
import ir.hesabyar.core.database.entity.ReminderEntity
import ir.hesabyar.core.database.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

data class ReminderRow(
    @Embedded val reminder: ReminderEntity,
    @ColumnInfo(name = "contact_name") val contactName: String?
)

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Query(
        """
        SELECT r.*, c.name AS contact_name FROM reminders r
        LEFT JOIN contacts c ON r.contact_id = c.id
        ORDER BY CASE r.status WHEN 'PENDING' THEN 0 WHEN 'DONE' THEN 1 ELSE 2 END, r.due_date ASC
        """
    )
    fun observeAll(): Flow<List<ReminderRow>>

    @Query("UPDATE reminders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("SELECT * FROM reminders WHERE status = 'PENDING' AND due_date <= :now ORDER BY due_date")
    suspend fun pendingDue(now: Long): List<ReminderEntity>

    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'PENDING'")
    suspend fun pendingCount(): Int
}

@Dao
interface BackupDao {
    @Insert
    suspend fun insert(backup: BackupEntity): Long

    @Query("SELECT * FROM backups ORDER BY backup_date DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<BackupEntity>
}

@Dao
interface SubscriptionDao {
    @Insert
    suspend fun insert(subscription: SubscriptionEntity): Long

    @Query("SELECT * FROM subscriptions ORDER BY id DESC LIMIT 1")
    fun observeLatest(): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscriptions ORDER BY id DESC LIMIT 1")
    suspend fun latest(): SubscriptionEntity?
}
