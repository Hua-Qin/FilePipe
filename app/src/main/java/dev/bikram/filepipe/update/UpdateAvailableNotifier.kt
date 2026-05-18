package dev.bikram.filepipe.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.MainActivity
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.preferences.AppPreferences
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.shortcuts.PendingShortcutRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateAvailableNotifier
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) {
        fun ensureNotificationChannel() {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_updates_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notification_channel_updates_description)
                }
            manager.createNotificationChannel(channel)
        }

        suspend fun notifyIfNewUpdateAvailable(
            info: UpdateInfo,
            prefs: AppPreferences,
        ) {
            if (!prefs.notifyOnNewUpdates) return
            postUpdateNotification(info, updateDedupe = true, currentDedupeKey = prefs.updateLastNotifiedDedupeKey)
        }

        suspend fun notifyDevMockUpdateAvailable(info: UpdateInfo) {
            postUpdateNotification(info, updateDedupe = false, currentDedupeKey = "")
        }

        private suspend fun postUpdateNotification(
            info: UpdateInfo,
            updateDedupe: Boolean,
            currentDedupeKey: String,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted =
                    ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }
            val dedupeKey = info.notificationDedupeKey()
            if (updateDedupe && dedupeKey == currentDedupeKey) return

            ensureNotificationChannel()

            val openIntent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(PendingShortcutRepository.EXTRA_OPEN_SETTINGS_UPDATES, true)
                }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val contentPendingIntent =
                PendingIntent.getActivity(
                    context,
                    REQUEST_CODE_OPEN_UPDATES,
                    openIntent,
                    pendingFlags,
                )

            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notification_update_available_title))
                    .setContentText(
                        context.getString(R.string.notification_update_available_text, info.versionName),
                    ).setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(contentPendingIntent)
                    .setAutoCancel(true)
                    .build()

            runCatching {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                if (updateDedupe) {
                    userPreferencesRepository.setUpdateLastNotifiedDedupeKey(dedupeKey)
                }
            }
        }

        companion object {
            const val CHANNEL_ID = "filepipe_updates"
            private const val NOTIFICATION_ID = 71002
            private const val REQUEST_CODE_OPEN_UPDATES = 1002
        }
    }
