package dev.bikram.filepipe.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.storage.isCanonicalPathUnderAllowedSharedStorage
import dev.bikram.filepipe.data.storage.isFilesystemFolderPathString
import dev.bikram.filepipe.data.storage.normalizeFilesystemFolderPath
import dev.bikram.filepipe.domain.model.ConflictPolicy
import dev.bikram.filepipe.domain.model.FileMoved
import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.model.PreviewFileResult
import dev.bikram.filepipe.domain.model.resolveRenameSuffixName
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal fun FileOperationRepository.moveFileFilesystemToFilesystem(
    sourceEntry: FileEntry,
    destFolderPath: String,
    conflictPolicy: ConflictPolicy,
    operationMode: OperationMode,
    destFoldersCreatedCollector: MutableCollection<String>?,
): FileMoved {
    val sourcePath =
        sourceEntry.uri.path ?: return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Invalid source path",
        )
    val sourceFile = File(sourcePath)
    if (!sourceFile.isFile || !sourceFile.canRead()) {
        return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Source file not accessible",
        )
    }
    val destRootCanonical =
        normalizeFilesystemFolderPath(destFolderPath) ?: return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Invalid destination folder",
        )
    if (!isCanonicalPathUnderAllowedSharedStorage(destRootCanonical)) {
        return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Destination outside allowed storage",
        )
    }
    val destRoot = File(destRootCanonical)
    if (!destRoot.isDirectory || !destRoot.canWrite()) {
        return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Destination folder not accessible",
        )
    }
    val destParent =
        ensureDestParentFolderFile(destRoot, sourceEntry.relativeParentSegments, destFoldersCreatedCollector)
            ?: return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = "Could not create destination folder structure",
            )
    var destName = sourceEntry.name
    val existing = File(destParent, destName)
    if (existing.exists()) {
        when (conflictPolicy) {
            ConflictPolicy.SKIP -> {
                return FileMoved(
                    fileName = sourceEntry.name,
                    sourceUri = sourceEntry.uri.toString(),
                    destinationUri = existing.toUri().toString(),
                    fileSizeBytes = sourceEntry.size,
                    relativeParentSegments = sourceEntry.relativeParentSegments,
                    success = true,
                    skipped = true,
                )
            }

            ConflictPolicy.OVERWRITE -> {}

            ConflictPolicy.RENAME_SUFFIX -> {
                destName = resolveDestNameFile(destParent, sourceEntry.name)
            }
        }
    }
    val destFile = File(destParent, destName)
    return try {
        if (operationMode == OperationMode.MOVE) {
            Files.move(
                sourceFile.toPath(),
                destFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        } else {
            Files.copy(
                sourceFile.toPath(),
                destFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
        FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = destFile.toUri().toString(),
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = true,
        )
    } catch (e: Exception) {
        FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = e.message ?: "IO error",
        )
    }
}

