package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.domain.model.FileUndoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoStateMachineTest {
    @Test
    fun retrySkipsOnlyPersistedUndoneFiles() {
        assertTrue(shouldAttemptUndo(FileUndoStatus.PENDING))
        assertTrue(shouldAttemptUndo(FileUndoStatus.IN_PROGRESS))
        assertTrue(shouldAttemptUndo(FileUndoStatus.FAILED))
        assertFalse(shouldAttemptUndo(FileUndoStatus.UNDONE))
    }

    @Test
    fun persistenceFailureAfterPhysicalUndoRemainsRecoverable() {
        assertEquals(
            FileUndoStatus.IN_PROGRESS,
            undoStatusAfterFailure(physicalUndoCompleted = true),
        )
    }

    @Test
    fun failureBeforePhysicalUndoCanBeRetriedAsFailed() {
        assertEquals(
            FileUndoStatus.FAILED,
            undoStatusAfterFailure(physicalUndoCompleted = false),
        )
    }
}
