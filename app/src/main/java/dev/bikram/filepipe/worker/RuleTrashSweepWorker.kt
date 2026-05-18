package dev.bikram.filepipe.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.bikram.filepipe.data.repository.RuleRepository

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
            ruleRepository.autoEmptyTrashOlderThan(cutoff)
            return Result.success()
        }

        companion object {
            const val WORK_NAME = "rule_trash_sweep_worker"
        }
    }
