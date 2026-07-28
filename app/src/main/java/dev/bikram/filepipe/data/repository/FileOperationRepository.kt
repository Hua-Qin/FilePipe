package dev.bikram.filepipe.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.data.storage.folderPathForFilesystemAccess
import dev.bikram.filepipe.data.storage.isCanonicalPathUnderAllowedSharedStorage
import dev.bikram.filepipe.data.storage.isFilesystemFolderPathString
import dev.bikram.filepipe.data.storage.normalizeFilesystemFolderPath
import dev.bikram.filepipe.di.IoDispatcher
import dev.bikram.filepipe.domain.model.ConflictPolicy
import dev.bikram.filepipe.domain.model.FileMoved
import dev.bikram.filepipe.domain.model.FileOrientation
import dev.bikram.filepipe.domain.model.FolderAccessResult
import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.model.PreviewFileResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** How long a cached scan stays reusable (preview/simulate → run) before it's treated as stale. */
internal const val SCAN_CACHE_TTL_MS = 300_000L

@Suppress("ktlint:standard:function-expression-body")
internal fun isCompleteCopy(
    expectedBytes: Long,
    copiedBytes: Long,
    sizeKnown: Boolean = true,
): Boolean {
    return !sizeKnown || copiedBytes == expectedBytes
}

