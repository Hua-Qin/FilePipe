package dev.bikram.filepipe.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileScanFiltersTest {
    @Test
    fun invalidFilenameRegexMatchesNothing() {
        val compiledPatterns = buildFilenameRegexes("[", isRegexPattern = true)

        assertFalse(matchesFilename("photo.jpg", compiledPatterns, isRegexPattern = true))
    }

    @Test
    fun missingFilenamePatternStillMatches() {
        val compiledPatterns = buildFilenameRegexes(null, isRegexPattern = true)

        assertTrue(matchesFilename("photo.jpg", compiledPatterns, isRegexPattern = true))
    }

    @Test
    fun invalidExcludeRegexExcludesEverything() {
        val compiledPatterns = buildExcludeRegexes(listOf("["), isRegexPattern = true)

        assertTrue(shouldExclude("photo.jpg", compiledPatterns, isRegexPattern = true))
    }

    @Test
    fun incompleteKnownSizeCopyIsRejected() {
        assertFalse(isCompleteCopy(expectedBytes = 100L, copiedBytes = 99L))
        assertTrue(isCompleteCopy(expectedBytes = 100L, copiedBytes = 100L))
        assertTrue(isCompleteCopy(expectedBytes = 0L, copiedBytes = 0L))
        assertFalse(isCompleteCopy(expectedBytes = 0L, copiedBytes = 1L))
        assertTrue(isCompleteCopy(expectedBytes = 0L, copiedBytes = 1L, sizeKnown = false))
    }
}
