package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.domain.model.ALL_FILES_EXTENSION
import dev.bikram.filepipe.domain.model.NO_EXTENSION_TOKEN
import dev.bikram.filepipe.domain.model.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleConflictDetectorTest {
    @Test
    fun detectsNoExtensionVsPatternConflict() {
        val rule =
            baseRule().copy(
                fileExtensions = listOf(NO_EXTENSION_TOKEN),
                filenamePattern = "*.json",
                isRegexPattern = false,
            )
        val conflicts = RuleConflictDetector.detectConflicts(rule)

        assertEquals(1, conflicts.size)
        assertTrue(conflicts.first() is RuleConflict.NoExtensionPatternConflict)
        val conflict = conflicts.first() as RuleConflict.NoExtensionPatternConflict
        assertEquals("json", conflict.patternExtension)
    }

    @Test
    fun allowsMatchingExtensionWhenNoExtensionAlsoSelected() {
        val rule =
            baseRule().copy(
                fileExtensions = listOf(NO_EXTENSION_TOKEN, "jpg"),
                filenamePattern = "*file.jpg",
                isRegexPattern = false,
            )
        val conflicts = RuleConflictDetector.detectConflicts(rule)

        assertTrue(conflicts.isEmpty())
    }

    @Test
    fun detectsExtensionMismatchWithPattern() {
        val rule =
            baseRule().copy(
                fileExtensions = listOf("jpg", "png"),
                filenamePattern = "*.pdf",
                isRegexPattern = false,
            )
        val conflicts = RuleConflictDetector.detectConflicts(rule)

        assertEquals(1, conflicts.size)
        assertTrue(conflicts.first() is RuleConflict.ExtensionPatternMismatch)
        val conflict = conflicts.first() as RuleConflict.ExtensionPatternMismatch
        assertEquals("pdf", conflict.patternExtension)
    }

    @Test
    fun ignoresPatternConflictWhenAllFilesSelected() {
        val rule =
            baseRule().copy(
                fileExtensions = listOf(ALL_FILES_EXTENSION),
                filenamePattern = "*.json",
                isRegexPattern = false,
            )
        val conflicts = RuleConflictDetector.detectConflicts(rule)

        assertTrue(conflicts.isEmpty())
    }

    @Test
    fun allowsMultiGlobPatternWhenOneAlternativeMatchesSelectedType() {
        val rule =
            baseRule().copy(
                fileExtensions = listOf("pdf"),
                filenamePattern = "*.pdf, *.png",
                isRegexPattern = false,
            )
        val conflicts = RuleConflictDetector.detectConflicts(rule)

        assertTrue(conflicts.isEmpty())
    }

    @Test
    fun detectsMismatchWhenNoMultiGlobAlternativeMatches() {
        val rule =
            baseRule().copy(
                fileExtensions = listOf("jpg"),
                filenamePattern = "*.pdf, *.png",
                isRegexPattern = false,
            )
        val conflicts = RuleConflictDetector.detectConflicts(rule)

        assertEquals(1, conflicts.size)
        assertTrue(conflicts.first() is RuleConflict.ExtensionPatternMismatch)
    }

    @Test
    fun allowsMultiGlobPatternWithUnconstrainedAlternative() {
        val rule =
            baseRule().copy(
                fileExtensions = listOf("pdf"),
                filenamePattern = "*.png, IMG_*",
                isRegexPattern = false,
            )
        val conflicts = RuleConflictDetector.detectConflicts(rule)

        assertTrue(conflicts.isEmpty())
    }

    @Test
    fun detectsInvalidSizeRange() {
        val rule =
            baseRule().copy(
                minFileSizeBytes = 10_000_000L,
                maxFileSizeBytes = 1_000_000L,
            )
        val conflicts = RuleConflictDetector.detectConflicts(rule)

        assertTrue(conflicts.contains(RuleConflict.InvalidSizeRange))
    }

    @Test
    fun detectsInvalidAgeRange() {
        val rule =
            baseRule().copy(
                minAgeDays = 30,
                maxAgeDays = 5,
            )
        val conflicts = RuleConflictDetector.detectConflicts(rule)

        assertTrue(conflicts.contains(RuleConflict.InvalidAgeRange))
    }

    @Test
    fun detectsExcludeAllPattern() {
        val rule =
            baseRule().copy(
                excludePatterns = listOf("*"),
            )
        val conflicts = RuleConflictDetector.detectConflicts(rule)

        assertEquals(1, conflicts.size)
        assertTrue(conflicts.first() is RuleConflict.ExcludeAllPattern)
    }

    private fun baseRule(): Rule =
        Rule(
            name = "Test Rule",
            sourceFolderPaths = listOf("content://source"),
            destinationFolderPath = "content://dest",
            fileExtensions = listOf("pdf"),
        )
}
