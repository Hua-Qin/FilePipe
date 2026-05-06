package dev.bikram.filepipe.data.preferences

import androidx.compose.ui.graphics.Color

data class CuratedColorTriplet(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
)

@Suppress("DEPRECATION")
enum class AppColorSource {
    DEFAULT,
    MATERIAL_YOU,

    /** Seed from [AppPreferences.activeCustomSeedHex] (Material Kolor, same as presets). */
    CUSTOM,

    CURATED_EMBER,
    CURATED_GROVE,
    CURATED_HONEY,
    CURATED_OCEAN,
    CURATED_IRIS,
    CURATED_DUSK,
    CURATED_BERRY,

    /** Legacy single-seed presets kept for backup/DataStore compatibility. */
    @Deprecated("Migrated to CURATED_OCEAN")
    PRESET_SAPPHIRE,

    @Deprecated("Migrated to DEFAULT")
    PRESET_EMERALD,

    @Deprecated("Migrated to CURATED_GROVE")
    PRESET_AMBER,

    @Deprecated("Migrated to CURATED_DUSK")
    PRESET_VIOLET,

    @Deprecated("Migrated to CURATED_EMBER")
    PRESET_CORAL,

    @Deprecated("Migrated to CURATED_OCEAN")
    PRESET_TEAL,

    @Deprecated("Migrated to DEFAULT")
    PRESET_LIME,

    @Deprecated("Migrated to CURATED_BERRY")
    PRESET_ROSE,

    @Deprecated("Migrated to DEFAULT")
    PRESET_SLATE,
    ;

    val isSeedBased: Boolean
        get() =
            when (this) {
                CUSTOM,
                DEFAULT,
                CURATED_EMBER,
                CURATED_GROVE,
                CURATED_HONEY,
                CURATED_OCEAN,
                CURATED_IRIS,
                CURATED_DUSK,
                CURATED_BERRY,
                PRESET_SAPPHIRE,
                PRESET_EMERALD,
                PRESET_AMBER,
                PRESET_VIOLET,
                PRESET_CORAL,
                PRESET_TEAL,
                PRESET_LIME,
                PRESET_ROSE,
                PRESET_SLATE,
                -> true
                else -> false
            }

    val supportsPaletteStyle: Boolean
        get() = this != MATERIAL_YOU

    fun curatedTriplet(): CuratedColorTriplet? =
        when (this.migrated()) {
            DEFAULT ->
                CuratedColorTriplet(
                    primary = Color(0xFF16A34A),
                    secondary = Color(0xFF0F766E),
                    tertiary = Color(0xFF84CC16),
                )
            CURATED_EMBER ->
                CuratedColorTriplet(
                    primary = Color(0xFFF97316),
                    secondary = Color(0xFFDC2626),
                    tertiary = Color(0xFFF59E0B),
                )
            CURATED_GROVE ->
                CuratedColorTriplet(
                    primary = Color(0xFF6B8E23),
                    secondary = Color(0xFF0F766E),
                    tertiary = Color(0xFFA16207),
                )
            CURATED_HONEY ->
                CuratedColorTriplet(
                    primary = Color(0xFFFACC15),
                    secondary = Color(0xFFD97706),
                    tertiary = Color(0xFF7C2D12),
                )
            CURATED_OCEAN ->
                CuratedColorTriplet(
                    primary = Color(0xFF0284C7),
                    secondary = Color(0xFF0D9488),
                    tertiary = Color(0xFF2563EB),
                )
            CURATED_IRIS ->
                CuratedColorTriplet(
                    primary = Color(0xFF7C3AED),
                    secondary = Color(0xFF4F46E5),
                    tertiary = Color(0xFFC084FC),
                )
            CURATED_DUSK ->
                CuratedColorTriplet(
                    primary = Color(0xFF6B7280),
                    secondary = Color(0xFFA78BFA),
                    tertiary = Color(0xFFF97316),
                )
            CURATED_BERRY ->
                CuratedColorTriplet(
                    primary = Color(0xFFD946EF),
                    secondary = Color(0xFFBE185D),
                    tertiary = Color(0xFF7C3AED),
                )
            else -> null
        }

    fun seedPrimary(): Color? =
        curatedTriplet()?.primary ?: when (this) {
            PRESET_SAPPHIRE -> Color(0xFF1565C0)
            PRESET_EMERALD -> Color(0xFF2E7D32)
            PRESET_AMBER -> Color(0xFFFF8F00)
            PRESET_VIOLET -> Color(0xFF7B1FA2)
            PRESET_CORAL -> Color(0xFFE53935)
            PRESET_TEAL -> Color(0xFF00796B)
            PRESET_LIME -> Color(0xFFAFB42B)
            PRESET_ROSE -> Color(0xFFE91E63)
            PRESET_SLATE -> Color(0xFF546E7A)
            else -> null
        }

    @Suppress("DEPRECATION")
    fun migrated(): AppColorSource =
        when (this) {
            PRESET_SAPPHIRE, PRESET_TEAL -> CURATED_OCEAN
            PRESET_EMERALD, PRESET_LIME, PRESET_SLATE -> DEFAULT
            PRESET_AMBER -> CURATED_GROVE
            PRESET_VIOLET -> CURATED_DUSK
            PRESET_CORAL -> CURATED_EMBER
            PRESET_ROSE -> CURATED_BERRY
            else -> this
        }

    companion object {
        val accentOptions: List<AppColorSource> =
            listOf(
                MATERIAL_YOU,
                DEFAULT,
                CURATED_EMBER,
                CURATED_GROVE,
                CURATED_HONEY,
                CURATED_OCEAN,
                CURATED_IRIS,
                CURATED_DUSK,
                CURATED_BERRY,
            )
    }
}
