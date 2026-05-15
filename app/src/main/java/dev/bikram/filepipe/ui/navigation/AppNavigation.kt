package dev.bikram.filepipe.ui.navigation

import android.app.Activity
import android.os.Build
import android.text.format.Formatter
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.bikram.filepipe.BuildConfig
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.preferences.AppPreferences
import dev.bikram.filepipe.data.preferences.AppThemeMode
import dev.bikram.filepipe.data.preferences.UpdateCheckSchedule
import dev.bikram.filepipe.shortcuts.PendingShortcutRepository
import dev.bikram.filepipe.ui.components.FilePipeButton
import dev.bikram.filepipe.ui.components.FilePipeFloatingActionButton
import dev.bikram.filepipe.ui.components.FilePipeIconButton
import dev.bikram.filepipe.ui.components.FilePipeTextButton
import dev.bikram.filepipe.ui.components.SwipeDismissableUpdatePromoBanner
import dev.bikram.filepipe.ui.feedback.rememberPlayTapSound
import dev.bikram.filepipe.ui.screens.help.FaqScreen
import dev.bikram.filepipe.ui.screens.history.HistoryScreen
import dev.bikram.filepipe.ui.screens.history.HistoryViewModel
import dev.bikram.filepipe.ui.screens.historydetail.HistoryDetailScreen
import dev.bikram.filepipe.ui.screens.onboarding.OnboardingPermissionsScreen
import dev.bikram.filepipe.ui.screens.onboarding.OnboardingRuleWizardScreen
import dev.bikram.filepipe.ui.screens.onboarding.OnboardingTitleScreen
import dev.bikram.filepipe.ui.screens.ruledetail.RuleDetailScreen
import dev.bikram.filepipe.ui.screens.rules.RulesScreen
import dev.bikram.filepipe.ui.screens.settings.SettingsBringIntoViewSection
import dev.bikram.filepipe.ui.screens.settings.SettingsScreen
import dev.bikram.filepipe.ui.screens.settings.SettingsViewModel
import dev.bikram.filepipe.ui.screens.settings.launchAppShareChooser
import dev.bikram.filepipe.ui.theme.LocalProgressiveBlurEnabled
import dev.bikram.filepipe.ui.theme.LocalProgressiveBlurStyle
import dev.bikram.filepipe.ui.theme.LocalReducedMotion
import dev.bikram.filepipe.ui.theme.LocalUseGradientBackground
import dev.bikram.filepipe.ui.theme.ProgressiveBlurStyle
import dev.bikram.filepipe.ui.theme.RoundedPolygonShape
import dev.bikram.filepipe.ui.theme.reducedMotionAwareSpec
import dev.bikram.filepipe.update.PlayInAppUpdateBannerUiState

private data class BottomNavItem(
    val screen: Screen,
    val label: Int,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit,
)

