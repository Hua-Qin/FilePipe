package dev.bikram.filepipe.data.preferences

data class AppPreferences(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val useMaterialYou: Boolean = false,
    val exportFolderUri: String = "",
    val autoExportOnRuleChange: Boolean = false,
    val scheduledExportEnabled: Boolean = false
) {
    companion object {
        val DEFAULT = AppPreferences()
    }
}
