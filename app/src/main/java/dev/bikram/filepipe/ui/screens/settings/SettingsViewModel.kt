package dev.bikram.filepipe.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.bikram.filepipe.data.preferences.AppThemeMode
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.domain.usecase.ExportRulesUseCase
import dev.bikram.filepipe.domain.usecase.ImportRulesUseCase
import dev.bikram.filepipe.domain.usecase.RulesAutoExportTrigger
import dev.bikram.filepipe.worker.ScheduledRulesExportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exportRulesUseCase: ExportRulesUseCase,
    private val importRulesUseCase: ImportRulesUseCase,
    private val workManager: WorkManager,
    private val rulesAutoExportTrigger: RulesAutoExportTrigger
) : ViewModel() {

    val preferencesFlow = userPreferencesRepository.preferencesFlow

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

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

    fun exportNow() = viewModelScope.launch {
        val folder = userPreferencesRepository.getPreferencesSnapshot().exportFolderUri
        if (folder.isBlank()) {
            _userMessage.value = "Choose an export folder first"
            return@launch
        }
        exportRulesUseCase.exportRulesToTreeUri(folder).fold(
            onSuccess = { _userMessage.value = "Rules exported to ${ExportRulesUseCase.EXPORT_FILE_NAME}" },
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

    companion object {
        private const val SCHEDULED_EXPORT_WORK_NAME = "scheduled_rules_export"
    }
}
