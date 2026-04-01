package dev.bikram.filepipe.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.domain.usecase.ExportRulesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScheduledRulesExportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exportRulesUseCase: ExportRulesUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = userPreferencesRepository.getPreferencesSnapshot()
        if (!prefs.scheduledExportEnabled || prefs.exportFolderUri.isBlank()) {
            return Result.success()
        }
        return exportRulesUseCase.exportRulesToTreeUri(prefs.exportFolderUri).fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}
