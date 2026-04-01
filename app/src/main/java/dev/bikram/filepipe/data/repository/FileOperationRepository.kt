package dev.bikram.filepipe.data.repository

import dev.bikram.filepipe.domain.model.ConflictPolicy
import dev.bikram.filepipe.domain.model.FileMoved
import dev.bikram.filepipe.domain.model.OperationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileOperationRepository @Inject constructor() {

    /**
     * Lists all files in the given folder whose extensions match the provided list.
     * Optionally scans subdirectories up to [maxDepth] levels deep.
     */
    suspend fun listMatchingFiles(
        folderPath: String,
        extensions: List<String>,
        scanSubdirectories: Boolean = false,
        maxDepth: Int = 5
    ): List<FileEntry> = withContext(Dispatchers.IO) {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.canRead()) return@withContext emptyList()

        val lowerExtensions = extensions.map { it.lowercase().let { e -> if (e.startsWith(".")) e else ".$e" } }.toSet()

        if (scanSubdirectories) {
            folder.walkTopDown()
                .maxDepth(maxDepth)
                .filter { it.isFile && ".${it.extension.lowercase()}" in lowerExtensions }
                .map { FileEntry(it) }
                .toList()
        } else {
            folder.listFiles()
                ?.filter { it.isFile && ".${it.extension.lowercase()}" in lowerExtensions }
                ?.map { FileEntry(it) }
                ?: emptyList()
        }
    }

    /**
     * Moves or copies a file to the destination folder, applying the given conflict policy.
     */
    suspend fun moveFile(
        sourceEntry: FileEntry,
        destFolderPath: String,
        conflictPolicy: ConflictPolicy,
        operationMode: OperationMode
    ): FileMoved = withContext(Dispatchers.IO) {
        val destFolder = File(destFolderPath)
        if (!destFolder.exists() || !destFolder.canWrite()) {
            return@withContext FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.file.absolutePath,
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                success = false,
                errorMessage = "Destination folder not accessible: $destFolderPath"
            )
        }

        val existingDest = File(destFolder, sourceEntry.name)
        if (existingDest.exists()) {
            when (conflictPolicy) {
                ConflictPolicy.SKIP -> {
                    return@withContext FileMoved(
                        fileName = sourceEntry.name,
                        sourceUri = sourceEntry.file.absolutePath,
                        destinationUri = existingDest.absolutePath,
                        fileSizeBytes = sourceEntry.size,
                        success = true,
                        skipped = true
                    )
                }
                ConflictPolicy.OVERWRITE -> {
                    existingDest.delete()
                }
                ConflictPolicy.RENAME_SUFFIX -> {
                    // handled by resolveDestFile below
                }
            }
        }

        val destFile = if (conflictPolicy == ConflictPolicy.RENAME_SUFFIX) {
            resolveDestFile(sourceEntry.name, destFolder)
        } else {
            File(destFolder, sourceEntry.name)
        }

        return@withContext try {
            sourceEntry.file.copyTo(destFile, overwrite = false)
            if (operationMode == OperationMode.MOVE) {
                sourceEntry.file.delete()
            }
            FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.file.absolutePath,
                destinationUri = destFile.absolutePath,
                fileSizeBytes = sourceEntry.size,
                success = true
            )
        } catch (e: IOException) {
            try { destFile.delete() } catch (_: Exception) {}
            FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.file.absolutePath,
                destinationUri = destFile.absolutePath,
                fileSizeBytes = sourceEntry.size,
                success = false,
                errorMessage = e.message ?: "IO error"
            )
        }
    }

    fun isAccessible(folderPath: String): Boolean {
        val folder = File(folderPath)
        return folder.exists() && folder.canRead()
    }

    private fun resolveDestFile(name: String, destFolder: File): File {
        var destFile = File(destFolder, name)
        if (!destFile.exists()) return destFile
        val ext = name.substringAfterLast('.', "")
        val base = if (ext.isNotEmpty()) name.dropLast(ext.length + 1) else name
        var n = 1
        while (true) {
            val candidate = if (ext.isNotEmpty()) "$base($n).$ext" else "$base($n)"
            destFile = File(destFolder, candidate)
            if (!destFile.exists()) return destFile
            n++
        }
    }
}

data class FileEntry(
    val file: File,
    val name: String = file.name,
    val size: Long = file.length()
)
