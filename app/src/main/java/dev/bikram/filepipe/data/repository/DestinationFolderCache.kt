package dev.bikram.filepipe.data.repository

import androidx.documentfile.provider.DocumentFile

/**
 * Destination folder lookups shared by the sequential file operations in one rule run.
 *
 * Callers must create a new instance for each run so SAF provider state is never retained across
 * independently scheduled work.
 */
class DestinationFolderCache {
    internal val safRoots = mutableMapOf<String, DocumentFile>()
    internal val safParents = mutableMapOf<SafDestinationParentKey, DocumentFile>()
}

internal data class SafDestinationParentKey(
    val destinationRoot: String,
    val relativeParentSegments: List<String>,
)

internal fun normalizeDestinationParentSegments(relativeParentSegments: List<String>): List<String> =
    relativeParentSegments
        .map { segment -> segment.trim() }
        .filter { segment -> segment.isNotEmpty() && segment != "." && segment != ".." }
