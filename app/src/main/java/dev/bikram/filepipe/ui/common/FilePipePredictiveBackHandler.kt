package dev.bikram.filepipe.ui.common

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable

@Composable
fun FilePipePredictiveBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
) {
    PredictiveBackHandler(enabled = enabled) { progress ->
        progress.collect { }
        onBack()
    }
}
