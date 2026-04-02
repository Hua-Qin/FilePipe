package dev.bikram.filepipe.data.preferences

enum class ThemePaletteStyle {
    TONAL_SPOT,
    NEUTRAL,
    VIBRANT,
    EXPRESSIVE,
    RAINBOW,
    FRUIT_SALAD,
    MONOCHROME,
    FIDELITY,
    CONTENT;

    companion object {
        val all: List<ThemePaletteStyle> = entries
    }
}
