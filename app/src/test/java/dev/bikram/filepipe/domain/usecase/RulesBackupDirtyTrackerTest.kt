package dev.bikram.filepipe.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesBackupDirtyTrackerTest {
    @Test
    fun consumePendingChangeReturnsFalseUntilRuleChangeIsMarked() {
        val tracker = RulesBackupDirtyTracker()

        assertFalse(tracker.consumePendingChangeSinceLastTreeExport())

        tracker.markRulesChangedSinceLastTreeExport()

        assertTrue(tracker.consumePendingChangeSinceLastTreeExport())
        assertFalse(tracker.consumePendingChangeSinceLastTreeExport())
    }

    @Test
    fun markingAgainAfterFailedExportKeepsNextFlushDirty() {
        val tracker = RulesBackupDirtyTracker()
        tracker.markRulesChangedSinceLastTreeExport()

        assertTrue(tracker.consumePendingChangeSinceLastTreeExport())

        tracker.markRulesChangedSinceLastTreeExport()

        assertTrue(tracker.consumePendingChangeSinceLastTreeExport())
    }
}
