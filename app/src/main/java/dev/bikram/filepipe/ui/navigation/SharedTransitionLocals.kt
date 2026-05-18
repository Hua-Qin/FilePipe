package dev.bikram.filepipe.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

const val DEV_OPTIONS_SHARED_BOUNDS_KEY = "settings_dev_options_container"

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope =
    compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope =
    compositionLocalOf<AnimatedVisibilityScope?> { null }
