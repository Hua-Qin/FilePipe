package dev.bikram.filepipe.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.provider.DocumentsContract
import dev.bikram.filepipe.ui.feedback.LocalTapSound

@Composable
fun FolderPickerButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val playTap = LocalTapSound.current
    OutlinedButton(
        onClick = {
            playTap()
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.FolderOpen, contentDescription = null)
        Text("  $label")
    }
}

/**
 * Returns a user-friendly display path, stripping the /storage/emulated/0/ prefix.
 * External SD cards are shown as "SD Card/...".
 */
fun displayPath(path: String): String {
    val primary = "/storage/emulated/0/"
    if (path.startsWith(primary)) return path.removePrefix(primary)
    val sdCardPrefix = Regex("^/storage/[A-F0-9]{4}-[A-F0-9]{4}/")
    return path.replace(sdCardPrefix) { "SD Card/" }
}

/**
 * Converts a SAF tree URI (content://...ExternalStorage.../tree/primary%3ADCIM%2FCamera)
 * to an absolute file system path (/storage/emulated/0/DCIM/Camera).
 *
 * Supports "primary" volume and external SD card volumes (e.g. "1A2B-3C4D").
 */
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
 * Normalizes user-facing paths (`/DCIM`, `DCIM/Camera`) to absolute storage paths for SAF.
 */
fun normalizeToAbsoluteStoragePath(path: String): String {
    val trimmed = path.trim().trimEnd('/')
    if (trimmed.isEmpty()) return ""
    if (trimmed.startsWith("/storage/")) return trimmed
    val relative = trimmed.removePrefix("/")
    return "/storage/emulated/0/$relative"
}

/**
 * Builds a SAF tree [Uri] so [ActivityResultContracts.OpenDocumentTree] can open at that folder (API 26+).
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

/**
 * Document URI derived from the tree (recommended for [ActivityResultContracts.OpenDocumentTree] initial location).
 */
fun absoluteStoragePathToOpenTreeInitialUri(path: String): Uri? {
    val treeUri = absoluteStoragePathToTreeUri(path) ?: return null
    val documentId = DocumentsContract.getTreeDocumentId(treeUri)
    return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
}
