package dev.bikram.filepipe.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.diagnostics.DiagnosticLog
import kotlinx.coroutines.CancellationException

@HiltWorker
class LogPruneWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val runHistoryRepository: RunHistoryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result =
            try {
                val prefs = userPreferencesRepository.getPreferencesSnapshot()
                runHistoryRepository.pruneOldHistory(prefs.logRetentionDays)
                Result.success()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val attemptNumber = runAttemptCount + 1
                val willRetry = attemptNumber < MAX_RUN_ATTEMPTS_PER_PERIOD
                DiagnosticLog.record(
                    applicationContext,
                    "History pruning failed: attempt=$attemptNumber, willRetry=$willRetry",
                    error,
                )
                if (willRetry) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }

        companion object {
            const val WORK_NAME = "log_prune_worker"
            private const val MAX_RUN_ATTEMPTS_PER_PERIOD = 3
        }
    }
