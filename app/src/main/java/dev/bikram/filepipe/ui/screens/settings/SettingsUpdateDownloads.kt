package dev.bikram.filepipe.ui.screens.settings

import android.content.Context
import dev.bikram.filepipe.update.FILEPIPE_UPDATE_APK_CACHE_NAME
import dev.bikram.filepipe.update.UpdateInfo
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal suspend fun downloadUpdateApk(
    context: Context,
    updateInfo: UpdateInfo,
    onProgress: suspend (Float) -> Unit,
): File {
    val connection = URL(updateInfo.downloadUrl).openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = true
    connection.connectTimeout = 15_000
    connection.readTimeout = 30_000
    return try {
        connection.connect()
        if (connection.responseCode !in 200..299) {
            error("Download returned HTTP ${connection.responseCode}")
        }
        val contentLength = connection.contentLength
        val updateFile = File(context.cacheDir, FILEPIPE_UPDATE_APK_CACHE_NAME)
        connection.inputStream.use { inputStream ->
            updateFile.outputStream().use { outputStream ->
                val buffer = ByteArray(8192)
                var totalBytesRead = 0L
                var bytesRead: Int
                if (contentLength > 0) {
                    while (inputStream.read(buffer).also { readCount -> bytesRead = readCount } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        val percent = (100f * totalBytesRead / contentLength).coerceIn(0f, 100f)
                        onProgress(percent)
                    }
                } else {
                    onProgress(-2f)
                    while (inputStream.read(buffer).also { readCount -> bytesRead = readCount } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
        updateFile
    } finally {
        connection.disconnect()
    }
}
