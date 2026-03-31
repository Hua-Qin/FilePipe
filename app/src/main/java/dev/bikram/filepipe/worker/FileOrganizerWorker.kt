package dev.bikram.filepipe.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.domain.model.TriggerType
import dev.bikram.filepipe.domain.usecase.ExecuteRulesUseCase
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
            executeRulesUseCase(listOf(rule), TriggerType.SCHEDULED)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
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

    companion object {
        const val KEY_RULE_ID = "rule_id"
        const val CHANNEL_ID = "file_organizer_channel"
        const val NOTIFICATION_ID = 1001
    }
}