@Singleton
class FileOperationRepository
    @Inject
    constructor(
        @param:ApplicationContext internal val context: Context,
        @IoDispatcher internal val ioDispatcher: CoroutineDispatcher,
    ) {
        internal val scanCache = ConcurrentHashMap<ScanCacheKey, CacheEntry>()
        private val accessCache = ConcurrentHashMap<String, Pair<FolderAccessResult, Long>>()
        private val accessCacheTtlMs = 5_000L

        suspend fun listMatchingFiles(
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
            useCache: Boolean = false,
        ): List<FileEntry> =
            withContext(ioDispatcher) {
                val cacheKey =
                    ScanCacheKey(
                        folderUriString = folderUriString,
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
                        filesystemAccessEnabled = filesystemAccessEnabled,
                        isRegexPattern = isRegexPattern,
                        isExcludeRegexPattern = isExcludeRegexPattern,
                    )

                if (useCache) {
                    val nowTime = System.currentTimeMillis()
                    val cached = scanCache[cacheKey]
                    if (cached != null && (nowTime - cached.timestamp) < SCAN_CACHE_TTL_MS) {
                        scanCache.remove(cacheKey)
                        return@withContext cached.files
                    }
                }

                val resultList =
                    listMatchingFilesScan(
                        folderUriString = folderUriString,
                        extensions = extensions,
                        scanSubdirectories = scanSubdirectories,
                        filenamePattern = filenamePattern,
                        minFileSizeBytes = minFileSizeBytes,
                        maxFileSizeBytes = maxFileSizeBytes,
                        minAgeDays = minAgeDays,
                        maxAgeDays = maxAgeDays,
                        excludePatterns = excludePatterns,
                        maxDepth = maxDepth,
                        filesystemAccessEnabled = filesystemAccessEnabled,
                        orientation = orientation,
                        isRegexPattern = isRegexPattern,
                        isExcludeRegexPattern = isExcludeRegexPattern,
                    )

                val writeTime = System.currentTimeMillis()
                scanCache.entries.removeAll { (_, entry) -> writeTime - entry.timestamp >= SCAN_CACHE_TTL_MS }
                scanCache[cacheKey] = CacheEntry(resultList, writeTime)
                resultList
            }

        suspend fun moveFile(
            sourceEntry: FileEntry,
            destFolderUriString: String,
            conflictPolicy: ConflictPolicy,
            operationMode: OperationMode,
            destFoldersCreatedCollector: MutableCollection<String>? = null,
            filesystemAccessEnabled: Boolean = false,
            requireUnchangedSource: Boolean = false,
        ): FileMoved =
            withContext(ioDispatcher + NonCancellable) {
                if (operationMode == OperationMode.DELETE) {
                    return@withContext deleteFile(
                        sourceEntry = sourceEntry,
                        filesystemAccessEnabled = filesystemAccessEnabled,
                        requireUnchangedSource = requireUnchangedSource,
                    )
                }

                val effectiveDestFolder =
                    folderPathForFilesystemAccess(destFolderUriString, filesystemAccessEnabled)
                val sourceIsFile = sourceEntry.uri.scheme == "file"
                val destIsFilesystem = isFilesystemFolderPathString(effectiveDestFolder)

                if (sourceIsFile && !filesystemAccessEnabled) {
                    return@withContext FileMoved(
                        fileName = sourceEntry.name,
                        sourceUri = sourceEntry.uri.toString(),
                        destinationUri = "",
                        fileSizeBytes = sourceEntry.size,
                        relativeParentSegments = sourceEntry.relativeParentSegments,
                        success = false,
                        errorMessage = "All files access is required for this source path",
                    )
                }
                if (destIsFilesystem && !filesystemAccessEnabled) {
                    return@withContext FileMoved(
                        fileName = sourceEntry.name,
                        sourceUri = sourceEntry.uri.toString(),
                        destinationUri = "",
                        fileSizeBytes = sourceEntry.size,
                        relativeParentSegments = sourceEntry.relativeParentSegments,
                        success = false,
                        errorMessage = "All files access is required for this destination path",
                    )
                }

                when {
                    destIsFilesystem && sourceIsFile -> {
                        moveFileFilesystemToFilesystem(
                            sourceEntry,
                            effectiveDestFolder,
                            conflictPolicy,
                            operationMode,
                            destFoldersCreatedCollector,
                        )
                    }

                    destIsFilesystem && !sourceIsFile -> {
                        moveFileDocumentToFilesystem(
                            sourceEntry,
                            effectiveDestFolder,
                            conflictPolicy,
                            operationMode,
                            destFoldersCreatedCollector,
                        )
                    }

                    !destIsFilesystem && sourceIsFile -> {
                        moveFileFilesystemToDocument(
                            sourceEntry,
                            effectiveDestFolder,
                            conflictPolicy,
                            operationMode,
                            destFoldersCreatedCollector,
                        )
                    }

                    else -> {
                        moveFileDocumentToDocument(
                            sourceEntry,
                            effectiveDestFolder,
                            conflictPolicy,
                            operationMode,
                            destFoldersCreatedCollector,
                        )
                    }
                }
            }

        suspend fun simulateMove(
            sourceEntry: FileEntry,
            destFolderUriString: String,
            conflictPolicy: ConflictPolicy,
            operationMode: OperationMode = OperationMode.MOVE,
            filesystemAccessEnabled: Boolean = false,
        ): PreviewFileResult =
            withContext(ioDispatcher) {
                if (operationMode == OperationMode.DELETE) {
                    return@withContext unchangedPreviewResult(sourceEntry, "")
                }

                val effectiveDestFolder =
                    folderPathForFilesystemAccess(destFolderUriString, filesystemAccessEnabled)
                val simulatedRootPath =
                    buildSimulatedDestPreviewPath(
                        effectiveDestFolder,
                        sourceEntry.relativeParentSegments,
                        sourceEntry.name,
                    )

                if (effectiveDestFolder.isBlank()) {
                    return@withContext unchangedPreviewResult(sourceEntry, simulatedRootPath)
                }

                if (isFilesystemFolderPathString(effectiveDestFolder)) {
                    return@withContext simulateFilesystemMove(
                        sourceEntry = sourceEntry,
                        destFolderUriString = effectiveDestFolder,
                        conflictPolicy = conflictPolicy,
                        filesystemAccessEnabled = filesystemAccessEnabled,
                        simulatedRootPath = simulatedRootPath,
                    )
                }

                simulateSafMove(
                    sourceEntry = sourceEntry,
                    destFolderUriString = effectiveDestFolder,
                    conflictPolicy = conflictPolicy,
                    simulatedRootPath = simulatedRootPath,
                )
            }

        fun resolveFolderAccess(
            folderPathOrUri: String,
            filesystemAccessEnabled: Boolean = false,
        ): FolderAccessResult {
            val effectivePath =
                folderPathForFilesystemAccess(folderPathOrUri, filesystemAccessEnabled)
            val cacheKey = "${folderPathOrUri}\u0000$filesystemAccessEnabled"
            val cached = accessCache[cacheKey]
            if (cached != null && System.currentTimeMillis() - cached.second < accessCacheTtlMs) {
                return cached.first
            }
            val resolved =
                when {
                    isFilesystemFolderPathString(effectivePath) -> {
                        when {
                            !filesystemAccessEnabled -> {
                                FolderAccessResult.PermissionDenied
                            }

                            else -> {
                                val canonical = normalizeFilesystemFolderPath(effectivePath)
                                when {
                                    canonical == null -> {
                                        FolderAccessResult.Unavailable
                                    }

                                    !isCanonicalPathUnderAllowedSharedStorage(canonical) -> {
                                        FolderAccessResult.Unavailable
                                    }

                                    else -> {
                                        val dir = File(canonical)
                                        when {
                                            !dir.exists() || !dir.isDirectory -> FolderAccessResult.Unavailable
                                            !dir.canRead() -> FolderAccessResult.PermissionDenied
                                            else -> FolderAccessResult.Accessible
                                        }
                                    }
                                }
                            }
                        }
                    }

                    effectivePath.startsWith("content://") -> {
                        try {
                            val document = DocumentFile.fromTreeUri(context, effectivePath.toUri())
                            when {
                                document == null -> FolderAccessResult.Unavailable
                                !document.exists() -> FolderAccessResult.Unavailable
                                !document.canRead() -> FolderAccessResult.PermissionDenied
                                else -> FolderAccessResult.Accessible
                            }
                        } catch (_: SecurityException) {
                            FolderAccessResult.PermissionDenied
                        }
                    }

                    else -> {
                        FolderAccessResult.Unavailable
                    }
                }
            accessCache[cacheKey] = resolved to System.currentTimeMillis()
            return resolved
        }

        fun isAccessible(
            folderPathOrUri: String,
            filesystemAccessEnabled: Boolean = false,
        ): Boolean = resolveFolderAccess(folderPathOrUri, filesystemAccessEnabled) == FolderAccessResult.Accessible

        fun invalidateAccessCache() {
            accessCache.clear()
        }
    }

