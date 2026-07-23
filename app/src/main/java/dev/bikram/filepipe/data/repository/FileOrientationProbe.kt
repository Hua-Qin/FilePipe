package dev.bikram.filepipe.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dev.bikram.filepipe.domain.model.FileOrientation
import dev.bikram.filepipe.domain.model.IMAGE_EXTENSIONS
import dev.bikram.filepipe.domain.model.VIDEO_EXTENSIONS
import dev.bikram.filepipe.domain.model.normalizeExtension
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Orientation probing for image/video files, extracted from [FileOperationRepository] to keep that
 * class under detekt's LargeClass limit. Stateless apart from the [Context] a caller passes for SAF
 * document access; disk-file probing needs no context at all.
 */

internal fun getDocumentUriOrientation(
    context: Context,
    name: String,
    uri: Uri,
): FileOrientation? {
    val ext = normalizeExtension(name.substringAfterLast('.', ""))
    return when (ext) {
        in IMAGE_EXTENSIONS -> imageOrientation { context.contentResolver.openInputStream(uri) }
        in VIDEO_EXTENSIONS -> videoOrientation { it.setDataSource(context, uri) }
        else -> null
    }
}

internal fun getDiskFileOrientation(file: File): FileOrientation? {
    val ext = normalizeExtension(file.name.substringAfterLast('.', ""))
    return when (ext) {
        in IMAGE_EXTENSIONS -> imageOrientation { FileInputStream(file) }
        in VIDEO_EXTENSIONS -> videoOrientation { it.setDataSource(file.absolutePath) }
        else -> null
    }
}

/**
 * Resolves image orientation from a single stream where possible: AndroidX [ExifInterface] exposes both the
 * rotation flag and (for formats that store them, e.g. JPEG/HEIF) the pixel dimensions in one pass. A second
 * stream is only opened to decode the bounds when EXIF doesn't carry dimensions (PNG/WebP/…).
 */
private fun imageOrientation(openStream: () -> InputStream?): FileOrientation? =
    try {
        var swapped = false
        var width = 0
        var height = 0
        openStream()?.use { stream ->
            val exif = ExifInterface(stream)
            swapped = exif.isOrientationSwapped()
            width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
        }
        if (width <= 0 || height <= 0) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream()?.use { BitmapFactory.decodeStream(it, null, options) }
            width = options.outWidth
            height = options.outHeight
        }
        orientationOf(width, height, swapped)
    } catch (_: Exception) {
        null
    }

private fun videoOrientation(setDataSource: (MediaMetadataRetriever) -> Unit): FileOrientation? {
    val retriever = MediaMetadataRetriever()
    return try {
        setDataSource(retriever)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        orientationOf(width, height, rotation == 90 || rotation == 270)
    } catch (_: Exception) {
        null
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}

private fun ExifInterface.isOrientationSwapped(): Boolean =
    when (getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_TRANSVERSE,
        -> true

        else -> false
    }

private fun orientationOf(
    width: Int,
    height: Int,
    swapped: Boolean,
): FileOrientation? {
    if (width <= 0 || height <= 0) return null
    val visualWidth = if (swapped) height else width
    val visualHeight = if (swapped) width else height
    return when {
        visualWidth > visualHeight -> FileOrientation.LANDSCAPE
        visualHeight > visualWidth -> FileOrientation.PORTRAIT
        else -> null
    }
}
