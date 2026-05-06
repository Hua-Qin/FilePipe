package dev.bikram.filepipe.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.view.SoundEffectConstants
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import dev.bikram.filepipe.data.preferences.AppColorSource
import dev.bikram.filepipe.data.preferences.AppThemeMode
import dev.bikram.filepipe.data.preferences.ThemePaletteStyle
import dev.bikram.filepipe.ui.feedback.LocalHapticEnabled
import dev.bikram.filepipe.ui.feedback.LocalTapSound

private val LightColors =
    lightColorScheme(
        primary = Blue40,
        secondary = BlueGrey40,
        tertiary = Teal40,
        background = Color(0xFFE2E8F0),
        surface = Color(0xFFF8FAFC),
        surfaceDim = Color(0xFFE2E8F0),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceContainerLowest = Color(0xFFEEF2F7),
        surfaceContainerLow = Color(0xFFF3F6FA),
        surfaceContainer = Color(0xFFF6F8FC),
        surfaceContainerHigh = Color(0xFFFCFDFF),
        surfaceContainerHighest = Color(0xFFFFFFFF),
    )

private val DarkColors =
    darkColorScheme(
        primary = Blue80,
        secondary = BlueGrey80,
        tertiary = Teal80,
        background = Color(0xFF0D1117),
        surface = Color(0xFF161B22),
        surfaceDim = Color(0xFF0D1117),
        surfaceBright = Color(0xFF2D333B),
        surfaceContainerLowest = Color(0xFF0D1117),
        surfaceContainerLow = Color(0xFF1C2128),
        surfaceContainer = Color(0xFF22272E),
        surfaceContainerHigh = Color(0xFF2D333B),
        surfaceContainerHighest = Color(0xFF373E47),
    )

private val OledSurfaceHighest = Color(0xFF222222)

private val BlackOledColors =
    darkColorScheme(
        primary = Blue80,
        secondary = BlueGrey80,
        tertiary = Teal80,
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF2E2E2E),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF080808),
        surfaceContainer = Color(0xFF0F0F0F),
        surfaceContainerHigh = Color(0xFF181818),
        surfaceContainerHighest = OledSurfaceHighest,
    )

/** Flatten surfaces to pure black for BLACK (OLED) mode. */
private fun ColorScheme.toOled(): ColorScheme =
    copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF080808),
        surfaceContainer = Color(0xFF0F0F0F),
        surfaceContainerHigh = Color(0xFF181818),
        surfaceContainerHighest = Color(0xFF222222),
    )

/** Blend every surface role toward the active accent so panels visibly pick up the theme hue. */
private fun ColorScheme.tintSurfacesTowardPrimary(darkTheme: Boolean): ColorScheme {
    val accentArgb =
        ColorUtils.blendARGB(
            primary.toArgb(),
            primaryContainer.toArgb(),
            if (darkTheme) 0.4f else 0.3f,
        )
    val blendAmount = if (darkTheme) 0.24f else 0.15f

    fun tinted(role: Color) = Color(ColorUtils.blendARGB(role.toArgb(), accentArgb, blendAmount))
    return copy(
        surface = tinted(surface),
        surfaceVariant = tinted(surfaceVariant),
        surfaceDim = tinted(surfaceDim),
        surfaceBright = tinted(surfaceBright),
        surfaceContainerLowest = tinted(surfaceContainerLowest),
        surfaceContainerLow = tinted(surfaceContainerLow),
        surfaceContainer = tinted(surfaceContainer),
        surfaceContainerHigh = tinted(surfaceContainerHigh),
        surfaceContainerHighest = tinted(surfaceContainerHighest),
    )
}

/** Pull outline roles toward on-surface so outlined chrome stays legible. */
private fun ColorScheme.boostOutlineForVisibility(darkTheme: Boolean): ColorScheme {
    val targetArgb = onSurface.toArgb()
    val outlineBlend = if (darkTheme) 0.32f else 0.28f
    val outlineVariantBlend = if (darkTheme) 0.20f else 0.16f
    return copy(
        outline = Color(ColorUtils.blendARGB(outline.toArgb(), targetArgb, outlineBlend)),
        outlineVariant =
            Color(
                ColorUtils.blendARGB(outlineVariant.toArgb(), targetArgb, outlineVariantBlend),
            ),
    )
}

