package dev.bikram.filepipe.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunOutcomeTest {
    @Test
    fun runResultClassifiesSuccessSkippedFailureAndPartialFailure() {
        val result =
            RunResult(
                ruleId = 1L,
                ruleName = "Downloads",
                historyId = 7L,
                filesMoved =
                    listOf(
                        moved("kept.pdf", success = true),
                        moved("duplicate.pdf", success = true, skipped = true),
                        moved("locked.pdf", success = false),
                    ),
                startedAt = 1L,
                completedAt = 2L,
            )

        assertEquals(1, result.totalMoved)
        assertEquals(1, result.totalSkipped)
        assertEquals(1, result.totalFailed)
        assertEquals(RunStatus.PARTIAL_FAILURE, result.status)
    }

    @Test
    fun runResultWithOnlyFailuresIsFailedAndEmptyRunIsNoChangesSuccess() {
        val failed =
            RunResult(
                ruleId = 1L,
                ruleName = "Downloads",
                historyId = 7L,
                filesMoved = listOf(moved("locked.pdf", success = false)),
                startedAt = 1L,
                completedAt = 2L,
            )
        val noChanges =
            RunResult(
                ruleId = 1L,
                ruleName = "Downloads",
                historyId = 8L,
                filesMoved = emptyList(),
                startedAt = 1L,
                completedAt = 2L,
            )

        assertEquals(RunStatus.FAILED, failed.status)
        assertEquals(RunStatus.SUCCESS, noChanges.status)
    }

    @Test
    fun failedSourceDeletionKeepsDestinationRecoverable() {
        val partialMove =
            RunResult(
                ruleId = 1L,
                ruleName = "Downloads",
                historyId = 9L,
                filesMoved =
                    listOf(
                        moved(
                            fileName = "duplicate.pdf",
                            success = false,
                            destinationCreated = true,
                        ),
                    ),
                startedAt = 1L,
                completedAt = 2L,
            )

        assertEquals(1, partialMove.totalMoved)
        assertEquals(1, partialMove.totalFailed)
        assertEquals(RunStatus.PARTIAL_FAILURE, partialMove.status)
        assertTrue(partialMove.filesMoved.single().hasRecoverableDestination)
    }

    @Test
    fun undoStateTreatsLegacyReversedRowsAndUndoneStatusAsUndone() {
        assertTrue(history(status = RunStatus.SUCCESS, isReversed = true).isEffectivelyUndone())
        assertTrue(history(status = RunStatus.UNDONE, isReversed = false).isEffectivelyUndone())
        assertFalse(history(status = RunStatus.SUCCESS, isReversed = false).isEffectivelyUndone())
    }

    private fun moved(
        fileName: String,
        success: Boolean,
        skipped: Boolean = false,
        destinationCreated: Boolean = success,
    ): FileMoved =
        FileMoved(
            fileName = fileName,
            sourceUri = "content://source/$fileName",
            destinationUri = if (destinationCreated) "content://destination/$fileName" else "",
            fileSizeBytes = 100L,
            success = success,
            skipped = skipped,
        )

    private fun history(
        status: RunStatus,
        isReversed: Boolean,
    ): RunHistory =
        RunHistory(
            ruleId = 1L,
            ruleName = "Downloads",
            triggeredBy = TriggerType.MANUAL,
            startedAt = 1L,
            completedAt = 2L,
            status = status,
            isReversed = isReversed,
        )
}