internal fun FileOperationRepository.moveFileFilesystemToDocument(
    sourceEntry: FileEntry,
    destFolderUriString: String,
    conflictPolicy: ConflictPolicy,
    operationMode: OperationMode,
    destFoldersCreatedCollector: MutableCollection<String>?,
    destinationFolderCache: DestinationFolderCache?,
): FileMoved {
    val sourcePath =
        sourceEntry.uri.path ?: return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Invalid source path",
        )
    val sourceFile = File(sourcePath)
    if (!sourceFile.isFile || !sourceFile.canRead()) {
        return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Source file not accessible",
        )
    }

    val cachedDestTree = destinationFolderCache?.safRoots?.get(destFolderUriString)
    val destTree =
        try {
            cachedDestTree ?: DocumentFile.fromTreeUri(context, destFolderUriString.toUri())
        } catch (e: SecurityException) {
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = e.message ?: "Permission denied for destination folder",
            )
        } ?: return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Invalid destination folder URI",
        )
    if (cachedDestTree == null) {
        destinationFolderCache?.safRoots?.put(destFolderUriString, destTree)
    }

    if (!destTree.exists() || !destTree.canWrite()) {
        return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Destination folder not accessible",
        )
    }

    val destParent =
        ensureDestParentFolder(
            destTree = destTree,
            destinationRoot = destFolderUriString,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            destFoldersCreatedCollector = destFoldersCreatedCollector,
            destinationFolderCache = destinationFolderCache,
        )
            ?: return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = "Could not create destination folder structure",
            )

    val existing = destParent.findFile(sourceEntry.name)
    if (existing != null) {
        when (conflictPolicy) {
            ConflictPolicy.SKIP -> {
                return FileMoved(
                    fileName = sourceEntry.name,
                    sourceUri = sourceEntry.uri.toString(),
                    destinationUri = existing.uri.toString(),
                    fileSizeBytes = sourceEntry.size,
                    relativeParentSegments = sourceEntry.relativeParentSegments,
                    success = true,
                    skipped = true,
                )
            }

            ConflictPolicy.OVERWRITE -> {
                existing.delete()
            }

            ConflictPolicy.RENAME_SUFFIX -> {}
        }
    }

    val destName =
        if (conflictPolicy == ConflictPolicy.RENAME_SUFFIX) {
            resolveDestName(sourceEntry.name, destParent)
        } else {
            sourceEntry.name
        }

    val mimeType = mimeTypeFromName(sourceEntry.name)
    val destFile =
        destParent.createFile(mimeType, destName)
            ?: return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = "Could not create destination file",
            )

    return try {
        val inputStream = FileInputStream(sourceFile)
        val outputStream = context.contentResolver.openOutputStream(destFile.uri)
        if (outputStream == null) {
            inputStream.close()
            destFile.delete()
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = "Could not write destination file",
            )
        }
        val bytesCopied = copyStreamWithProgress(inputStream, outputStream)
        if (!isCompleteCopy(sourceEntry.size, bytesCopied, sourceEntry.sizeKnown)) {
            val destinationRemoved = runCatching { destFile.delete() }.getOrDefault(false)
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = if (destinationRemoved) "" else destFile.uri.toString(),
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = context.getString(R.string.file_operation_incomplete_copy),
            )
        }

        if (operationMode == OperationMode.MOVE) {
            val sourceDeleted =
                try {
                    sourceFile.delete()
                } catch (_: SecurityException) {
                    false
                }
            if (!sourceDeleted) {
                return FileMoved(
                    fileName = sourceEntry.name,
                    sourceUri = sourceEntry.uri.toString(),
                    destinationUri = destFile.uri.toString(),
                    fileSizeBytes = sourceEntry.size,
                    relativeParentSegments = sourceEntry.relativeParentSegments,
                    success = false,
                    errorMessage = context.getString(R.string.file_operation_source_delete_failed),
                )
            }
        }

        FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = destFile.uri.toString(),
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = true,
        )
    } catch (e: IOException) {
        destFile.delete()
        FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = e.message ?: "IO error",
        )
    }
}

