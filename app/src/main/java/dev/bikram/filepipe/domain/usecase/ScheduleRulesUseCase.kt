package dev.bikram.filepipe.domain.usecase

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.ScheduleType
import dev.bikram.filepipe.worker.FileOrganizerWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ScheduleRulesUseCase @Inject constructor(
    private val workManager: WorkManager
) {
    fun scheduleRule(rule: Rule) {
        val schedule = rule.schedule ?: run {
            cancelRule(rule)
            return
        }
        if (!rule.isEnabled) {
            cancelRule(rule)
            return
        }

        val tag = workTagFor(rule.id)
        val inputData = workDataOf(FileOrganizerWorker.KEY_RULE_ID to rule.id)
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val delayMs = when (schedule.type) {
            ScheduleType.EVERY_N_HOURS -> 0L
            else -> calculateDelayUntilNextRun(
                schedule.hour,
                schedule.minute,
                if (schedule.type == ScheduleType.WEEKLY) schedule.dayOfWeek else null
            )
        }

        val request = when (schedule.type) {
            ScheduleType.EVERY_N_HOURS -> {
                val hours = schedule.intervalHours?.toLong()?.coerceIn(1L, 24L) ?: 1L
                PeriodicWorkRequestBuilder<FileOrganizerWorker>(hours, TimeUnit.HOURS)
            }
            ScheduleType.DAILY -> PeriodicWorkRequestBuilder<FileOrganizerWorker>(1L, TimeUnit.DAYS)
            ScheduleType.WEEKLY -> PeriodicWorkRequestBuilder<FileOrganizerWorker>(7L, TimeUnit.DAYS)
        }
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(tag)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            tag,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request
        )
    }

    fun cancelRule(rule: Rule) {
        workManager.cancelAllWorkByTag(workTagFor(rule.id))
    }

    fun cancelRuleById(ruleId: Long) {
        workManager.cancelAllWorkByTag(workTagFor(ruleId))
    }

    private fun workTagFor(ruleId: Long) = "rule_$ruleId"

    private fun calculateDelayUntilNextRun(hour: Int, minute: Int, dayOfWeek: Int?): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (dayOfWeek != null) {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
            }
        }
        if (!target.after(now)) {
            if (dayOfWeek != null) {
                target.add(Calendar.WEEK_OF_YEAR, 1)
            } else {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}
