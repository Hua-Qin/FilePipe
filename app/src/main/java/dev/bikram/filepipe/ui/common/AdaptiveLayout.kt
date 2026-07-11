@file:Suppress("ConfigurationScreenWidthHeight")

package dev.bikram.filepipe.ui.common

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Single source of truth for the app's responsive breakpoints. Screens used to each re-derive
 * "is this a short landscape window?" inline from [LocalConfiguration], duplicating the same
 * orientation + height check (and the magic 480 threshold). Everything now routes through the
 * helpers below so the threshold and measurement stay consistent.
 */

/** Below this many dp of height in landscape, screens switch to space-saving layouts. */
const val SMALL_LANDSCAPE_HEIGHT_DP = 480

enum class ResponsiveActionLayout {
    HORIZONTAL,
    STACKED,
}

fun responsiveTextScaleForWidth(availableWidth: Dp): Float =
    when {
        availableWidth < 320.dp -> 0.84f
        availableWidth < 360.dp -> 0.88f
        availableWidth < 430.dp -> 0.93f
        else -> 1f
    }

fun responsiveActionLayout(
    availableWidth: Dp,
    effectiveFontScale: Float,
    itemCount: Int,
): ResponsiveActionLayout {
    if (itemCount <= 1) return ResponsiveActionLayout.HORIZONTAL
    val tooNarrowForRow =
        availableWidth < 360.dp ||
            (availableWidth < 430.dp && effectiveFontScale > 1.10f) ||
            (availableWidth < 520.dp && effectiveFontScale > 1.15f)
    return if (tooNarrowForRow) ResponsiveActionLayout.STACKED else ResponsiveActionLayout.HORIZONTAL
}

@Composable
@ReadOnlyComposable
fun isLandscape(): Boolean = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

/**
 * True in landscape on a short window (e.g. most phones rotated, or a small split-screen pane),
 * where screens drop to compact spacing / smaller controls / hide non-essential chrome.
 */
@Composable
@ReadOnlyComposable
fun isSmallLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.screenHeightDp < SMALL_LANDSCAPE_HEIGHT_DP
}
