package dev.bikram.filepipe.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.bikram.filepipe.data.preferences.CuratedColorTriplet
import dev.bikram.filepipe.data.preferences.generateTripletForSeed
import java.util.Locale

/**
 * Parses user-entered hex into [Color]. Optional `#`; supports 3-digit RGB, 6-digit RRGGBB, 8-digit AARRGGBB.
 * Returns null if empty or invalid.
 */
fun parseSeedColorHexToColorOrNull(raw: String): Color? {
    val compact = raw.trim().removePrefix("#").uppercase()
    if (compact.isEmpty() || !compact.all { it in '0'..'9' || it in 'A'..'F' }) {
        return null
    }
    val expanded =
        when (compact.length) {
            3 -> compact.map { char -> "$char$char" }.joinToString("")
            6, 8 -> compact
            else -> return null
        }
    val parsedLong = expanded.toLongOrNull(16) ?: return null
    val argb =
        if (expanded.length == 6) {
            (0xFF000000L or parsedLong).toInt()
        } else {
            parsedLong.toInt()
        }
    return Color(argb)
}

/** Canonical `#RRGGBB` for storage and deduplication; null if [raw] does not parse as a single color. */
fun normalizeSeedHexOrNull(raw: String): String? {
    val color = parseSeedColorHexToColorOrNull(raw) ?: return null
    val rgb = color.toArgb() and 0xFFFFFF
    return String.format(Locale.US, "#%06X", rgb)
}

/** Canonical custom seed, either one `#RRGGBB` or a primary|secondary|tertiary triplet. */
fun normalizeCustomSeedHexOrNull(raw: String): String? {
    if (raw.contains("|")) {
        val parts = raw.split("|")
        val normalizedParts =
            parts.map { part ->
                normalizeSeedHexOrNull(part) ?: return null
            }
        return normalizedParts.joinToString("|")
    }
    return normalizeSeedHexOrNull(raw)
}

fun parseCustomTriplet(activeCustomSeed: String): CuratedColorTriplet? {
    if (activeCustomSeed.isBlank()) return null
    val parts = normalizeCustomSeedHexOrNull(activeCustomSeed)?.split("|") ?: return null
    val primaryColor = parseSeedColorHexToColorOrNull(parts.getOrNull(0).orEmpty()) ?: return null
    if (parts.size >= 3) {
        val secondaryColor = parseSeedColorHexToColorOrNull(parts[1])
        val tertiaryColor = parseSeedColorHexToColorOrNull(parts[2])
        if (secondaryColor != null && tertiaryColor != null) {
            return CuratedColorTriplet(primaryColor, secondaryColor, tertiaryColor)
        }
    }
    return generateTripletForSeed(primaryColor)
}
