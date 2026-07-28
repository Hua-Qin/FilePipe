package dev.bikram.filepipe.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.diagnostics.DiagnosticLog

@HiltWorker
class RuleTrashSweepWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val ruleRepository: RuleRepository,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            val cutoff = System.currentTimeMillis() - RuleRepository.TRASH_RETENTION_MILLIS
            return runCatching {
                ruleRepository.autoEmptyTrashOlderThan(cutoff)
            }.fold(
                onSuccess = { Result.success() },
                onFailure = { error ->
                    val attemptNumber = runAttemptCount + 1
                    val willRetry = attemptNumber < MAX_RUN_ATTEMPTS_PER_PERIOD
                    DiagnosticLog.record(
                        applicationContext,
                        "Rule trash sweep failed: attempt=$attemptNumber, willRetry=$willRetry",
                        error,
                    )
                    if (willRetry) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                },
            )
        }

        companion object {
            const val WORK_NAME = "rule_trash_sweep_worker"
            private const val MAX_RUN_ATTEMPTS_PER_PERIOD = 3
        }
    }
