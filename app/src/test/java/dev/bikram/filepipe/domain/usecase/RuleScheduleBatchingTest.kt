package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RuleSchedule
import dev.bikram.filepipe.domain.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RuleScheduleBatchingTest {
    @Test
    fun coalescedGroupsIncludeOnlyEnabledScheduledRulesWithIdenticalSchedules() {
        val dailyNine = RuleSchedule(ScheduleType.DAILY, hour = 9, minute = 0)
        val everyTwoHours = RuleSchedule(ScheduleType.EVERY_N_HOURS, hour = 0, minute = 0, repeatInterval = 2)
        val groups =
            coalescedRuleScheduleGroups(
                listOf(
                    rule(id = 3L, schedule = dailyNine),
                    rule(id = 1L, schedule = dailyNine),
                    rule(id = 9L, schedule = dailyNine, isEnabled = false),
                    rule(id = 10L, schedule = null),
                    rule(id = 5L, schedule = everyTwoHours),
                ),
            )

        assertEquals(2, groups.size)
        assertEquals(dailyNine, groups[0].schedule)
        assertEquals(listOf(3L, 1L), groups[0].ruleIds)
        assertEquals(everyTwoHours, groups[1].schedule)
        assertEquals(listOf(5L), groups[1].ruleIds)
    }

    @Test
    fun batchTagSortsRuleIdsForStableUniqueWorkNames() {
        assertEquals("batch_1_3_9", batchTagForRuleIds(longArrayOf(9L, 1L, 3L)))
    }

    @Test
    fun nextDailyRunUsesTodayWhenTimeIsStillAhead() {
        val now = millisFor(day = 10, hour = 8, minute = 30)
        val next =
            nextRunAtMillis(
                RuleSchedule(ScheduleType.DAILY, hour = 9, minute = 15),
                nowMillis = now,
            )

        assertEquals(millisFor(day = 10, hour = 9, minute = 15), next)
    }

    @Test
    fun nextDailyRunMovesToTomorrowWhenTimeAlreadyPassed() {
        val now = millisFor(day = 10, hour = 10, minute = 0)
        val next =
            nextRunAtMillis(
                RuleSchedule(ScheduleType.DAILY, hour = 9, minute = 15),
                nowMillis = now,
            )

        assertEquals(millisFor(day = 11, hour = 9, minute = 15), next)
    }

    @Test
    fun nextIntervalRunCanBeImmediateForNewSchedulesAndDelayedForRecurringAlarms() {
        val now = millisFor(day = 10, hour = 8, minute = 30)
        val schedule = RuleSchedule(ScheduleType.EVERY_N_HOURS, hour = 0, minute = 0, repeatInterval = 3)

        assertEquals(now, nextRunAtMillis(schedule, nowMillis = now, allowImmediateIntervalRun = true))
        assertEquals(
            millisFor(day = 10, hour = 9, minute = 0),
            nextRunAtMillis(schedule, nowMillis = now, allowImmediateIntervalRun = false),
        )
    }

    @Test
    fun nextDailyRunEveryTwoDaysTimePassed() {
        val now = millisFor(day = 10, hour = 10, minute = 0)
        val next =
            nextRunAtMillis(
                RuleSchedule(ScheduleType.DAILY, hour = 9, minute = 0, repeatInterval = 2),
                nowMillis = now,
            )
        // 9:00 today has passed, so it runs on day 12 at 9:00 AM (2 days later)
        assertEquals(millisFor(day = 12, hour = 9, minute = 0), next)
    }

    @Test
    fun nextWeeklyRunEveryTwoWeeksMultipleDays() {
        // Monday, Jan 5, 2026 is week 0 (active for every 2 weeks).
        // Jan 10, 2026 is Saturday (which is in week 0).
        // Let's set now to Saturday, Jan 10 at 10:00 AM.
        // Selected days are Monday and Friday.
        // In week 0: Monday was Jan 5 (passed), Friday was Jan 9 (passed).
        // Since all selected days in week 0 have passed, it must run in week 2 (since week 1 is inactive).
        // Week 2 Monday is Jan 19, Friday is Jan 23.
        // First candidate is Jan 19 at 9:00 AM.
        val now =
            Calendar
                .getInstance()
                .apply {
                    clear()
                    set(2026, Calendar.JANUARY, 10, 10, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

        val bitmask = RuleSchedule.daysOfWeekToBitmask(listOf(Calendar.MONDAY, Calendar.FRIDAY))
        val schedule =
            RuleSchedule(
                type = ScheduleType.WEEKLY,
                dayOfWeek = bitmask,
                hour = 9,
                minute = 0,
                repeatInterval = 2,
            )
        val next = nextRunAtMillis(schedule, nowMillis = now)

        val expected =
            Calendar
                .getInstance()
                .apply {
                    clear()
                    set(2026, Calendar.JANUARY, 19, 9, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

        assertEquals(expected, next)
    }

    @Test
    fun nextLegacyIntervalRunWithoutStartTimeUsesRelativeDelay() {
        val now = millisFor(day = 10, hour = 8, minute = 30)
        val schedule =
            RuleSchedule(
                type = ScheduleType.EVERY_N_HOURS,
                hour = 0,
                minute = 0,
                repeatInterval = 3,
                usesStartTime = false,
            )

        assertEquals(now, nextRunAtMillis(schedule, nowMillis = now, allowImmediateIntervalRun = true))
        assertEquals(
            now + TimeUnit.HOURS.toMillis(3),
            nextRunAtMillis(schedule, nowMillis = now, allowImmediateIntervalRun = false),
        )
    }

    private fun rule(
        id: Long,
        schedule: RuleSchedule?,
        isEnabled: Boolean = true,
    ): Rule =
        Rule(
            id = id,
            name = "Rule $id",
            sourceFolderPaths = listOf("content://source/$id"),
            destinationFolderPath = "content://destination",
            fileExtensions = listOf("jpg"),
            isEnabled = isEnabled,
            schedule = schedule,
        )

    private fun millisFor(
        day: Int,
        hour: Int,
        minute: Int,
    ): Long =
        Calendar
            .getInstance()
            .apply {
                clear()
                set(2026, Calendar.JANUARY, day, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
}
