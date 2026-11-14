package ir.hesabyar.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.hesabyar.domain.model.AppSettings
import ir.hesabyar.domain.model.ThemeMode
import ir.hesabyar.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "hesabyar_settings")

private object Keys {
    val THEME = stringPreferencesKey("theme_mode")
    val TEXT_SCALE = floatPreferencesKey("text_scale")
    val PERSIAN_DIGITS = booleanPreferencesKey("persian_digits")
    val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    val LOW_STOCK_ALERTS = booleanPreferencesKey("low_stock_alerts")
    val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
    val DEFAULT_RECEIVE = longPreferencesKey("default_receive_account")
    val DEFAULT_PAY = longPreferencesKey("default_pay_account")
    val LOW_STOCK_THRESHOLD = floatPreferencesKey("low_stock_threshold")
    val LAST_BACKUP = longPreferencesKey("last_auto_backup_at")
}

private fun Preferences.toSettings(): AppSettings = AppSettings(
    themeMode = this[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
    textScale = this[Keys.TEXT_SCALE] ?: 1f,
    persianDigits = this[Keys.PERSIAN_DIGITS] ?: true,
    notificationsEnabled = this[Keys.NOTIFICATIONS] ?: true,
    lowStockAlerts = this[Keys.LOW_STOCK_ALERTS] ?: true,
    autoBackupEnabled = this[Keys.AUTO_BACKUP] ?: true,
    defaultReceiveAccountId = this[Keys.DEFAULT_RECEIVE]?.takeIf { it > 0 },
    defaultPayAccountId = this[Keys.DEFAULT_PAY]?.takeIf { it > 0 },
    lowStockThreshold = (this[Keys.LOW_STOCK_THRESHOLD] ?: 0f).toDouble(),
    lastAutoBackupAt = this[Keys.LAST_BACKUP]?.takeIf { it > 0 }
)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override val settings: Flow<AppSettings> = context.settingsStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toSettings() }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs[Keys.THEME] = updated.themeMode.name
            prefs[Keys.TEXT_SCALE] = updated.textScale
            prefs[Keys.PERSIAN_DIGITS] = updated.persianDigits
            prefs[Keys.NOTIFICATIONS] = updated.notificationsEnabled
            prefs[Keys.LOW_STOCK_ALERTS] = updated.lowStockAlerts
            prefs[Keys.AUTO_BACKUP] = updated.autoBackupEnabled
            prefs[Keys.DEFAULT_RECEIVE] = updated.defaultReceiveAccountId ?: -1L
            prefs[Keys.DEFAULT_PAY] = updated.defaultPayAccountId ?: -1L
            prefs[Keys.LOW_STOCK_THRESHOLD] = updated.lowStockThreshold.toFloat()
            prefs[Keys.LAST_BACKUP] = updated.lastAutoBackupAt ?: -1L
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBindings {
    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
