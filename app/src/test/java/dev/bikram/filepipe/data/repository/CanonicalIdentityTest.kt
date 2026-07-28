package dev.bikram.filepipe.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class CanonicalIdentityTest {
    @Test
    fun filesystemPathUsesCanonicalIdentity() {
        val rawPath = "build/../build/canonical-test.txt"
        val identity = canonicalFilesystemIdentity(rawPath)

        assertEquals(File(rawPath).canonicalPath, identity)
    }

    @Test
    fun equivalentSafTreeAndDocumentUrisShareIdentity() {
        val treeIdentity =
            canonicalSafTreeIdentity(
                "content://com.android.externalstorage.documents/tree/primary%3APictures",
            )
        val documentIdentity =
            canonicalSafTreeIdentity(
                "content://com.android.externalstorage.documents/tree/primary%3APictures/document/primary%3APictures",
            )

        assertEquals(treeIdentity, documentIdentity)
    }

    @Test
    fun safIdentityKeepsLiteralPlusDistinctFromEncodedSpace() {
        val plusIdentity =
            canonicalSafTreeIdentity(
                "content://com.android.externalstorage.documents/tree/primary%3AFolder+Name",
            )
        val spaceIdentity =
            canonicalSafTreeIdentity(
                "content://com.android.externalstorage.documents/tree/primary%3AFolder%20Name",
            )

        assertEquals("content://com.android.externalstorage.documents/primary:Folder+Name", plusIdentity)
        assertEquals("content://com.android.externalstorage.documents/primary:Folder Name", spaceIdentity)
    }
}