internal fun FileOperationRepository.moveFileDocumentToFilesystem(
    sourceEntry: FileEntry,
    destFolderPath: String,
    conflictPolicy: ConflictPolicy,
    operationMode: OperationMode,
    destFoldersCreatedCollector: MutableCollection<String>?,
): FileMoved {
    val destRootCanonical =
        normalizeFilesystemFolderPath(destFolderPath) ?: return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Invalid destination folder",
        )
    if (!isCanonicalPathUnderAllowedSharedStorage(destRootCanonical)) {
        return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Destination outside allowed storage",
        )
    }
    val destRoot = File(destRootCanonical)
    if (!destRoot.isDirectory || !destRoot.canWrite()) {
        return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Destination folder not accessible",
        )
    }
    val destParent =
        ensureDestParentFolderFile(destRoot, sourceEntry.relativeParentSegments, destFoldersCreatedCollector)
            ?: return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = "Could not create destination folder structure",
            )
    var destName = sourceEntry.name
    val existing = File(destParent, destName)
    if (existing.exists()) {
        when (conflictPolicy) {
            ConflictPolicy.SKIP -> {
                return FileMoved(
                    fileName = sourceEntry.name,
                    sourceUri = sourceEntry.uri.toString(),
                    destinationUri = existing.toUri().toString(),
                    fileSizeBytes = sourceEntry.size,
                    relativeParentSegments = sourceEntry.relativeParentSegments,
                    success = true,
                    skipped = true,
                )
            }

            ConflictPolicy.OVERWRITE -> {}

            ConflictPolicy.RENAME_SUFFIX -> {
                destName = resolveDestNameFile(destParent, sourceEntry.name)
            }
        }
    }
    val destFile = File(destParent, destName)
    return try {
        val inputStream =
            context.contentResolver.openInputStream(sourceEntry.uri)
                ?: return FileMoved(
                    fileName = sourceEntry.name,
                    sourceUri = sourceEntry.uri.toString(),
                    destinationUri = "",
                    fileSizeBytes = sourceEntry.size,
                    relativeParentSegments = sourceEntry.relativeParentSegments,
                    success = false,
                    errorMessage = "Could not read source document",
                )
        val outputStream = FileOutputStream(destFile)
        val bytesCopied = copyStreamWithProgress(inputStream, outputStream)
        if (!isCompleteCopy(sourceEntry.size, bytesCopied, sourceEntry.sizeKnown)) {
            val destinationRemoved = runCatching { destFile.delete() }.getOrDefault(false)
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = if (destinationRemoved) "" else destFile.toUri().toString(),
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = context.getString(R.string.file_operation_incomplete_copy),
            )
        }

        if (operationMode == OperationMode.MOVE) {
            val sourceDeleted =
                runCatching {
                    DocumentFile.fromSingleUri(context, sourceEntry.uri)?.delete() == true
                }.getOrDefault(false)
            if (!sourceDeleted) {
                return FileMoved(
                    fileName = sourceEntry.name,
                    sourceUri = sourceEntry.uri.toString(),
                    destinationUri = destFile.toUri().toString(),
                    fileSizeBytes = sourceEntry.size,
                    relativeParentSegments = sourceEntry.relativeParentSegments,
                    success = false,
                    errorMessage = context.getString(R.string.file_operation_source_delete_failed),
                )
            }
        }

        FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = destFile.toUri().toString(),
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = true,
        )
    } catch (e: IOException) {
        destFile.delete()
        FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = e.message ?: "IO error",
        )
    }
}