data class FileEntry(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModifiedMs: Long = 0L,
    val sizeKnown: Boolean = true,
    val lastModifiedKnown: Boolean = true,
    val relativeParentSegments: List<String> = emptyList(),
)

fun FileEntry.canonicalIdentity(): String {
    if (uri.scheme == "content") {
        return try {
            val docId = DocumentsContract.getDocumentId(uri)
            "${uri.authority}:$docId"
        } catch (_: Exception) {
            uri.toString()
        }
    }
    val rawPath = uri.path ?: uri.toString()
    return canonicalFilesystemIdentity(rawPath)
}

internal fun canonicalFilesystemIdentity(rawPath: String): String =
    try {
        File(rawPath).canonicalPath
    } catch (_: Exception) {
        normalizeFilesystemFolderPath(rawPath) ?: rawPath.trimEnd('/')
    }

fun normalizeSourcePath(
    path: String,
    filesystemAccessEnabled: Boolean,
): String {
    val effectivePath = folderPathForFilesystemAccess(path, filesystemAccessEnabled)
    if (effectivePath.startsWith("content://")) {
        return canonicalSafTreeIdentity(effectivePath)
    }
    if (effectivePath.startsWith("file:")) {
        val rawPath = effectivePath.toUri().path ?: return effectivePath
        return try {
            File(rawPath).canonicalPath
        } catch (_: Exception) {
            rawPath.trimEnd('/')
        }
    }
    if (effectivePath.startsWith("/")) {
        return try {
            File(effectivePath).canonicalPath
        } catch (_: Exception) {
            effectivePath.trimEnd('/')
        }
    }
    return effectivePath.trimEnd('/')
}

internal fun canonicalSafTreeIdentity(uriString: String): String =
    runCatching {
        val parsedUri = URI(uriString)
        val rawSegments = parsedUri.rawPath.split('/').filter { it.isNotBlank() }
        val treeSegmentIndex = rawSegments.indexOf("tree")
        val documentSegmentIndex = rawSegments.indexOf("document")
        val documentId =
            when {
                treeSegmentIndex >= 0 && treeSegmentIndex + 1 < rawSegments.size -> {
                    rawSegments[treeSegmentIndex + 1]
                }

                documentSegmentIndex >= 0 && documentSegmentIndex + 1 < rawSegments.size -> {
                    rawSegments[documentSegmentIndex + 1]
                }

                else -> {
                    return@runCatching uriString.trimEnd('/')
                }
            }
        val decodedDocumentId = URLDecoder.decode(documentId.replace("+", "%2B"), "UTF-8")
        "content://${parsedUri.authority.lowercase()}/$decodedDocumentId"
    }.getOrElse {
        uriString.trimEnd('/')
    }
