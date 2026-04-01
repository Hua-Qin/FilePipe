package dev.bikram.filepipe.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.bikram.filepipe.R
import dev.bikram.filepipe.ui.screens.history.HistoryScreen
import dev.bikram.filepipe.ui.screens.historydetail.HistoryDetailScreen
import dev.bikram.filepipe.ui.screens.ruledetail.RuleDetailScreen
import dev.bikram.filepipe.ui.screens.rules.RulesScreen
import dev.bikram.filepipe.ui.screens.settings.SettingsScreen
import dev.bikram.filepipe.ui.feedback.rememberPlayTapSound

private data class BottomNavItem(
    val screen: Screen,
    val label: Int,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
)

private val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Rules,
        label = R.string.nav_rules,
        selectedIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
        unselectedIcon = { Icon(Icons.Outlined.Tune, contentDescription = null) }
    ),
    BottomNavItem(
        screen = Screen.History,
        label = R.string.nav_history,
        selectedIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
        unselectedIcon = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null) }
    ),
    BottomNavItem(
        screen = Screen.Settings,
        label = R.string.nav_settings,
        selectedIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
        unselectedIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
    )
)

@Composable
fun AppNavigation() {
    val playTap = rememberPlayTapSound()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any {
        currentDestination?.hierarchy?.any { destination -> destination.route == it.screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { destination -> destination.route == item.screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                playTap()
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (selected) item.selectedIcon() else item.unselectedIcon()
                            },
                            label = { Text(stringResource(item.label)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Rules.route,
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            composable(Screen.Rules.route) {
                RulesScreen(
                    contentPadding = innerPadding,
                    onCreateRule = { navController.navigate(Screen.RuleDetail.createRoute()) },
                    onEditRule = { ruleId -> navController.navigate(Screen.RuleDetail.createRoute(ruleId)) },
                    onNavigateToHistoryDetail = { historyId ->
                        navController.navigate(Screen.HistoryDetail.createRoute(historyId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHistoryList = {
                        navController.navigate(Screen.History.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToRuleHistory = { ruleId ->
                        navController.navigate(Screen.HistoryForRule.createRoute(ruleId))
                    }
                )
            }
            composable(
                route = Screen.RuleDetail.route,
                arguments = listOf(navArgument(Screen.RuleDetail.ARG_RULE_ID) {
                    type = NavType.LongType
                })
            ) {
                RuleDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    contentPadding = innerPadding,
                    onHistoryClick = { historyId ->
                        navController.navigate(Screen.HistoryDetail.createRoute(historyId))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(contentPadding = innerPadding)
            }
            composable(
                route = Screen.HistoryDetail.route,
                arguments = listOf(navArgument(Screen.HistoryDetail.ARG_HISTORY_ID) {
                    type = NavType.LongType
                })
            ) {
                HistoryDetailScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.HistoryForRule.route,
                arguments = listOf(navArgument(Screen.HistoryForRule.ARG_RULE_ID) {
                    type = NavType.LongType
                })
            ) {
                HistoryScreen(
                    onHistoryClick = { historyId ->
                        navController.navigate(Screen.HistoryDetail.createRoute(historyId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
