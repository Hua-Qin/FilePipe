package dev.bikram.filepipe.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.bikram.filepipe.data.preferences.AppThemeMode
import dev.bikram.filepipe.data.preferences.SwipeAction
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.domain.usecase.ExportRulesUseCase
import dev.bikram.filepipe.domain.usecase.ImportRulesUseCase
import dev.bikram.filepipe.domain.usecase.RulesAutoExportTrigger
import dev.bikram.filepipe.update.UpdateChecker
import dev.bikram.filepipe.update.UpdateInfo
import dev.bikram.filepipe.worker.ScheduledRulesExportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exportRulesUseCase: ExportRulesUseCase,
    private val importRulesUseCase: ImportRulesUseCase,
    private val workManager: WorkManager,
    private val rulesAutoExportTrigger: RulesAutoExportTrigger,
    private val updateChecker: UpdateChecker
) : ViewModel() {

    val preferencesFlow = userPreferencesRepository.preferencesFlow

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = userPreferencesRepository.getPreferencesSnapshot()
            if (prefs.scheduledExportEnabled && prefs.exportFolderUri.isNotBlank()) {
                enqueueScheduledExportWork()
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch {
        userPreferencesRepository.setThemeMode(mode)
    }

    fun setUseMaterialYou(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.setUseMaterialYou(enabled)
    }

    fun setExportFolderUri(uriString: String) = viewModelScope.launch {
        if (uriString.startsWith("content://")) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    Uri.parse(uriString),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        userPreferencesRepository.setExportFolderUri(uriString)
    }

    fun setAutoExportOnChange(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.setAutoExportOnRuleChange(enabled)
        if (enabled) {
            rulesAutoExportTrigger.maybeExportAfterRuleChange()
        }
    }

    fun setScheduledExportEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.setScheduledExportEnabled(enabled)
        if (enabled) {
            enqueueScheduledExportWork()
        } else {
            workManager.cancelUniqueWork(SCHEDULED_EXPORT_WORK_NAME)
        }
    }

    private fun enqueueScheduledExportWork() {
        val request = PeriodicWorkRequestBuilder<ScheduledRulesExportWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            SCHEDULED_EXPORT_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun setLogRetentionDays(days: Int) = viewModelScope.launch {
        userPreferencesRepository.setLogRetentionDays(days)
    }

    fun setSwipeStartToEnd(action: SwipeAction) = viewModelScope.launch {
        userPreferencesRepository.setSwipeStartToEnd(action)
    }

    fun setSwipeEndToStart(action: SwipeAction) = viewModelScope.launch {
        userPreferencesRepository.setSwipeEndToStart(action)
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.setHapticFeedbackEnabled(enabled)
    }

    fun setProgressiveBlurEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.setProgressiveBlurEnabled(enabled)
    }

    fun exportNow() = viewModelScope.launch {
        val folder = userPreferencesRepository.getPreferencesSnapshot().exportFolderUri
        if (folder.isBlank()) {
            _userMessage.value = "Choose an export folder first"
            return@launch
        }
        exportRulesUseCase.exportRulesToTreeUri(folder).fold(
            onSuccess = { _userMessage.value = "Rules exported successfully" },
            onFailure = { _userMessage.value = "Export failed: ${it.message}" }
        )
    }

    fun importFromUri(uri: Uri) = viewModelScope.launch {
        val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().decodeToString()
            }
        } ?: run {
            _userMessage.value = "Could not read file"
            return@launch
        }
        importRulesUseCase.importFromJson(text).fold(
            onSuccess = { count -> _userMessage.value = "Imported $count rules" },
            onFailure = { _userMessage.value = "Import failed: ${it.message}" }
        )
    }

    fun checkForUpdate() = viewModelScope.launch {
        _isCheckingUpdate.value = true
        val info = updateChecker.checkForUpdate()
        _isCheckingUpdate.value = false
        if (info != null) {
            _updateInfo.value = info
        } else {
            _userMessage.value = "You're up to date"
        }
    }

    fun downloadAndInstall(downloadUrl: String) = viewModelScope.launch {
        _userMessage.value = "Downloading update…"
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(context.cacheDir, "filepipe_update.apk")
                URL(downloadUrl).openStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            }.onFailure { _userMessage.value = "Download failed: ${it.message}" }
        }
    }

    companion object {
        private const val SCHEDULED_EXPORT_WORK_NAME = "scheduled_rules_export"
    }
}
