package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RulesAutoExportTrigger @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exportRulesUseCase: ExportRulesUseCase
) {
    @Volatile private var pendingExport = false

    /** Called on every rule change — just marks a pending export, no I/O. */
    suspend fun maybeExportAfterRuleChange() {
        val prefs = userPreferencesRepository.getPreferencesSnapshot()
        if (prefs.autoExportOnRuleChange && prefs.exportFolderUri.isNotBlank()) {
            pendingExport = true
        }
    }

    /** Call from MainActivity.onStop() — runs the export once if dirty, then clears the flag. */
    suspend fun flushIfPending() {
        if (!pendingExport) return
        pendingExport = false
        val prefs = userPreferencesRepository.getPreferencesSnapshot()
        if (prefs.autoExportOnRuleChange && prefs.exportFolderUri.isNotBlank()) {
            exportRulesUseCase.exportRulesToTreeUri(prefs.exportFolderUri)
        }
    }
}
