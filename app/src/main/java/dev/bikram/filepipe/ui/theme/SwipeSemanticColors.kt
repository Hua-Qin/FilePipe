package dev.bikram.filepipe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import dev.bikram.filepipe.data.preferences.SwipeAction

/**
 * Fixed M3-style semantic hues for swipe affordances (not derived from the dynamic color scheme).
 * Light values align with Material baseline error / standard container tones; dark values are
 * tuned for contrast on dark UI backgrounds.
 */
@Composable
fun SwipeAction.semanticSwipeBackground(): Color {
    val darkUi = ColorUtils.calculateLuminance(MaterialTheme.colorScheme.background.toArgb()) < 0.35
    return semanticSwipeBackgroundForUi(this, darkUi)
}

@Composable
fun SwipeAction.semanticSwipeIconTint(): Color {
    val darkUi = ColorUtils.calculateLuminance(MaterialTheme.colorScheme.background.toArgb()) < 0.35
    return semanticSwipeIconTintForUi(this, darkUi)
}

internal fun semanticSwipeBackgroundForUi(
    action: SwipeAction,
    darkUi: Boolean,
): Color =
    when (action) {
        SwipeAction.DELETE -> if (darkUi) Color(0xFF5C1414) else Color(0xFFF9DEDC)
        SwipeAction.EDIT -> if (darkUi) Color(0xFF4D3E00) else Color(0xFFFFF3E0)
        SwipeAction.PREVIEW -> if (darkUi) Color(0xFF0F3D1A) else Color(0xFFC4EED0)
        SwipeAction.DUPLICATE -> if (darkUi) Color(0xFF0A3050) else Color(0xFFD0E4FF)
        SwipeAction.VIEW_HISTORY -> if (darkUi) Color(0xFF3F3F3F) else Color(0xFFE7E0EC)
    }

internal fun semanticSwipeIconTintForUi(
    action: SwipeAction,
    darkUi: Boolean,
): Color =
    when (action) {
        SwipeAction.DELETE -> if (darkUi) Color(0xFFF2B8B5) else Color(0xFFB3261E)
        SwipeAction.EDIT -> if (darkUi) Color(0xFFFFE082) else Color(0xFF6B5A00)
        SwipeAction.PREVIEW -> if (darkUi) Color(0xFFA3D9B0) else Color(0xFF146C2E)
        SwipeAction.DUPLICATE -> if (darkUi) Color(0xFF9ECAFF) else Color(0xFF0B57D0)
        SwipeAction.VIEW_HISTORY -> if (darkUi) Color(0xFFCAC4D0) else Color(0xFF49454F)
    }
