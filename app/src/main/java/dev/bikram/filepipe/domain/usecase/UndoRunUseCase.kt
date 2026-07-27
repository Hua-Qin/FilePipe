package dev.bikram.filepipe.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.data.repository.FileEntry
import dev.bikram.filepipe.data.repository.FileOperationRepository
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.data.storage.isFilesystemAccessEffective
import dev.bikram.filepipe.data.storage.isFilesystemFolderPathString
import dev.bikram.filepipe.data.storage.normalizeFilesystemFolderPath
import dev.bikram.filepipe.devtools.DevMockFileMove
import dev.bikram.filepipe.di.IoDispatcher
import dev.bikram.filepipe.diagnostics.DiagnosticLog
import dev.bikram.filepipe.domain.model.ConflictPolicy
import dev.bikram.filepipe.domain.model.FileMoved
import dev.bikram.filepipe.domain.model.FileUndoStatus
import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.model.isEffectivelyUndone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class UndoResult(
    val totalReversed: Int,
    val totalFailed: Int,
    val errors: List<String>,
    val operationMode: OperationMode = OperationMode.MOVE,
)

data class UndoProgress(
    val processedFiles: Int,
    val totalFiles: Int,
    val processedBytes: Long,
    val totalBytes: Long,
)

private const val TAG = "UndoRunUseCase"