internal fun FileOperationRepository.moveFileDocumentToDocument(
    sourceEntry: FileEntry,
    destFolderUriString: String,
    conflictPolicy: ConflictPolicy,
    operationMode: OperationMode,
    destFoldersCreatedCollector: MutableCollection<String>?,
    destinationFolderCache: DestinationFolderCache?,
): FileMoved {
    val cachedDestTree = destinationFolderCache?.safRoots?.get(destFolderUriString)
    val destTree =
        try {
            cachedDestTree ?: DocumentFile.fromTreeUri(context, destFolderUriString.toUri())
        } catch (e: SecurityException) {
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = e.message ?: "Permission denied for destination folder",
            )
        } ?: return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Invalid destination folder URI",
        )
    if (cachedDestTree == null) {
        destinationFolderCache?.safRoots?.put(destFolderUriString, destTree)
    }

    if (!destTree.exists() || !destTree.canWrite()) {
        return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = "Destination folder not accessible",
        )
    }

    val destParent =
        ensureDestParentFolder(
            destTree = destTree,
            destinationRoot = destFolderUriString,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            destFoldersCreatedCollector = destFoldersCreatedCollector,
            destinationFolderCache = destinationFolderCache,
        )
            ?: return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = "Could not create destination folder structure",
            )

    val existing = destParent.findFile(sourceEntry.name)
    if (existing != null) {
        when (conflictPolicy) {
            ConflictPolicy.SKIP -> {
                return FileMoved(
                    fileName = sourceEntry.name,
                    sourceUri = sourceEntry.uri.toString(),
                    destinationUri = existing.uri.toString(),
                    fileSizeBytes = sourceEntry.size,
                    relativeParentSegments = sourceEntry.relativeParentSegments,
                    success = true,
                    skipped = true,
                )
            }

            ConflictPolicy.OVERWRITE -> {
                existing.delete()
            }

            ConflictPolicy.RENAME_SUFFIX -> {}
        }
    }

    val destName =
        if (conflictPolicy == ConflictPolicy.RENAME_SUFFIX) {
            resolveDestName(sourceEntry.name, destParent)
        } else {
            sourceEntry.name
        }

    val mimeType =
        runCatching { context.contentResolver.getType(sourceEntry.uri) }.getOrNull()
            ?: mimeTypeFromName(sourceEntry.name)

    return try {
        val destFile =
            destParent.createFile(mimeType, destName)
                ?: return FileMoved(
                    fileName = sourceEntry.name,
                    sourceUri = sourceEntry.uri.toString(),
                    destinationUri = "",
                    fileSizeBytes = sourceEntry.size,
                    relativeParentSegments = sourceEntry.relativeParentSegments,
                    success = false,
                    errorMessage = "Could not create destination file",
                )

        val inputStream = context.contentResolver.openInputStream(sourceEntry.uri)
        if (inputStream == null) {
            destFile.delete()
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = "Could not read source file",
            )
        }

        val outputStream = context.contentResolver.openOutputStream(destFile.uri)
        if (outputStream == null) {
            inputStream.close()
            destFile.delete()
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = "Could not write destination file",
            )
        }

        val bytesCopied = copyStreamWithProgress(inputStream, outputStream)
        if (!isCompleteCopy(sourceEntry.size, bytesCopied, sourceEntry.sizeKnown)) {
            val destinationRemoved = runCatching { destFile.delete() }.getOrDefault(false)
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = if (destinationRemoved) "" else destFile.uri.toString(),
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = context.getString(R.string.file_operation_incomplete_copy),
            )
        }

        if (operationMode == OperationMode.MOVE) {
            val sourceDeleted =
                runCatching {
                    DocumentFile
                        .fromSingleUri(context, sourceEntry.uri)
                        ?.delete() == true
                }.getOrDefault(false)
            if (!sourceDeleted) {
                return FileMoved(
                    fileName = sourceEntry.name,
                    sourceUri = sourceEntry.uri.toString(),
                    destinationUri = destFile.uri.toString(),
                    fileSizeBytes = sourceEntry.size,
                    relativeParentSegments = sourceEntry.relativeParentSegments,
                    success = false,
                    errorMessage = context.getString(R.string.file_operation_source_delete_failed),
                )
            }
        }

        FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = destFile.uri.toString(),
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = true,
        )
    } catch (e: IOException) {
        FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = false,
            errorMessage = e.message ?: "IO error",
        )
    }
}

