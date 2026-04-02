package dev.bikram.filepipe.data.preferences

enum class SwipeAction {
    EDIT, DELETE, DUPLICATE, PREVIEW, VIEW_HISTORY
}

data class AppPreferences(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val useMaterialYou: Boolean = false,
    val exportFolderUri: String = "",
    val autoExportOnRuleChange: Boolean = false,
    val scheduledExportEnabled: Boolean = false,
    val logRetentionDays: Int = 30,
    val swipeStartToEnd: SwipeAction = SwipeAction.DUPLICATE,
    val swipeEndToStart: SwipeAction = SwipeAction.DELETE,
    val bookmarkedFolders: List<String> = emptyList(),
    val hasSeenIntro: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val progressiveBlurEnabled: Boolean = true
) {
    companion object {
        val DEFAULT = AppPreferences()
    }
}
