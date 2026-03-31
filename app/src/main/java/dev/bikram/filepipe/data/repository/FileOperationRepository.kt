package dev.bikram.filepipe.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import dev.bikram.filepipe.domain.model.FileMoved
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileOperationRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /**
     * Lists all files in the given SAF tree URI whose extensions match the provided list.
     * Uses a single ContentResolver query (efficient for large directories).
     */
    suspend fun listMatchingFiles(
        folderUriString: String,
        extensions: List<String>
    ): List<FileEntry> = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(folderUriString)
        val lowerExtensions = extensions.map { it.lowercase().let { e -> if (e.startsWith(".")) e else ".$e" } }

        val childrenUri = try {
            DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
        } catch (e: Exception) {
            return@withContext emptyList()
        }

        val results = mutableListOf<FileEntry>()
        val cursor = context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            ),
            null, null, null
        ) ?: return@withContext emptyList()

        cursor.use {
            while (it.moveToNext()) {
                val docId = it.getString(0) ?: continue
                val name = it.getString(1) ?: continue
                val mimeType = it.getString(2) ?: continue
                val size = it.getLong(3)

                // Skip directories
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) continue

                val nameLower = name.lowercase()
                val matches = lowerExtensions.any { ext -> nameLower.endsWith(ext) }
                if (!matches) continue

                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                results.add(FileEntry(docUri, name, mimeType, size))
            }
        }
        results
    }

    /**
     * Moves a file to the destination folder.
     * Copy + delete strategy via ContentResolver streams.
     */
    suspend fun moveFile(
        sourceEntry: FileEntry,
        destFolderUriString: String
    ): FileMoved = withContext(Dispatchers.IO) {
        val destFolderUri = Uri.parse(destFolderUriString)
        val destFolder = DocumentFile.fromTreeUri(context, destFolderUri)
        if (destFolder == null || !destFolder.exists()) {
            return@withContext FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                success = false,
                errorMessage = "Destination folder not accessible"
            )
        }

        // createFile auto-renames if a file with the same name already exists
        val destFile = destFolder.createFile(sourceEntry.mimeType, sourceEntry.name)
        if (destFile == null) {
            return@withContext FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = "",
                fileSizeBytes = sourceEntry.size,
                success = false,
                errorMessage = "Failed to create destination file"
            )
        }

        return@withContext try {
            context.contentResolver.openInputStream(sourceEntry.uri)?.use { input ->
                context.contentResolver.openOutputStream(destFile.uri)?.use { output ->
                    input.copyTo(output)
                }
            }
            // Delete source to complete the "move"
            DocumentsContract.deleteDocument(context.contentResolver, sourceEntry.uri)

            FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = destFile.uri.toString(),
                fileSizeBytes = sourceEntry.size,
                success = true
            )
        } catch (e: IOException) {
            // Clean up partial destination file on failure
            try { destFile.delete() } catch (_: Exception) {}
            FileMoved(
                fileName = sourceEntry.name,
                sourceUri = sourceEntry.uri.toString(),
                destinationUri = destFile.uri.toString(),
                fileSizeBytes = sourceEntry.size,
                success = false,
                errorMessage = e.message ?: "IO error"
            )
        }
    }

    fun hasPersistedPermission(uriString: String): Boolean {
        val uri = Uri.parse(uriString)
        return context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
    }
}

data class FileEntry(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long
)
