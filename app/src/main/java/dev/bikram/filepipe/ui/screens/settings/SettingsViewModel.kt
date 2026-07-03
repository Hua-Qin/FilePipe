package dev.bikram.filepipe.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.preferences.AppColorSource
import dev.bikram.filepipe.data.preferences.AppPreferences
import dev.bikram.filepipe.data.preferences.AppThemeMode
import dev.bikram.filepipe.data.preferences.FolderAccessMode
import dev.bikram.filepipe.data.preferences.SwipeAction
import dev.bikram.filepipe.data.preferences.ThemePaletteStyle
import dev.bikram.filepipe.data.preferences.UpdateCheckSchedule
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.data.storage.isFilesystemFolderPathString
import dev.bikram.filepipe.data.storage.treeUriFromDocumentUri
import dev.bikram.filepipe.di.IoDispatcher
import dev.bikram.filepipe.diagnostics.DiagnosticLog
import dev.bikram.filepipe.domain.backupFileTimestamp
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.usecase.BackupImportPickAction
import dev.bikram.filepipe.domain.usecase.ExportRulesUseCase
import dev.bikram.filepipe.domain.usecase.ImportRulesUseCase
import dev.bikram.filepipe.domain.usecase.RulesAutoExportTrigger
import dev.bikram.filepipe.update.UpdateCheckWorkScheduler
import dev.bikram.filepipe.worker.ScheduledRulesExportWorker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val exportRulesUseCase: ExportRulesUseCase,
        private val importRulesUseCase: ImportRulesUseCase,
        private val workManager: WorkManager,
        private val rulesAutoExportTrigger: RulesAutoExportTrigger,
        private val updateCheckWorkScheduler: UpdateCheckWorkScheduler,
        private val ruleRepository: RuleRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        val preferencesFlow = userPreferencesRepository.preferencesFlow
        val preferencesState: StateFlow<AppPreferences?> =
            preferencesFlow
                .map<AppPreferences, AppPreferences?> { preferences -> preferences }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        val developerOptionsEnabledFlow = userPreferencesRepository.developerOptionsEnabledFlow

        // One-shot snackbar messages: a Channel so each is delivered exactly once (no rotation
        // replay, no conflation of identical/rapid messages).
        private val _userMessages = Channel<String>(Channel.BUFFERED)
        val userMessages: Flow<String> = _userMessages.receiveAsFlow()

        private fun postUserMessage(message: String) {
            _userMessages.trySend(message)
        }

        private val _manualExportPickerRequested =
            MutableSharedFlow<String>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val manualExportPickerRequested = _manualExportPickerRequested.asSharedFlow()

        init {
            viewModelScope.launch {
                val prefs = userPreferencesRepository.getPreferencesSnapshot()
                val backupDestinations =
                    listOf(
                        prefs.exportFolderUri,
                        prefs.cloudExportFolderUri,
                    ).filter { it.isNotBlank() }
                if (prefs.scheduledExportEnabled && backupDestinations.isNotEmpty()) {
                    enqueueScheduledExportWork()
                }
            }
        }

        fun setFolderAccessMode(mode: FolderAccessMode) {
            viewModelScope.launch {
                userPreferencesRepository.setFolderAccessMode(mode)
            }
        }

        suspend fun setFolderAccessModeNow(mode: FolderAccessMode) {
            userPreferencesRepository.setFolderAccessMode(mode)
        }

        /**
         * Rules that use filesystem paths (not SAF) will stop working when switching to Selective Access
         * until the user re-picks folders with the system picker.
         */
        suspend fun countRulesUsingFilesystemFolderPaths(): Int =
            withContext(ioDispatcher) {
                val rules = ruleRepository.getAllRules().first()
                rules.count { rule -> ruleUsesFilesystemFolderPaths(rule) }
            }

        private fun ruleUsesFilesystemFolderPaths(rule: Rule): Boolean {
            val paths = rule.sourceFolderPaths.toMutableList()
            if (rule.destinationFolderPath.isNotBlank()) {
                paths.add(rule.destinationFolderPath)
            }
            return paths.any { path -> isFilesystemFolderPathString(path) }
        }

        fun markIntroSeen(onComplete: () -> Unit = {}) =
            viewModelScope.launch {
                userPreferencesRepository.markIntroSeen()
                onComplete()
            }

        fun setThemeMode(mode: AppThemeMode) =
            viewModelScope.launch {
                userPreferencesRepository.setThemeMode(mode)
            }

        fun setColorSource(source: AppColorSource) =
            viewModelScope.launch {
                userPreferencesRepository.setColorSource(source)
            }

        fun addCustomSeedHex(hex: String) =
            viewModelScope.launch {
                userPreferencesRepository.addCustomSeedHex(hex)
            }

        fun selectCustomSeedHex(hex: String) =
            viewModelScope.launch {
                userPreferencesRepository.selectCustomSeedHex(hex)
            }

        fun previewCustomSeedHex(hex: String) =
            viewModelScope.launch {
                userPreferencesRepository.previewCustomSeedHex(hex)
            }

        fun removeCustomSeedHex(hex: String) =
            viewModelScope.launch {
                userPreferencesRepository.removeCustomSeedHex(hex)
            }

        fun setThemePaletteStyle(style: ThemePaletteStyle) =
            viewModelScope.launch {
                userPreferencesRepository.setThemePaletteStyle(style)
            }

        fun setSettingsCollapsedSectionKeys(sectionKeys: Collection<String>) =
            viewModelScope.launch {
                userPreferencesRepository.setSettingsCollapsedSectionKeys(sectionKeys)
            }

        fun setExportFolderUri(uriString: String) =
            viewModelScope.launch {
                persistBackupFolderUri(uriString)
                userPreferencesRepository.setExportFolderUri(uriString)
                disableAutomationsIfNoBackupDestination()
            }

        fun setCloudExportFolderUri(uriString: String) =
            viewModelScope.launch {
                persistBackupFolderUri(uriString)
                userPreferencesRepository.setCloudExportFolderUri(uriString)
                disableAutomationsIfNoBackupDestination()
            }

        private suspend fun persistBackupFolderUri(uriString: String) {
            if (uriString.startsWith("content://")) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uriString.toUri(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
            }
        }

        private suspend fun disableAutomationsIfNoBackupDestination() {
            val prefs = userPreferencesRepository.getPreferencesSnapshot()
            val backupDestinations =
                listOf(
                    prefs.exportFolderUri,
                    prefs.cloudExportFolderUri,
                ).filter { it.isNotBlank() }
            if (backupDestinations.isEmpty()) {
                userPreferencesRepository.setAutoExportOnRuleChange(false)
                userPreferencesRepository.setScheduledExportEnabled(false)
                workManager.cancelUniqueWork(ScheduledRulesExportWorker.WORK_NAME)
            }
        }

        fun setAutoExportOnChange(enabled: Boolean) =
            viewModelScope.launch {
                val prefs = userPreferencesRepository.getPreferencesSnapshot()
                val backupDestinations =
                    listOf(
                        prefs.exportFolderUri,
                        prefs.cloudExportFolderUri,
                    ).filter { it.isNotBlank() }
                if (enabled && backupDestinations.isEmpty()) {
                    postUserMessage(context.getString(R.string.settings_export_select_folder_first))
                    return@launch
                }
                userPreferencesRepository.setAutoExportOnRuleChange(enabled)
                if (enabled) {
                    rulesAutoExportTrigger.maybeExportAfterRuleChange()
                }
            }

        fun setScheduledExportEnabled(enabled: Boolean) =
            viewModelScope.launch {
                val prefs = userPreferencesRepository.getPreferencesSnapshot()
                val backupDestinations =
                    listOf(
                        prefs.exportFolderUri,
                        prefs.cloudExportFolderUri,
                    ).filter { it.isNotBlank() }
                if (enabled && backupDestinations.isEmpty()) {
                    postUserMessage(context.getString(R.string.settings_export_select_folder_first))
                    return@launch
                }
                userPreferencesRepository.setScheduledExportEnabled(enabled)
                if (enabled) {
                    enqueueScheduledExportWork()
                } else {
                    workManager.cancelUniqueWork(ScheduledRulesExportWorker.WORK_NAME)
                }
            }

        private fun enqueueScheduledExportWork() {
            val request =
                PeriodicWorkRequestBuilder<ScheduledRulesExportWorker>(1, TimeUnit.DAYS)
                    .build()
            workManager.enqueueUniquePeriodicWork(
                ScheduledRulesExportWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun setDeveloperOptionsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                userPreferencesRepository.setDeveloperOptionsEnabled(enabled)
            }
        }

        fun setLogRetentionDays(days: Int) =
            viewModelScope.launch {
                userPreferencesRepository.setLogRetentionDays(days)
            }

        fun setSwipeStartToEnd(action: SwipeAction) =
            viewModelScope.launch {
                userPreferencesRepository.setSwipeStartToEnd(action)
            }

        fun setSwipeEndToStart(action: SwipeAction) =
            viewModelScope.launch {
                userPreferencesRepository.setSwipeEndToStart(action)
            }

        fun setHapticFeedbackEnabled(enabled: Boolean) =
            viewModelScope.launch {
                userPreferencesRepository.setHapticFeedbackEnabled(enabled)
            }

        fun setProgressiveBlurEnabled(enabled: Boolean) =
            viewModelScope.launch {
                userPreferencesRepository.setProgressiveBlurEnabled(enabled)
            }

        fun setUseGradientBackground(enabled: Boolean) =
            viewModelScope.launch {
                userPreferencesRepository.setUseGradientBackground(enabled)
            }

        fun setUseEnhancedShading(enabled: Boolean) =
            viewModelScope.launch {
                userPreferencesRepository.setUseEnhancedShading(enabled)
            }

        fun setShadingIntensity(intensity: Float) =
            viewModelScope.launch {
                userPreferencesRepository.setShadingIntensity(intensity)
            }

        fun setUpdateCheckSchedule(schedule: UpdateCheckSchedule) =
            viewModelScope.launch {
                userPreferencesRepository.setUpdateCheckSchedule(schedule)
                if (schedule == UpdateCheckSchedule.NEVER) {
                    userPreferencesRepository.setNotifyOnNewUpdates(false)
                }
                updateCheckWorkScheduler.syncFromPreferences()
            }

        fun setNotifyOnNewUpdates(enabled: Boolean) =
            viewModelScope.launch {
                userPreferencesRepository.setNotifyOnNewUpdates(enabled)
            }

        fun markPlayAutoReviewPromptHandledForCurrentInstall(lastUpdateTimeMillis: Long) =
            viewModelScope.launch {
                userPreferencesRepository.setPlayAutoReviewPromptedForLastUpdateTime(lastUpdateTimeMillis)
            }

        fun setInAppReviewAutoNeverAskAgain(neverAskAgain: Boolean) =
            viewModelScope.launch {
                userPreferencesRepository.setInAppReviewAutoNeverAskAgain(neverAskAgain)
            }

        fun setSaveUpdateApkToDownloads(enabled: Boolean) =
            viewModelScope.launch {
                userPreferencesRepository.setSaveUpdateApkToDownloads(enabled)
            }

        fun requestManualExportPicker() {
            _manualExportPickerRequested.tryEmit(defaultManualExportFileName())
        }

        fun completeManualExportToUri(targetUri: Uri) =
            viewModelScope.launch {
                exportRulesUseCase.exportBackupJsonToDocumentUri(targetUri).fold(
                    onSuccess = { displayName ->
                        val prefs = userPreferencesRepository.getPreferencesSnapshot()
                        if (prefs.exportFolderUri.isBlank()) {
                            val treeUri = treeUriFromDocumentUri(context, targetUri)
                            if (treeUri != null) {
                                persistBackupFolderUri(treeUri.toString())
                                userPreferencesRepository.setExportFolderUri(treeUri.toString())
                            }
                        }
                        postUserMessage(context.getString(R.string.settings_export_success, displayName))
                    },
                    onFailure = { err ->
                        DiagnosticLog.record(context, "Manual backup export failed", err)
                        postUserMessage("Export failed: ${err.message}")
                    },
                )
            }

        fun completeCloudBackupDocumentSelection(targetUri: Uri) =
            viewModelScope.launch {
                persistBackupFolderUri(targetUri.toString())
                userPreferencesRepository.setCloudExportFolderUri(targetUri.toString())
                exportRulesUseCase.exportBackupJsonToDocumentUri(targetUri).fold(
                    onSuccess = {
                        val providerName = providerDisplayName(targetUri.authority)
                        postUserMessage(
                            if (providerName != null) {
                                context.getString(R.string.settings_backup_export_success_to, providerName)
                            } else {
                                context.getString(R.string.settings_backup_export_success)
                            },
                        )
                    },
                    onFailure = { err ->
                        DiagnosticLog.record(context, "Cloud backup export failed", err)
                        postUserMessage(
                            context.getString(
                                R.string.settings_backup_export_failed,
                                err.message.orEmpty(),
                            ),
                        )
                    },
                )
            }

        private fun providerDisplayName(authority: String?): String? {
            val providerAuthority = authority?.takeIf { it.isNotBlank() } ?: return null
            val normalizedAuthority = providerAuthority.lowercase()
            return when {
                normalizedAuthority.contains("google.android.apps.docs") -> {
                    context.getString(R.string.cloud_provider_google_drive)
                }

                normalizedAuthority.contains("skydrive") || normalizedAuthority.contains("onedrive") -> {
                    context.getString(R.string.cloud_provider_onedrive)
                }

                normalizedAuthority.contains("dropbox") -> {
                    context.getString(R.string.cloud_provider_dropbox)
                }

                normalizedAuthority.contains("box.android") -> {
                    context.getString(R.string.cloud_provider_box)
                }

                else -> {
                    null
                }
            }
        }

        private fun defaultManualExportFileName(): String {
            val stamp = backupFileTimestamp()
            return "filepipe_backup_$stamp.json"
        }

        fun exportToConfiguredBackupFolders() =
            viewModelScope.launch {
                val prefs = userPreferencesRepository.getPreferencesSnapshot()
                val backupDestinations =
                    listOf(
                        prefs.exportFolderUri,
                        prefs.cloudExportFolderUri,
                    ).filter { it.isNotBlank() }
                if (backupDestinations.isEmpty()) {
                    postUserMessage(context.getString(R.string.settings_export_select_folder_first))
                    return@launch
                }
                exportRulesUseCase.exportRulesToTreeUris(backupDestinations).fold(
                    onSuccess = { fileNames ->
                        DiagnosticLog.record(
                            context,
                            "Configured backup export completed: destinations=${backupDestinations.size}, files=${fileNames.size}",
                        )
                        postUserMessage(
                            context.resources.getQuantityString(
                                R.plurals.settings_backup_exported_to_destinations,
                                fileNames.size,
                                fileNames.size,
                            ),
                        )
                    },
                    onFailure = { error ->
                        postUserMessage(context.getString(R.string.settings_backup_export_failed, error.message.orEmpty()))
                        DiagnosticLog.record(context, "Configured backup export failed: destinations=${backupDestinations.size}", error)
                    },
                )
            }

        fun importFromUri(
            uri: Uri,
            action: BackupImportPickAction,
        ) = viewModelScope.launch {
            val text =
                withContext(ioDispatcher) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.readBytes().decodeToString()
                        }
                    }
                }.onFailure { error ->
                    DiagnosticLog.record(context, "Backup file read failed for $action", error)
                }.getOrNull() ?: run {
                    postUserMessage("Could not read file")
                    return@launch
                }
            when (action) {
                BackupImportPickAction.ImportMerge -> {
                    importRulesUseCase.mergeRulesFromJson(text).fold(
                        onSuccess = { result ->
                            postUserMessage(
                                context.resources.getQuantityString(
                                    R.plurals.settings_import_merge_success,
                                    result.rulesAdded,
                                    result.rulesAdded,
                                    result.rulesUpdated,
                                ),
                            )
                        },
                        onFailure = {
                            DiagnosticLog.record(context, "Backup merge import failed", it)
                            postUserMessage("Import failed: ${it.message}")
                        },
                    )
                }

                BackupImportPickAction.RestoreFull -> {
                    importRulesUseCase.restoreFromBackupJson(text).fold(
                        onSuccess = { result ->
                            val parts =
                                buildList {
                                    add("${result.rulesImported} rules")
                                    if (result.historyRunsImported > 0) {
                                        add("${result.historyRunsImported} history runs")
                                    }
                                    if (result.settingsRestored) add("settings")
                                }
                            postUserMessage(
                                context.getString(
                                    R.string.settings_restore_success,
                                    parts.joinToString(", "),
                                ),
                            )
                        },
                        onFailure = {
                            DiagnosticLog.record(context, "Full backup restore failed", it)
                            postUserMessage("Restore failed: ${it.message}")
                        },
                    )
                }
            }
        }

        fun openAppNotificationSettings() {
            val intent =
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }

        fun openManageAllFilesAccessSettings() {
            val manageIntent =
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = "package:${context.packageName}".toUri()
                }
            manageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(manageIntent) }
        }
    }
