package dev.bikram.filepipe.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.diagnostics.DiagnosticLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RulesAutoExportTrigger
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val exportRulesUseCase: ExportRulesUseCase,
        private val rulesBackupDirtyTracker: RulesBackupDirtyTracker,
    ) {
        /** Called on every rule change — just marks a pending export, no I/O. */
        suspend fun maybeExportAfterRuleChange() {
            val prefs = userPreferencesRepository.getPreferencesSnapshot()
            val backupDestinations =
                listOf(
                    prefs.exportFolderUri,
                    prefs.cloudExportFolderUri,
                ).filter { it.isNotBlank() }
            if (prefs.autoExportOnRuleChange && backupDestinations.isNotEmpty()) {
                rulesBackupDirtyTracker.markRulesChangedSinceLastTreeExport()
            }
        }

        /** Call from MainActivity.onStop() — runs the export once if dirty, keeping dirty state on failure. */
        suspend fun flushIfPending() {
            val prefs = userPreferencesRepository.getPreferencesSnapshot()
            val backupDestinations =
                listOf(
                    prefs.exportFolderUri,
                    prefs.cloudExportFolderUri,
                ).filter { it.isNotBlank() }
            if (!prefs.autoExportOnRuleChange || backupDestinations.isEmpty()) return
            if (!rulesBackupDirtyTracker.consumePendingChangeSinceLastTreeExport()) return

            exportRulesUseCase.exportRulesToTreeUris(backupDestinations).fold(
                onSuccess = { fileNames ->
                    DiagnosticLog.record(
                        context,
                        "Auto backup export completed: destinations=${backupDestinations.size}, files=${fileNames.size}",
                    )
                },
                onFailure = { error ->
                    rulesBackupDirtyTracker.markRulesChangedSinceLastTreeExport()
                    DiagnosticLog.record(
                        context,
                        "Auto backup export failed: destinations=${backupDestinations.size}",
                        error,
                    )
                },
            )
        }
    }
