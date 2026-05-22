package dev.bikram.filepipe.update

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

interface PlayUpdateSessionHandle {
    fun clearPendingPlayUpdate()
}

interface PlayInAppUpdateStarter {
    fun startUpdateIfPending(
        activity: ComponentActivity,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ): Boolean
}
