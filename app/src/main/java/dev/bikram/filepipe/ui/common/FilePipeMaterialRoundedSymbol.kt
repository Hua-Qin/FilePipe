package dev.bikram.filepipe.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.bikram.filepipe.R

private val MaterialSymbolsRoundedFilledFontFamily: FontFamily =
    FontFamily(Font(R.font.material_symbols_rounded))

private val MaterialSymbolsRoundedOutlinedFontFamily: FontFamily =
    FontFamily(Font(R.font.material_symbols_rounded_outlined))

private val FlatLineHeightStyle =
    LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    )

@Composable
fun FilePipeMaterialRoundedSymbol(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
    @Suppress("UNUSED_PARAMETER") weight: FontWeight = FontWeight.Medium,
    @Suppress("UNUSED_PARAMETER") grade: Float = 0f,
    filled: Boolean = true,
    autoMirror: Boolean = false,
    opticalCenterYOffset: Dp = 0.dp,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val fontSize = remember(size, density) { with(density) { size.toSp() } }
    val brush = remember(tint) { SolidColor(tint) }
    val fontFamily =
        if (filled) {
            MaterialSymbolsRoundedFilledFontFamily
        } else {
            MaterialSymbolsRoundedOutlinedFontFamily
        }
    val mirroredModifier =
        if (autoMirror && layoutDirection == LayoutDirection.Rtl) {
            modifier.graphicsLayer { scaleX = -1f }
        } else {
            modifier
        }
    val offsetModifier =
        if (opticalCenterYOffset == 0.dp) {
            mirroredModifier
        } else {
            mirroredModifier.offset(y = opticalCenterYOffset)
        }
    val semanticsModifier =
        offsetModifier.clearAndSetSemantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
        }

    Box(
        modifier = semanticsModifier,
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = name,
            style =
                TextStyle(
                    brush = brush,
                    fontSize = fontSize,
                    lineHeight = fontSize,
                    fontFamily = fontFamily,
                    fontFeatureSettings = "\"rlig\" 1, \"liga\" 1",
                    textAlign = TextAlign.Center,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = FlatLineHeightStyle,
                ),
        )
    }
}
