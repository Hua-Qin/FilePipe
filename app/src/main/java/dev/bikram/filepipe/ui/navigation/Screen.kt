package dev.bikram.filepipe.ui.navigation

sealed class Screen(val route: String) {
    data object Rules : Screen("rules")
    data object History : Screen("history")
    data object Settings : Screen("settings")

    data object RuleDetail : Screen("rule_detail/{ruleId}") {
        fun createRoute(ruleId: Long = NEW_RULE_ID) = "rule_detail/$ruleId"
        const val ARG_RULE_ID = "ruleId"
        const val NEW_RULE_ID = -1L
    }

    data object HistoryDetail : Screen("history_detail/{historyId}") {
        fun createRoute(historyId: Long) = "history_detail/$historyId"
        const val ARG_HISTORY_ID = "historyId"
    }

    data object HistoryForRule : Screen("history_for_rule/{ruleId}") {
        fun createRoute(ruleId: Long) = "history_for_rule/$ruleId"
        const val ARG_RULE_ID = "ruleId"
    }

    data object OnboardingTitle : Screen("onboarding_title?fromSettings={fromSettings}") {
        fun createRoute(fromSettings: Boolean = false) = "onboarding_title?fromSettings=$fromSettings"
        const val ARG = "fromSettings"
    }

    data object OnboardingPrefs : Screen("onboarding_prefs")
}
