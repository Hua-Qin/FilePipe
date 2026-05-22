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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
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
private const val TRASH_ARTBOARD = 220f

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeColoredEmptyTrashIllustration(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.size(width = 132.dp, height = 132.dp).clearAndSetSemantics { }) {
        ExpressiveEmptyBackdrop(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .size(56.dp),
            polygon = MaterialShapes.Sunny,
            morphTo = MaterialShapes.Cookie9Sided,
            color = scheme.errorContainer.copy(alpha = 0.42f),
        )
        ExpressiveEmptyBackdrop(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .size(46.dp),
            polygon = MaterialShapes.Cookie9Sided,
            morphTo = MaterialShapes.Sunny,
            color = scheme.tertiaryContainer.copy(alpha = 0.52f),
        )
        Canvas(Modifier.matchParentSize()) {
            val scaleX = size.width / TRASH_ARTBOARD
            val scaleY = size.height / TRASH_ARTBOARD
            val strokeWidth = 1.4.dp.toPx()
            val outlineColor = scheme.outline.copy(alpha = 0.55f)
            val ribColor = scheme.onSurfaceVariant.copy(alpha = 0.30f)
            val shadowColor = scheme.scrim.copy(alpha = 0.10f)

            val binPath =
                Path().apply {
                    moveTo(52f * scaleX, 66f * scaleY)
                    lineTo(168f * scaleX, 66f * scaleY)
                    lineTo(162f * scaleX, 184f * scaleY)
                    quadraticTo(160f * scaleX, 192f * scaleY, 154f * scaleX, 192f * scaleY)
                    lineTo(66f * scaleX, 192f * scaleY)
                    quadraticTo(60f * scaleX, 192f * scaleY, 58f * scaleX, 184f * scaleY)
                    close()
                }
            translate(left = 4f * scaleX, top = 6f * scaleY) {
                drawPath(binPath, color = shadowColor, style = Fill)
            }
            drawPath(binPath, color = scheme.surfaceContainerHigh, style = Fill)
            drawPath(binPath, color = outlineColor, style = Stroke(width = strokeWidth))

            drawLine(
                color = ribColor,
                start = Offset(86f * scaleX, 82f * scaleY),
                end = Offset(88f * scaleX, 180f * scaleY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = ribColor,
                start = Offset(110f * scaleX, 80f * scaleY),
                end = Offset(110f * scaleX, 182f * scaleY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = ribColor,
                start = Offset(134f * scaleX, 82f * scaleY),
                end = Offset(132f * scaleX, 180f * scaleY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            drawRoundRect(
                color = scheme.errorContainer.copy(alpha = 0.78f),
                topLeft = Offset(44f * scaleX, 52f * scaleY),
                size = Size(132f * scaleX, 14f * scaleY),
                cornerRadius = CornerRadius(7f * scaleX, 7f * scaleY),
                style = Fill,
            )
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(44f * scaleX, 52f * scaleY),
                size = Size(132f * scaleX, 14f * scaleY),
                cornerRadius = CornerRadius(7f * scaleX, 7f * scaleY),
                style = Stroke(width = strokeWidth),
            )
            drawRoundRect(
                color = scheme.onErrorContainer.copy(alpha = 0.55f),
                topLeft = Offset(98f * scaleX, 42f * scaleY),
                size = Size(24f * scaleX, 10f * scaleY),
                cornerRadius = CornerRadius(5f * scaleX, 5f * scaleY),
                style = Fill,
            )

            drawSparkle(Offset(192f * scaleX, 56f * scaleY), 9f * scaleX, scheme.tertiary)
            drawSparkle(Offset(28f * scaleX, 78f * scaleY), 6f * scaleX, scheme.primary.copy(alpha = 0.78f))
            drawSparkle(Offset(200f * scaleX, 138f * scaleY), 5f * scaleX, scheme.tertiary.copy(alpha = 0.70f))
        }
    }
}

private fun DrawScope.drawSparkle(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val waist = radius * 0.32f
    val sparkle =
        Path().apply {
            moveTo(center.x, center.y - radius)
            quadraticTo(center.x + waist, center.y - waist, center.x + radius, center.y)
            quadraticTo(center.x + waist, center.y + waist, center.x, center.y + radius)
            quadraticTo(center.x - waist, center.y + waist, center.x - radius, center.y)
            quadraticTo(center.x - waist, center.y - waist, center.x, center.y - radius)
            close()
        }
    drawPath(sparkle, color = color, style = Fill)
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