internal fun FileOperationRepository.deleteFile(
    sourceEntry: FileEntry,
    filesystemAccessEnabled: Boolean,
    requireUnchangedSource: Boolean,
): FileMoved {
    val sourceIsFile = sourceEntry.uri.scheme == "file"
    if (sourceIsFile) {
        if (!filesystemAccessEnabled) {
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = "All files access is required for this source path",
            )
        }
        val path = sourceEntry.uri.path
        if (path.isNullOrBlank()) {
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = "Invalid source path",
            )
        }
        val sourceFile = File(path)
        if (!sourceFile.isFile) {
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = "Source file not accessible",
            )
        }
        val sourceChanged =
            requireUnchangedSource &&
                (
                    sourceFile.length() != sourceEntry.size ||
                        (
                            sourceEntry.lastModifiedMs > 0L &&
                                sourceFile.lastModified() != sourceEntry.lastModifiedMs
                        )
                )
        if (sourceChanged) {
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = context.getString(R.string.file_operation_source_changed_after_confirmation),
            )
        }
        val deleted =
            try {
                sourceFile.delete()
            } catch (_: SecurityException) {
                false
            }
        return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = deleted,
            errorMessage = if (deleted) null else "Could not delete file",
        )
    } else {
        val doc =
            try {
                DocumentFile.fromSingleUri(context, sourceEntry.uri)
            } catch (_: SecurityException) {
                null
            }
        if (doc == null || !doc.exists()) {
            return FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                success = false,
                errorMessage = "Source document not accessible",
            )
        }
        if (requireUnchangedSource) {
            val meta = queryDocumentMetadata(sourceEntry.uri)
            val docSize = meta?.size
            val docModified = meta?.lastModifiedMs
            val sizeChanged = docSize != null && docSize != sourceEntry.size
            val modChanged =
                sourceEntry.lastModifiedMs > 0L && docModified != null && docModified > 0L && docModified != sourceEntry.lastModifiedMs
            if (sizeChanged || modChanged) {
                return FileMoved(
                    fileName = sourceEntry.name,
                    sourceUri = sourceEntry.uri.toString(),
                    destinationUri = "",
                    fileSizeBytes = sourceEntry.size,
                    relativeParentSegments = sourceEntry.relativeParentSegments,
                    success = false,
                    errorMessage = context.getString(R.string.file_operation_source_changed_after_confirmation),
                )
            }
        }
        val deleted =
            try {
                doc.delete()
            } catch (_: SecurityException) {
                false
            }
        return FileMoved(
            fileName = sourceEntry.name,
            sourceUri = sourceEntry.uri.toString(),
            destinationUri = "",
            fileSizeBytes = sourceEntry.size,
            relativeParentSegments = sourceEntry.relativeParentSegments,
            success = deleted,
            errorMessage = if (deleted) null else "Could not delete document",
        )
    }
}

internal fun copyStreamWithProgress(
    inputStream: InputStream,
    outputStream: OutputStream,
): Long {
    var bytesCopied = 0L
    inputStream.use { input ->
        outputStream.use { output ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } >= 0) {
                output.write(buffer, 0, read)
                bytesCopied += read
            }
        }
    }
    return bytesCopied
}

internal fun ensureDestParentFolderFile(
    destRoot: File,
    relativeParentSegments: List<String>,
    destFoldersCreatedCollector: MutableCollection<String>?,
): File? {
    var current = destRoot
    for (rawSegment in relativeParentSegments) {
        val segment = rawSegment.trim()
        if (segment.isEmpty() || segment == "." || segment == "..") continue
        val next = File(current, segment)
        when {
            next.isDirectory -> {
                current = next
            }

            next.exists() -> {
                return null
            }

            else -> {
                if (!next.mkdirs() && !next.isDirectory) return null
                destFoldersCreatedCollector?.add(next.toUri().toString())
                current = next
            }
        }
    }
    return current
}

