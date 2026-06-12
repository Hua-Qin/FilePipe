package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RuleSchedule
import dev.bikram.filepipe.domain.model.ScheduleType
import java.util.Calendar
import java.util.concurrent.TimeUnit

internal data class CoalescedRuleScheduleGroup(
    val schedule: RuleSchedule,
    val ruleIds: List<Long>,
)

internal fun coalescedRuleScheduleGroups(rules: List<Rule>): List<CoalescedRuleScheduleGroup> =
    rules
        .filter { rule -> rule.isEnabled && rule.schedule != null }
        .groupBy { rule -> scheduleKey(rule.schedule!!) }
        .values
        .map { group ->
            CoalescedRuleScheduleGroup(
                schedule = group.first().schedule!!,
                ruleIds = group.map { rule -> rule.id },
            )
        }

internal fun batchTagForRuleIds(ruleIds: LongArray): String = "batch_${ruleIds.sorted().joinToString("_")}"

internal fun scheduledRuleWorkTag(ruleId: Long): String = "rule_$ruleId"

internal fun scheduledBatchWorkTag(ruleIds: LongArray): String = batchTagForRuleIds(ruleIds)

internal fun scheduledRuleRunWorkName(ruleId: Long): String = "scheduled_rule_run_$ruleId"

internal fun scheduledBatchRunWorkName(ruleIds: LongArray): String = "scheduled_${batchTagForRuleIds(ruleIds)}"

internal fun scheduleKey(schedule: RuleSchedule): String = "${schedule.type}_${schedule.hour}_${schedule.minute}_${schedule.dayOfWeek}_${schedule.intervalHours}"

private fun getLocalEpochMonday(): Long =
    Calendar
        .getInstance()
        .apply {
            firstDayOfWeek = Calendar.MONDAY
            set(1970, Calendar.JANUARY, 5, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

private fun getWeeksBetween(
    startMillis: Long,
    endMillis: Long,
): Int {
    val startCal =
        Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = startMillis
        }
    val endCal =
        Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = endMillis
        }

    // Align both to the Monday of their respective weeks
    startCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    startCal.set(Calendar.HOUR_OF_DAY, 0)
    startCal.set(Calendar.MINUTE, 0)
    startCal.set(Calendar.SECOND, 0)
    startCal.set(Calendar.MILLISECOND, 0)

    endCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    endCal.set(Calendar.HOUR_OF_DAY, 0)
    endCal.set(Calendar.MINUTE, 0)
    endCal.set(Calendar.SECOND, 0)
    endCal.set(Calendar.MILLISECOND, 0)

    val diffDays = (endCal.timeInMillis - startCal.timeInMillis) / (24 * 60 * 60 * 1000L)
    return (diffDays / 7).toInt()
}

private fun getDayOffsetFromMonday(dayOfWeek: Int): Int =
    when (dayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

internal fun nextRunAtMillis(
    schedule: RuleSchedule,
    nowMillis: Long = System.currentTimeMillis(),
    allowImmediateIntervalRun: Boolean = true,
): Long {
    when (schedule.type) {
        ScheduleType.EVERY_N_HOURS -> {
            val anchor =
                Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, schedule.hour)
                    set(Calendar.MINUTE, schedule.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            val anchorMillis = anchor.timeInMillis
            val intervalHours = schedule.intervalHours?.coerceAtLeast(1) ?: 1
            val intervalMillis = TimeUnit.HOURS.toMillis(intervalHours.toLong())

            if (nowMillis <= anchorMillis) {
                return anchorMillis
            }

            if (allowImmediateIntervalRun) {
                return nowMillis
            }

            val diffMillis = nowMillis - anchorMillis
            val k = (diffMillis / intervalMillis) + 1
            return anchorMillis + k * intervalMillis
        }

        ScheduleType.DAILY -> {
            val anchor =
                Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, schedule.hour)
                    set(Calendar.MINUTE, schedule.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            val anchorMillis = anchor.timeInMillis
            val days = schedule.intervalHours?.coerceAtLeast(1) ?: 1
            val intervalMillis = TimeUnit.DAYS.toMillis(days.toLong())

            if (nowMillis <= anchorMillis) {
                return anchorMillis
            }

            val diffMillis = nowMillis - anchorMillis
            val k = (diffMillis / intervalMillis) + 1
            return anchorMillis + k * intervalMillis
        }

        ScheduleType.WEEKLY -> {
            val selectedDays = RuleSchedule.bitmaskToDaysOfWeek(schedule.dayOfWeek)
            val intervalWeeks = schedule.intervalHours?.coerceAtLeast(1) ?: 1
            val localEpoch = getLocalEpochMonday()
            val currentWeek = getWeeksBetween(localEpoch, nowMillis)

            var weekOffset = 0
            while (true) {
                val candidateWeek = currentWeek + weekOffset
                if (candidateWeek % intervalWeeks == 0) {
                    val candidates = mutableListOf<Long>()
                    for (day in selectedDays) {
                        val candidate =
                            Calendar.getInstance().apply {
                                firstDayOfWeek = Calendar.MONDAY
                                timeInMillis = localEpoch
                                add(Calendar.WEEK_OF_YEAR, candidateWeek)
                                add(Calendar.DAY_OF_YEAR, getDayOffsetFromMonday(day))
                                set(Calendar.HOUR_OF_DAY, schedule.hour)
                                set(Calendar.MINUTE, schedule.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                        if (candidate.timeInMillis > nowMillis) {
                            candidates.add(candidate.timeInMillis)
                        }
                    }
                    if (candidates.isNotEmpty()) {
                        return candidates.minOrNull()!!
                    }
                }
                weekOffset++
                if (weekOffset > 260) {
                    break
                }
            }
            return nowMillis + TimeUnit.HOURS.toMillis(1)
        }
    }
}
