package dev.bikram.filepipe.data.preferences

/** Stored theme_mode value from versions that used a fourth "Black" segment in the picker. */
const val LEGACY_BLACK_THEME_MODE_NAME = "BLACK"

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,

    /** Legacy: migrated to [DARK] + [AppPreferences.useBlackTheme]. Kept so [valueOf] can parse old backups. */
    @Deprecated("Use DARK with useBlackTheme")
    BLACK,
}

fun isLegacyBlackThemeModeName(raw: String?): Boolean = raw == LEGACY_BLACK_THEME_MODE_NAME

fun AppThemeMode.migrated(): AppThemeMode =
    if (name == LEGACY_BLACK_THEME_MODE_NAME) {
        AppThemeMode.DARK
    } else {
        this
    }

/** Whether [themeMode] resolves to a dark UI, given current system appearance. */
fun AppThemeMode.effectiveDarkTheme(systemDark: Boolean): Boolean =
    when (name) {
        AppThemeMode.LIGHT.name -> false
        AppThemeMode.DARK.name, LEGACY_BLACK_THEME_MODE_NAME -> true
        else -> systemDark
    }

/** Whether pure-black OLED styling may apply for this mode while the UI is dark. */
fun AppThemeMode.blackThemeEligible(isDarkTheme: Boolean): Boolean =
    when (name) {
        AppThemeMode.LIGHT.name -> false
        AppThemeMode.DARK.name, LEGACY_BLACK_THEME_MODE_NAME -> true
        else -> isDarkTheme
    }
