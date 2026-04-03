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
import dev.bikram.filepipe.data.storage.absoluteStoragePathToTreeUri
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
 * Returns a user-friendly display label for a folder.
 * Accepts both SAF content:// URIs (new format) and legacy /storage/... absolute paths.
 * Examples:
 *   content://...tree/primary%3ADCIM%2FCamera  →  "DCIM/Camera"
 *   /storage/emulated/0/Pictures               →  "Pictures"
 *   content://...tree/1A2B-3C4D%3AMovies       →  "SD Card/Movies"
 */
fun displayPath(path: String): String {
    if (path.startsWith("content://")) {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(Uri.parse(path))
            val relative = docId.substringAfter(":", "")
            when {
                relative.isBlank() -> docId  // root of a volume
                docId.startsWith("primary", ignoreCase = true) -> relative
                else -> "SD Card/$relative"
            }
        } catch (_: Exception) { path }
    }
    // Legacy absolute path fallback
    val primary = "/storage/emulated/0/"
    if (path.startsWith(primary)) return path.removePrefix(primary)
    val sdCardPrefix = Regex("^/storage/[A-F0-9]{4}-[A-F0-9]{4}/")
    return path.replace(sdCardPrefix) { "SD Card/" }
}

/**
 * Document URI derived from the tree (recommended for [ActivityResultContracts.OpenDocumentTree] initial location).
 * For legacy absolute paths, converts via [absoluteStoragePathToTreeUri].
 * For content:// URIs (SAF), uses them directly.
 */
fun absoluteStoragePathToOpenTreeInitialUri(path: String): Uri? {
    if (path.startsWith("content://")) {
        // Already a SAF URI — just wrap as a document URI for the picker hint
        return try {
            val treeUri = Uri.parse(path)
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        } catch (_: Exception) { null }
    }
    val treeUri = absoluteStoragePathToTreeUri(path) ?: return null
    val documentId = DocumentsContract.getTreeDocumentId(treeUri)
    return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
}
