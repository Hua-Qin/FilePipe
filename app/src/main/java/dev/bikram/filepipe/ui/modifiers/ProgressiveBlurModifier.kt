package dev.bikram.filepipe.ui.modifiers

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import dev.bikram.filepipe.ui.theme.LocalGradientBackgroundColors
import dev.bikram.filepipe.ui.theme.LocalUseGradientBackground
import dev.bikram.filepipe.ui.theme.ProgressiveBlurStyle

enum class BlurDirection {
    TOP,
    BOTTOM,
}

private val dualEdgeBlurAgsl =
    """
    uniform shader content;
    uniform float blurRadius;
    uniform float topHeight;
    uniform float bottomHeight;
    uniform float contentHeight;
    uniform float topBlurProgressPower;

    half4 main(float2 fragCoord) {
        float topProgress = topHeight > 0.0
            ? 1.0 - clamp(fragCoord.y / topHeight, 0.0, 1.0)
            : 0.0;
        float bottomProgress = bottomHeight > 0.0
            ? 1.0 - clamp((contentHeight - fragCoord.y) / bottomHeight, 0.0, 1.0)
            : 0.0;

        float progress = max(
            topHeight > 0.0 ? pow(topProgress, topBlurProgressPower) : 0.0,
            bottomHeight > 0.0 ? pow(bottomProgress, 1.5) : 0.0
        );
        float radius = progress * blurRadius;

        if (radius <= 0.0) {
            return content.eval(fragCoord);
        }

        half4 accum = half4(0.0);
        float weightSum = 0.0;

        float dither = fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453);
        float2 jitter = float2(dither - 0.5, fract(dither * 1.618) - 0.5);

        const int SAMPLES = 4;
        float offsetScale = radius / float(SAMPLES);

        for (int x = -SAMPLES; x <= SAMPLES; x++) {
            for (int y = -SAMPLES; y <= SAMPLES; y++) {
                float2 offset = (float2(float(x), float(y)) + jitter) * offsetScale;
                float distSq = dot(offset, offset);
                float radiusSq = radius * radius;

                if (distSq <= radiusSq) {
                    float weight = exp(-3.0 * distSq / radiusSq);
                    accum += content.eval(fragCoord + offset) * weight;
                    weightSum += weight;
                }
            }
        }

        return accum / weightSum;
    }
    """.trimIndent()

/**
 * Progressive blur on both edges simultaneously.
 * Full shader path requires API 33+; below that, only the gradient overlays run.
 */
