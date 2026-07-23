package dev.bikram.filepipe.domain.usecase

import dev.bikram.filepipe.domain.model.ALL_FILES_EXTENSION
import dev.bikram.filepipe.domain.model.NO_EXTENSION_TOKEN
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.normalizeExtension

sealed class RuleConflict {
    data class NoExtensionPatternConflict(
        val pattern: String,
        val patternExtension: String,
    ) : RuleConflict()

    data class ExtensionPatternMismatch(
        val patternExtension: String,
        val selectedExtensions: List<String>,
    ) : RuleConflict()

    data object InvalidSizeRange : RuleConflict()

    data object InvalidAgeRange : RuleConflict()

    data class ExcludeAllPattern(
        val pattern: String,
    ) : RuleConflict()
}

object RuleConflictDetector {
    fun detectConflicts(rule: Rule): List<RuleConflict> =
        buildList {
            // 1. Range inversions
            if (rule.minFileSizeBytes != null &&
                rule.maxFileSizeBytes != null &&
                rule.minFileSizeBytes > rule.maxFileSizeBytes
            ) {
                add(RuleConflict.InvalidSizeRange)
            }
            if (rule.minAgeDays != null &&
                rule.maxAgeDays != null &&
                rule.minAgeDays > rule.maxAgeDays
            ) {
                add(RuleConflict.InvalidAgeRange)
            }

            // 2. Exclusion pattern wipes out all files
            rule.excludePatterns.firstOrNull { it.trim() == "*" || it.trim() == ".*" }?.let { wildcardPattern ->
                add(RuleConflict.ExcludeAllPattern(wildcardPattern))
            }

            // 3. Extension vs. Name pattern contradictions
            val pattern = rule.filenamePattern?.trim()
            if (!pattern.isNullOrBlank()) {
                val patternExt = extractExtensionFromPattern(pattern, rule.isRegexPattern)
                if (patternExt != null) {
                    val hasAllFiles = rule.fileExtensions.contains(ALL_FILES_EXTENSION)
                    val normalizedAllowedExts = rule.fileExtensions.map { normalizeExtension(it) }.toSet()

                    if (!hasAllFiles && patternExt !in normalizedAllowedExts) {
                        val onlyNoExt = rule.fileExtensions.size == 1 && rule.fileExtensions.contains(NO_EXTENSION_TOKEN)
                        if (onlyNoExt) {
                            add(RuleConflict.NoExtensionPatternConflict(pattern, patternExt))
                        } else {
                            add(RuleConflict.ExtensionPatternMismatch(patternExt, rule.fileExtensions))
                        }
                    }
                }
            }
        }

    fun extractExtensionFromPattern(
        pattern: String,
        isRegex: Boolean,
    ): String? {
        if (!isRegex) {
            // Glob pattern like "*.json", "doc_*.pdf", "invoice.zip"
            if (pattern.contains('.') && !pattern.endsWith('.')) {
                val candidate = pattern.substringAfterLast('.').lowercase().trim()
                if (candidate.isNotEmpty() && candidate.all { it.isLetterOrDigit() }) {
                    return candidate
                }
            }
        } else {
            // Regex pattern like ".*\.json$", ".*\.pdf$", ".*\.docx"
            val match = Regex("""\.\$?([a-zA-Z0-9]+)\$$""").find(pattern)
                ?: Regex("""\.\$?([a-zA-Z0-9]+)$""").find(pattern)
            if (match != null) {
                return match.groupValues[1].lowercase()
            }
        }
        return null
    }
}
