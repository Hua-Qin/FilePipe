package dev.bikram.filepipe

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.diagnostics.DiagnosticLog
import dev.bikram.filepipe.update.UpdateApkCacheMaintenance
import dev.bikram.filepipe.update.UpdateAvailableNotifier
import dev.bikram.filepipe.update.UpdateCheckWorkScheduler
import dev.bikram.filepipe.worker.LogPruneWorker
import dev.bikram.filepipe.worker.RuleTrashSweepWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FilePipeApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var userPreferencesRepository: Lazy<UserPreferencesRepository>

    @Inject
    lateinit var updateApkCacheMaintenance: UpdateApkCacheMaintenance

    @Inject
    lateinit var updateCheckWorkScheduler: UpdateCheckWorkScheduler

    @Inject
    lateinit var updateAvailableNotifier: UpdateAvailableNotifier

    @Inject
    lateinit var runHistoryRepository: Lazy<RunHistoryRepository>

    private val appStartupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        val processStartedAt = System.currentTimeMillis()
        DiagnosticLog.installCrashHandler(this)
        DiagnosticLog.record(this, "FilePipeApp.onCreate started")
        appStartupScope.launch {
            runCatching {
                val prefs = userPreferencesRepository.get()
                prefs.migrateLegacyEnhancedShadingPreferenceIfNeeded()
                prefs.migrateLegacyCustomSeedIfNeeded()
                prefs.migrateLegacyAutoCheckToScheduleIfNeeded()
                prefs.migrateLegacyBlackThemeIfNeeded()
                prefs.migrateDeferredFolderAccessIfNeeded()
                updateCheckWorkScheduler.syncFromPreferences()
            }.onFailure { error ->
                DiagnosticLog.record(this@FilePipeApp, "Startup preference migration/update scheduling failed", error)
            }
            runCatching {
                runHistoryRepository
                    .get()
                    .reconcileInterruptedRuns(
                        startedBefore = processStartedAt,
                        errorMessage = getString(R.string.history_run_interrupted),
                    ).takeIf { interruptedCount -> interruptedCount > 0 }
                    ?.let { interruptedCount ->
                        DiagnosticLog.record(
                            this@FilePipeApp,
                            "Marked stale in-progress runs as interrupted: count=$interruptedCount",
                        )
                    }
            }.onFailure { error ->
                DiagnosticLog.record(this@FilePipeApp, "Interrupted run reconciliation failed", error)
            }
        }
        updateAvailableNotifier.ensureNotificationChannel()
        updateApkCacheMaintenance.enqueueStartupCleanup(appStartupScope)
        scheduleLogPruneWorker()
        scheduleRuleTrashSweepWorker()
    }

    private fun scheduleLogPruneWorker() {
        val request = PeriodicWorkRequestBuilder<LogPruneWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            LogPruneWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun scheduleRuleTrashSweepWorker() {
        val request = PeriodicWorkRequestBuilder<RuleTrashSweepWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RuleTrashSweepWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