fun Modifier.progressiveBlur(
    blurRadius: Float,
    topHeight: Float = 0f,
    bottomHeight: Float = 0f,
    showGradientOverlay: Boolean = true,
    overlayAlpha: Float = 0.28f,
    overlayAlphaBottom: Float = overlayAlpha,
    topBlurProgressPower: Float = 1.1f,
    topAlphaMultiplier: Float = 1f,
    bottomAlphaMultiplier: Float = 1f,
): Modifier =
    composed {
        val useGradient = LocalUseGradientBackground.current
        val colors = LocalGradientBackgroundColors.current

        val baseColorTop =
            if (useGradient && colors.gradientTop != Color.Unspecified) {
                if (colors.gradientBase != Color.Unspecified) {
                    androidx.compose.ui.graphics
                        .lerp(colors.gradientBase, colors.gradientTop, 0.48f)
                } else {
                    colors.gradientTop
                }
            } else if (colors.pageBackground != Color.Unspecified) {
                colors.pageBackground
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }

        val baseColorBottom =
            if (useGradient && colors.gradientBase != Color.Unspecified) {
                colors.gradientBase
            } else if (colors.pageBackground != Color.Unspecified) {
                colors.pageBackground
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }

        val finalAlphaTop = (if (blurRadius <= 0f) 1.0f else overlayAlpha) * topAlphaMultiplier
        val finalAlphaBottom = (if (blurRadius <= 0f) 1.0f else overlayAlphaBottom) * bottomAlphaMultiplier

        val overlayColorTop = baseColorTop.copy(alpha = finalAlphaTop)
        val overlayColorBottom = baseColorBottom.copy(alpha = finalAlphaBottom)

        val blurModifier =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && blurRadius > 0f) {
                val shader = remember { RuntimeShader(dualEdgeBlurAgsl) }
                Modifier.graphicsLayer {
                    shader.setFloatUniform("blurRadius", blurRadius)
                    shader.setFloatUniform("topHeight", topHeight * topAlphaMultiplier)
                    shader.setFloatUniform("bottomHeight", bottomHeight * bottomAlphaMultiplier)
                    shader.setFloatUniform("contentHeight", size.height)
                    shader.setFloatUniform("topBlurProgressPower", topBlurProgressPower)

                    renderEffect =
                        RenderEffect
                            .createRuntimeShaderEffect(shader, "content")
                            .asComposeRenderEffect()
                }
            } else {
                Modifier
            }

        val gradientModifier =
            if (showGradientOverlay) {
                Modifier.drawWithContent {
                    drawContent()
                    val activeTopHeight = topHeight * topAlphaMultiplier
                    if (activeTopHeight > 0f) {
                        val brush =
                            if (blurRadius <= 0f) {
                                Brush.verticalGradient(
                                    colorStops =
                                        arrayOf(
                                            0.0f to overlayColorTop,
                                            0.75f to overlayColorTop.copy(alpha = overlayColorTop.alpha * 0.95f),
                                            1.0f to Color.Transparent,
                                        ),
                                    endY = activeTopHeight,
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(overlayColorTop, Color.Transparent),
                                    endY = activeTopHeight,
                                )
                            }
                        drawRect(brush = brush)
                    }
                    val activeBottomHeight = bottomHeight * bottomAlphaMultiplier
                    if (activeBottomHeight > 0f) {
                        val brush =
                            if (blurRadius <= 0f) {
                                Brush.verticalGradient(
                                    colorStops =
                                        arrayOf(
                                            0.0f to Color.Transparent,
                                            0.65f to overlayColorBottom.copy(alpha = overlayColorBottom.alpha * 0.9f),
                                            1.0f to overlayColorBottom,
                                        ),
                                    startY = size.height - activeBottomHeight,
                                    endY = size.height,
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, overlayColorBottom),
                                    startY = size.height - activeBottomHeight,
                                )
                            }
                        drawRect(brush = brush)
                    }
                }
            } else {
                Modifier
            }

        this.then(blurModifier).then(gradientModifier)
    }

/**
 * Single-edge progressive blur kept for backward compatibility.
 */
fun Modifier.progressiveBlur(
    blurRadius: Float,
    height: Float,
    direction: BlurDirection = BlurDirection.TOP,
    showGradientOverlay: Boolean = true,
): Modifier =
    progressiveBlur(
        blurRadius = blurRadius,
        topHeight = if (direction == BlurDirection.TOP) height else 0f,
        bottomHeight = if (direction == BlurDirection.BOTTOM) height else 0f,
        showGradientOverlay = showGradientOverlay,
    )

/** Blur for [LazyColumn] under a transparent [LargeTopAppBar] (app bar is a sibling, not blurred). */
fun Modifier.progressiveBlurScrollableList(
    style: ProgressiveBlurStyle,
    topAlphaMultiplier: Float = 1f,
    bottomAlphaMultiplier: Float = 1f,
): Modifier =
    progressiveBlur(
        blurRadius = style.blurRadius,
        topHeight = style.topHeightPx,
        bottomHeight = style.bottomHeightPx,
        showGradientOverlay = true,
        overlayAlpha = style.overlayAlpha,
        overlayAlphaBottom = style.overlayAlphaBottom,
        topBlurProgressPower = style.topBlurProgressPower,
        topAlphaMultiplier = topAlphaMultiplier,
        bottomAlphaMultiplier = bottomAlphaMultiplier,
    )

/** Blur for a full-screen layer (y=0 at window top), e.g. rule edit scroll under transparent chrome. */
fun Modifier.progressiveBlurFullBleedLayer(
    style: ProgressiveBlurStyle,
    topAlphaMultiplier: Float = 1f,
    bottomAlphaMultiplier: Float = 1f,
): Modifier =
    progressiveBlur(
        blurRadius = style.blurRadius,
        topHeight = style.topHeightPx,
        bottomHeight = style.bottomHeightPx,
        showGradientOverlay = true,
        overlayAlpha = style.overlayAlpha,
        overlayAlphaBottom = style.overlayAlphaBottom,
        topBlurProgressPower = style.topBlurProgressPower,
        topAlphaMultiplier = topAlphaMultiplier,
        bottomAlphaMultiplier = bottomAlphaMultiplier,
    )
