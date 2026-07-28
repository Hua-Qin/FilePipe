package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.domain.model.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportRulesValidationTest {
    @Test
    fun invalidFilenameAndExcludeRegexRulesAreRejected() {
        val invalidFilenameRule =
            Rule(
                name = "Invalid filename",
                sourceFolderPaths = listOf("content://source"),
                destinationFolderPath = "content://destination",
                fileExtensions = listOf("txt"),
                filenamePattern = "[",
                isRegexPattern = true,
            )
        val invalidExcludeRule =
            Rule(
                name = "Invalid exclusion",
                sourceFolderPaths = listOf("content://source"),
                destinationFolderPath = "content://destination",
                fileExtensions = listOf("txt"),
                excludePatterns = listOf("("),
                isExcludeRegexPattern = true,
            )

        assertEquals(
            listOf("Invalid filename", "Invalid exclusion"),
            findRulesWithInvalidRegexPatterns(listOf(invalidFilenameRule, invalidExcludeRule)),
        )
    }

    @Test
    fun validRegexRulesPassImportValidation() {
        val validRule =
            Rule(
                name = "Valid",
                sourceFolderPaths = listOf("content://source"),
                destinationFolderPath = "content://destination",
                fileExtensions = listOf("txt"),
                filenamePattern = "^report",
                excludePatterns = listOf("temporary$"),
                isRegexPattern = true,
                isExcludeRegexPattern = true,
            )

        assertTrue(findRulesWithInvalidRegexPatterns(listOf(validRule)).isEmpty())
    }
}
