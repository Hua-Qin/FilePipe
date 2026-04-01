package dev.bikram.filepipe.data.preferences

enum class SwipeAction {
    DELETE, EDIT, DUPLICATE, VIEW_HISTORY
}

data class AppPreferences(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val useMaterialYou: Boolean = false,
    val exportFolderUri: String = "",
    val autoExportOnRuleChange: Boolean = false,
    val scheduledExportEnabled: Boolean = false,
    val logRetentionDays: Int = 30,
    val swipeStartToEnd: SwipeAction = SwipeAction.DUPLICATE,
    val swipeEndToStart: SwipeAction = SwipeAction.DELETE
) {
    companion object {
        val DEFAULT = AppPreferences()
    }
}
