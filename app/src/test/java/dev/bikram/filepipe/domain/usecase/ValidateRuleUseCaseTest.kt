package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RuleSchedule
import dev.bikram.filepipe.domain.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateRuleUseCaseTest {
    private val validate = ValidateRuleUseCase()

    @Test
    fun validRuleWithDailySchedulePasses() {
        val result =
            validate(
                Rule(
                    name = "Docs",
                    sourceFolderPaths = listOf("content://source"),
                    destinationFolderPath = "content://destination",
                    fileExtensions = listOf("pdf"),
                    schedule = RuleSchedule(ScheduleType.DAILY, hour = 9, minute = 45),
                ),
            )

        assertEquals(ValidateRuleUseCase.Result.Valid, result)
    }

    @Test
    fun invalidRuleReportsRequiredPathsTypesAndSameDestination() {
        val result =
            validate(
                Rule(
                    name = " ",
                    sourceFolderPaths = listOf("content://same"),
                    destinationFolderPath = "content://same",
                    fileExtensions = emptyList(),
                ),
            ) as ValidateRuleUseCase.Result.Invalid

        assertTrue(result.errors.contains("Rule name is required"))
        assertTrue(result.errors.contains("At least one file type is required"))
        assertTrue(result.errors.contains("Source and destination folders cannot be the same"))
    }

    @Test
    fun scheduledRulesValidateTheirRequiredTimeFields() {
        val weeklyErrors =
            validate(
                baseRule().copy(
                    schedule = RuleSchedule(ScheduleType.WEEKLY, dayOfWeek = null, hour = 24, minute = 60),
                ),
            ) as ValidateRuleUseCase.Result.Invalid
        val intervalErrors =
            validate(
                baseRule().copy(
                    schedule = RuleSchedule(ScheduleType.EVERY_N_HOURS, hour = 0, minute = 0, intervalHours = 25),
                ),
            ) as ValidateRuleUseCase.Result.Invalid

        assertTrue(weeklyErrors.errors.contains("Weekday is required for weekly schedule"))
        assertTrue(weeklyErrors.errors.contains("Invalid hour in schedule"))
        assertTrue(weeklyErrors.errors.contains("Invalid minute in schedule"))
        assertEquals(listOf("Interval must be between 1 and 24 hours"), intervalErrors.errors)
    }

    private fun baseRule(): Rule =
        Rule(
            name = "Screenshots",
            sourceFolderPaths = listOf("content://source"),
            destinationFolderPath = "content://destination",
            fileExtensions = listOf("png"),
        )
}
