package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.domain.export.parseRulesBackupJson
import dev.bikram.filepipe.domain.export.toDomain
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class BackupImportResult(
    val rulesImported: Int,
    val historyRunsImported: Int,
    val settingsRestored: Boolean
)

class ImportRulesUseCase @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val runHistoryRepository: RunHistoryRepository,
    private val scheduleRulesUseCase: ScheduleRulesUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend fun importFromJson(jsonText: String): Result<BackupImportResult> {
        val backup = parseRulesBackupJson(jsonText).getOrElse { return Result.failure(it) }
        val oldIds = ruleRepository.getAllRuleIds()
        oldIds.forEach { ruleId -> scheduleRulesUseCase.cancelRuleById(ruleId) }

        val rules = backup.rules.map { it.toDomain() }
        ruleRepository.replaceAllRules(rules)
        val saved = ruleRepository.getAllRules().first()
        val nameToRuleId = saved.groupBy { it.name }.mapValues { entry -> entry.value.first().id }

        runHistoryRepository.replaceHistoryFromBackup(backup.history, nameToRuleId)

        val settingsApplied = backup.settings != null
        backup.settings?.let { userPreferencesRepository.applySettingsFromBackup(it) }

        saved.filter { it.isEnabled && it.schedule != null }.forEach { rule ->
            scheduleRulesUseCase.scheduleRule(rule)
        }

        return Result.success(
            BackupImportResult(
                rulesImported = saved.size,
                historyRunsImported = backup.history.size,
                settingsRestored = settingsApplied
            )
        )
    }
}
