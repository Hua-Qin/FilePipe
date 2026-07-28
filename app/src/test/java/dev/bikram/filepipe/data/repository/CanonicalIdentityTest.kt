package dev.bikram.filepipe.data.repository

import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalIdentityTest {

    @Test
    fun filesystemUriUsesPathForCanonicalIdentity() {
        val entry = FileEntry(
            uri = "file:///storage/emulated/0/Pictures/Vacation/photo.jpg".toUri(),
            name = "photo.jpg",
            size = 1024L,
        )

        assertEquals("/storage/emulated/0/Pictures/Vacation/photo.jpg", entry.canonicalIdentity())
    }
}