@Singleton
class UndoRunUseCase
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val runHistoryRepository: RunHistoryRepository,
        private val fileOperationRepository: FileOperationRepository,
        private val ruleRepository: RuleRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private val _activeUndoProgress = MutableStateFlow<Map<Long, Float>>(emptyMap())
        val activeUndoProgress: StateFlow<Map<Long, Float>> = _activeUndoProgress.asStateFlow()

        fun isUndoInProgress(historyId: Long): Boolean = _activeUndoProgress.value.containsKey(historyId)

        fun getUndoProgress(historyId: Long): Float? = _activeUndoProgress.value[historyId]

        suspend operator fun invoke(
            historyId: Long,
            onProgress: (UndoProgress) -> Unit = {},
        ): UndoResult =
            withContext(ioDispatcher) {
                synchronized(this@UndoRunUseCase) {
                    if (_activeUndoProgress.value.containsKey(historyId)) {
                        return@withContext UndoResult(0, 0, listOf("Undo operation is already in progress"))
                    }
                    _activeUndoProgress.update { it + (historyId to 0f) }
                }
                try {
                    performUndo(historyId, onProgress)
                } finally {
                    try {
                        withContext(NonCancellable) {
                            syncRunUndoStatus(historyId)
                        }
                    } finally {
                        _activeUndoProgress.update { it - historyId }
                    }
                }
            }

        private suspend fun performUndo(
            historyId: Long,
            onProgress: (UndoProgress) -> Unit,
        ): UndoResult {
            val history =
                runHistoryRepository.getHistoryById(historyId)
                    ?: return UndoResult(0, 0, listOf("Run not found"))

            if (history.isEffectivelyUndone()) {
                return UndoResult(
                    0,
                    0,
                    listOf("This run has already been undone"),
                    operationMode = history.operationMode,
                )
            }

            val operationMode = history.operationMode
            if (operationMode == OperationMode.DELETE) {
                return UndoResult(
                    0,
                    0,
                    listOf("Delete operations cannot be undone"),
                    operationMode = OperationMode.DELETE,
                )
            }

            val movedFiles =
                runHistoryRepository
                    .getFilesForRunOnce(historyId)
                    .filter { it.success && !it.skipped && it.destinationUri.isNotBlank() }
            val pendingFiles = movedFiles.filter { it.undoStatus != FileUndoStatus.UNDONE }
            val totalBytes = pendingFiles.sumOf { fileMoved -> fileMoved.fileSizeBytes.coerceAtLeast(0L) }
            var processedFiles = 0
            var processedBytes = 0L
            onProgress(
                UndoProgress(
                    processedFiles = processedFiles,
                    totalFiles = pendingFiles.size,
                    processedBytes = processedBytes,
                    totalBytes = totalBytes,
                ),
            )

            if (isMockMoveRun(operationMode, movedFiles)) {
                return undoMockRun(
                    historyId = historyId,
                    movedFiles = pendingFiles,
                    totalBytes = totalBytes,
                    onProgress = onProgress,
                )
            }

            val filesystemAccessEnabled =
                isFilesystemAccessEffective(userPreferencesRepository.preferencesFlow.first().folderAccessMode)

            var reversed = 0
            var failed = 0
            val errors = mutableListOf<String>()
            val copyDeletedDestinationUris = mutableListOf<String>()

            pendingFiles.forEach { fileMoved ->
                val wasInterrupted = fileMoved.undoStatus == FileUndoStatus.IN_PROGRESS
                var physicalUndoCompleted = false
                var outcomeCounted = false
                runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.IN_PROGRESS)
                try {
                    when (operationMode) {
                        OperationMode.COPY -> {
                            if (fileMoved.destinationUri.startsWith("file:")) {
                                val path = fileMoved.destinationUri.toUri().path
                                if (path.isNullOrBlank()) {
                                    errors.add("${fileMoved.fileName}: invalid destination path")
                                    failed++
                                    outcomeCounted = true
                                    runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.FAILED)
                                    return@forEach
                                }
                                val destFile = File(path)
                                if (!destFile.isFile) {
                                    reversed++
                                    physicalUndoCompleted = true
                                    outcomeCounted = true
                                    runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.UNDONE)
                                    return@forEach
                                }
                                val deleted =
                                    try {
                                        destFile.delete()
                                    } catch (_: SecurityException) {
                                        false
                                    }
                                if (deleted) {
                                    reversed++
                                    physicalUndoCompleted = true
                                    outcomeCounted = true
                                    runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.UNDONE)
                                    copyDeletedDestinationUris.add(fileMoved.destinationUri)
                                } else {
                                    failed++
                                    outcomeCounted = true
                                    runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.FAILED)
                                    errors.add("${fileMoved.fileName}: could not delete at destination")
                                }
                            } else {
                                val destUri = fileMoved.destinationUri.toUri()
                                val destDoc = DocumentFile.fromSingleUri(context, destUri)
                                if (destDoc == null) {
                                    errors.add("${fileMoved.fileName}: could not open destination document")
                                    failed++
                                    outcomeCounted = true
                                    runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.FAILED)
                                    return@forEach
                                }
                                if (!destDoc.exists()) {
                                    reversed++
                                    physicalUndoCompleted = true
                                    outcomeCounted = true
                                    runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.UNDONE)
                                    return@forEach
                                }
                                val deleted =
                                    try {
                                        destDoc.delete()
                                    } catch (_: SecurityException) {
                                        false
                                    }
                                if (deleted) {
                                    reversed++
                                    physicalUndoCompleted = true
                                    outcomeCounted = true
                                    runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.UNDONE)
                                    copyDeletedDestinationUris.add(fileMoved.destinationUri)
                                } else {
                                    failed++
                                    outcomeCounted = true
                                    runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.FAILED)
                                    errors.add("${fileMoved.fileName}: could not delete at destination")
                                }
                            }
                        }

                        OperationMode.MOVE -> {
                            val destUri = fileMoved.destinationUri.toUri()
                            val sourceFolderUriString = parentSourceFolderForUndo(fileMoved.sourceUri)
                            if (sourceFolderUriString == null) {
                                errors.add("${fileMoved.fileName}: cannot determine original source folder")
                                failed++
                                outcomeCounted = true
                                runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.FAILED)
                                return@forEach
                            }
                            val sizeBytes =
                                when {
                                    fileMoved.destinationUri.startsWith("file:") -> {
                                        val path = destUri.path
                                        if (path.isNullOrBlank()) {
                                            errors.add("${fileMoved.fileName}: invalid destination path")
                                            failed++
                                            outcomeCounted = true
                                            runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.FAILED)
                                            return@forEach
                                        }
                                        val destFile = File(path)
                                        if (!destFile.isFile) {
                                            if (wasInterrupted && originalSourceMatches(fileMoved)) {
                                                reversed++
                                                physicalUndoCompleted = true
                                                outcomeCounted = true
                                                runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.UNDONE)
                                            } else {
                                                failed++
                                                outcomeCounted = true
                                                errors.add("${fileMoved.fileName}: file no longer exists at destination")
                                                runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.FAILED)
                                            }
                                            return@forEach
                                        }
                                        destFile.length()
                                    }

                                    else -> {
                                        val destDoc = DocumentFile.fromSingleUri(context, destUri)
                                        if (destDoc == null || !destDoc.exists()) {
                                            if (wasInterrupted && originalSourceMatches(fileMoved)) {
                                                reversed++
                                                physicalUndoCompleted = true
                                                outcomeCounted = true
                                                runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.UNDONE)
                                            } else {
                                                failed++
                                                outcomeCounted = true
                                                errors.add("${fileMoved.fileName}: file no longer exists at destination")
                                                runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.FAILED)
                                            }
                                            return@forEach
                                        }
                                        destDoc.length()
                                    }
                                }

                            val sourceEntry =
                                FileEntry(
                                    uri = destUri,
                                    name = fileMoved.fileName,
                                    size = sizeBytes,
                                    relativeParentSegments = fileMoved.relativeParentSegments,
                                )

                            val reverseResult =
                                fileOperationRepository.moveFile(
                                    sourceEntry = sourceEntry,
                                    destFolderUriString = sourceFolderUriString,
                                    conflictPolicy = ConflictPolicy.RENAME_SUFFIX,
                                    operationMode = OperationMode.MOVE,
                                    filesystemAccessEnabled = filesystemAccessEnabled,
                                )

                            if (reverseResult.success) {
                                reversed++
                                physicalUndoCompleted = true
                                outcomeCounted = true
                                runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.UNDONE)
                            } else {
                                failed++
                                outcomeCounted = true
                                runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.FAILED)
                                reverseResult.errorMessage?.let { errors.add("${fileMoved.fileName}: $it") }
                            }
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    if (!outcomeCounted) {
                        failed++
                    }
                    errors.add(
                        "${fileMoved.fileName}: ${error.message ?: context.getString(R.string.undo_unknown_error)}",
                    )
                    if (!physicalUndoCompleted) {
                        runCatching {
                            runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.FAILED)
                        }.onFailure { persistenceError ->
                            Log.e(TAG, "Failed to persist undo failure for file ${fileMoved.id}", persistenceError)
                        }
                    }
                } finally {
                    processedFiles++
                    processedBytes += fileMoved.fileSizeBytes.coerceAtLeast(0L)
                    val undoProg =
                        UndoProgress(
                            processedFiles = processedFiles,
                            totalFiles = pendingFiles.size,
                            processedBytes = processedBytes,
                            totalBytes = totalBytes,
                        )
                    val fraction =
                        when {
                            totalBytes > 0L -> processedBytes.toFloat() / totalBytes.toFloat()
                            pendingFiles.isNotEmpty() -> processedFiles.toFloat() / pendingFiles.size.toFloat()
                            else -> 0f
                        }.coerceIn(0f, 1f)
                    _activeUndoProgress.update { it + (historyId to fraction) }
                    onProgress(undoProg)
                }
            }

            if (operationMode == OperationMode.COPY && history.copyCreatedDestFolderUris.isNotEmpty()) {
                deleteEmptyRecordedCopyFolders(history.copyCreatedDestFolderUris)
            }

            if (operationMode == OperationMode.COPY && copyDeletedDestinationUris.isNotEmpty()) {
                val destTreeUriString =
                    history.ruleId?.let { ruleId ->
                        ruleRepository.getRuleById(ruleId)?.destinationFolderPath?.takeIf { it.isNotBlank() }
                    }
                if (destTreeUriString != null) {
                    deleteEmptyDestSubfoldersAfterCopyUndo(destTreeUriString, copyDeletedDestinationUris)
                }
            }

            if (failed > 0) {
                DiagnosticLog.record(
                    context,
                    "Undo completed with failures: historyId=$historyId, reversed=$reversed, failed=$failed",
                )
            }
            return UndoResult(reversed, failed, errors, operationMode = operationMode)
        }

        private suspend fun syncRunUndoStatus(historyId: Long) {
            val persistedUndoFiles =
                runHistoryRepository
                    .getFilesForRunOnce(historyId)
                    .filter { it.success && !it.skipped && it.destinationUri.isNotBlank() }
            val undoneFileCount = persistedUndoFiles.count { it.undoStatus == FileUndoStatus.UNDONE }
            if (persistedUndoFiles.isNotEmpty() && undoneFileCount == persistedUndoFiles.size) {
                runHistoryRepository.markRunReversed(historyId)
            } else if (undoneFileCount > 0) {
                runHistoryRepository.markRunPartiallyUndone(historyId)
            }
        }

        private fun isMockMoveRun(
            operationMode: OperationMode,
            movedFiles: List<FileMoved>,
        ): Boolean {
            if (operationMode != OperationMode.MOVE || movedFiles.isEmpty()) return false
            return movedFiles.all { fileMoved ->
                DevMockFileMove.isMockMovedFile(
                    sourceUri = fileMoved.sourceUri,
                    destinationUri = fileMoved.destinationUri,
                )
            }
        }

        private suspend fun undoMockRun(
            historyId: Long,
            movedFiles: List<FileMoved>,
            totalBytes: Long,
            onProgress: (UndoProgress) -> Unit,
        ): UndoResult {
            var processedBytes = 0L
            movedFiles.forEachIndexed { index, fileMoved ->
                runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.IN_PROGRESS)
                delay(DevMockFileMove.FILE_OPERATION_DELAY_MILLIS)
                processedBytes += fileMoved.fileSizeBytes.coerceAtLeast(0L)
                runHistoryRepository.markFileUndoStatus(fileMoved.id, FileUndoStatus.UNDONE)
                val fraction =
                    when {
                        totalBytes > 0L -> processedBytes.toFloat() / totalBytes.toFloat()
                        movedFiles.isNotEmpty() -> (index + 1).toFloat() / movedFiles.size.toFloat()
                        else -> 0f
                    }.coerceIn(0f, 1f)
                _activeUndoProgress.update { it + (historyId to fraction) }
                onProgress(
                    UndoProgress(
                        processedFiles = index + 1,
                        totalFiles = movedFiles.size,
                        processedBytes = processedBytes,
                        totalBytes = totalBytes,
                    ),
                )
            }
            runHistoryRepository.markRunReversed(historyId)
            return UndoResult(
                totalReversed = movedFiles.size,
                totalFailed = 0,
                errors = emptyList(),
                operationMode = OperationMode.MOVE,
            )
        }

        /**
         * After copied files are removed, deletes empty subfolders under the rule destination tree that
         * were only holding those files. Skips non-empty dirs (e.g. pre-existing content).
         */
        private fun deleteEmptyDestSubfoldersAfterCopyUndo(
            destTreeUriString: String,
            deletedFileDestinationUriStrings: List<String>,
        ) {
            if (isFilesystemFolderPathString(destTreeUriString)) {
                deleteEmptyFilesystemFoldersAfterCopyUndo(destTreeUriString, deletedFileDestinationUriStrings)
                return
            }
            val treeUri = destTreeUriString.toUri()
            val authority = treeUri.authority ?: return
            val treeDocumentId =
                try {
                    DocumentsContract.getTreeDocumentId(treeUri)
                } catch (_: IllegalArgumentException) {
                    return
                }
            val folderDocumentIds = mutableSetOf<String>()
            for (fileUriString in deletedFileDestinationUriStrings) {
                folderDocumentIds.addAll(
                    parentFolderDocumentIdsUnderTree(treeDocumentId, fileUriString),
                )
            }
            val deepestFirst =
                folderDocumentIds.sortedByDescending { documentId ->
                    documentId.count { segment -> segment == '/' }
                }
            for (folderDocumentId in deepestFirst) {
                try {
                    val folderUri = DocumentsContract.buildDocumentUri(authority, folderDocumentId)
                    val folderDoc =
                        try {
                            DocumentFile.fromSingleUri(context, folderUri)
                        } catch (_: Exception) {
                            null
                        } ?: continue
                    val isDirectory =
                        try {
                            folderDoc.isDirectory
                        } catch (_: Exception) {
                            continue
                        }
                    if (!isDirectory) continue
                    val children =
                        try {
                            folderDoc.listFiles()
                        } catch (_: Exception) {
                            null
                        }
                    if (children?.isEmpty() != true) continue
                    deleteDocumentUriWithFallback(folderUri)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete empty dest subfolder $folderDocumentId", e)
                }
            }
        }

        /**
         * Document IDs for folders strictly between the tree root and the file (i.e. parents of the
         * file, excluding the destination root). Empty if the file lived directly under the tree root.
         */
        private fun parentFolderDocumentIdsUnderTree(
            treeDocumentId: String,
            fileDocumentUriString: String,
        ): List<String> {
            val fileDocumentId =
                try {
                    DocumentsContract.getDocumentId(fileDocumentUriString.toUri())
                } catch (_: IllegalArgumentException) {
                    return emptyList()
                }
            val treePrefix = "$treeDocumentId/"
            if (!fileDocumentId.startsWith(treePrefix)) return emptyList()
            val relative = fileDocumentId.removePrefix(treePrefix)
            val segments = relative.split('/').filter { segment -> segment.isNotEmpty() }
            if (segments.size < 2) return emptyList()
            val volume = treeDocumentId.substringBefore(':')
            val treePathAfterColon = treeDocumentId.substringAfter(':', "")
            val result = mutableListOf<String>()
            for (depth in 1 until segments.size) {
                val underTree = segments.take(depth).joinToString("/")
                val fullPath =
                    if (treePathAfterColon.isEmpty()) {
                        underTree
                    } else {
                        "$treePathAfterColon/$underTree"
                    }
                result.add("$volume:$fullPath")
            }
            return result
        }

        /**
         * Removes destination folders that were created during the copy run, deepest first,
         * only when still empty (so pre-existing folders or folders with leftover content stay).
         */
        private fun deleteEmptyRecordedCopyFolders(folderUriStrings: List<String>) {
            val distinctSorted = folderUriStrings.distinct().sortedByDescending { documentPathDepth(it) }
            for (uriString in distinctSorted) {
                try {
                    if (uriString.startsWith("file:")) {
                        val path = uriString.toUri().path ?: continue
                        val dir = File(path)
                        if (!dir.isDirectory) continue
                        val listed =
                            try {
                                dir.list()
                            } catch (_: Exception) {
                                null
                            }
                        if (listed?.isEmpty() != true) continue
                        try {
                            dir.delete()
                        } catch (_: Exception) {
                        }
                        continue
                    }
                    val folderUri = uriString.toUri()
                    val folderDoc =
                        try {
                            DocumentFile.fromSingleUri(context, folderUri)
                        } catch (_: Exception) {
                            null
                        } ?: continue
                    val exists =
                        try {
                            folderDoc.exists()
                        } catch (_: Exception) {
                            continue
                        }
                    if (!exists) continue
                    val isDirectory =
                        try {
                            folderDoc.isDirectory
                        } catch (_: Exception) {
                            continue
                        }
                    if (!isDirectory) continue
                    val children =
                        try {
                            folderDoc.listFiles()
                        } catch (_: Exception) {
                            null
                        }
                    if (children?.isEmpty() != true) continue
                    deleteDocumentUriWithFallback(folderUri)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete empty recorded copy folder $uriString", e)
                }
            }
        }

        private fun deleteEmptyFilesystemFoldersAfterCopyUndo(
            destRootRaw: String,
            deletedFileUriStrings: List<String>,
        ) {
            val destRoot = normalizeFilesystemFolderPath(destRootRaw) ?: return
            val folderPaths = mutableSetOf<String>()
            for (uriStr in deletedFileUriStrings) {
                if (!uriStr.startsWith("file:")) continue
                val filePath = uriStr.toUri().path ?: continue
                val file = File(filePath)
                var parent = file.parentFile ?: continue
                while (true) {
                    val canon =
                        try {
                            parent.canonicalPath
                        } catch (_: Exception) {
                            break
                        }
                    if (canon == destRoot) break
                    if (!canon.startsWith(destRoot + File.separator)) break
                    folderPaths.add(canon)
                    parent = parent.parentFile ?: break
                }
            }
            val deepestFirst =
                folderPaths.sortedByDescending { folderPath ->
                    folderPath.count { segment -> segment == '/' }
                }
            for (folderPath in deepestFirst) {
                val dir = File(folderPath)
                try {
                    if (!dir.isDirectory) continue
                    val listed =
                        try {
                            dir.list()
                        } catch (_: Exception) {
                            null
                        }
                    if (listed?.isEmpty() != true) continue
                    dir.delete()
                } catch (_: Exception) {
                }
            }
        }

        private fun deleteDocumentUriWithFallback(documentUri: Uri) {
            try {
                if (DocumentsContract.deleteDocument(context.contentResolver, documentUri)) {
                    return
                }
            } catch (_: Exception) {
            }
            val doc =
                try {
                    DocumentFile.fromSingleUri(context, documentUri)
                } catch (_: Exception) {
                    null
                }
            try {
                doc?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "DocumentFile fallback delete failed for $documentUri", e)
            }
        }

        private fun documentPathDepth(uriString: String): Int {
            if (uriString.startsWith("file:")) {
                val path = uriString.toUri().path ?: return 0
                return path.trimEnd('/').count { it == '/' }
            }
            if (!uriString.startsWith("content://")) return 0
            return try {
                val docId = DocumentsContract.getDocumentId(uriString.toUri())
                val path = docId.substringAfter(':', "")
                path.count { it == '/' }
            } catch (_: Exception) {
                0
            }
        }

        private fun parentSourceFolderForUndo(sourceUriString: String): String? {
            if (sourceUriString.startsWith("content://")) return parentTreeUriString(sourceUriString)
            if (sourceUriString.startsWith("file:")) {
                val path = sourceUriString.toUri().path ?: return null
                val parent = File(path).parentFile ?: return null
                return normalizeFilesystemFolderPath(parent.absolutePath)
            }
            return null
        }

        private fun originalSourceMatches(fileMoved: FileMoved): Boolean =
            try {
                when {
                    fileMoved.sourceUri.startsWith("file:") -> {
                        val path = fileMoved.sourceUri.toUri().path
                        val sourceFile = path?.let(::File)
                        sourceFile != null &&
                            sourceFile.isFile &&
                            (fileMoved.fileSizeBytes <= 0L || sourceFile.length() == fileMoved.fileSizeBytes)
                    }

                    fileMoved.sourceUri.startsWith("content://") -> {
                        val sourceDocument = DocumentFile.fromSingleUri(context, fileMoved.sourceUri.toUri())
                        sourceDocument != null &&
                            sourceDocument.exists() &&
                            sourceDocument.isFile &&
                            (fileMoved.fileSizeBytes <= 0L || sourceDocument.length() == fileMoved.fileSizeBytes)
                    }

                    else -> {
                        false
                    }
                }
            } catch (_: Exception) {
                false
            }

        /**
         * Derives the parent folder as a SAF tree URI string from a document URI.
         * e.g. content://...document/primary%3ADCIM%2FCamera%2Fphoto.jpg
         *   → content://...tree/primary%3ADCIM%2FCamera
         */
        private fun parentTreeUriString(documentUriString: String): String? {
            if (!documentUriString.startsWith("content://")) return null
            return try {
                val parsed = documentUriString.toUri()
                val docAuthority = parsed.authority ?: return null
                val docId = DocumentsContract.getDocumentId(parsed)
                val relativePath = docId.substringAfter(":", "")
                val parentDocId =
                    if ('/' in relativePath) {
                        docId.substringBeforeLast('/')
                    } else {
                        // File is directly at the volume root — parent is the root itself
                        docId.substringBefore(':') + ":"
                    }
                DocumentsContract.buildTreeDocumentUri(docAuthority, parentDocId).toString()
            } catch (_: Exception) {
                null
            }
        }
    }
