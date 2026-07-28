package dev.bikram.filepipe.data.repository

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dev.bikram.filepipe.data.storage.folderPathForFilesystemAccess
import dev.bikram.filepipe.data.storage.isCanonicalPathUnderAllowedSharedStorage
import dev.bikram.filepipe.data.storage.isFilesystemFolderPathString
import dev.bikram.filepipe.data.storage.normalizeFilesystemFolderPath
import dev.bikram.filepipe.domain.model.FileOrientation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

internal data class ScanCacheKey(
    val folderUriString: String,
    val extensions: List<String>,
    val scanSubdirectories: Boolean,
    val filenamePattern: String?,
    val minFileSizeBytes: Long?,
    val maxFileSizeBytes: Long?,
    val minAgeDays: Int?,
    val maxAgeDays: Int?,
    val excludePatterns: List<String>,
    val maxDepth: Int,
    val orientation: FileOrientation?,
    val filesystemAccessEnabled: Boolean,
    val isRegexPattern: Boolean,
    val isExcludeRegexPattern: Boolean,
)

internal data class CacheEntry(
    val files: List<FileEntry>,
    val timestamp: Long,
)

internal data class SafDocEntry(
    val documentId: String,
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModifiedMs: Long,
    val sizeKnown: Boolean,
    val lastModifiedKnown: Boolean,
    val isFile: Boolean,
    val isDirectory: Boolean,
)

internal data class DocumentMetadata(
    val size: Long?,
    val lastModifiedMs: Long?,
)

internal suspend fun FileOperationRepository.listMatchingFilesScan(
    folderUriString: String,
    extensions: List<String>,
    scanSubdirectories: Boolean = false,
    filenamePattern: String? = null,
    minFileSizeBytes: Long? = null,
    maxFileSizeBytes: Long? = null,
    minAgeDays: Int? = null,
    maxAgeDays: Int? = null,
    excludePatterns: List<String> = emptyList(),
    maxDepth: Int = 5,
    filesystemAccessEnabled: Boolean = false,
    orientation: FileOrientation? = null,
    isRegexPattern: Boolean = false,
    isExcludeRegexPattern: Boolean = false,
): List<FileEntry> =
    withContext(ioDispatcher) {
        val effectiveFolderUriString =
            folderPathForFilesystemAccess(folderUriString, filesystemAccessEnabled)
        if (isFilesystemFolderPathString(effectiveFolderUriString)) {
            if (!filesystemAccessEnabled) return@withContext emptyList()
            val canonical = normalizeFilesystemFolderPath(effectiveFolderUriString) ?: return@withContext emptyList()
            if (!isCanonicalPathUnderAllowedSharedStorage(canonical)) return@withContext emptyList()
            val rootDir = File(canonical)
            if (!rootDir.isDirectory || !rootDir.canRead()) return@withContext emptyList()
            return@withContext listMatchingFilesFromFilesystemRoot(
                rootDir = rootDir,
                extensions = extensions,
                scanSubdirectories = scanSubdirectories,
                filenamePattern = filenamePattern,
                minFileSizeBytes = minFileSizeBytes,
                maxFileSizeBytes = maxFileSizeBytes,
                minAgeDays = minAgeDays,
                maxAgeDays = maxAgeDays,
                excludePatterns = excludePatterns,
                maxDepth = maxDepth,
                orientation = orientation,
                isRegexPattern = isRegexPattern,
                isExcludeRegexPattern = isExcludeRegexPattern,
            )
        }

        if (!effectiveFolderUriString.startsWith("content://")) return@withContext emptyList()

        val treeUri = effectiveFolderUriString.toUri()
        val folder =
            try {
                DocumentFile.fromTreeUri(context, treeUri)
            } catch (_: SecurityException) {
                return@withContext emptyList()
            } ?: return@withContext emptyList()

        if (!folder.exists() || !folder.canRead()) return@withContext emptyList()

        val filenameRegexes = buildFilenameRegexes(filenamePattern, isRegexPattern)
        val excludeRegexes = buildExcludeRegexes(excludePatterns, isExcludeRegexPattern)
        val now = System.currentTimeMillis()
        val minAgeMs = minAgeDays?.let { TimeUnit.DAYS.toMillis(it.toLong()) }
        val maxAgeMs = maxAgeDays?.let { TimeUnit.DAYS.toMillis(it.toLong()) }

        val scanContext = currentCoroutineContext()

        val candidates =
            try {
                val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
                collectSafFiles(treeUri, rootDocumentId, scanSubdirectories, maxDepth)
            } catch (_: SecurityException) {
                return@withContext emptyList()
            } catch (_: IllegalArgumentException) {
                return@withContext emptyList()
            }

        candidates
            .asSequence()
            .filter { (doc, _) -> matchesExtensions(doc.name, extensions) }
            .filter { (doc, _) -> matchesFilename(doc.name, filenameRegexes, isRegexPattern) }
            .filter { (doc, _) -> !shouldExclude(doc.name, excludeRegexes, isExcludeRegexPattern) }
            .filter { (doc, _) -> minFileSizeBytes == null || doc.size >= minFileSizeBytes }
            .filter { (doc, _) -> maxFileSizeBytes == null || doc.size <= maxFileSizeBytes }
            .filter { (doc, _) ->
                if (minAgeMs == null && maxAgeMs == null) return@filter true
                val ageMs = now - doc.lastModifiedMs
                (minAgeMs == null || ageMs >= minAgeMs) && (maxAgeMs == null || ageMs <= maxAgeMs)
            }.filter { (doc, _) ->
                scanContext.ensureActive()
                if (orientation == null) return@filter true
                getDocumentUriOrientation(context, doc.name, doc.uri) == orientation
            }.map { (doc, relativeParentSegments) ->
                FileEntry(
                    uri = doc.uri,
                    name = doc.name,
                    size = doc.size,
                    lastModifiedMs = doc.lastModifiedMs,
                    sizeKnown = doc.sizeKnown,
                    lastModifiedKnown = doc.lastModifiedKnown,
                    relativeParentSegments = relativeParentSegments,
                )
            }.toList()
    }

