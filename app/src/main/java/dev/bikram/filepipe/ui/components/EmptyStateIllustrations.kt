package dev.bikram.filepipe.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import dev.bikram.filepipe.ui.theme.LocalReducedMotion
import dev.bikram.filepipe.ui.theme.MorphPolygonShape
import dev.bikram.filepipe.ui.theme.RoundedPolygonShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val ARTBOARD = 120f

/**
 * History empty state: clock + timeline bars (120×120 artboard proportions).
 * Timeline is drawn after the clock so the upper line overlaps the bottom of the circle.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeColoredEmptyHistoryIllustration(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.size(120.dp).clearAndSetSemantics { }) {
        ExpressiveEmptyBackdrop(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 7.dp, top = 7.dp)
                    .size(46.dp),
            polygon = MaterialShapes.Cookie6Sided,
            morphTo = MaterialShapes.Clover4Leaf,
            color = scheme.primaryContainer.copy(alpha = 0.54f),
        )
        ExpressiveEmptyBackdrop(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 9.dp, bottom = 4.dp)
                    .size(42.dp),
            polygon = MaterialShapes.Sunny,
            morphTo = MaterialShapes.Cookie9Sided,
            color = scheme.tertiaryContainer.copy(alpha = 0.48f),
        )
        Canvas(Modifier.matchParentSize()) {
            val scaleX = size.width / ARTBOARD
            val scaleY = size.height / ARTBOARD

            fun sx(x: Float) = x * scaleX

            fun sy(y: Float) = y * scaleY

            val centerClock = Offset(sx(60f), sy(60f))
            val radiusOuter = 42f * scaleX.coerceAtMost(scaleY)
            val radiusInner = 34f * scaleX.coerceAtMost(scaleY)

            drawCircle(color = scheme.primaryContainer, radius = radiusOuter, center = centerClock)
            drawCircle(color = scheme.surfaceContainerLow, radius = radiusInner, center = centerClock)

            val accent = scheme.primary
            val hourStroke = 4.dp.toPx()
            val minuteStroke = 3.dp.toPx()
            val minuteHandLength = radiusInner * 0.88f
            val hourHandLength = minuteHandLength * (2f / 3f)

            fun tipClockwiseFrom12(
                clockwiseFrom12Rad: Double,
                lengthPx: Float,
            ): Offset {
                val phi = clockwiseFrom12Rad
                return Offset(
                    centerClock.x + (sin(phi) * lengthPx).toFloat(),
                    centerClock.y + (-cos(phi) * lengthPx).toFloat(),
                )
            }
            val minuteClockwiseFrom12Rad = (10.0 / 60.0) * 2.0 * PI
            val hourClockwiseFrom12Rad = ((10.0 + 10.0 / 60.0) / 12.0) * 2.0 * PI
            drawLine(
                color = accent,
                start = centerClock,
                end = tipClockwiseFrom12(hourClockwiseFrom12Rad, hourHandLength),
                strokeWidth = hourStroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = accent,
                start = centerClock,
                end = tipClockwiseFrom12(minuteClockwiseFrom12Rad, minuteHandLength),
                strokeWidth = minuteStroke,
                cap = StrokeCap.Round,
            )
            drawCircle(color = accent, radius = 3.dp.toPx(), center = centerClock)

            val lineColor = scheme.tertiary
            val lineStroke = 4.dp.toPx()
            val barY1 = sy(96f)
            val barY2 = sy(106f)
            drawLine(
                color = lineColor,
                start = Offset(sx(28f), barY1),
                end = Offset(sx(92f), barY1),
                strokeWidth = lineStroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = lineColor,
                start = Offset(sx(36f), barY2),
                end = Offset(sx(84f), barY2),
                strokeWidth = lineStroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Rules empty state: folder + plus using original 120×120 vector proportions (theme colors).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeColoredEmptyRulesIllustration(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.size(120.dp).clearAndSetSemantics { }) {
        ExpressiveEmptyBackdrop(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 4.dp)
                    .size(48.dp),
            polygon = MaterialShapes.Clover4Leaf,
            morphTo = MaterialShapes.Cookie9Sided,
            color = scheme.tertiaryContainer.copy(alpha = 0.50f),
        )
        ExpressiveEmptyBackdrop(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 8.dp)
                    .size(44.dp),
            polygon = MaterialShapes.Cookie9Sided,
            morphTo = MaterialShapes.Clover4Leaf,
            color = scheme.primaryContainer.copy(alpha = 0.46f),
        )
        Canvas(Modifier.matchParentSize()) {
            val scaleX = size.width / ARTBOARD
            val scaleY = size.height / ARTBOARD

            fun sx(x: Float) = x * scaleX

            fun sy(y: Float) = y * scaleY

            val bodyColor = scheme.secondaryContainer
            val tabColor = scheme.secondary

            val folderBody =
                Path().apply {
                    moveTo(sx(14f), sy(40f))
                    lineTo(sx(14f), sy(92f))
                    quadraticTo(sx(14f), sy(96f), sx(18f), sy(96f))
                    lineTo(sx(102f), sy(96f))
                    quadraticTo(sx(106f), sy(96f), sx(106f), sy(92f))
                    lineTo(sx(106f), sy(44f))
                    quadraticTo(sx(106f), sy(40f), sx(102f), sy(40f))
                    close()
                }
            drawPath(folderBody, color = bodyColor, style = Fill)

            val folderTab =
                Path().apply {
                    moveTo(sx(14f), sy(40f))
                    lineTo(sx(14f), sy(30f))
                    quadraticTo(sx(14f), sy(26f), sx(18f), sy(26f))
                    lineTo(sx(46f), sy(26f))
                    quadraticTo(sx(49f), sy(26f), sx(51f), sy(29f))
                    lineTo(sx(57f), sy(36f))
                    lineTo(sx(14f), sy(40f))
                    close()
                }
            drawPath(folderTab, color = tabColor, style = Fill)

            val folderTopEdge =
                Path().apply {
                    moveTo(sx(57f), sy(36f))
                    lineTo(sx(102f), sy(36f))
                    quadraticTo(sx(106f), sy(36f), sx(106f), sy(40f))
                    lineTo(sx(106f), sy(44f))
                    lineTo(sx(14f), sy(44f))
                    lineTo(sx(14f), sy(40f))
                    close()
                }
            drawPath(folderTopEdge, color = tabColor, style = Fill)

            val plusColor = scheme.error
            val plusStroke = 5.dp.toPx()
            drawLine(
                color = plusColor,
                start = Offset(sx(60f), sy(56f)),
                end = Offset(sx(60f), sy(80f)),
                strokeWidth = plusStroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = plusColor,
                start = Offset(sx(48f), sy(68f)),
                end = Offset(sx(72f), sy(68f)),
                strokeWidth = plusStroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ExpressiveEmptyBackdrop(
    modifier: Modifier,
    polygon: RoundedPolygon,
    morphTo: RoundedPolygon,
    color: Color,
) {
    val reducedMotion = LocalReducedMotion.current
    val shape =
        if (reducedMotion) {
            RoundedPolygonShape(polygon)
        } else {
            val morph = remember(polygon, morphTo) { Morph(polygon, morphTo) }
            val transition = rememberInfiniteTransition(label = "emptyBackdropMorph")
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 2_500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "emptyBackdropMorphProgress",
            )
            MorphPolygonShape(morph, progress)
        }

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(color),
    )
}
