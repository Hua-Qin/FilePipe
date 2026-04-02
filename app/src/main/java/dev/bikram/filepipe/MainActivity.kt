package dev.bikram.filepipe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import dev.bikram.filepipe.data.preferences.AppPreferences
import dev.bikram.filepipe.data.preferences.AppThemeMode
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.domain.usecase.RulesAutoExportTrigger
import dev.bikram.filepipe.shortcuts.AppShortcutsManager
import dev.bikram.filepipe.shortcuts.PendingShortcutRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import dev.bikram.filepipe.ui.navigation.AppNavigation
import dev.bikram.filepipe.ui.theme.FilePipeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var pendingShortcutRepository: PendingShortcutRepository

    @Inject
    lateinit var rulesAutoExportTrigger: RulesAutoExportTrigger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShortcutIntent(intent)

        setContent {
            val preferences by userPreferencesRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = AppPreferences.DEFAULT)

            val hasSeenIntro by userPreferencesRepository.preferencesFlow
                .map { it.hasSeenIntro }
                .collectAsStateWithLifecycle(initialValue = null)

            SideEffect {
                val nightMode = when (preferences.themeMode) {
                    AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    AppThemeMode.DARK, AppThemeMode.BLACK -> AppCompatDelegate.MODE_NIGHT_YES
                    AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }

            FilePipeTheme(
                themeMode = preferences.themeMode,
                colorSource = preferences.colorSource,
                themePaletteStyle = preferences.themePaletteStyle,
                hapticFeedbackEnabled = preferences.hapticFeedbackEnabled
            ) {
                if (hasSeenIntro != null) {
                    AppNavigation(
                        hasSeenIntro = hasSeenIntro!!,
                        preferences = preferences
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShortcutIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            rulesAutoExportTrigger.flushIfPending()
        }
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val ruleId = intent?.getLongExtra(AppShortcutsManager.EXTRA_SHORTCUT_RULE_ID, -1L) ?: -1L
        if (ruleId != -1L) {
            pendingShortcutRepository.requestRunRule(ruleId)
        }
    }
}

