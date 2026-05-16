package dev.bikram.filepipe.diagnostics

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.bikram.filepipe.BuildConfig
import dev.bikram.filepipe.data.preferences.AppPreferences
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import kotlin.system.exitProcess

object DiagnosticLog {
    private const val DIAGNOSTICS_DIR = "diagnostics"
    private const val LOG_FILE_NAME = "filepipe-diagnostics.log"
    private const val SHARE_FILE_NAME = "filepipe-diagnostics.txt"
    private const val MAX_LOG_BYTES = 256 * 1024

    @Volatile
    private var crashHandlerInstalled = false

    fun installCrashHandler(context: Context) {
        if (crashHandlerInstalled) return
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            record(appContext, "Uncaught exception on ${thread.name}", throwable)
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                exitProcess(2)
            }
        }
        crashHandlerInstalled = true
    }

    fun record(
        context: Context,
        message: String,
        throwable: Throwable? = null,
    ) {
        runCatching {
            val logFile = logFile(context)
            logFile.parentFile?.mkdirs()
            trimIfNeeded(logFile)
            logFile.appendText(
                buildString {
                    append(Instant.now())
                    append(" | ")
                    append(message)
                    append('\n')
                    if (throwable != null) {
                        append(stackTraceText(throwable))
                        append('\n')
                    }
                },
            )
        }
    }

    fun createShareFile(
        context: Context,
        preferences: AppPreferences? = null,
    ): File {
        val shareFile = File(File(context.cacheDir, DIAGNOSTICS_DIR), SHARE_FILE_NAME)
        shareFile.parentFile?.mkdirs()
        val logText = runCatching { logFile(context).readText() }.getOrDefault("")
        shareFile.writeText(
            buildString {
                appendLine("FilePipe diagnostics")
                appendLine("Generated: ${Instant.now()}")
                appendLine("Package: ${context.packageName}")
                appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("Flavor: ${BuildConfig.FLAVOR}")
                appendLine("Build type: ${BuildConfig.BUILD_TYPE}")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                appendLine()
                appendSystemSnapshot(context)
                appendLine()
                appendPreferencesSnapshot(preferences)
                appendLine()
                appendLine("App log")
                appendLine("=======")
                append(logText.ifBlank { "No app log entries captured yet.\n" })
            },
        )
        return shareFile
    }

    private fun logFile(context: Context): File = File(File(context.filesDir, DIAGNOSTICS_DIR), LOG_FILE_NAME)

    private fun StringBuilder.appendSystemSnapshot(context: Context) {
        val packageManager = context.packageManager
        val packageInfo =
            runCatching {
                packageManager.getPackageInfo(context.packageName, 0)
            }.getOrNull()
        val appInfo = packageInfo?.applicationInfo
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        appendLine("Environment")
        appendLine("===========")
        appendLine("Locale: ${Locale.getDefault()}")
        appendLine("Timezone: ${TimeZone.getDefault().id}")
        appendLine("Uptime: ${SystemClock.uptimeMillis()} ms")
        appendLine("Elapsed realtime: ${SystemClock.elapsedRealtime()} ms")
        appendLine("Target SDK: ${appInfo?.targetSdkVersion ?: "unknown"}")
        appendLine("First install: ${packageInfo?.firstInstallTime?.let(Instant::ofEpochMilli) ?: "unknown"}")
        appendLine("Last update: ${packageInfo?.lastUpdateTime?.let(Instant::ofEpochMilli) ?: "unknown"}")
        appendLine("Installer: ${installerPackageName(context)}")
        appendLine("Files dir usable space: ${context.filesDir.usableSpace} bytes")
        appendLine("Cache dir usable space: ${context.cacheDir.usableSpace} bytes")
        appendLine("External storage state: ${Environment.getExternalStorageState()}")
        appendLine()
        appendLine("Permissions and app access")
        appendLine("==========================")
        appendLine("Notifications enabled: ${notificationManagerCompat.areNotificationsEnabled()}")
        appendLine("POST_NOTIFICATIONS granted: ${postNotificationsGranted(context)}")
        appendLine("All files access granted: ${allFilesAccessGranted()}")
        appendLine("Ignoring battery optimizations: ${powerManager.isIgnoringBatteryOptimizations(context.packageName)}")
        appendPersistedUriPermissions(context)
        appendLine()
        appendNotificationChannels(context)
    }

    private fun StringBuilder.appendPreferencesSnapshot(preferences: AppPreferences?) {
        appendLine("FilePipe settings")
        appendLine("=================")
        if (preferences == null) {
            appendLine("Settings snapshot unavailable.")
            return
        }
        appendLine("Theme mode: ${preferences.themeMode}")
        appendLine("Color source: ${preferences.colorSource}")
        appendLine("Palette style: ${preferences.themePaletteStyle}")
        appendLine("Saved custom seeds: ${preferences.savedCustomSeedHexes.size}")
        appendLine("Active custom seed set: ${preferences.activeCustomSeedHex.isNotBlank()}")
        appendLine("Gradient background: ${preferences.useGradientBackground}")
        appendLine("Enhanced shading: ${preferences.useEnhancedShading}")
        appendLine("Folder access mode: ${preferences.folderAccessMode}")
        appendLine("Log retention days: ${preferences.logRetentionDays}")
        appendLine("Auto export on rule change: ${preferences.autoExportOnRuleChange}")
        appendLine("Scheduled export enabled: ${preferences.scheduledExportEnabled}")
        appendLine("Local backup folder: ${redactedLocation(preferences.exportFolderUri)}")
        appendLine("Cloud backup folder: ${redactedLocation(preferences.cloudExportFolderUri)}")
        appendLine("Update check schedule: ${preferences.updateCheckSchedule}")
        appendLine("Notify on new updates: ${preferences.notifyOnNewUpdates}")
        appendLine("Save GitHub APK to Downloads: ${preferences.saveUpdateApkToDownloads}")
        appendLine("APK Downloads copy succeeded: ${preferences.updateApkDownloadsCopySucceeded}")
        appendLine("Haptics enabled: ${preferences.hapticFeedbackEnabled}")
        appendLine("Progressive blur enabled: ${preferences.progressiveBlurEnabled}")
        appendLine("Swipe start-to-end: ${preferences.swipeStartToEnd}")
        appendLine("Swipe end-to-start: ${preferences.swipeEndToStart}")
        appendLine("Bookmarked folders: ${preferences.bookmarkedFolders.size}")
        appendLine("Intro seen: ${preferences.hasSeenIntro}")
        appendLine("In-app review never ask again: ${preferences.inAppReviewAutoNeverAskAgain}")
    }

    private fun StringBuilder.appendPersistedUriPermissions(context: Context) {
        val permissions = context.contentResolver.persistedUriPermissions
        val readCount = permissions.count { it.isReadPermission }
        val writeCount = permissions.count { it.isWritePermission }
        val treeCount = permissions.count { it.uri.toString().contains("/tree/") }
        appendLine("Persisted URI permissions: ${permissions.size}")
        appendLine("Persisted URI read grants: $readCount")
        appendLine("Persisted URI write grants: $writeCount")
        appendLine("Persisted tree grants: $treeCount")
    }

    private fun StringBuilder.appendNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels =
            runCatching {
                notificationManager.notificationChannels.sortedBy { channel -> channel.id }
            }.getOrDefault(emptyList())
        appendLine("Notification channels")
        appendLine("=====================")
        if (channels.isEmpty()) {
            appendLine("No channels registered.")
            return
        }
        channels.forEach { channel ->
            appendLine(
                buildString {
                    append(channel.id)
                    append(": importance=")
                    append(channel.importance)
                    append(", sound=")
                    append(channel.sound != null)
                    append(", vibrate=")
                    append(channel.shouldVibrate())
                    append(", lights=")
                    append(channel.shouldShowLights())
                    append(", bypassDnd=")
                    append(channel.canBypassDnd())
                    append(", lockscreenVisibility=")
                    append(channel.lockscreenVisibility)
                },
            )
        }
    }

    private fun postNotificationsGranted(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            ).toString()
        } else {
            "not required"
        }

    private fun allFilesAccessGranted(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager().toString()
        } else {
            "not supported"
        }

    @Suppress("DEPRECATION")
    private fun installerPackageName(context: Context): String =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                context.packageManager.getInstallerPackageName(context.packageName)
            }.orEmpty().ifBlank { "unknown" }
        }.getOrDefault("unknown")

    private fun redactedLocation(value: String): String =
        when {
            value.isBlank() -> "not configured"
            value.startsWith("content://") -> "configured: content URI"
            value.startsWith("/") -> "configured: filesystem path"
            else -> "configured: ${value.substringBefore(':', missingDelimiterValue = "unknown")} reference"
        }

    private fun trimIfNeeded(logFile: File) {
        if (!logFile.exists() || logFile.length() <= MAX_LOG_BYTES) return
        val text = logFile.readText()
        val keepFrom = (text.length / 2).coerceAtLeast(0)
        logFile.writeText(text.substring(keepFrom))
    }

    private fun stackTraceText(throwable: Throwable): String {
        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))
        return stringWriter.toString()
    }
}
