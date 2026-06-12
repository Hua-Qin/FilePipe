package dev.bikram.filepipe.data.local.entity

import dev.bikram.filepipe.domain.model.ScheduleType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEntityTest {
    @Test
    fun hourlyScheduleWithoutStoredTimeUsesLegacyRelativeIntervalMode() {
        val rule =
            baseEntity(
                scheduleType = ScheduleType.EVERY_N_HOURS,
                scheduleHour = null,
                scheduleMinute = null,
                scheduleIntervalHours = 3,
            ).toDomain()

        val schedule = requireNotNull(rule.schedule)
        assertFalse(schedule.usesStartTime)
    }

    @Test
    fun hourlyScheduleWithoutStartTimePersistsNullTimeFields() {
        val entity =
            baseEntity(
                scheduleType = ScheduleType.EVERY_N_HOURS,
                scheduleHour = null,
                scheduleMinute = null,
                scheduleIntervalHours = 3,
            ).toDomain()
                .toEntity()

        assertNull(entity.scheduleHour)
        assertNull(entity.scheduleMinute)
    }

    @Test
    fun hourlyScheduleWithStoredTimeUsesAnchoredStartTimeMode() {
        val rule =
            baseEntity(
                scheduleType = ScheduleType.EVERY_N_HOURS,
                scheduleHour = 9,
                scheduleMinute = 30,
                scheduleIntervalHours = 3,
            ).toDomain()

        val schedule = requireNotNull(rule.schedule)
        assertTrue(schedule.usesStartTime)
    }

    private fun baseEntity(
        scheduleType: ScheduleType?,
        scheduleHour: Int?,
        scheduleMinute: Int?,
        scheduleIntervalHours: Int?,
    ): RuleEntity =
        RuleEntity(
            name = "Screenshots",
            sourceFolderPaths = listOf("content://source"),
            destinationFolderPath = "content://destination",
            fileExtensions = listOf("png"),
            scheduleType = scheduleType,
            scheduleHour = scheduleHour,
            scheduleMinute = scheduleMinute,
            scheduleIntervalHours = scheduleIntervalHours,
        )
}