internal suspend fun FileOperationRepository.listMatchingFilesFromFilesystemRoot(
    rootDir: File,
    extensions: List<String>,
    scanSubdirectories: Boolean,
    filenamePattern: String?,
    minFileSizeBytes: Long?,
    maxFileSizeBytes: Long?,
    minAgeDays: Int?,
    maxAgeDays: Int?,
    excludePatterns: List<String>,
    maxDepth: Int,
    orientation: FileOrientation?,
    isRegexPattern: Boolean = false,
    isExcludeRegexPattern: Boolean = false,
): List<FileEntry> {
    val scanContext = currentCoroutineContext()
    val filenameRegexes = buildFilenameRegexes(filenamePattern, isRegexPattern)
    val excludeRegexes = buildExcludeRegexes(excludePatterns, isExcludeRegexPattern)
    val now = System.currentTimeMillis()
    val minAgeMs = minAgeDays?.let { TimeUnit.DAYS.toMillis(it.toLong()) }
    val maxAgeMs = maxAgeDays?.let { TimeUnit.DAYS.toMillis(it.toLong()) }
    val sequence: Sequence<Pair<File, List<String>>> =
        if (scanSubdirectories) {
            walkDiskFilesWithRelativeParents(rootDir, maxDepth, emptyList())
        } else {
            (rootDir.listFiles()?.asSequence() ?: emptySequence())
                .filter { it.isFile }
                .map { it to emptyList() }
        }
    return sequence
        .onEach { scanContext.ensureActive() }
        .filter { (file, _) -> matchesExtensions(file.name, extensions) }
        .filter { (file, _) -> matchesFilename(file.name, filenameRegexes, isRegexPattern) }
        .filter { (file, _) -> !shouldExclude(file.name, excludeRegexes, isExcludeRegexPattern) }
        .filter { (file, _) -> minFileSizeBytes == null || file.length() >= minFileSizeBytes }
        .filter { (file, _) -> maxFileSizeBytes == null || file.length() <= maxFileSizeBytes }
        .filter { (file, _) ->
            if (minAgeMs == null && maxAgeMs == null) return@filter true
            val ageMs = now - file.lastModified()
            (minAgeMs == null || ageMs >= minAgeMs) && (maxAgeMs == null || ageMs <= maxAgeMs)
        }.filter { (file, _) ->
            if (orientation == null) return@filter true
            val fileOrientation = getDiskFileOrientation(file)
            fileOrientation == orientation
        }.map { (file, relativeParentSegments) ->
            FileEntry(
                uri = file.toUri(),
                name = file.name,
                size = file.length(),
                lastModifiedMs = file.lastModified(),
                relativeParentSegments = relativeParentSegments,
            )
        }.toList()
}

