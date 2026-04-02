package dev.bikram.filepipe.ui.theme

import androidx.compose.runtime.compositionLocalOf

/** When true, main tab [androidx.compose.material3.Scaffold]s use a transparent container so the root gradient shows through. */
val LocalUseGradientBackground = compositionLocalOf { false }

/** When true, root chrome may apply progressive edge blur; inner screens can match with transparent app bars. */
val LocalProgressiveBlurEnabled = compositionLocalOf { true }
