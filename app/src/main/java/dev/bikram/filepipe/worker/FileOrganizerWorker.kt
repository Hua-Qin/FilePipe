package dev.bikram.filepipe.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.model.TriggerType
import dev.bikram.filepipe.domain.usecase.ExecuteRulesUseCase
import dev.bikram.filepipe.receiver.NotificationActionReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FileOrganizerWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val executeRulesUseCase: ExecuteRulesUseCase,
    private val ruleRepository: RuleRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val ruleId = inputData.getLong(KEY_RULE_ID, -1L)
        if (ruleId == -1L) return Result.failure()

        val rule = ruleRepository.getRuleById(ruleId) ?: return Result.failure()

        setForeground(createForegroundInfo(rule.name))

        return try {
            val results = executeRulesUseCase(listOf(rule), TriggerType.SCHEDULED)
            val result = results.firstOrNull()
            if (result != null) {
                val movedFileNames = result.filesMoved
                    .filter { it.success && !it.skipped }
                    .map { it.fileName }
                postSummaryNotification(rule.name, result.totalMoved, result.totalFailed, rule.operationMode, result.historyId, movedFileNames)
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
        val builder = NotificationCompat.Builder(appContext, SUMMARY_CHANNEL_ID)
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

        // Add Undo action if files were moved
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
        manager.notify(SUMMARY_NOTIFICATION_ID, builder.build())
    }

    private fun createForegroundInfo(ruleName: String): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(appContext.getString(R.string.notification_running, ruleName))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
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
        if (manager.getNotificationChannel(SUMMARY_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    SUMMARY_CHANNEL_ID,
                    appContext.getString(R.string.notification_summary_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = appContext.getString(R.string.notification_summary_channel_desc)
                }
            )
        }
    }

    companion object {
        const val KEY_RULE_ID = "rule_id"
        const val CHANNEL_ID = "file_organizer_channel"
        const val NOTIFICATION_ID = 1001
        const val SUMMARY_CHANNEL_ID = "run_summary_channel"
        const val SUMMARY_NOTIFICATION_ID = 1002
    }
}
