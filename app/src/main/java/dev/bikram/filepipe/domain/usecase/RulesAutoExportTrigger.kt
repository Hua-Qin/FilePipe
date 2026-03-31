package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RulesAutoExportTrigger @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exportRulesUseCase: ExportRulesUseCase
) {
    suspend fun maybeExportAfterRuleChange() {
        val prefs = userPreferencesRepository.getPreferencesSnapshot()
        if (prefs.autoExportOnRuleChange && prefs.exportFolderUri.isNotBlank()) {
            exportRulesUseCase.exportRulesToTreeUri(prefs.exportFolderUri)
        }
    }
}
