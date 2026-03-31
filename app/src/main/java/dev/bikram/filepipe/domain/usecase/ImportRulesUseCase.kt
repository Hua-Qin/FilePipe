package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.domain.export.parseRulesBackupJson
import dev.bikram.filepipe.domain.export.toDomain
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ImportRulesUseCase @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val scheduleRulesUseCase: ScheduleRulesUseCase
) {
    suspend fun importFromJson(jsonText: String): Result<Int> {
        val backup = parseRulesBackupJson(jsonText).getOrElse { return Result.failure(it) }
        val oldIds = ruleRepository.getAllRuleIds()
        oldIds.forEach { ruleId -> scheduleRulesUseCase.cancelRuleById(ruleId) }
        val rules = backup.rules.map { it.toDomain() }
        ruleRepository.replaceAllRules(rules)
        val saved = ruleRepository.getAllRules().first()
        saved.filter { it.isEnabled && it.schedule != null }.forEach { rule ->
            scheduleRulesUseCase.scheduleRule(rule)
        }
        return Result.success(saved.size)
    }
}
