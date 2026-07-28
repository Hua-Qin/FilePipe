package dev.bikram.filepipe.ui.feedback

import android.content.Context
import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.usecase.UndoResult
import org.junit.Assert.assertNull
import org.junit.Test

class UndoUserMessageTest {

    @Test
    fun concurrentUndoResultReturnsNullUserMessage() {
        val result = UndoResult(
            totalReversed = 0,
            totalFailed = 0,
            errors = emptyList(),
            operationMode = OperationMode.MOVE,
            isAlreadyInProgress = true,
        )

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val message = result.toUserMessage(null as Context)
        assertNull("Concurrent undo should not display a user failure message", message)
    }
}
