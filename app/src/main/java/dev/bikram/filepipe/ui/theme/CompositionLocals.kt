package dev.bikram.filepipe.ui.theme

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.bikram.filepipe.data.preferences.AppColorSource
import dev.bikram.filepipe.data.preferences.AppThemeMode
import dev.bikram.filepipe.data.preferences.ThemePaletteStyle

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }

val LocalReducedMotion = compositionLocalOf { false }

val LocalIsDark = compositionLocalOf { false }

/** When true, main tab [androidx.compose.material3.Scaffold]s use a transparent container so the root gradient shows through. */
val LocalUseGradientBackground = compositionLocalOf { false }

val LocalUseEnhancedShading = compositionLocalOf { false }

val LocalHeroOnCards = compositionLocalOf { false }

data class GradientBackgroundColors(
    val pageBackground: Color,
    val gradientBase: Color,
    val gradientTop: Color,
)

val LocalGradientBackgroundColors =
    compositionLocalOf {
        GradientBackgroundColors(
            pageBackground = Color.Unspecified,
            gradientBase = Color.Unspecified,
            gradientTop = Color.Unspecified,
        )
    }

/** When true, root chrome may apply progressive edge blur; inner screens can match with transparent app bars. */
val LocalProgressiveBlurEnabled = compositionLocalOf { true }

val LocalBlurBars = compositionLocalOf { true }

/**
 * Edge blur parameters for the current route, or null when progressive blur is off.
 * Apply via [dev.bikram.filepipe.ui.modifiers.progressiveBlurScrollableList] or [dev.bikram.filepipe.ui.modifiers.progressiveBlurFullBleedLayer] so app bars stay sharp.
 */
data class ProgressiveBlurStyle(
    /** Top fade/blur band in px (local to the blurred composable; match app bar + status insets). */
    val topHeightPx: Float,
    val bottomHeightPx: Float,
    val blurRadius: Float,
    /** Gradient overlay strength at the top edge. */
    val overlayAlpha: Float,
    /** Gradient overlay strength at the bottom edge (may exceed [overlayAlpha] for stronger bottom scrim). */
    val overlayAlphaBottom: Float,
    val topBlurProgressPower: Float = 1.1f,
)

val LocalProgressiveBlurStyle = compositionLocalOf<ProgressiveBlurStyle?> { null }

data class FilePipeThemeState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val colorSource: AppColorSource = AppColorSource.DEFAULT,
    val savedCustomSeedHexes: List<String> = emptyList(),
    val activeCustomSeedHex: String = "",
    val themePaletteStyle: ThemePaletteStyle = ThemePaletteStyle.TONAL_SPOT,
    val useGradientBackground: Boolean = true,
    val useEnhancedShading: Boolean = false,
    val progressiveBlurEnabled: Boolean = true,
)

val LocalFilePipeThemeState = compositionLocalOf { FilePipeThemeState() }
