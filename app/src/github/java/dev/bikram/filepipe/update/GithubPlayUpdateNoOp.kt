package dev.bikram.filepipe.update

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GithubPlayUpdateNoOp @Inject constructor() : PlayUpdateSessionHandle, PlayInAppUpdateStarter {
    override fun clearPendingPlayUpdate() {}

    override fun startUpdateIfPending(
        activity: ComponentActivity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ): Boolean = false
}