internal fun FileOperationRepository.ensureDestParentFolder(
    destTree: DocumentFile,
    destinationRoot: String,
    relativeParentSegments: List<String>,
    destFoldersCreatedCollector: MutableCollection<String>? = null,
    destinationFolderCache: DestinationFolderCache? = null,
): DocumentFile? {
    val normalizedSegments = normalizeDestinationParentSegments(relativeParentSegments)
    val fullPathKey =
        SafDestinationParentKey(
            destinationRoot = destinationRoot,
            relativeParentSegments = normalizedSegments,
        )
    destinationFolderCache?.safParents?.get(fullPathKey)?.let { cachedParent ->
        return cachedParent
    }

    var current = destTree
    val resolvedSegments = mutableListOf<String>()
    for (segment in normalizedSegments) {
        resolvedSegments += segment
        val prefixKey =
            SafDestinationParentKey(
                destinationRoot = destinationRoot,
                relativeParentSegments = resolvedSegments.toList(),
            )
        val cachedPrefix = destinationFolderCache?.safParents?.get(prefixKey)
        if (cachedPrefix != null) {
            current = cachedPrefix
            continue
        }
        val next = current.findFile(segment)
        current =
            when {
                next != null && next.isDirectory -> {
                    next
                }

                next != null -> {
                    return null
                }

                else -> {
                    val created = current.createDirectory(segment) ?: return null
                    destFoldersCreatedCollector?.add(created.uri.toString())
                    created
                }
            }
        destinationFolderCache?.safParents?.put(prefixKey, current)
    }
    destinationFolderCache?.safParents?.put(fullPathKey, current)
    return current
}

internal fun resolveDestNameFile(
    parent: File,
    name: String,
): String = resolveRenameSuffixName(name) { candidate -> File(parent, candidate).exists() }

internal fun resolveDestName(
    name: String,
    destTree: DocumentFile,
): String = resolveRenameSuffixName(name) { candidate -> destTree.findFile(candidate) != null }

internal fun unchangedPreviewResult(
    sourceEntry: FileEntry,
    simulatedDestPath: String,
): PreviewFileResult =
    PreviewFileResult(
        fileName = sourceEntry.name,
        sourcePath = sourceEntry.uri.toString(),
        simulatedDestPath = simulatedDestPath,
        wouldSkip = false,
        wouldOverwrite = false,
        renamedTo = null,
        sizeBytes = sourceEntry.size,
    )

internal fun FileOperationRepository.simulateFilesystemMove(
    sourceEntry: FileEntry,
    destFolderUriString: String,
    conflictPolicy: ConflictPolicy,
    filesystemAccessEnabled: Boolean,
    simulatedRootPath: String,
): PreviewFileResult {
    if (!filesystemAccessEnabled) {
        return unchangedPreviewResult(sourceEntry, simulatedRootPath)
    }
    val canonical =
        normalizeFilesystemFolderPath(destFolderUriString) ?: return unchangedPreviewResult(sourceEntry, simulatedRootPath)
    if (!isCanonicalPathUnderAllowedSharedStorage(canonical)) {
        return unchangedPreviewResult(sourceEntry, simulatedRootPath)
    }
    val destRoot = File(canonical)
    if (!destRoot.isDirectory) {
        return unchangedPreviewResult(sourceEntry, simulatedRootPath)
    }

    return when (val resolution = peekDestParentForPreviewFile(destRoot, sourceEntry.relativeParentSegments)) {
        is DestParentFilePreview.Partial, is DestParentFilePreview.BlockedByFile -> {
            unchangedPreviewResult(sourceEntry, simulatedRootPath)
        }

        is DestParentFilePreview.Resolved -> {
            val existing = File(resolution.parent, sourceEntry.name)
            if (!existing.exists()) {
                unchangedPreviewResult(sourceEntry, simulatedRootPath)
            } else {
                simulateExistingFilesystemMove(
                    sourceEntry = sourceEntry,
                    destFolderUriString = destFolderUriString,
                    conflictPolicy = conflictPolicy,
                    existing = existing,
                    parent = resolution.parent,
                )
            }
        }
    }
}

