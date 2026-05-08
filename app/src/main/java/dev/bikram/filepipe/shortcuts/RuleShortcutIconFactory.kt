package dev.bikram.filepipe.shortcuts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.PathParser
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RuleIcon
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt

class RuleShortcutIconFactory
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun createIcon(rule: Rule): IconCompat = IconCompat.createWithAdaptiveBitmap(createBitmap(rule))

        private fun createBitmap(rule: Rule): Bitmap {
            val density = context.resources.displayMetrics.density
            val iconSizePixels = (ICON_SIZE_DP * density).roundToInt()
            val bitmap = Bitmap.createBitmap(iconSizePixels, iconSizePixels, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            canvas.drawColor(SHORTCUT_BACKGROUND_COLOR)

            val emoji = rule.iconEmoji?.trim()?.takeIf { it.isNotEmpty() }
            if (emoji != null) {
                drawEmoji(canvas, emoji, iconSizePixels, density)
            } else {
                drawRuleIcon(canvas, rule.icon, iconSizePixels, density)
            }

            return bitmap
        }

        private fun drawEmoji(
            canvas: Canvas,
            emoji: String,
            iconSizePixels: Int,
            density: Float,
        ) {
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                    color = SHORTCUT_GLYPH_COLOR
                    textAlign = Paint.Align.CENTER
                    textSize = EMOJI_SIZE_DP * density
                }
            val fontMetrics = paint.fontMetrics
            val center = iconSizePixels / 2f
            val baseline = center - (fontMetrics.ascent + fontMetrics.descent) / 2f

            canvas.drawText(emoji, center, baseline, paint)
        }

        private fun drawRuleIcon(
            canvas: Canvas,
            ruleIcon: RuleIcon,
            iconSizePixels: Int,
            density: Float,
        ) {
            val iconPath = PathParser.createPathFromPathData(ruleIcon.toPathData())
            val pathBounds = RectF()
            iconPath.computeBounds(pathBounds, true)

            val glyphSizePixels = GLYPH_SIZE_DP * density
            val scale = min(glyphSizePixels / pathBounds.width(), glyphSizePixels / pathBounds.height())
            val center = iconSizePixels / 2f
            val scaledWidth = pathBounds.width() * scale
            val scaledHeight = pathBounds.height() * scale
            val translateX = center - scaledWidth / 2f - pathBounds.left * scale
            val translateY = center - scaledHeight / 2f - pathBounds.top * scale

            val matrix =
                Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(translateX, translateY)
                }
            iconPath.transform(matrix)

            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = SHORTCUT_GLYPH_COLOR
                    style = Paint.Style.FILL
                }
            canvas.drawPath(iconPath, paint)
        }

        private fun RuleIcon.toPathData(): String =
            when (this) {
                RuleIcon.DEFAULT ->
                    "M10 4H2c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h20c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2H12l-2-2zM18.94 17 17 15.84 15.06 17l.52-2.21-1.71-1.49 2.26-.19L17 11.03l.87 2.08 2.26.19-1.71 1.49.52 2.21z"

                RuleIcon.IMAGE ->
                    "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 11.5l2.5 3.01L14.5 10l4.5 6H5l3.5-4.5z"

                RuleIcon.SCREENSHOT ->
                    "M20 3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM8 19H5c-.55 0-1-.45-1-1v-3h2v2h2v2zm0-12H6v2H4V6c0-.55.45-1 1-1h3v2zm6 12h-4v-2h4v2zm0-12h-4V5h4v2zm6 11c0 .55-.45 1-1 1h-3v-2h2v-2h2v3zm0-9h-2V7h-2V5h3c.55 0 1 .45 1 1v3z"

                RuleIcon.VIDEO ->
                    "M18 4l2 4h-3l-2-4h3zm-4 0l2 4h-3l-2-4h3zm-4 0l2 4H9L7 4h3zM6 4l2 4H5L3 4h3zm-2 6v10h16V10H4z"

                RuleIcon.MUSIC ->
                    "M12 3v10.55A4 4 0 1 0 14 17V7h4V3h-6z"

                RuleIcon.DOWNLOAD ->
                    "M5 20h14v-2H5v2zM19 9h-4V3H9v6H5l7 7 7-7z"

                RuleIcon.DOCUMENT ->
                    "M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zM13 9V3.5L18.5 9H13zm3 9H8v-2h8v2zm0-4H8v-2h8v2z"

                RuleIcon.INSTALLABLE ->
                    "M17.6 9.48l1.84-3.18c.16-.31.04-.69-.26-.85-.29-.15-.65-.06-.83.22l-1.88 3.24c-1.35-.6-2.86-.94-4.47-.94s-3.12.34-4.47.94L5.65 5.67c-.19-.29-.58-.38-.87-.2-.28.18-.37.54-.22.83L6.4 9.48C3.3 11.25 1.28 14.44 1 18h22c-.28-3.56-2.3-6.75-5.4-8.52zM7 15.25c-.69 0-1.25-.56-1.25-1.25s.56-1.25 1.25-1.25 1.25.56 1.25 1.25-.56 1.25-1.25 1.25zm10 0c-.69 0-1.25-.56-1.25-1.25s.56-1.25 1.25-1.25 1.25.56 1.25 1.25-.56 1.25-1.25 1.25z"
            }

        private companion object {
            private const val ICON_SIZE_DP = 108f
            private const val GLYPH_SIZE_DP = 54f
            private const val EMOJI_SIZE_DP = 50f
            private const val SHORTCUT_BACKGROUND_COLOR = 0xFF001C38.toInt()
            private const val SHORTCUT_GLYPH_COLOR = 0xFFD4E3FF.toInt()
        }
    }
