package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val code: String) {
    INDONESIAN("id"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INDONESIAN
        }
    }
}

data class UserPreferences(
    val soundOnScan: Boolean = true,
    val vibrationOnScan: Boolean = true,
    val autoSaveHistory: Boolean = true,
    val batchScanMode: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val language: AppLanguage = AppLanguage.INDONESIAN
)

private val Context.scanovaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "scanova_preferences"
)

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.scanovaDataStore)

    private object Keys {
        val SOUND_ON_SCAN = booleanPreferencesKey("sound_on_scan")
        val VIBRATION_ON_SCAN = booleanPreferencesKey("vibration_on_scan")
        val AUTO_SAVE_HISTORY = booleanPreferencesKey("auto_save_history")
        val BATCH_SCAN_MODE = booleanPreferencesKey("batch_scan_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val preferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val themeName = prefs[Keys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
            val themeMode = try {
                AppThemeMode.valueOf(themeName)
            } catch (_: Exception) {
                AppThemeMode.SYSTEM
            }
            val langCode = prefs[Keys.APP_LANGUAGE] ?: AppLanguage.INDONESIAN.code
            UserPreferences(
                soundOnScan = prefs[Keys.SOUND_ON_SCAN] ?: true,
                vibrationOnScan = prefs[Keys.VIBRATION_ON_SCAN] ?: true,
                autoSaveHistory = prefs[Keys.AUTO_SAVE_HISTORY] ?: true,
                batchScanMode = prefs[Keys.BATCH_SCAN_MODE] ?: false,
                themeMode = themeMode,
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
                language = AppLanguage.fromCode(langCode)
            )
        }

    suspend fun setSoundOnScan(enabled: Boolean) {
        dataStore.edit { it[Keys.SOUND_ON_SCAN] = enabled }
    }

    suspend fun setVibrationOnScan(enabled: Boolean) {
        dataStore.edit { it[Keys.VIBRATION_ON_SCAN] = enabled }
    }

    suspend fun setAutoSaveHistory(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_SAVE_HISTORY] = enabled }
    }

    suspend fun setBatchScanMode(enabled: Boolean) {
        dataStore.edit { it[Keys.BATCH_SCAN_MODE] = enabled }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.APP_LANGUAGE] = language.code }
    }
}
