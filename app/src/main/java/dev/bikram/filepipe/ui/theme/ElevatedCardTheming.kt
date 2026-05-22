package dev.bikram.filepipe.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
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
    val containerColor = resolveElevatedCardContainerColor(scheme, themeState.themeMode, darkUi)
    return CardDefaults.elevatedCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

/** Same elevated surface as section cards (source, destination, etc.). */
@Composable
fun cardIconContainerColor(): Color = elevatedCardColors().containerColor

@Composable
fun cardFilledTonalIconButtonColors(): IconButtonColors =
    IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = cardIconContainerColor(),
        contentColor = MaterialTheme.colorScheme.primary,
    )

private fun resolveElevatedCardContainerColor(
    scheme: ColorScheme,
    themeMode: AppThemeMode,
    darkUi: Boolean,
): Color {
    val baseRung =
        when {
            themeMode == AppThemeMode.BLACK -> scheme.surfaceContainerLow
            darkUi -> scheme.surfaceContainerHigh
            else -> scheme.surfaceContainer
        }
    val liftTarget = if (darkUi) scheme.surfaceContainerHighest else scheme.surfaceBright
    val liftAmount = if (darkUi) 0.34f else 0.18f
    return Color(ColorUtils.blendARGB(baseRung.toArgb(), liftTarget.toArgb(), liftAmount))
}
