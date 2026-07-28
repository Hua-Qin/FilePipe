package dev.bikram.filepipe.data.storage

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri

fun safTreeUriToPath(uri: Uri): String? {
    return try {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val parts = docId.split(":", limit = 2)
        if (parts.size < 2) return null
        val volumeName = parts[0]
        val relativePath = parts[1]
        if (volumeName.equals("primary", ignoreCase = true)) {
            "/storage/emulated/0/$relativePath"
        } else {
            "/storage/$volumeName/$relativePath"
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * When all-files access is active, maps persisted SAF tree URIs to absolute paths for I/O and access checks.
 * Returns [folderPathOrUri] unchanged when filesystem access is off or conversion fails.
 */
fun folderPathForFilesystemAccess(
    folderPathOrUri: String,
    filesystemAccessEnabled: Boolean,
): String {
    if (!filesystemAccessEnabled || !folderPathOrUri.startsWith("content://")) {
        return folderPathOrUri
    }
    return safTreeUriToPath(folderPathOrUri.toUri()) ?: folderPathOrUri
}

/**
 * Normalizes user-facing paths (`/DCIM`, `DCIM/Camera`) to absolute primary emulated storage paths for SAF.
 */
fun normalizeToAbsoluteStoragePath(path: String): String {
    val trimmed = path.trim().trimEnd('/')
    if (trimmed.isEmpty()) return ""
    if (trimmed.startsWith("/storage/")) return trimmed
    val relative = trimmed.removePrefix("/")
    return "/storage/emulated/0/$relative"
}

/**
 * Builds a SAF tree [Uri] for [androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree].
 * Supports primary emulated storage and adoptable SD volumes (e.g. /storage/ABCD-1234/...).
 */
fun absoluteStoragePathToTreeUri(path: String): Uri? {
    val absolute = normalizeToAbsoluteStoragePath(path)
    if (absolute.isEmpty()) return null
    val authority = "com.android.externalstorage.documents"
    val primaryPrefix = "/storage/emulated/0"
    if (absolute.startsWith(primaryPrefix)) {
        val relative = absolute.removePrefix(primaryPrefix).trimStart('/')
        val documentId = if (relative.isEmpty()) "primary:" else "primary:$relative"
        return DocumentsContract.buildTreeDocumentUri(authority, documentId)
    }
    val sdMatch = Regex("^/storage/([A-F0-9]{4}-[A-F0-9]{4})(/.*)?$").find(absolute) ?: return null
    val volumeId = sdMatch.groupValues[1]
    val tail = sdMatch.groupValues[2].trimStart('/').trimEnd('/')
    val documentId = if (tail.isEmpty()) "$volumeId:" else "$volumeId:$tail"
    return DocumentsContract.buildTreeDocumentUri(authority, documentId)
}
