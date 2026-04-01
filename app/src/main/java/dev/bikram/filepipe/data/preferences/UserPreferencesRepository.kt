package dev.bikram.filepipe.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_settings"
)

private object PrefKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val USE_MATERIAL_YOU = booleanPreferencesKey("use_material_you")
    val EXPORT_FOLDER_URI = stringPreferencesKey("export_folder_uri")
    val AUTO_EXPORT_ON_CHANGE = booleanPreferencesKey("auto_export_on_change")
    val SCHEDULED_EXPORT = booleanPreferencesKey("scheduled_export_enabled")
    val LOG_RETENTION_DAYS = intPreferencesKey("log_retention_days")
    val SWIPE_START_TO_END = stringPreferencesKey("swipe_start_to_end")
    val SWIPE_END_TO_START = stringPreferencesKey("swipe_end_to_start")
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.userPreferencesDataStore

    val preferencesFlow: Flow<AppPreferences> = dataStore.data.map { prefs ->
        val rawTheme = prefs[PrefKeys.THEME_MODE]
        val parsedMode = rawTheme?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
        val legacyMaterialYou = rawTheme == "MATERIAL_YOU"
        val themeMode = when {
            legacyMaterialYou -> AppThemeMode.DARK
            parsedMode != null -> parsedMode
            else -> AppThemeMode.SYSTEM
        }
        val useMaterialYou = when {
            legacyMaterialYou -> true
            else -> prefs[PrefKeys.USE_MATERIAL_YOU] ?: false
        }
        AppPreferences(
            themeMode = themeMode,
            useMaterialYou = useMaterialYou,
            exportFolderUri = prefs[PrefKeys.EXPORT_FOLDER_URI].orEmpty(),
            autoExportOnRuleChange = prefs[PrefKeys.AUTO_EXPORT_ON_CHANGE] ?: false,
            scheduledExportEnabled = prefs[PrefKeys.SCHEDULED_EXPORT] ?: false,
            logRetentionDays = prefs[PrefKeys.LOG_RETENTION_DAYS] ?: 30,
            swipeStartToEnd = prefs[PrefKeys.SWIPE_START_TO_END]
                ?.let { runCatching { SwipeAction.valueOf(it) }.getOrNull() }
                ?: SwipeAction.DUPLICATE,
            swipeEndToStart = prefs[PrefKeys.SWIPE_END_TO_START]
                ?.let { runCatching { SwipeAction.valueOf(it) }.getOrNull() }
                ?: SwipeAction.DELETE
        )
    }

    suspend fun getPreferencesSnapshot(): AppPreferences = preferencesFlow.first()

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { it[PrefKeys.THEME_MODE] = mode.name }
    }

    suspend fun setUseMaterialYou(enabled: Boolean) {
        dataStore.edit { it[PrefKeys.USE_MATERIAL_YOU] = enabled }
    }

    suspend fun setExportFolderUri(uriString: String) {
        dataStore.edit { it[PrefKeys.EXPORT_FOLDER_URI] = uriString }
    }

    suspend fun setAutoExportOnRuleChange(enabled: Boolean) {
        dataStore.edit { it[PrefKeys.AUTO_EXPORT_ON_CHANGE] = enabled }
    }

    suspend fun setScheduledExportEnabled(enabled: Boolean) {
        dataStore.edit { it[PrefKeys.SCHEDULED_EXPORT] = enabled }
    }

    suspend fun setLogRetentionDays(days: Int) {
        dataStore.edit { it[PrefKeys.LOG_RETENTION_DAYS] = days }
    }

    suspend fun setSwipeStartToEnd(action: SwipeAction) {
        dataStore.edit { it[PrefKeys.SWIPE_START_TO_END] = action.name }
    }

    suspend fun setSwipeEndToStart(action: SwipeAction) {
        dataStore.edit { it[PrefKeys.SWIPE_END_TO_START] = action.name }
    }
}