private val bottomNavItems =
    listOf(
        BottomNavItem(
            screen = Screen.Rules,
            label = R.string.nav_rules,
            selectedIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
            unselectedIcon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
        ),
        BottomNavItem(
            screen = Screen.History,
            label = R.string.nav_history,
            selectedIcon = { Icon(Icons.Filled.History, contentDescription = null) },
            unselectedIcon = { Icon(Icons.Outlined.History, contentDescription = null) },
        ),
        BottomNavItem(
            screen = Screen.Settings,
            label = R.string.nav_settings,
            selectedIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            unselectedIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
        ),
    )

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3AdaptiveNavigationSuiteApi::class,
)
@Composable
fun AppNavigation(
    hasSeenIntro: Boolean = true,
    introSeenAtLaunch: Boolean = hasSeenIntro,
    preferences: AppPreferences = AppPreferences(),
    pendingShortcutRepository: PendingShortcutRepository,
) {
    val playTap = rememberPlayTapSound()
    val navController = rememberNavController()
    val pendingHistoryId by pendingShortcutRepository.pendingHistoryDetailId.collectAsStateWithLifecycle()
    val pendingOpenSettingsUpdates by pendingShortcutRepository.pendingOpenSettingsForUpdates.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar =
        bottomNavItems.any {
            currentDestination?.hierarchy?.any { destination -> destination.route == it.screen.route } == true
        }
    val navigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    val useAdaptiveNavigationRail =
        showBottomBar && navigationSuiteType != NavigationSuiteType.NavigationBar
    val showFloatingBottomBar = showBottomBar && !useAdaptiveNavigationRail

    val isRuleDetailRoute =
        currentDestination?.hierarchy?.any { destination ->
            destination.route == Screen.RuleDetail.route
        } == true
    val isHistoryDetailRoute =
        currentDestination?.hierarchy?.any { destination ->
            destination.route == Screen.HistoryDetail.route
        } == true
    val isFaqRoute =
        currentDestination?.hierarchy?.any { destination ->
            destination.route == Screen.Faq.route
        } == true

    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val floatingBarHeight = 64.dp
    val scrimHeight = navBarInset + floatingBarHeight + 24.dp

    /** Extra top blur under History filter chips (must match [isHistoryFilterRoute] detection). */
    val historyFilterChipsBand = 96.dp
    val isHistoryFilterRoute =
        currentDestination?.hierarchy?.any { destination ->
            destination.route == Screen.History.route ||
                (destination.route?.startsWith("history_for_rule") == true)
        } == true
    val fullScreenBottomBlurShort = navBarInset + 48.dp
    val fullScreenBottomBlurRuleEdit = navBarInset + 88.dp
    val bottomBlurHeightDp =
        when {
            isFaqRoute -> 0.dp
            showFloatingBottomBar -> scrimHeight
            showBottomBar -> navBarInset + 96.dp
            isRuleDetailRoute -> fullScreenBottomBlurRuleEdit
            else -> fullScreenBottomBlurShort
        }
    val primaryTabContentPadding =
        PaddingValues(bottom = if (showFloatingBottomBar) scrimHeight else navBarInset + 96.dp)
    val density = LocalDensity.current
    val bottomBlurHeightPx = with(density) { bottomBlurHeightDp.toPx() }

    // Activity-scoped VMs for nav bar FAB actions
    val historyVm: HistoryViewModel = hiltViewModel()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val hasAnyHistory by historyVm.hasAnyHistory.collectAsStateWithLifecycle()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotion.current

    // Lock from disk-backed snapshot (MainActivity) so the first frame does not use
    // AppPreferences.DEFAULT.hasSeenIntro (false) and force onboarding on every cold start.
    // Still stable for the activity so live hasSeenIntro updates do not change startDestination.
    val lockedNavStartDestination =
        remember(introSeenAtLaunch) {
            if (introSeenAtLaunch) Screen.Rules.route else Screen.OnboardingTitle.route
        }

    LaunchedEffect(hasSeenIntro, preferences.updateCheckSchedule) {
        if (hasSeenIntro &&
            BuildConfig.SHOW_UPDATES &&
            preferences.updateCheckSchedule == UpdateCheckSchedule.AT_APP_START
        ) {
            settingsVm.checkForUpdate(silent = true)
        }
    }

    LaunchedEffect(hasSeenIntro, pendingOpenSettingsUpdates, navController) {
        if (!pendingOpenSettingsUpdates || !hasSeenIntro) return@LaunchedEffect
        navController.navigate(Screen.Settings.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        settingsVm.flagOpenUpdateSheetFromNotification()
        pendingShortcutRepository.clearPendingOpenSettingsForUpdates()
    }

    LaunchedEffect(hasSeenIntro, pendingHistoryId, navController) {
        val historyId = pendingHistoryId ?: return@LaunchedEffect
        if (!hasSeenIntro) return@LaunchedEffect
        navController.navigate(Screen.HistoryDetail.createRoute(historyId)) {
            launchSingleTop = true
        }
        pendingShortcutRepository.clearPendingHistoryDetail()
    }

    val updateInfo by settingsVm.updateInfo.collectAsStateWithLifecycle()
    val updatePromoDismissed by settingsVm.updatePromoBannerDismissedThisSession.collectAsStateWithLifecycle()
    val playBannerState by settingsVm.playInAppUpdateBannerUiState.collectAsStateWithLifecycle()
    val navRoute = navBackStackEntry?.destination?.route
    val primaryTabRoute =
        navRoute != null &&
            (
                navRoute == Screen.Rules.route ||
                    navRoute == Screen.History.route ||
                    navRoute == Screen.Settings.route
            )
    val showGlobalUpdatePromoBanner =
        BuildConfig.SHOW_UPDATES &&
            updateInfo != null &&
            !updatePromoDismissed &&
            playBannerState is PlayInAppUpdateBannerUiState.Hidden &&
            primaryTabRoute

    val showPlayUpdateBanner = playBannerState != PlayInAppUpdateBannerUiState.Hidden
    val primaryTabTopBannerActive =
        (showPlayUpdateBanner && primaryTabRoute) || showGlobalUpdatePromoBanner
    val primaryTabTopBannerBlurInsetDp = if (primaryTabTopBannerActive) 100.dp else 0.dp
    val topBlurSmallChrome = statusBarInset + 96.dp + primaryTabTopBannerBlurInsetDp
    val topBlurHeightDp =
        if (isHistoryFilterRoute) {
            topBlurSmallChrome + historyFilterChipsBand
        } else {
            topBlurSmallChrome
        }
    val topBlurHeightPx = with(density) { topBlurHeightDp.toPx() }
    val progressiveBlurStyle: ProgressiveBlurStyle? =
        if (!preferences.progressiveBlurEnabled) {
            null
        } else {
            val blurRadius =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    when {
                        isRuleDetailRoute || isFaqRoute -> 56f
                        isHistoryFilterRoute -> 54f
                        else -> 44f
                    }
                } else {
                    0f
                }
            val overlayAlpha =
                when {
                    isRuleDetailRoute -> 0.38f
                    isFaqRoute -> 0.38f
                    isHistoryDetailRoute -> 0.30f
                    isHistoryFilterRoute -> 0.52f
                    navRoute == Screen.Rules.route -> 0.46f
                    navRoute == Screen.Settings.route -> 0.40f
                    else -> 0.34f
                }
            val primaryTabBottomBlurBoost =
                showFloatingBottomBar &&
                    (
                        navRoute == Screen.Rules.route ||
                            navRoute == Screen.History.route ||
                            navRoute == Screen.Settings.route
                    )
            val overlayAlphaBottom =
                if (primaryTabBottomBlurBoost) {
                    (overlayAlpha + 0.14f).coerceAtMost(0.58f)
                } else {
                    overlayAlpha
                }
            ProgressiveBlurStyle(
                topHeightPx = topBlurHeightPx,
                bottomHeightPx = bottomBlurHeightPx,
                blurRadius = blurRadius,
                overlayAlpha = overlayAlpha,
                overlayAlphaBottom = overlayAlphaBottom,
            )
        }

    val currentTab =
        bottomNavItems
            .find { item ->
                currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
            }?.screen

    if (showClearHistoryDialog && hasAnyHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = { Text(stringResource(R.string.history_clear_confirm_message)) },
            confirmButton = {
                FilePipeTextButton(onClick = {
                    showClearHistoryDialog = false
                    historyVm.clearAllHistory()
                }) { Text(stringResource(R.string.history_clear)) }
            },
            dismissButton = {
                FilePipeTextButton(onClick = {
                    showClearHistoryDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    val effectiveUseGradientBackground =
        preferences.useGradientBackground && preferences.themeMode != AppThemeMode.BLACK
    CompositionLocalProvider(
        LocalUseGradientBackground provides effectiveUseGradientBackground,
        LocalProgressiveBlurEnabled provides preferences.progressiveBlurEnabled,
        LocalProgressiveBlurStyle provides progressiveBlurStyle,
    ) {
        val context = LocalContext.current
        CompositionLocalProvider(LocalPrimaryTabTopBannerActive provides primaryTabTopBannerActive) {
            CompositionLocalProvider(
                LocalPrimaryTabTopBanner provides {
                    if (primaryTabTopBannerActive) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                                .consumeWindowInsets(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
                        ) {
                            if (showPlayUpdateBanner && primaryTabRoute) {
                                PlayStoreGlobalUpdateBanner(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp),
                                    state = playBannerState,
                                    onInstallClick = {
                                        val activity = context as? Activity
                                        settingsVm.completePlayFlexibleUpdateIfReady(activity)
                                    },
                                )
                            }
                            if (showGlobalUpdatePromoBanner) {
                                SwipeDismissableUpdatePromoBanner(
                                    onDismiss = { settingsVm.dismissUpdatePromoBanner() },
                                    onOpenSettingsClick = {
                                        settingsVm.flagOpenUpdateSheetFromRulesPromo()
                                        navController.navigate(Screen.Settings.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
            ) {
                val navigationContent: @Composable () -> Unit = {
                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            NavHost(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(Color.Transparent),
                                navController = navController,
                                startDestination = lockedNavStartDestination,
                                enterTransition = {
                                    if (reducedMotion) {
                                        fadeIn(animationSpec = tween(durationMillis = 0))
                                    } else {
                                        slideInHorizontally { it } + fadeIn()
                                    }
                                },
                                exitTransition = {
                                    if (reducedMotion) {
                                        fadeOut(animationSpec = tween(durationMillis = 0))
                                    } else {
                                        slideOutHorizontally { -it / 3 } + fadeOut()
                                    }
                                },
                                popEnterTransition = {
                                    if (reducedMotion) {
                                        fadeIn(animationSpec = tween(durationMillis = 0))
                                    } else {
                                        slideInHorizontally { -it } + fadeIn()
                                    }
                                },
                                popExitTransition = {
                                    if (reducedMotion) {
                                        fadeOut(animationSpec = tween(durationMillis = 0))
                                    } else {
                                        slideOutHorizontally { it } + fadeOut()
                                    }
                                },
                            ) {
                                composable(
                                    route = Screen.OnboardingTitle.route,
                                ) {
                                    OnboardingTitleScreen(
                                        onLetsBegan = {
                                            navController.navigate(Screen.OnboardingPermissions.route)
                                        },
                                    )
                                }

                                composable(Screen.OnboardingPermissions.route) {
                                    OnboardingPermissionsScreen(
                                        onContinue = {
                                            navController.navigate(Screen.OnboardingRuleWizard.route) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onOpenStorageAccessFaq = {
                                            navController.navigate(Screen.Faq.createRoute(Screen.Faq.FOCUS_STORAGE_ACCESS))
                                        },
                                    )
                                }

                                composable(Screen.OnboardingRuleWizard.route) {
                                    OnboardingRuleWizardScreen(
                                        onBackToPermissions = {
                                            navController.popBackStack()
                                        },
                                        onUseTemplate = { templateIndex ->
                                            navController.navigate(Screen.Rules.route) {
                                                popUpTo(navController.graph.id) { inclusive = true }
                                            }
                                            navController.navigate(
                                                Screen.RuleDetail.createRoute(
                                                    templateIndex = templateIndex,
                                                    skipTemplatePicker = true,
                                                ),
                                            )
                                            settingsVm.markIntroSeen()
                                        },
                                        onStartBlank = {
                                            navController.navigate(Screen.Rules.route) {
                                                popUpTo(navController.graph.id) { inclusive = true }
                                            }
                                            navController.navigate(
                                                Screen.RuleDetail.createRoute(skipTemplatePicker = true),
                                            )
                                            settingsVm.markIntroSeen()
                                        },
                                        onSkip = {
                                            navController.navigate(Screen.Rules.route) {
                                                popUpTo(navController.graph.id) { inclusive = true }
                                            }
                                            settingsVm.markIntroSeen()
                                        },
                                    )
                                }

                                composable(Screen.Rules.route) {
                                    RulesScreen(
                                        contentPadding = primaryTabContentPadding,
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
                                        },
                                    )
                                }
                                composable(
                                    route = Screen.RuleDetail.route,
                                    arguments =
                                        listOf(
                                            navArgument(Screen.RuleDetail.ARG_RULE_ID) {
                                                type = NavType.LongType
                                            },
                                            navArgument(Screen.RuleDetail.ARG_TEMPLATE_INDEX) {
                                                type = NavType.IntType
                                                defaultValue = -1
                                            },
                                            navArgument(Screen.RuleDetail.ARG_SKIP_TEMPLATE_PICKER) {
                                                type = NavType.BoolType
                                                defaultValue = false
                                            },
                                        ),
                                ) {
                                    RuleDetailScreen(
                                        onNavigateBack = { navController.popBackStack() },
                                        onOpenFaq = { navController.navigate(Screen.Faq.createRoute()) },
                                    )
                                }
                                composable(Screen.History.route) {
                                    HistoryScreen(
                                        contentPadding = primaryTabContentPadding,
                                        onHistoryClick = { historyId ->
                                            navController.navigate(Screen.HistoryDetail.createRoute(historyId))
                                        },
                                    )
                                }
                                composable(Screen.Settings.route) {
                                    SettingsScreen(
                                        contentPadding = primaryTabContentPadding,
                                        onOpenIntro = {
                                            navController.navigate(Screen.OnboardingTitle.route)
                                        },
                                        onOpenFaqStorageSection = {
                                            navController.navigate(Screen.Faq.createRoute(Screen.Faq.FOCUS_STORAGE_ACCESS))
                                        },
                                        onOpenHelp = {
                                            navController.navigate(Screen.Faq.createRoute())
                                        },
                                        viewModel = settingsVm,
                                    )
                                }
                                composable(
                                    route = Screen.Faq.route,
                                    arguments =
                                        listOf(
                                            navArgument(Screen.Faq.ARG_FOCUS_SECTION) {
                                                type = NavType.StringType
                                                defaultValue = ""
                                            },
                                        ),
                                ) { backStackEntry ->
                                    val focusSection =
                                        backStackEntry.arguments?.getString(Screen.Faq.ARG_FOCUS_SECTION).orEmpty()
                                    val goToSettingsFromFaq: () -> Unit = {
                                        val poppedToExistingSettings =
                                            navController.popBackStack(Screen.Settings.route, inclusive = false)
                                        if (!poppedToExistingSettings) {
                                            navController.popBackStack()
                                            navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                                        }
                                    }
                                    val openFolderAccessInSettings: () -> Unit = {
                                        settingsVm.requestFolderAccessSectionHighlight()
                                        settingsVm.requestBringSettingsSectionIntoView(
                                            SettingsBringIntoViewSection.FolderAccess,
                                        )
                                        goToSettingsFromFaq()
                                    }
                                    FaqScreen(
                                        initialFocusSectionId = focusSection,
                                        onNavigateBack = { navController.popBackStack() },
                                        onOpenFolderAccessInSettings = openFolderAccessInSettings,
                                        onOpenSettingsNotifications = {
                                            settingsVm.requestNotificationsSectionHighlight()
                                            settingsVm.requestBringSettingsSectionIntoView(
                                                SettingsBringIntoViewSection.Notifications,
                                            )
                                            goToSettingsFromFaq()
                                        },
                                        onOpenAppNotificationSettings = {
                                            settingsVm.openAppNotificationSettings()
                                        },
                                    )
                                }
                                composable(
                                    route = Screen.HistoryDetail.route,
                                    arguments =
                                        listOf(
                                            navArgument(Screen.HistoryDetail.ARG_HISTORY_ID) {
                                                type = NavType.LongType
                                            },
                                        ),
                                ) {
                                    HistoryDetailScreen(onNavigateBack = { navController.popBackStack() })
                                }
                                composable(
                                    route = Screen.HistoryForRule.route,
                                    arguments =
                                        listOf(
                                            navArgument(Screen.HistoryForRule.ARG_RULE_ID) {
                                                type = NavType.LongType
                                            },
                                        ),
                                ) {
                                    HistoryScreen(
                                        onHistoryClick = { historyId ->
                                            navController.navigate(Screen.HistoryDetail.createRoute(historyId))
                                        },
                                        onNavigateBack = { navController.popBackStack() },
                                    )
                                }
                            }
                        }

                        if (showFloatingBottomBar) {
                            FloatingNavBar(
                                items = bottomNavItems,
                                currentDestination = currentDestination,
                                onItemClick = { item ->
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                fabContent = {
                                    val hostContext = LocalContext.current
                                    MainNavFabSlot(
                                        currentTab = currentTab,
                                        hasAnyHistory = hasAnyHistory,
                                        onAddRule = {
                                            navController.navigate(Screen.RuleDetail.createRoute())
                                        },
                                        onClearHistory = { showClearHistoryDialog = true },
                                        onShareApp = { launchAppShareChooser(hostContext) },
                                    )
                                },
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                        if (showBottomBar && useAdaptiveNavigationRail) {
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .padding(end = 24.dp, bottom = 24.dp),
                            ) {
                                val hostContext = LocalContext.current
                                MainNavFabSlot(
                                    currentTab = currentTab,
                                    hasAnyHistory = hasAnyHistory,
                                    onAddRule = {
                                        navController.navigate(Screen.RuleDetail.createRoute())
                                    },
                                    onClearHistory = { showClearHistoryDialog = true },
                                    onShareApp = { launchAppShareChooser(hostContext) },
                                )
                            }
                        }
                    }
                }
                if (showBottomBar && useAdaptiveNavigationRail) {
                    NavigationSuiteScaffold(
                        layoutType = navigationSuiteType,
                        containerColor = Color.Transparent,
                        navigationSuiteItems = {
                            bottomNavItems.forEach { navItem ->
                                val selected =
                                    currentDestination
                                        ?.hierarchy
                                        ?.any { it.route == navItem.screen.route } == true
                                item(
                                    selected = selected,
                                    onClick = {
                                        playTap()
                                        navController.navigate(navItem.screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        if (selected) navItem.selectedIcon() else navItem.unselectedIcon()
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(navItem.label),
                                            style = MaterialTheme.typography.labelLargeEmphasized,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    },
                                )
                            }
                        },
                    ) {
                        navigationContent()
                    }
                } else {
                    navigationContent()
                }
            }
        }
    }
}

@Composable
private fun PlayStoreGlobalUpdateBanner(
    modifier: Modifier = Modifier,
    state: PlayInAppUpdateBannerUiState,
    onInstallClick: () -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val expressiveShape = RoundedCornerShape(28.dp)
    when (state) {
        is PlayInAppUpdateBannerUiState.Hidden -> Unit
        is PlayInAppUpdateBannerUiState.Downloading -> {
            OutlinedCard(
                modifier = modifier.fillMaxWidth(),
                shape = expressiveShape,
                border = BorderStroke(1.dp, scheme.outlineVariant),
                colors =
                    CardDefaults.outlinedCardColors(
                        containerColor = scheme.surfaceContainerHigh,
                        contentColor = scheme.onSurface,
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = scheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.play_update_banner_downloading_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        val progressLabel =
                            if (state.indeterminateProgress) {
                                stringResource(R.string.play_update_banner_downloading)
                            } else {
                                val downloaded = Formatter.formatFileSize(context, state.bytesDownloaded)
                                val total = Formatter.formatFileSize(context, state.totalBytesToDownload)
                                stringResource(R.string.play_update_banner_downloading_bytes, downloaded, total)
                            }
                        Text(
                            text = progressLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (state.indeterminateProgress) {
                            LinearProgressIndicator(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(expressiveShape),
                                color = scheme.primary,
                                trackColor = scheme.surfaceContainerHighest,
                            )
                        } else if (state.totalBytesToDownload > 0L) {
                            val fraction =
                                (state.bytesDownloaded.toFloat() / state.totalBytesToDownload.toFloat())
                                    .coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(expressiveShape),
                                color = scheme.primary,
                                trackColor = scheme.surfaceContainerHighest,
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(expressiveShape),
                                color = scheme.primary,
                                trackColor = scheme.surfaceContainerHighest,
                            )
                        }
                    }
                }
            }
        }
        PlayInAppUpdateBannerUiState.ReadyToInstall -> {
            OutlinedCard(
                modifier = modifier.fillMaxWidth(),
                shape = expressiveShape,
                border = BorderStroke(1.dp, scheme.outlineVariant),
                colors =
                    CardDefaults.outlinedCardColors(
                        containerColor = scheme.surfaceContainerHigh,
                        contentColor = scheme.onSurface,
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = scheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.play_update_banner_install_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.play_update_banner_install_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FilePipeButton(
                        onClick = onInstallClick,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = scheme.primary,
                                contentColor = scheme.onPrimary,
                            ),
                    ) {
                        Text(
                            text = stringResource(R.string.play_update_banner_install_action),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingNavBar(
    items: List<BottomNavItem>,
    currentDestination: NavDestination?,
    onItemClick: (BottomNavItem) -> Unit,
    fabContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = fabContent,
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 12.dp, start = 24.dp, end = 24.dp),
        colors =
            FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                toolbarContainerColor = MaterialTheme.colorScheme.primary,
                toolbarContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        items.forEachIndexed { index, item ->
            val selected =
                currentDestination
                    ?.hierarchy
                    ?.any { it.route == item.screen.route } == true

            val labelWidth by animateDpAsState(
                targetValue = if (selected) 72.dp else 0.dp,
                animationSpec =
                    reducedMotionAwareSpec(
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    ),
                label = "nav_label_$index",
            )
            val labelString = stringResource(item.label)

            FilePipeIconButton(
                onClick = { onItemClick(item) },
                modifier =
                    Modifier
                        .height(48.dp)
                        .width(48.dp + labelWidth)
                        .semantics { contentDescription = labelString },
                colors =
                    if (selected) {
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = if (selected) 6.dp else 0.dp),
                ) {
                    Box(modifier = Modifier.size(24.dp)) {
                        if (selected) item.selectedIcon() else item.unselectedIcon()
                    }
                    if (labelWidth > 4.dp) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = labelString,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainNavFabSlot(
    currentTab: Screen?,
    hasAnyHistory: Boolean,
    onAddRule: () -> Unit,
    onClearHistory: () -> Unit,
    onShareApp: () -> Unit,
) {
    when (currentTab) {
        Screen.Rules ->
            SimpleNavFab(
                icon = { tint ->
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = tint,
                    )
                },
                description = stringResource(R.string.rules_add_rule),
                enabled = true,
                onClick = onAddRule,
            )

        Screen.History ->
            SimpleNavFab(
                icon = { tint ->
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = tint,
                    )
                },
                description = stringResource(R.string.history_clear),
                enabled = hasAnyHistory,
                onClick = onClearHistory,
            )

        Screen.Settings ->
            SimpleNavFab(
                icon = { tint ->
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = tint,
                    )
                },
                description = stringResource(R.string.settings_share_app),
                enabled = true,
                onClick = onShareApp,
            )

        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SimpleNavFab(
    icon: @Composable (Color) -> Unit,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val containerColor =
        if (enabled) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    val contentColor =
        if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        }
    val fabShape = remember { RoundedPolygonShape(MaterialShapes.Cookie9Sided) }

    FilePipeFloatingActionButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .size(64.dp)
                .then(
                    if (enabled) {
                        Modifier
                    } else {
                        Modifier
                            .alpha(0.58f)
                            .semantics { disabled() }
                    },
                ),
        shape = fabShape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
        tooltipLabel = description,
    ) {
        Box(modifier = Modifier.semantics { contentDescription = description }) {
            icon(contentColor)
        }
    }
}
