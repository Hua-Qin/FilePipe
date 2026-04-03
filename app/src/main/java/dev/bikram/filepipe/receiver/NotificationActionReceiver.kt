package dev.bikram.filepipe.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import dagger.hilt.android.AndroidEntryPoint
import dev.bikram.filepipe.domain.usecase.UndoRunUseCase
import dev.bikram.filepipe.worker.FileOrganizerWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var undoRunUseCase: UndoRunUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_UNDO_RUN) return

        val historyId = intent.getLongExtra(EXTRA_HISTORY_ID, -1L)
        if (historyId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                undoRunUseCase(historyId)
            } finally {
                // Dismiss the summary notification
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(FileOrganizerWorker.SUMMARY_NOTIFICATION_ID)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_UNDO_RUN = "dev.bikram.filepipe.ACTION_UNDO_RUN"
        const val EXTRA_HISTORY_ID = "extra_history_id"
    }
}
