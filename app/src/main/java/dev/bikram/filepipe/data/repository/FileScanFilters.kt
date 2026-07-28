package dev.bikram.filepipe.data.repository

import android.webkit.MimeTypeMap
import dev.bikram.filepipe.domain.model.isAllFilesExtension
import dev.bikram.filepipe.domain.model.isNoExtensionToken

/**
 * Pure, stateless helpers for filtering scanned files by name / extension / exclusion pattern.
 * Extracted from [FileOperationRepository] so that class stays under detekt's LargeClass limit;
 * none of these depend on injected repository state (context, dispatcher, caches), so they live
 * as module-internal top-level functions in the same package.
 */

internal fun mimeTypeFromName(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
}

internal fun globToRegex(pattern: String): Regex {
    val sb = StringBuilder("^")
    for (ch in pattern) {
        when (ch) {
            '*' -> {
                sb.append(".*")
            }

            '?' -> {
                sb.append(".")
            }

            '.', '(', ')', '[', ']', '^', '$', '+', '{', '}', '|', '\\' -> {
                sb.append('\\')
                sb.append(ch)
            }

            else -> {
                sb.append(ch)
            }
        }
    }
    sb.append("$")
    return Regex(sb.toString(), RegexOption.IGNORE_CASE)
}

internal fun buildFilenameRegexes(
    filenamePattern: String?,
    isRegexPattern: Boolean,
): List<Regex>? {
    val trimmed = filenamePattern?.takeIf { it.isNotBlank() } ?: return null
    if (isRegexPattern) {
        return runCatching {
            listOf(Regex(trimmed, RegexOption.IGNORE_CASE))
        }.getOrElse { emptyList() }
    }
    return trimmed
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { globToRegex(it) }
}

internal fun matchesFilename(
    name: String,
    filenameRegexes: List<Regex>?,
    isRegexPattern: Boolean,
): Boolean {
    if (filenameRegexes == null) return true
    if (filenameRegexes.isEmpty()) return false
    return if (isRegexPattern) {
        filenameRegexes.any { it.containsMatchIn(name) }
    } else {
        filenameRegexes.any { it.matches(name) }
    }
}

internal fun buildExcludeRegexes(
    excludePatterns: List<String>,
    isRegexPattern: Boolean,
): List<Regex>? {
    val nonBlank = excludePatterns.filter { it.isNotBlank() }
    if (nonBlank.isEmpty()) return emptyList()
    return if (isRegexPattern) {
        runCatching {
            nonBlank.map { pattern ->
                Regex(pattern.trim(), RegexOption.IGNORE_CASE)
            }
        }.getOrNull()
    } else {
        nonBlank.map { pattern ->
            globToRegex(pattern.trim())
        }
    }
}

internal fun shouldExclude(
    name: String,
    excludeRegexes: List<Regex>?,
    isRegexPattern: Boolean,
): Boolean {
    if (excludeRegexes == null) return true
    if (excludeRegexes.isEmpty()) return false
    return if (isRegexPattern) {
        excludeRegexes.any { it.containsMatchIn(name) }
    } else {
        excludeRegexes.any { it.matches(name) }
    }
}

internal fun matchesExtensions(
    fileName: String,
    extensions: List<String>,
): Boolean {
    if (extensions.isEmpty()) return false
    if (extensions.any { isAllFilesExtension(it) }) return true

    // Use the LAST dot so a leading-dot file that still has a real suffix
    // (e.g. ".config.json", ".env.local") is treated as having extension "json"/"local",
    // not as "no extension". Only pure dotfiles (".gitignore") and trailing-dot names
    // ("file.") count as extensionless.
    val lastDot = fileName.lastIndexOf('.')
    val hasNoExtension = lastDot <= 0 || fileName.endsWith('.')
    if (hasNoExtension) return extensions.any { isNoExtensionToken(it) }

    val formattedFileExt = fileName.substring(lastDot).lowercase()
    val lowerExtensions =
        extensions
            .map {
                val lower = it.lowercase()
                if (lower.startsWith(".")) lower else ".$lower"
            }.toSet()

    return formattedFileExt in lowerExtensions
}
