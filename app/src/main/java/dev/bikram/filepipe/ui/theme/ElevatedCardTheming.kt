package dev.bikram.filepipe.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import dev.bikram.filepipe.data.preferences.AppThemeMode

/**
 * Elevated card colors used by every list card so all cards read the same as
 * Settings and edit surfaces. BLACK mode uses the lower surface rung so cards
 * stay OLED-dark while still separating from the page background.
 */
@Composable
fun elevatedCardColors(): CardColors {
    val scheme = MaterialTheme.colorScheme
    val themeState = LocalFilePipeThemeState.current
    val darkUi = ColorUtils.calculateLuminance(scheme.background.toArgb()) < 0.35
    val contentColor = if (darkUi) Color(0xFFE6E6EA) else Color(0xFF1C1B1F)
    val containerColor =
        if (themeState.themeMode == AppThemeMode.BLACK) {
            scheme.surfaceContainerLow
        } else {
            scheme.surfaceContainer
        }
    return CardDefaults.elevatedCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )
}
