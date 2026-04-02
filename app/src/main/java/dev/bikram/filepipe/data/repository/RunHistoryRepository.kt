package dev.bikram.filepipe.data.repository

import dev.bikram.filepipe.data.local.dao.FileMovedDao
import dev.bikram.filepipe.data.local.dao.RunHistoryDao
import dev.bikram.filepipe.data.local.entity.FileMovedEntity
import dev.bikram.filepipe.data.local.entity.RunHistoryEntity
import dev.bikram.filepipe.data.local.entity.toDomain
import dev.bikram.filepipe.domain.model.FileMoved
import dev.bikram.filepipe.domain.model.RunHistory
import dev.bikram.filepipe.domain.model.RunResult
import dev.bikram.filepipe.domain.model.RunStatus
import dev.bikram.filepipe.domain.model.TriggerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunHistoryRepository @Inject constructor(
    private val runHistoryDao: RunHistoryDao,
    private val fileMovedDao: FileMovedDao
) {
    fun getAllHistory(): Flow<List<RunHistory>> =
        runHistoryDao.getAllHistory().map { it.map { entity -> entity.toDomain() } }

    suspend fun getAllHistoryOnce(): List<RunHistory> =
        runHistoryDao.getAllHistoryOnce().map { it.toDomain() }

    fun getHistoryForRule(ruleId: Long): Flow<List<RunHistory>> =
        runHistoryDao.getHistoryForRule(ruleId).map { it.map { entity -> entity.toDomain() } }

    suspend fun getHistoryById(id: Long): RunHistory? =
        runHistoryDao.getHistoryById(id)?.toDomain()

    fun getFilesForRun(runHistoryId: Long): Flow<List<FileMoved>> =
        fileMovedDao.getFilesForRun(runHistoryId).map { it.map { entity -> entity.toDomain() } }

    suspend fun getFilesForRunOnce(runHistoryId: Long): List<FileMoved> =
        fileMovedDao.getFilesForRunOnce(runHistoryId).map { it.toDomain() }

    suspend fun startRun(ruleId: Long?, ruleName: String, triggerType: TriggerType): Long =
        runHistoryDao.insertHistory(
            RunHistoryEntity(
                ruleId = ruleId,
                ruleName = ruleName,
                triggeredBy = triggerType,
                startedAt = System.currentTimeMillis(),
                status = RunStatus.IN_PROGRESS
            )
        )

    suspend fun completeRun(result: RunResult) {
        val history = runHistoryDao.getHistoryById(result.historyId) ?: return
        runHistoryDao.updateHistory(
            history.copy(
                completedAt = result.completedAt,
                status = result.status,
                totalFilesFound = result.filesMoved.size,
                totalFilesMoved = result.totalMoved,
                totalFilesFailed = result.totalFailed
            )
        )
        fileMovedDao.insertFilesMoved(
            result.filesMoved.map { fileMoved ->
                FileMovedEntity(
                    runHistoryId = result.historyId,
                    fileName = fileMoved.fileName,
                    sourceUri = fileMoved.sourceUri,
                    destinationUri = fileMoved.destinationUri,
                    fileSizeBytes = fileMoved.fileSizeBytes,
                    movedAt = fileMoved.movedAt,
                    success = fileMoved.success,
                    skipped = fileMoved.skipped,
                    errorMessage = fileMoved.errorMessage
                )
            }
        )
    }

    suspend fun markRunFailed(historyId: Long, errorMessage: String) {
        val history = runHistoryDao.getHistoryById(historyId) ?: return
        runHistoryDao.updateHistory(
            history.copy(
                completedAt = System.currentTimeMillis(),
                status = RunStatus.FAILED,
                errorMessage = errorMessage
            )
        )
    }

    suspend fun markRunReversed(historyId: Long) {
        val history = runHistoryDao.getHistoryById(historyId) ?: return
        runHistoryDao.updateHistory(history.copy(isReversed = true))
    }

    suspend fun deleteHistoryById(historyId: Long) {
        runHistoryDao.deleteHistoryById(historyId)
    }

    suspend fun pruneOldHistory(retentionDays: Int) {
        if (retentionDays <= 0) return
        val threshold = System.currentTimeMillis() - (retentionDays * 86_400_000L)
        runHistoryDao.deleteHistoryOlderThan(threshold)
    }

    suspend fun clearAllHistory() {
        runHistoryDao.deleteAllHistory()
    }
}
