package ir.hesabyar.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_REMINDERS = "hesabyar_reminders"
        const val REMINDER_NOTIFICATION_ID = 4101
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_REMINDERS,
                "یادآوری‌های حسابیار",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "یادآوری بدهی مشتریان، سررسید تأمین‌کنندگان و کالاهای کم‌موجود"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun showReminderSummary(remindersCount: Int, lowStockCount: Int) {
        if (!canPostNotifications()) return
        ensureChannel()
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val text = buildString {
            if (remindersCount > 0) append("$remindersCount یادآوری سررسید")
            if (remindersCount > 0 && lowStockCount > 0) append(" و ")
            if (lowStockCount > 0) append("$lowStockCount کالای کم‌موجود")
            append(" دارید.")
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("حسابیار")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply { pending?.let { setContentIntent(it) } }
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
        }
    }
}
