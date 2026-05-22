package dev.bikram.filepipe.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RuleConflictResolverTest {
    @Test
    fun renameSuffixKeepsNameWhenNoConflictExists() {
        val resolved = resolveRenameSuffixName("report.pdf") { false }

        assertEquals("report.pdf", resolved)
    }

    @Test
    fun renameSuffixPreservesExtensionAndSkipsExistingCandidates() {
        val existing = setOf("report.pdf", "report(1).pdf", "report(2).pdf")

        val resolved = resolveRenameSuffixName("report.pdf") { it in existing }

        assertEquals("report(3).pdf", resolved)
    }

    @Test
    fun renameSuffixHandlesNamesWithoutExtensions() {
        val existing = setOf("Archive", "Archive(1)")

        val resolved = resolveRenameSuffixName("Archive") { it in existing }

        assertEquals("Archive(2)", resolved)
    }
}
