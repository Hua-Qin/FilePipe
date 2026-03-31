package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.data.repository.FileOperationRepository
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.domain.model.FileMoved
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RunProgress
import dev.bikram.filepipe.domain.model.RunResult
import dev.bikram.filepipe.domain.model.TriggerType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class ExecuteRulesUseCase @Inject constructor(
    private val fileOperationRepository: FileOperationRepository,
    private val runHistoryRepository: RunHistoryRepository
) {
    suspend operator fun invoke(
        rules: List<Rule>,
        triggerType: TriggerType,
        onProgress: (RunProgress) -> Unit = {}
    ): List<RunResult> = coroutineScope {
        rules.map { rule ->
            async { executeRule(rule, triggerType, onProgress) }
        }.awaitAll()
    }

    private suspend fun executeRule(
        rule: Rule,
        triggerType: TriggerType,
        onProgress: (RunProgress) -> Unit
    ): RunResult {
        val startedAt = System.currentTimeMillis()
        val historyId = runHistoryRepository.startRun(rule.id, rule.name, triggerType)

        onProgress(RunProgress(rule.id, rule.name, 0f, totalFiles = 0))

        val allFiles = mutableListOf<FileMoved>()

        try {
            // Collect all matching files across all source folders
            val fileEntries = rule.sourceFolderUris.flatMap { sourceUri ->
                fileOperationRepository.listMatchingFiles(sourceUri, rule.fileExtensions)
            }

            val total = fileEntries.size
            fileEntries.forEachIndexed { index, entry ->
                onProgress(
                    RunProgress(
                        ruleId = rule.id,
                        ruleName = rule.name,
                        progress = index.toFloat() / total.coerceAtLeast(1),
                        currentFileName = entry.name,
                        filesMoved = allFiles.count { it.success },
                        totalFiles = total
                    )
                )

                val result = fileOperationRepository.moveFile(entry, rule.destinationFolderUri)
                allFiles.add(result)
            }
        } catch (e: Exception) {
            val result = RunResult(
                ruleId = rule.id,
                ruleName = rule.name,
                historyId = historyId,
                filesMoved = allFiles,
                startedAt = startedAt,
                completedAt = System.currentTimeMillis()
            )
            runHistoryRepository.completeRun(result)
            onProgress(
                RunProgress(rule.id, rule.name, 1f, isComplete = true, error = e.message)
            )
            return result
        }

        val completedAt = System.currentTimeMillis()
        val result = RunResult(
            ruleId = rule.id,
            ruleName = rule.name,
            historyId = historyId,
            filesMoved = allFiles,
            startedAt = startedAt,
            completedAt = completedAt
        )
        runHistoryRepository.completeRun(result)

        onProgress(
            RunProgress(
                ruleId = rule.id,
                ruleName = rule.name,
                progress = 1f,
                filesMoved = result.totalMoved,
                totalFiles = allFiles.size,
                isComplete = true
            )
        )

        return result
    }
}
