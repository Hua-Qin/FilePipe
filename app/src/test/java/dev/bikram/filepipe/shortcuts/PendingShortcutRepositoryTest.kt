package dev.bikram.filepipe.shortcuts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingShortcutRepositoryTest {
    @Test
    fun historyDetailUndoRequestIsConsumedOnce() {
        val repository = PendingShortcutRepository()
        val request = PendingHistoryUndoRequest(historyId = 42L, notificationId = 700)

        repository.requestOpenHistoryDetail(
            historyId = request.historyId,
            undoNotificationId = request.notificationId,
        )

        assertEquals(request.historyId, repository.pendingHistoryDetailId.value)
        assertEquals(request, repository.pendingHistoryUndoRequest.value)
        assertTrue(repository.consumePendingHistoryUndo(request))
        assertNull(repository.pendingHistoryUndoRequest.value)
        assertFalse(repository.consumePendingHistoryUndo(request))
    }

    @Test
    fun ordinaryHistoryDetailRequestDoesNotRequestUndo() {
        val repository = PendingShortcutRepository()

        repository.requestOpenHistoryDetail(historyId = 42L)

        assertEquals(42L, repository.pendingHistoryDetailId.value)
        assertNull(repository.pendingHistoryUndoRequest.value)
    }
}
