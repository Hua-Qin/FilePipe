package dev.bikram.filepipe.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeFidelity
import com.materialkolor.scheme.SchemeFruitSalad
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeRainbow
import com.materialkolor.scheme.SchemeVibrant
import com.materialkolor.scheme.Variant
import dev.bikram.filepipe.data.preferences.ThemePaletteStyle

private const val contrastLevel = 0.0

fun colorSchemeFromSeed(
    seedPrimary: Color,
    paletteStyle: ThemePaletteStyle,
    darkTheme: Boolean
): ColorScheme {
    val hct = Hct.fromInt(seedPrimary.toArgb())
    val hue = hct.hue
    val chroma = hct.chroma

    val primaryPalette = TonalPalette.fromInt(seedPrimary.toArgb())
    val secondaryPalette = TonalPalette.fromHueAndChroma(hue, chroma / 3.0)
    val tertiaryPalette = TonalPalette.fromHueAndChroma(hue + 60.0, chroma / 2.0)
    val neutralPalette = TonalPalette.fromHueAndChroma(hue, (chroma / 12.0).coerceAtMost(4.0))
    val neutralVariantPalette = TonalPalette.fromInt(neutralPalette.tone(90))

    val dynamicScheme: DynamicScheme = when (paletteStyle) {
        ThemePaletteStyle.TONAL_SPOT -> DynamicScheme(
            hct,
            Variant.TONAL_SPOT,
            darkTheme,
            contrastLevel,
            primaryPalette,
            secondaryPalette,
            tertiaryPalette,
            neutralPalette,
            neutralVariantPalette
        )
        ThemePaletteStyle.NEUTRAL -> SchemeNeutral(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.VIBRANT -> SchemeVibrant(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.EXPRESSIVE -> SchemeExpressive(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.RAINBOW -> SchemeRainbow(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.FRUIT_SALAD -> SchemeFruitSalad(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.MONOCHROME -> SchemeMonochrome(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.FIDELITY -> SchemeFidelity(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.CONTENT -> SchemeContent(hct, darkTheme, contrastLevel)
    }

    return dynamicScheme.toComposeColorScheme()
}

private fun DynamicScheme.toComposeColorScheme(): ColorScheme {
    val colors = MaterialDynamicColors()
    return ColorScheme(
        primary = Color(colors.primary().getArgb(this)),
        onPrimary = Color(colors.onPrimary().getArgb(this)),
        primaryContainer = Color(colors.primaryContainer().getArgb(this)),
        onPrimaryContainer = Color(colors.onPrimaryContainer().getArgb(this)),
        inversePrimary = Color(colors.inversePrimary().getArgb(this)),
        secondary = Color(colors.secondary().getArgb(this)),
        onSecondary = Color(colors.onSecondary().getArgb(this)),
        secondaryContainer = Color(colors.secondaryContainer().getArgb(this)),
        onSecondaryContainer = Color(colors.onSecondaryContainer().getArgb(this)),
        tertiary = Color(colors.tertiary().getArgb(this)),
        onTertiary = Color(colors.onTertiary().getArgb(this)),
        tertiaryContainer = Color(colors.tertiaryContainer().getArgb(this)),
        onTertiaryContainer = Color(colors.onTertiaryContainer().getArgb(this)),
        background = Color(colors.background().getArgb(this)),
        onBackground = Color(colors.onBackground().getArgb(this)),
        surface = Color(colors.surface().getArgb(this)),
        onSurface = Color(colors.onSurface().getArgb(this)),
        surfaceVariant = Color(colors.surfaceVariant().getArgb(this)),
        onSurfaceVariant = Color(colors.onSurfaceVariant().getArgb(this)),
        surfaceTint = Color(colors.surfaceTint().getArgb(this)),
        inverseSurface = Color(colors.inverseSurface().getArgb(this)),
        inverseOnSurface = Color(colors.inverseOnSurface().getArgb(this)),
        error = Color(colors.error().getArgb(this)),
        onError = Color(colors.onError().getArgb(this)),
        errorContainer = Color(colors.errorContainer().getArgb(this)),
        onErrorContainer = Color(colors.onErrorContainer().getArgb(this)),
        outline = Color(colors.outline().getArgb(this)),
        outlineVariant = Color(colors.outlineVariant().getArgb(this)),
        scrim = Color(colors.scrim().getArgb(this)),
        surfaceBright = Color(colors.surfaceBright().getArgb(this)),
        surfaceDim = Color(colors.surfaceDim().getArgb(this)),
        surfaceContainer = Color(colors.surfaceContainer().getArgb(this)),
        surfaceContainerHigh = Color(colors.surfaceContainerHigh().getArgb(this)),
        surfaceContainerHighest = Color(colors.surfaceContainerHighest().getArgb(this)),
        surfaceContainerLow = Color(colors.surfaceContainerLow().getArgb(this)),
        surfaceContainerLowest = Color(colors.surfaceContainerLowest().getArgb(this)),
        primaryFixed = Color(colors.primaryFixed().getArgb(this)),
        primaryFixedDim = Color(colors.primaryFixedDim().getArgb(this)),
        onPrimaryFixed = Color(colors.onPrimaryFixed().getArgb(this)),
        onPrimaryFixedVariant = Color(colors.onPrimaryFixedVariant().getArgb(this)),
        secondaryFixed = Color(colors.secondaryFixed().getArgb(this)),
        secondaryFixedDim = Color(colors.secondaryFixedDim().getArgb(this)),
        onSecondaryFixed = Color(colors.onSecondaryFixed().getArgb(this)),
        onSecondaryFixedVariant = Color(colors.onSecondaryFixedVariant().getArgb(this)),
        tertiaryFixed = Color(colors.tertiaryFixed().getArgb(this)),
        tertiaryFixedDim = Color(colors.tertiaryFixedDim().getArgb(this)),
        onTertiaryFixed = Color(colors.onTertiaryFixed().getArgb(this)),
        onTertiaryFixedVariant = Color(colors.onTertiaryFixedVariant().getArgb(this))
    )
}
