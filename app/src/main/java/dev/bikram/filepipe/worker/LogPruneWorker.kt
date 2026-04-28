package dev.bikram.filepipe.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.data.repository.RunHistoryRepository

@HiltWorker
class LogPruneWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val runHistoryRepository: RunHistoryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            val prefs = userPreferencesRepository.getPreferencesSnapshot()
            runHistoryRepository.pruneOldHistory(prefs.logRetentionDays)
            return Result.success()
        }

        companion object {
            const val WORK_NAME = "log_prune_worker"
        }
    }
