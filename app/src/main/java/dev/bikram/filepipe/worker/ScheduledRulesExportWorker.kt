package dev.bikram.filepipe.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.diagnostics.DiagnosticLog
import dev.bikram.filepipe.domain.usecase.ExportRulesUseCase

@HiltWorker
class ScheduledRulesExportWorker
    @AssistedInject
    constructor(
        @Assisted private val context: Context,
        @Assisted params: WorkerParameters,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val exportRulesUseCase: ExportRulesUseCase,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val prefs = userPreferencesRepository.getPreferencesSnapshot()
            val backupDestinations =
                listOf(
                    prefs.exportFolderUri,
                    prefs.cloudExportFolderUri,
                ).filter { it.isNotBlank() }
            if (!prefs.scheduledExportEnabled || backupDestinations.isEmpty()) {
                return Result.success()
            }
            return exportRulesUseCase.exportRulesToTreeUris(backupDestinations).fold(
                onSuccess = { fileNames ->
                    DiagnosticLog.record(
                        context,
                        "Scheduled backup export completed: destinations=${backupDestinations.size}, files=${fileNames.size}",
                    )
                    Result.success()
                },
                onFailure = { error ->
                    DiagnosticLog.record(
                        context,
                        "Scheduled backup export failed: destinations=${backupDestinations.size}, attempt=$runAttemptCount",
                        error,
                    )
                    Result.retry()
                },
            )
        }
    }