/** Pull accent containers toward their accent hues so selected pills and tonal buttons pop. */
private fun ColorScheme.boostContainersForSeedThemes(darkTheme: Boolean): ColorScheme {
    val primaryBlend = if (darkTheme) 0.30f else 0.24f
    val secondaryBlend = if (darkTheme) 0.26f else 0.20f
    val tertiaryBlend = if (darkTheme) 0.28f else 0.22f
    return copy(
        primaryContainer = Color(ColorUtils.blendARGB(primaryContainer.toArgb(), primary.toArgb(), primaryBlend)),
        secondaryContainer = Color(ColorUtils.blendARGB(secondaryContainer.toArgb(), secondary.toArgb(), secondaryBlend)),
        tertiaryContainer = Color(ColorUtils.blendARGB(tertiaryContainer.toArgb(), tertiary.toArgb(), tertiaryBlend)),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilePipeTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    colorSource: AppColorSource = AppColorSource.DEFAULT,
    themePaletteStyle: ThemePaletteStyle = ThemePaletteStyle.TONAL_SPOT,
    hapticFeedbackEnabled: Boolean = true,
    /** When true, omit primary surface boost (Remember-style enhanced shading). */
    useEnhancedShading: Boolean = false,
    activeCustomSeedHex: String = "",
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val darkTheme =
        when (themeMode) {
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
            AppThemeMode.BLACK -> true
            AppThemeMode.SYSTEM -> systemDark
        }

    val useDynamic = colorSource == AppColorSource.MATERIAL_YOU
    val black = themeMode == AppThemeMode.BLACK

    /** Non-wallpaper themes use either curated triplets or a custom seed ramp. */
    val staticTriplet =
        if (useDynamic || colorSource == AppColorSource.CUSTOM) {
            null
        } else {
            colorSource.curatedTriplet()
        }
    val staticSeedColor =
        if (useDynamic || staticTriplet != null) {
            null
        } else {
            when (colorSource) {
                AppColorSource.CUSTOM -> parseSeedColorHexToColorOrNull(activeCustomSeedHex) ?: Blue40
                else -> colorSource.seedPrimary() ?: Blue40
            }
        }

    val baseColorScheme =
        when {
            useDynamic && darkTheme -> dynamicDarkColorScheme(context)
            useDynamic && !darkTheme -> dynamicLightColorScheme(context)
            staticTriplet != null -> {
                val tripletOverrides =
                    staticTriplet.takeIf { themePaletteStyle == ThemePaletteStyle.TONAL_SPOT }
                rememberDynamicColorScheme(
                    seedColor = staticTriplet.primary,
                    isDark = darkTheme,
                    primary = tripletOverrides?.primary,
                    secondary = tripletOverrides?.secondary,
                    tertiary = tripletOverrides?.tertiary,
                    style = themePaletteStyle.toLib(),
                    isAmoled = black,
                )
            }
            staticSeedColor != null ->
                rememberDynamicColorScheme(
                    seedColor = staticSeedColor,
                    isDark = darkTheme,
                    style = themePaletteStyle.toLib(),
                    isAmoled = black,
                )
            darkTheme -> DarkColors
            else -> LightColors
        }
    val oledAdjusted = if (black) baseColorScheme.toOled() else baseColorScheme
    val colorScheme =
        if (!useEnhancedShading && !black) {
            oledAdjusted.tintSurfacesTowardPrimary(darkTheme = darkTheme)
        } else {
            oledAdjusted
        }.let { scheme ->
            if (useDynamic) {
                scheme
            } else {
                scheme
                    .boostOutlineForVisibility(darkTheme = darkTheme)
                    .boostContainersForSeedThemes(darkTheme = darkTheme)
            }
        }

    val view = LocalView.current
    SideEffect {
        view.isSoundEffectsEnabled = true
    }
    SideEffect {
        var context: Context? = view.context
        var hostingActivity: Activity? = null
        while (context != null) {
            if (context is Activity) {
                hostingActivity = context
                break
            }
            context = (context as? ContextWrapper)?.baseContext
        }
        val window = hostingActivity?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
    val realTapSound =
        remember(view) {
            val lastTapTimeMs = longArrayOf(0L)
            val minTapSoundSpacingMs = 85L
            {
                val now = SystemClock.uptimeMillis()
                if (now - lastTapTimeMs[0] >= minTapSoundSpacingMs) {
                    lastTapTimeMs[0] = now
                    if (view.isShown) {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    }
                }
            }
        }
    val noopSound = remember { {} }
    val playTapSound = if (hapticFeedbackEnabled) realTapSound else noopSound
    val gradientBackgroundColors =
        GradientBackgroundColors(
            pageBackground = oledAdjusted.background,
            gradientBase = oledAdjusted.surface,
            gradientTop = oledAdjusted.primaryContainer,
        )

    CompositionLocalProvider(
        LocalGradientBackgroundColors provides gradientBackgroundColors,
        LocalTapSound provides playTapSound,
        LocalHapticEnabled provides hapticFeedbackEnabled,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

private fun ThemePaletteStyle.toLib(): PaletteStyle =
    when (this) {
        ThemePaletteStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
        ThemePaletteStyle.NEUTRAL -> PaletteStyle.Neutral
        ThemePaletteStyle.VIBRANT -> PaletteStyle.Vibrant
        ThemePaletteStyle.EXPRESSIVE -> PaletteStyle.Expressive
        ThemePaletteStyle.RAINBOW -> PaletteStyle.Rainbow
        ThemePaletteStyle.FRUIT_SALAD -> PaletteStyle.FruitSalad
        ThemePaletteStyle.MONOCHROME -> PaletteStyle.Monochrome
        ThemePaletteStyle.FIDELITY -> PaletteStyle.Fidelity
        ThemePaletteStyle.CONTENT -> PaletteStyle.Content
    }

/**
 * [TopAppBarDefaults.topAppBarColors] with a transparent bar body and explicit on-surface chrome colors.
 * Needed over the root gradient: some expressive theme defaults leave titles/icons dark while the scrim is dark.
 */
@Composable
fun gradientOverlayTopAppBarColors() =
    TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    )
