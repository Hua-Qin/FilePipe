package dev.bikram.filepipe.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.model.TriggerType
import dev.bikram.filepipe.domain.usecase.ExecuteRulesUseCase
import dev.bikram.filepipe.receiver.NotificationActionReceiver

/**
 * Coalesced worker that processes multiple rules in a single foreground service pass.
 * Use [ScheduleRulesUseCase.scheduleCoalesced] to schedule rules with the same schedule
 * configuration under one worker instead of spinning up separate workers per rule.
 */
@HiltWorker
class RunAllScheduledRulesWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val executeRulesUseCase: ExecuteRulesUseCase,
    private val ruleRepository: RuleRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val ruleIds = inputData.getLongArray(KEY_RULE_IDS) ?: return Result.failure()
        val rules = buildList {
            for (id in ruleIds) {
                ruleRepository.getRuleById(id)?.let { if (it.isEnabled) add(it) }
            }
        }
        if (rules.isEmpty()) return Result.success()

        setForeground(createForegroundInfo(rules.size))

        return try {
            val results = executeRulesUseCase(rules, TriggerType.SCHEDULED)
            results.forEach { result ->
                val rule = rules.find { it.id == result.ruleId } ?: return@forEach
                val movedFileNames = result.filesMoved
                    .filter { it.success && !it.skipped }
                    .map { it.fileName }
                postSummaryNotification(
                    ruleName = rule.name,
                    moved = result.totalMoved,
                    failed = result.totalFailed,
                    operationMode = rule.operationMode,
                    historyId = result.historyId,
                    movedFileNames = movedFileNames
                )
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun postSummaryNotification(
        ruleName: String,
        moved: Int,
        failed: Int,
        operationMode: OperationMode,
        historyId: Long,
        movedFileNames: List<String> = emptyList()
    ) {
        ensureSummaryChannel()
        val body = when {
            failed > 0 -> appContext.getString(R.string.notification_summary_body_partial, moved, failed)
            operationMode == OperationMode.COPY ->
                appContext.getString(R.string.notification_summary_body_copied, moved)
            else -> appContext.getString(R.string.notification_summary_body_moved, moved)
        }
        val builder = NotificationCompat.Builder(appContext, FileOrganizerWorker.SUMMARY_CHANNEL_ID)
            .setContentTitle(appContext.getString(R.string.notification_summary_title, ruleName))
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)

        // Show up to 3 filenames using InboxStyle
        if (movedFileNames.isNotEmpty()) {
            val style = NotificationCompat.InboxStyle()
                .setBigContentTitle(appContext.getString(R.string.notification_summary_title, ruleName))
                .setSummaryText(body)
            movedFileNames.take(3).forEach { style.addLine(it) }
            if (movedFileNames.size > 3) {
                style.addLine("+ ${movedFileNames.size - 3} more")
            }
            builder.setStyle(style)
        }

        if (moved > 0) {
            val undoIntent = Intent(appContext, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_UNDO_RUN
                putExtra(NotificationActionReceiver.EXTRA_HISTORY_ID, historyId)
            }
            val undoPendingIntent = PendingIntent.getBroadcast(
                appContext,
                historyId.toInt(),
                undoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, appContext.getString(R.string.notification_action_undo), undoPendingIntent)
        }

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((SUMMARY_NOTIFICATION_BASE_ID + historyId).toInt(), builder.build())
    }

    private fun createForegroundInfo(ruleCount: Int): ForegroundInfo {
        ensureProgressChannel()
        val notification = NotificationCompat.Builder(appContext, FileOrganizerWorker.CHANNEL_ID)
            .setContentTitle(appContext.getString(R.string.notification_running_batch, ruleCount))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun ensureProgressChannel() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(FileOrganizerWorker.CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                android.app.NotificationChannel(
                    FileOrganizerWorker.CHANNEL_ID,
                    appContext.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = appContext.getString(R.string.notification_channel_desc)
                }
            )
        }
    }

    private fun ensureSummaryChannel() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(FileOrganizerWorker.SUMMARY_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                android.app.NotificationChannel(
                    FileOrganizerWorker.SUMMARY_CHANNEL_ID,
                    appContext.getString(R.string.notification_summary_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = appContext.getString(R.string.notification_summary_channel_desc)
                }
            )
        }
    }

    companion object {
        const val KEY_RULE_IDS = "rule_ids"
        const val NOTIFICATION_ID = 1003
        const val SUMMARY_NOTIFICATION_BASE_ID = 2000
    }
}