internal fun walkDiskFilesWithRelativeParents(
    dir: File,
    maxDepth: Int,
    relativeParentSegments: List<String>,
): Sequence<Pair<File, List<String>>> =
    sequence {
        if (maxDepth <= 0) return@sequence
        dir.listFiles()?.forEach { child ->
            val segment = child.name.trim()
            if (child.isFile) {
                yield(child to relativeParentSegments)
            } else if (child.isDirectory && segment.isNotEmpty() && segment != "." && segment != "..") {
                yieldAll(
                    walkDiskFilesWithRelativeParents(
                        child,
                        maxDepth - 1,
                        relativeParentSegments + segment,
                    ),
                )
            }
        }
    }

internal fun FileOperationRepository.querySafChildren(
    treeUri: Uri,
    parentDocumentId: String,
): List<SafDocEntry> {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
    val projection =
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
    val results = mutableListOf<SafDocEntry>()
    try {
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modifiedIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            if (idIdx == -1) return emptyList()
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(idIdx) ?: continue
                val mimeType = if (mimeIdx != -1) cursor.getString(mimeIdx) else null
                val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                val isFile = !mimeType.isNullOrEmpty() && !isDirectory
                results +=
                    SafDocEntry(
                        documentId = documentId,
                        uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                        name = (if (nameIdx != -1) cursor.getString(nameIdx) else null).orEmpty(),
                        size = if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L,
                        lastModifiedMs = if (modifiedIdx != -1 && !cursor.isNull(modifiedIdx)) cursor.getLong(modifiedIdx) else 0L,
                        sizeKnown = sizeIdx != -1 && !cursor.isNull(sizeIdx),
                        lastModifiedKnown = modifiedIdx != -1 && !cursor.isNull(modifiedIdx),
                        isFile = isFile,
                        isDirectory = isDirectory,
                    )
            }
        }
    } catch (_: Exception) {
        return emptyList()
    }
    return results
}

internal fun FileOperationRepository.queryDocumentMetadata(documentUri: Uri): DocumentMetadata? {
    val projection =
        arrayOf(
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    return try {
        context.contentResolver.query(documentUri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            DocumentMetadata(
                size =
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        cursor.getLong(sizeIndex)
                    } else {
                        null
                    },
                lastModifiedMs =
                    if (modifiedIndex != -1 && !cursor.isNull(modifiedIndex)) {
                        cursor.getLong(modifiedIndex)
                    } else {
                        null
                    },
            )
        }
    } catch (_: Exception) {
        null
    }
}

internal suspend fun FileOperationRepository.collectSafFiles(
    treeUri: Uri,
    rootDocumentId: String,
    scanSubdirectories: Boolean,
    maxDepth: Int,
): List<Pair<SafDocEntry, List<String>>> {
    val scanContext = currentCoroutineContext()
    val out = mutableListOf<Pair<SafDocEntry, List<String>>>()

    fun visit(
        parentDocumentId: String,
        relativeParents: List<String>,
        depth: Int,
    ) {
        if (depth <= 0) return
        for (child in querySafChildren(treeUri, parentDocumentId)) {
            scanContext.ensureActive()
            if (child.isFile) {
                out += child to relativeParents
            } else if (scanSubdirectories && child.isDirectory) {
                val segment = child.name.trim()
                if (segment.isNotEmpty() && segment != "." && segment != "..") {
                    visit(child.documentId, relativeParents + segment, depth - 1)
                }
            }
        }
    }

    visit(rootDocumentId, emptyList(), if (scanSubdirectories) maxDepth else 1)
    return out
}
