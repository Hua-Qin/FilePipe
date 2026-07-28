package dev.bikram.filepipe.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.data.repository.BackupSnapshot
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.di.IoDispatcher
import dev.bikram.filepipe.domain.backupFileTimestamp
import dev.bikram.filepipe.domain.export.buildAppBackupJson
import dev.bikram.filepipe.domain.model.FileUndoStatus
import dev.bikram.filepipe.domain.model.RunStatus
import dev.bikram.filepipe.domain.model.hasRecoverableDestination
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class ExportRulesUseCase
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val runHistoryRepository: RunHistoryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun exportRulesToTreeUri(folderPath: String): Result<String> =
            withContext(ioDispatcher) {
                if (folderPath.isBlank()) return@withContext Result.failure(IllegalStateException("No export folder"))

                val exportResult = exportRulesToTreeUris(listOf(folderPath))
                exportResult.fold(
                    onSuccess = { fileNames -> Result.success(fileNames.first()) },
                    onFailure = { error -> Result.failure(error) },
                )
            }

        suspend fun exportRulesToTreeUris(folderPaths: List<String>): Result<List<String>> =
            withContext(ioDispatcher) {
                val destinations = folderPaths.map { it.trim() }.filter { it.isNotBlank() }.distinct()
                if (destinations.isEmpty()) return@withContext Result.failure(IllegalStateException("No export folder"))

                val backupSnapshot = normalizedBackupSnapshot()
                val settings = userPreferencesRepository.getPreferencesSnapshot()

                val backupBytes =
                    buildAppBackupJson(backupSnapshot.rules, backupSnapshot.historyWithFiles, settings)
                        .toByteArray(Charsets.UTF_8)
                val stamp = backupFileTimestamp()
                val fileName = "filepipe_backup_$stamp.json"

                val failures = mutableListOf<Throwable>()
                val exportedFileNames = mutableListOf<String>()
                destinations.forEach { destinationPath ->
                    val result =
                        if (destinationPath.startsWith("content://")) {
                            writeToContentDestination(destinationPath, fileName, backupBytes).map { fileName }
                        } else {
                            writeToFilePath(destinationPath, fileName, backupBytes).map { fileName }
                        }
                    result.fold(
                        onSuccess = { exportedFileNames.add(it) },
                        onFailure = { failures.add(it) },
                    )
                }

                if (failures.isEmpty()) {
                    Result.success(exportedFileNames)
                } else {
                    Result.failure(failures.first())
                }
            }

        /**
         * Writes the same backup JSON as [exportRulesToTreeUri] to a URI from [androidx.activity.result.contract.ActivityResultContracts.CreateDocument].
         */
        suspend fun exportBackupJsonToDocumentUri(targetUri: Uri): Result<String> =
            withContext(ioDispatcher) {
                val backupSnapshot = normalizedBackupSnapshot()
                val settings = userPreferencesRepository.getPreferencesSnapshot()
                val backupBytes =
                    buildAppBackupJson(backupSnapshot.rules, backupSnapshot.historyWithFiles, settings)
                        .toByteArray(Charsets.UTF_8)
                runCatching {
                    writeDocumentBytes(targetUri, backupBytes)
                    friendlyFileNameFromDocumentUri(targetUri)
                }.fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(it) })
            }

        private suspend fun normalizedBackupSnapshot(): BackupSnapshot {
            val snapshot = runHistoryRepository.getBackupSnapshot()
            val normalizedHistory =
                snapshot.historyWithFiles.map { (run, files) ->
                    val recoverableFiles = files.filter { fileMoved -> fileMoved.hasRecoverableDestination }
                    val undoneFileCount = recoverableFiles.count { fileMoved -> fileMoved.undoStatus == FileUndoStatus.UNDONE }
                    val effectiveRun =
                        when {
                            recoverableFiles.isNotEmpty() && undoneFileCount == recoverableFiles.size -> {
                                run.copy(status = RunStatus.UNDONE, isReversed = true)
                            }

                            undoneFileCount > 0 -> {
                                run.copy(status = RunStatus.PARTIAL_UNDONE, isReversed = false)
                            }

                            else -> {
                                run
                            }
                        }
                    effectiveRun to files
                }
            return snapshot.copy(historyWithFiles = normalizedHistory)
        }

        /**
         * [Uri.getLastPathSegment] for SAF document URIs is the full document id (e.g. `primary:Download/foo.json`).
         * For snackbars we only want the leaf file name (e.g. `foo.json`).
         */
        private fun friendlyFileNameFromDocumentUri(documentUri: Uri): String {
            val segment = documentUri.lastPathSegment ?: return "filepipe_backup.json"
            val decoded = Uri.decode(segment)
            val lastSlash = decoded.lastIndexOf('/')
            return if (lastSlash >= 0) {
                decoded.substring(lastSlash + 1)
            } else {
                val lastColon = decoded.lastIndexOf(':')
                if (lastColon >= 0) decoded.substring(lastColon + 1) else decoded
            }
        }

        private fun writeToContentDestination(
            destinationUriString: String,
            fileName: String,
            backupBytes: ByteArray,
        ): Result<Unit> {
            val destinationUri = destinationUriString.toUri()
            return runCatching {
                if (DocumentsContract.isTreeUri(destinationUri)) {
                    writeTreeDocument(destinationUri, fileName, "application/json", backupBytes)
                } else {
                    writeDocumentBytes(destinationUri, backupBytes, mode = "wt")
                }
            }.fold(onSuccess = { Result.success(Unit) }, onFailure = { Result.failure(it) })
        }

        private fun writeToFilePath(
            folderPath: String,
            fileName: String,
            backupBytes: ByteArray,
        ): Result<Unit> {
            val folder = File(folderPath)
            if (!folder.exists() || !folder.canWrite()) {
                return Result.failure(IllegalStateException("Export folder not accessible: $folderPath"))
            }
            return runCatching {
                val destinationFile = File(folder, fileName)
                val temporaryFile = File(folder, ".$fileName.${UUID.randomUUID()}.partial")
                try {
                    FileOutputStream(temporaryFile).use { outputStream ->
                        outputStream.write(backupBytes)
                        outputStream.flush()
                        outputStream.fd.sync()
                    }
                    if (!temporaryFile.renameTo(destinationFile)) {
                        throw IOException("Failed to publish completed backup file")
                    }
                } finally {
                    if (temporaryFile.exists()) {
                        temporaryFile.delete()
                    }
                }
            }.fold(onSuccess = { Result.success(Unit) }, onFailure = { Result.failure(it) })
        }

        private fun writeTreeDocument(
            treeUri: Uri,
            fileName: String,
            mimeType: String,
            backupBytes: ByteArray,
        ) {
            val resolver = context.contentResolver
            val documentTreeUri =
                DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri),
                )
            val temporaryName = "$fileName.${UUID.randomUUID()}.partial"
            val temporaryUri =
                DocumentsContract.createDocument(
                    resolver,
                    documentTreeUri,
                    mimeType,
                    temporaryName,
                ) ?: throw IOException("Failed to create temporary backup document")
            var fallbackDestinationUri: Uri? = null
            try {
                writeDocumentBytes(temporaryUri, backupBytes)
                val publishedUri =
                    runCatching {
                        DocumentsContract.renameDocument(resolver, temporaryUri, fileName)
                    }.getOrNull()
                if (publishedUri == null) {
                    val createdDestinationUri =
                        DocumentsContract.createDocument(
                            resolver,
                            documentTreeUri,
                            mimeType,
                            fileName,
                        ) ?: throw IOException("Failed to create backup document")
                    fallbackDestinationUri = createdDestinationUri
                    writeDocumentBytes(createdDestinationUri, backupBytes)
                    runCatching { resolver.delete(temporaryUri, null, null) }
                }
            } catch (error: Exception) {
                runCatching { resolver.delete(temporaryUri, null, null) }
                fallbackDestinationUri?.let { destinationUri ->
                    runCatching { resolver.delete(destinationUri, null, null) }
                }
                throw error
            }
        }

        private fun writeDocumentBytes(
            documentUri: Uri,
            backupBytes: ByteArray,
            mode: String? = null,
        ) {
            val outputStream =
                if (mode == null) {
                    context.contentResolver.openOutputStream(documentUri)
                } else {
                    context.contentResolver.openOutputStream(documentUri, mode)
                } ?: throw IOException("Failed to open output stream for backup document")
            outputStream.use { stream ->
                stream.write(backupBytes)
                stream.flush()
            }
        }
    }
