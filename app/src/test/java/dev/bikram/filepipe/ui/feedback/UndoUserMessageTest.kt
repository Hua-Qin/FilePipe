package dev.bikram.filepipe.ui.feedback

import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.usecase.UndoResult
import org.junit.Assert.assertFalse
import org.junit.Test

class UndoUserMessageTest {
    @Test
    fun concurrentUndoResultReturnsNullUserMessage() {
        val result =
            UndoResult(
                totalReversed = 0,
                totalFailed = 0,
                errors = emptyList(),
                operationMode = OperationMode.MOVE,
                isAlreadyInProgress = true,
            )

        assertFalse(
            "Concurrent undo should not display a user failure message",
            result.shouldDisplayUserMessage(),
        )
    }
}