internal fun simulateExistingFilesystemMove(
    sourceEntry: FileEntry,
    destFolderUriString: String,
    conflictPolicy: ConflictPolicy,
    existing: File,
    parent: File,
): PreviewFileResult =
    when (conflictPolicy) {
        ConflictPolicy.SKIP -> {
            PreviewFileResult(
                fileName = sourceEntry.name,
                sourcePath = sourceEntry.uri.toString(),
                simulatedDestPath = existing.toUri().toString(),
                wouldSkip = true,
                wouldOverwrite = false,
                renamedTo = null,
                sizeBytes = sourceEntry.size,
            )
        }

        ConflictPolicy.OVERWRITE -> {
            PreviewFileResult(
                fileName = sourceEntry.name,
                sourcePath = sourceEntry.uri.toString(),
                simulatedDestPath = existing.toUri().toString(),
                wouldSkip = false,
                wouldOverwrite = true,
                renamedTo = null,
                sizeBytes = sourceEntry.size,
            )
        }

        ConflictPolicy.RENAME_SUFFIX -> {
            val resolvedName = resolveDestNameFile(parent, sourceEntry.name)
            PreviewFileResult(
                fileName = sourceEntry.name,
                sourcePath = sourceEntry.uri.toString(),
                simulatedDestPath =
                    buildSimulatedDestPreviewPath(
                        destFolderUriString,
                        sourceEntry.relativeParentSegments,
                        resolvedName,
                    ),
                wouldSkip = false,
                wouldOverwrite = false,
                renamedTo = if (resolvedName != sourceEntry.name) resolvedName else null,
                sizeBytes = sourceEntry.size,
            )
        }
    }

internal fun FileOperationRepository.simulateSafMove(
    sourceEntry: FileEntry,
    destFolderUriString: String,
    conflictPolicy: ConflictPolicy,
    simulatedRootPath: String,
    destinationFolderCache: DestinationFolderCache?,
): PreviewFileResult {
    val cachedDestTree = destinationFolderCache?.safRoots?.get(destFolderUriString)
    val destTree =
        try {
            cachedDestTree ?: DocumentFile.fromTreeUri(context, destFolderUriString.toUri())
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SecurityException) {
            null
        }
    if (destTree == null || !destTree.exists()) {
        return unchangedPreviewResult(sourceEntry, simulatedRootPath)
    }
    if (cachedDestTree == null) {
        destinationFolderCache?.safRoots?.put(destFolderUriString, destTree)
    }

    return when (
        val resolution =
            peekDestParentForPreview(
                destTree = destTree,
                destinationRoot = destFolderUriString,
                relativeParentSegments = sourceEntry.relativeParentSegments,
                destinationFolderCache = destinationFolderCache,
            )
    ) {
        is DestParentPreview.Partial, is DestParentPreview.BlockedByFile -> {
            unchangedPreviewResult(sourceEntry, simulatedRootPath)
        }

        is DestParentPreview.Resolved -> {
            val existing = resolution.parent.findFile(sourceEntry.name)
            if (existing == null) {
                unchangedPreviewResult(sourceEntry, simulatedRootPath)
            } else {
                simulateExistingSafMove(
                    sourceEntry = sourceEntry,
                    destFolderUriString = destFolderUriString,
                    conflictPolicy = conflictPolicy,
                    existing = existing,
                    parent = resolution.parent,
                )
            }
        }
    }
}

internal fun simulateExistingSafMove(
    sourceEntry: FileEntry,
    destFolderUriString: String,
    conflictPolicy: ConflictPolicy,
    existing: DocumentFile,
    parent: DocumentFile,
): PreviewFileResult =
    when (conflictPolicy) {
        ConflictPolicy.SKIP -> {
            PreviewFileResult(
                fileName = sourceEntry.name,
                sourcePath = sourceEntry.uri.toString(),
                simulatedDestPath = existing.uri.toString(),
                wouldSkip = true,
                wouldOverwrite = false,
                renamedTo = null,
                sizeBytes = sourceEntry.size,
            )
        }

        ConflictPolicy.OVERWRITE -> {
            PreviewFileResult(
                fileName = sourceEntry.name,
                sourcePath = sourceEntry.uri.toString(),
                simulatedDestPath = existing.uri.toString(),
                wouldSkip = false,
                wouldOverwrite = true,
                renamedTo = null,
                sizeBytes = sourceEntry.size,
            )
        }

        ConflictPolicy.RENAME_SUFFIX -> {
            val resolvedName = resolveDestName(sourceEntry.name, parent)
            PreviewFileResult(
                fileName = sourceEntry.name,
                sourcePath = sourceEntry.uri.toString(),
                simulatedDestPath =
                    buildSimulatedDestPreviewPath(
                        destFolderUriString,
                        sourceEntry.relativeParentSegments,
                        resolvedName,
                    ),
                wouldSkip = false,
                wouldOverwrite = false,
                renamedTo = if (resolvedName != sourceEntry.name) resolvedName else null,
                sizeBytes = sourceEntry.size,
            )
        }
    }

