package dev.bikram.filepipe.data.local.entity

import dev.bikram.filepipe.domain.model.FileMoved
import dev.bikram.filepipe.domain.model.FileUndoStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class FileMovedEntityTest {
    @Test
    fun undoStatusSurvivesEntityMapping() {
        val domainFile =
            FileMoved(
                id = 9L,
                runHistoryId = 4L,
                fileName = "photo.jpg",
                sourceUri = "content://source/photo.jpg",
                destinationUri = "content://destination/photo.jpg",
                fileSizeBytes = 512L,
                movedAt = 123L,
                success = true,
                undoStatus = FileUndoStatus.FAILED,
            )

        val restoredFile = domainFile.toEntity(domainFile.runHistoryId).toDomain()

        assertEquals(FileUndoStatus.FAILED, restoredFile.undoStatus)
    }
}
