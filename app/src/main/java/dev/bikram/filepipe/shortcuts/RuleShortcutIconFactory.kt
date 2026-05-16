package dev.bikram.filepipe.shortcuts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.R
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RuleIcon
import dev.bikram.filepipe.domain.model.materialSymbolName
import javax.inject.Inject
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
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                    color = SHORTCUT_GLYPH_COLOR
                    textAlign = Paint.Align.CENTER
                    textSize = GLYPH_SIZE_DP * density
                    typeface = ResourcesCompat.getFont(context, R.font.material_symbols_rounded)
                    fontFeatureSettings = "\"rlig\" 1, \"liga\" 1"
                }
            val fontMetrics = paint.fontMetrics
            val center = iconSizePixels / 2f
            val baseline = center - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(ruleIcon.materialSymbolName(), center, baseline, paint)
        }

        private companion object {
            private const val ICON_SIZE_DP = 108f
            private const val GLYPH_SIZE_DP = 54f
            private const val EMOJI_SIZE_DP = 50f
            private const val SHORTCUT_BACKGROUND_COLOR = 0xFF001C38.toInt()
            private const val SHORTCUT_GLYPH_COLOR = 0xFFD4E3FF.toInt()
        }
    }