internal sealed class DestParentFilePreview {
    data class Resolved(
        val parent: File,
    ) : DestParentFilePreview()

    data object Partial : DestParentFilePreview()

    data object BlockedByFile : DestParentFilePreview()
}

internal fun peekDestParentForPreviewFile(
    destRoot: File,
    relativeParentSegments: List<String>,
): DestParentFilePreview {
    var current = destRoot
    for (rawSegment in relativeParentSegments) {
        val segment = rawSegment.trim()
        if (segment.isEmpty() || segment == "." || segment == "..") continue
        val next = File(current, segment)
        when {
            !next.exists() -> return DestParentFilePreview.Partial
            next.isDirectory -> current = next
            else -> return DestParentFilePreview.BlockedByFile
        }
    }
    return DestParentFilePreview.Resolved(current)
}

internal fun peekDestParentForPreview(
    destTree: DocumentFile,
    destinationRoot: String,
    relativeParentSegments: List<String>,
    destinationFolderCache: DestinationFolderCache? = null,
): DestParentPreview {
    val normalizedSegments = normalizeDestinationParentSegments(relativeParentSegments)
    val fullPathKey =
        SafDestinationParentKey(
            destinationRoot = destinationRoot,
            relativeParentSegments = normalizedSegments,
        )
    destinationFolderCache?.safParents?.get(fullPathKey)?.let { cachedParent ->
        return DestParentPreview.Resolved(cachedParent)
    }

    var current = destTree
    val resolvedSegments = mutableListOf<String>()
    for (segment in normalizedSegments) {
        resolvedSegments += segment
        val prefixKey =
            SafDestinationParentKey(
                destinationRoot = destinationRoot,
                relativeParentSegments = resolvedSegments.toList(),
            )
        val cachedPrefix = destinationFolderCache?.safParents?.get(prefixKey)
        if (cachedPrefix != null) {
            current = cachedPrefix
            continue
        }
        val next = current.findFile(segment)
        when {
            next == null -> {
                return DestParentPreview.Partial
            }

            !next.isDirectory -> {
                return DestParentPreview.BlockedByFile
            }

            else -> {
                current = next
                destinationFolderCache?.safParents?.put(prefixKey, current)
            }
        }
    }
    destinationFolderCache?.safParents?.put(fullPathKey, current)
    return DestParentPreview.Resolved(current)
}

internal fun relativePathSuffixForDisplay(
    relativeParentSegments: List<String>,
    fileName: String,
): String {
    val clean =
        relativeParentSegments
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "." && it != ".." }
    return if (clean.isEmpty()) fileName else clean.joinToString("/", postfix = "/") + fileName
}

internal fun buildSimulatedDestPreviewPath(
    destFolderUriString: String,
    relativeParentSegments: List<String>,
    fileName: String,
): String {
    val pathSuffix = relativePathSuffixForDisplay(relativeParentSegments, fileName)
    return when {
        destFolderUriString.startsWith("content://") -> pathSuffix
        destFolderUriString.endsWith("/") -> destFolderUriString + pathSuffix
        else -> "$destFolderUriString/$pathSuffix"
    }
}

internal sealed class DestParentPreview {
    data class Resolved(
        val parent: DocumentFile,
    ) : DestParentPreview()

    data object Partial : DestParentPreview()

    data object BlockedByFile : DestParentPreview()
}
