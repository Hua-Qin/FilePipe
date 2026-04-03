package dev.bikram.filepipe.data.preferences

import androidx.compose.ui.graphics.Color

enum class AppColorSource {
    DEFAULT,
    MATERIAL_YOU,
    PRESET_SAPPHIRE,
    PRESET_EMERALD,
    PRESET_AMBER,
    PRESET_VIOLET,
    PRESET_CORAL;

    val isSeedBased: Boolean
        get() = when (this) {
            PRESET_SAPPHIRE, PRESET_EMERALD, PRESET_AMBER, PRESET_VIOLET, PRESET_CORAL -> true
            else -> false
        }

    fun seedPrimary(): Color? = when (this) {
        PRESET_SAPPHIRE -> Color(0xFF1565C0)
        PRESET_EMERALD -> Color(0xFF2E7D32)
        PRESET_AMBER -> Color(0xFFFF8F00)
        PRESET_VIOLET -> Color(0xFF7B1FA2)
        PRESET_CORAL -> Color(0xFFE53935)
        else -> null
    }

    companion object {
        val accentOptions: List<AppColorSource> = listOf(
            DEFAULT,
            MATERIAL_YOU,
            PRESET_SAPPHIRE,
            PRESET_EMERALD,
            PRESET_AMBER,
            PRESET_VIOLET,
            PRESET_CORAL
        )
    }
}
