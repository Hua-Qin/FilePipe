package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.domain.export.buildRulesBackupJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportRulesUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend fun exportRulesToTreeUri(folderPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (folderPath.isBlank()) return@withContext Result.failure(IllegalStateException("No export folder"))
        val folder = File(folderPath)
        if (!folder.exists() || !folder.canWrite()) {
            return@withContext Result.failure(IllegalStateException("Export folder not accessible: $folderPath"))
        }
        val rules = ruleRepository.getAllRules().first()
        val json = buildRulesBackupJson(rules)
        val dateSuffix = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val fileName = "filepipe_rules_$dateSuffix.json"
        runCatching {
            File(folder, fileName).writeText(json, Charsets.UTF_8)
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }
}
