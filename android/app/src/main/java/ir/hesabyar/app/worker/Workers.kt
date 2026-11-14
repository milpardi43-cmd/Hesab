package ir.hesabyar.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.hesabyar.core.notifications.NotificationHelper
import ir.hesabyar.domain.repository.BackupRepository
import ir.hesabyar.domain.repository.ProductRepository
import ir.hesabyar.domain.repository.ReminderRepository
import ir.hesabyar.domain.repository.SettingsRepository
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminders: ReminderRepository,
    private val products: ProductRepository,
    private val settings: SettingsRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val current = settings.current()
        if (!current.notificationsEnabled) return Result.success()
        val due = reminders.pendingDue(System.currentTimeMillis())
        val lowStock = if (current.lowStockAlerts) products.observeLowStockCount().firstOrNull() ?: 0 else 0
        if (due.isNotEmpty() || lowStock > 0) {
            notificationHelper.showReminderSummary(due.size, lowStock)
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "hesabyar_reminders"
    }
}

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val settings: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val current = settings.current()
        if (!current.autoBackupEnabled) return Result.success()
        return runCatching {
            val json = backupRepository.exportJson()
            val dir = File(context.filesDir, "backups").apply { mkdirs() }
            val file = File(dir, "hesabyar-auto-${System.currentTimeMillis()}.json")
            file.writeText(json, Charsets.UTF_8)
            val files = dir.listFiles { f -> f.name.startsWith("hesabyar-auto-") }?.sortedBy { it.lastModified() }.orEmpty()
            files.dropLast(7).forEach { it.delete() }
            settings.update { it.copy(lastAutoBackupAt = System.currentTimeMillis()) }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val UNIQUE_NAME = "hesabyar_auto_backup"
    }
}
