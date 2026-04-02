package dev.bikram.filepipe.ui.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.preferences.AppPreferences
import dev.bikram.filepipe.data.preferences.AppThemeMode
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.ui.components.containers.RoundedCardContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    fun markIntroSeen() {
        viewModelScope.launch { preferencesRepository.markIntroSeen() }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHapticFeedbackEnabled(enabled) }
    }

    fun setBlurEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setProgressiveBlurEnabled(enabled) }
    }

    fun setUseMaterialYou(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setUseMaterialYou(enabled) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnboardingSwitchListItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        colors = ListItemDefaults.colors(containerColor = scheme.surfaceBright),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = scheme.primary
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp),
                    color = scheme.outlineVariant
                )
                Switch(
                    checked = if (enabled) checked else false,
                    onCheckedChange = { value -> if (enabled) onCheckedChange(value) },
                    enabled = enabled
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingPrefsScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var notificationsGranted by remember {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        mutableStateOf(granted)
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted }

    val scheme = MaterialTheme.colorScheme
    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surfaceContainer)
            .systemBarsPadding()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 40.dp,
                bottom = 100.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = stringResource(R.string.onboarding_prefs_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_prefs_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
            }

            item {
                Text(
                    text = stringResource(R.string.onboarding_prefs_settings_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )

                RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = scheme.surfaceBright),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = scheme.primary
                            )
                        },
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.onboarding_prefs_theme_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface
                            )
                        },
                        supportingContent = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                listOf(AppThemeMode.LIGHT, AppThemeMode.DARK, AppThemeMode.SYSTEM).forEach { mode ->
                                    FilterChip(
                                        selected = prefs.themeMode == mode,
                                        onClick = { viewModel.setThemeMode(mode) },
                                        label = {
                                            Text(
                                                when (mode) {
                                                    AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                                    AppThemeMode.DARK -> stringResource(R.string.theme_dark)
                                                    else -> stringResource(R.string.theme_system)
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    )

                    OnboardingSwitchListItem(
                        icon = Icons.Default.AutoAwesome,
                        title = stringResource(R.string.theme_material_you),
                        description = stringResource(R.string.settings_material_you_hint),
                        checked = prefs.useMaterialYou,
                        onCheckedChange = { viewModel.setUseMaterialYou(it) },
                        enabled = dynamicColorSupported
                    )

                    OnboardingSwitchListItem(
                        icon = Icons.Default.Vibration,
                        title = stringResource(R.string.settings_haptic_feedback),
                        description = stringResource(R.string.settings_haptic_feedback_desc),
                        checked = prefs.hapticFeedbackEnabled,
                        onCheckedChange = { viewModel.setHapticEnabled(it) }
                    )

                    OnboardingSwitchListItem(
                        icon = Icons.Default.BlurOn,
                        title = stringResource(R.string.settings_progressive_blur),
                        description = stringResource(R.string.settings_progressive_blur_desc),
                        checked = prefs.progressiveBlurEnabled,
                        onCheckedChange = { viewModel.setBlurEnabled(it) }
                    )

                    OnboardingSwitchListItem(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.settings_notifications),
                        description = stringResource(R.string.settings_notifications_desc),
                        checked = notificationsGranted,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier
                    .height(56.dp)
                    .width(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }

            Button(
                onClick = {
                    viewModel.markIntroSeen()
                    onComplete()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_get_started),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            }
        }
    }
}
