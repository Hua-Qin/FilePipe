package dev.bikram.filepipe.update

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

private const val MAX_UPDATE_APK_DISPLAY_NAME_LENGTH = 120
private const val INVALID_UPDATE_APK_FILENAME_CHARACTERS = "<>:\"/\\|?*"
private const val APK_EXTENSION = ".apk"

internal fun sanitizeUpdateApkDisplayName(
    displayName: String,
    fallbackName: String,
): String {
    val cleanedName =
        buildString(displayName.length) {
            displayName.forEach { character ->
                if (character.isISOControl() || character in INVALID_UPDATE_APK_FILENAME_CHARACTERS) {
                    append('_')
                } else {
                    append(character)
                }
            }
        }.trim(' ', '.')
    val nameWithoutExtension =
        if (cleanedName.endsWith(".apk", ignoreCase = true)) {
            cleanedName.dropLast(4)
        } else {
            cleanedName
        }
    val boundedName =
        nameWithoutExtension
            .trim(' ', '.')
            .take(MAX_UPDATE_APK_DISPLAY_NAME_LENGTH - APK_EXTENSION.length)
            .trimEnd(' ', '.')
    return if (boundedName.isBlank()) fallbackName else boundedName + APK_EXTENSION
}

/**
 * Copies [cacheApkFile] into the public Downloads collection with [displayName] as shown in Files.
 * Uses [MediaStore.Downloads].
 */
fun copyUpdateApkToMediaStoreDownloads(
    context: Context,
    cacheApkFile: File,
    displayName: String,
): Result<Unit> =
    runCatching {
        val safeName = sanitizeUpdateApkDisplayName(displayName, FILEPIPE_UPDATE_APK_CACHE_NAME)
        val resolver = context.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val itemUri =
            resolver.insert(collection, values)
                ?: error("MediaStore insert returned null")
        try {
            resolver.openOutputStream(itemUri, "w")?.use { output ->
                FileInputStream(cacheApkFile).use { input ->
                    input.copyTo(output)
                }
            } ?: error("openOutputStream returned null")
            val publish =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
            resolver.update(itemUri, publish, null, null)
        } catch (throwable: Throwable) {
            runCatching { resolver.delete(itemUri, null, null) }
            throw throwable
        }
    }

/** SHA-256 of file contents; reads in chunks. */
fun sha256HexOfFile(file: File): String? {
    if (!file.isFile || !file.canRead()) return null
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(8192)
    FileInputStream(file).use { input ->
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}
