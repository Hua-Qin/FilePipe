package dev.bikram.filepipe.ui.screens.rules

import dev.bikram.filepipe.domain.model.PreviewFileResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeleteConfirmationTest {
    @Test
    fun noDeleteResultsProducesNoConfirmation() {
        assertNull(deleteConfirmationFor(emptyList()))
    }

    @Test
    fun allFilesSkippedProducesNoConfirmation() {
        val results = listOf(listOf(preview("a.tmp", wouldSkip = true), preview("b.tmp", wouldSkip = true)))
        assertNull(deleteConfirmationFor(results))
    }

    @Test
    fun onlyNonSkippedFilesAreCounted() {
        val results =
            listOf(
                listOf(
                    preview("a.tmp"),
                    preview("b.tmp", wouldSkip = true),
                    preview("c.tmp"),
                ),
            )

        val confirmation = deleteConfirmationFor(results)

        assertEquals(2, confirmation?.fileCount)
        assertEquals(listOf("a.tmp", "c.tmp"), confirmation?.sampleFileNames)
    }

    @Test
    fun sampleIsCappedButCountReflectsAllAffected() {
        val results = listOf((1..8).map { preview("file$it.tmp") })

        val confirmation = deleteConfirmationFor(results, sampleSize = 5)

        assertEquals(8, confirmation?.fileCount)
        assertEquals(5, confirmation?.sampleFileNames?.size)
        assertEquals("file1.tmp", confirmation?.sampleFileNames?.first())
    }

    @Test
    fun affectedFilesAcrossMultipleDeleteRulesAreAggregated() {
        val results =
            listOf(
                listOf(preview("a.tmp"), preview("b.tmp")),
                listOf(preview("c.bak")),
            )

        val confirmation = deleteConfirmationFor(results)

        assertEquals(3, confirmation?.fileCount)
        assertEquals(listOf("a.tmp", "b.tmp", "c.bak"), confirmation?.sampleFileNames)
    }

    private fun preview(
        name: String,
        wouldSkip: Boolean = false,
    ): PreviewFileResult =
        PreviewFileResult(
            fileName = name,
            sourcePath = "content://source/$name",
            simulatedDestPath = "",
            wouldSkip = wouldSkip,
            wouldOverwrite = false,
            renamedTo = null,
            sizeBytes = 1_024L,
        )
}
