package dev.bikram.filepipe.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.bikram.filepipe.data.preferences.AppThemeMode

private val LightColors = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = Teal40
)

private val DarkColors = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = Teal80
)

private val BlackOledColors = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = Teal80,
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = BlackSurfaceHigh,
    surfaceContainer = BlackSurfaceHigh,
    surfaceContainerHigh = BlackSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2C2C2C)
)

private val OledSurfaceHighest = Color(0xFF2C2C2C)

/** Keeps true-black OLED surfaces while preserving dynamic (Material You) accent colors. */
private fun oledSurfacesFrom(dynamicScheme: ColorScheme): ColorScheme = dynamicScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = BlackSurfaceHigh,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = BlackSurfaceHigh,
    surfaceContainer = BlackSurfaceHigh,
    surfaceContainerHigh = BlackSurfaceHigh,
    surfaceContainerHighest = OledSurfaceHighest
)

@Composable
fun MediaOrganizerTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    useMaterialYou: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.BLACK -> true
        AppThemeMode.SYSTEM -> systemDark
    }

    val useDynamic = useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        themeMode == AppThemeMode.BLACK && useDynamic ->
            oledSurfacesFrom(dynamicDarkColorScheme(context))
        themeMode == AppThemeMode.BLACK -> BlackOledColors
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
