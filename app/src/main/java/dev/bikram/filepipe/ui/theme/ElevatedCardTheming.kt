package dev.bikram.filepipe.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkContent = Color(0xFFE6E6EA)
private val LightContent = Color(0xFF1C1B1F)

/**
 * Elevated card colors used by every list card so all cards read the same as
 * Settings and edit surfaces. BLACK mode uses the lower surface rung so cards
 * stay OLED-dark while still separating from the page background.
 */
@Composable
fun elevatedCardColors(): CardColors {
    val themeState = LocalFilePipeThemeState.current
    val blackThemeActive = themeState.blackThemeActive(LocalIsDark.current)
    val containerColor =
        if (blackThemeActive) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    val contentColor = if (LocalIsDark.current) DarkContent else LightContent
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
