package dev.bikram.filepipe.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.preferences.AppColorSource
import dev.bikram.filepipe.data.preferences.AppThemeMode
import dev.bikram.filepipe.data.preferences.ThemePaletteStyle
import dev.bikram.filepipe.ui.components.CustomSeedHexDialog
import dev.bikram.filepipe.ui.components.containers.GroupPosition
import dev.bikram.filepipe.ui.components.containers.GroupedListColumn
import dev.bikram.filepipe.ui.components.containers.GroupedListItem
import dev.bikram.filepipe.ui.feedback.tapSoundCombinedClickable
import dev.bikram.filepipe.ui.theme.normalizeCustomSeedHexOrNull
import dev.bikram.filepipe.ui.theme.parseSeedColorHexToColorOrNull

@Composable
fun themePaletteStyleLabel(style: ThemePaletteStyle): String =
    stringResource(
        when (style) {
            ThemePaletteStyle.TONAL_SPOT -> R.string.theme_palette_tonal_spot
            ThemePaletteStyle.NEUTRAL -> R.string.theme_palette_neutral
            ThemePaletteStyle.VIBRANT -> R.string.theme_palette_vibrant
            ThemePaletteStyle.EXPRESSIVE -> R.string.theme_palette_expressive
            ThemePaletteStyle.RAINBOW -> R.string.theme_palette_rainbow
            ThemePaletteStyle.FRUIT_SALAD -> R.string.theme_palette_fruit_salad
            ThemePaletteStyle.MONOCHROME -> R.string.theme_palette_monochrome
            ThemePaletteStyle.FIDELITY -> R.string.theme_palette_fidelity
            ThemePaletteStyle.CONTENT -> R.string.theme_palette_content
        },
    )

private fun customHexSwatchSelected(
    colorSource: AppColorSource,
    activeCustomSeedHex: String,
    storedHex: String,
): Boolean {
    if (colorSource != AppColorSource.CUSTOM) return false
    val activeNorm = normalizeCustomSeedHexOrNull(activeCustomSeedHex)
    val storedNorm = normalizeCustomSeedHexOrNull(storedHex)
    return when {
        activeNorm != null && storedNorm != null -> activeNorm == storedNorm
        else -> activeCustomSeedHex.trim() == storedHex.trim()
    }
}

@Composable
fun ThemeAccentRow(
    colorSource: AppColorSource,
    activeCustomSeedHex: String,
    savedCustomSeedHexes: List<String>,
    onSelectPreset: (AppColorSource) -> Unit,
    onSelectCustomHex: (String) -> Unit,
    onCustomHexLongPress: (String) -> Unit,
    onAddCustomHexClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(AppColorSource.accentOptions, key = { "preset_${it.name}" }) { source ->
            val isSelected = colorSource == source
            val borderColor =
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                }
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = borderColor,
                            shape = CircleShape,
                        ).clickable(
                            onClick = { onSelectPreset(source) },
                            indication = ripple(bounded = true),
                            interactionSource = remember { MutableInteractionSource() },
                        ).semantics { role = Role.RadioButton },
                contentAlignment = Alignment.Center,
            ) {
                ThemeAccentCircleContent(source = source)
            }
        }
        items(savedCustomSeedHexes, key = { "hex_$it" }) { storedHex ->
            val isSelected = customHexSwatchSelected(colorSource, activeCustomSeedHex, storedHex)
            val borderColor =
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                }
            val fillColor =
                parseSeedColorHexToColorOrNull(storedHex)
                    ?: MaterialTheme.colorScheme.surfaceVariant
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = borderColor,
                            shape = CircleShape,
                        ).combinedClickable(
                            onClick = { onSelectCustomHex(storedHex) },
                            onLongClick = { onCustomHexLongPress(storedHex) },
                            indication = ripple(bounded = true),
                            interactionSource = remember { MutableInteractionSource() },
                        ).semantics { role = Role.RadioButton },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(fillColor),
                )
            }
        }
        item(key = "add_custom_seed") {
            val addBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(width = 1.dp, color = addBorder, shape = CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                        .clickable(
                            onClick = onAddCustomHexClick,
                            indication = ripple(bounded = true),
                            interactionSource = remember { MutableInteractionSource() },
                        ).semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.settings_custom_seed_dialog_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun ThemeAccentCircleContent(source: AppColorSource) {
    when (source) {
        AppColorSource.MATERIAL_YOU ->
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        Color(0xFF6750A4),
                                        Color(0xFF625B71),
                                        Color(0xFF7D5260),
                                    ),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(22.dp),
                )
            }
        else -> {
            val triplet = source.curatedTriplet()
            if (triplet != null) {
                CuratedTripletSwatch(
                    primary = triplet.primary,
                    secondary = triplet.secondary,
                    tertiary = triplet.tertiary,
                )
            } else {
                val seed = source.seedPrimary() ?: Color.Gray
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(seed),
                )
            }
        }
    }
}

@Composable
private fun CuratedTripletSwatch(
    primary: Color,
    secondary: Color,
    tertiary: Color,
) {
    Column(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(primary),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            Box(Modifier.weight(1f).fillMaxHeight().background(secondary))
            Box(Modifier.weight(1f).fillMaxHeight().background(tertiary))
        }
    }
}

@Composable
fun ThemePaletteStyleRow(
    selected: ThemePaletteStyle,
    enabled: Boolean,
    onSelect: (ThemePaletteStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ThemePaletteStyle.all, key = { it.name }) { style ->
            FilterChip(
                selected = selected == style,
                onClick = { if (enabled) onSelect(style) },
                enabled = enabled,
                label = {
                    Text(
                        text = themePaletteStyleLabel(style),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    ),
            )
        }
    }
}

@Composable
fun AppearanceSection(
    themeMode: AppThemeMode,
    colorSource: AppColorSource,
    savedCustomSeedHexes: List<String>,
    activeCustomSeedHex: String,
    themePaletteStyle: ThemePaletteStyle,
    useGradientBackground: Boolean,
    useEnhancedShading: Boolean,
    progressiveBlurEnabled: Boolean,
    onThemeMode: (AppThemeMode) -> Unit,
    onColorSource: (AppColorSource) -> Unit,
    onPaletteStyle: (ThemePaletteStyle) -> Unit,
    onAddCustomSeedHex: (String) -> Unit,
    onSelectCustomSeedHex: (String) -> Unit,
    onRemoveCustomSeedHex: (String) -> Unit,
    onUseGradientBackground: (Boolean) -> Unit,
    onUseEnhancedShading: (Boolean) -> Unit,
    onProgressiveBlurEnabled: (Boolean) -> Unit,
    onBlackThemeEffectClick: () -> Unit,
) {
    var showCustomHexDialog by remember { mutableStateOf(false) }
    var hexPendingRemove by remember { mutableStateOf<String?>(null) }

    if (showCustomHexDialog) {
        CustomSeedHexDialog(
            initialDraft = "",
            onDismiss = { showCustomHexDialog = false },
            onConfirm = { raw ->
                onAddCustomSeedHex(raw)
                showCustomHexDialog = false
            },
        )
    }

    val hexToConfirmRemove = hexPendingRemove
    if (hexToConfirmRemove != null) {
        AlertDialog(
            onDismissRequest = { hexPendingRemove = null },
            title = { Text(stringResource(R.string.settings_custom_seed_remove_title)) },
            text = { Text(stringResource(R.string.settings_custom_seed_remove_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveCustomSeedHex(hexToConfirmRemove)
                    hexPendingRemove = null
                }) {
                    Text(stringResource(R.string.schedule_remove_short))
                }
            },
            dismissButton = {
                TextButton(onClick = { hexPendingRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    val blackThemeEffectsDisabled = themeMode == AppThemeMode.BLACK

    GroupedListColumn {
        GroupedListItem(position = GroupPosition.FIRST) {
            AppearanceStudioControls(
                themeMode = themeMode,
                colorSource = colorSource,
                savedCustomSeedHexes = savedCustomSeedHexes,
                activeCustomSeedHex = activeCustomSeedHex,
                themePaletteStyle = themePaletteStyle,
                onThemeMode = onThemeMode,
                onColorSource = onColorSource,
                onPaletteStyle = onPaletteStyle,
                onSelectCustomSeedHex = onSelectCustomSeedHex,
                onCustomHexLongPress = { hexPendingRemove = it },
                onAddCustomHexClick = { showCustomHexDialog = true },
            )
        }
        GroupedListItem(position = GroupPosition.MIDDLE) {
            AppearanceToggleItem(
                title = stringResource(R.string.settings_gradient_background),
                subtitle = stringResource(R.string.settings_gradient_background_desc),
                checked = useGradientBackground && !blackThemeEffectsDisabled,
                enabled = !blackThemeEffectsDisabled,
                onDisabledClick = onBlackThemeEffectClick,
                onCheckedChange = onUseGradientBackground,
            )
        }
        GroupedListItem(position = GroupPosition.MIDDLE) {
            AppearanceToggleItem(
                title = stringResource(R.string.settings_enhanced_shading),
                subtitle = stringResource(R.string.settings_enhanced_shading_desc),
                checked = useEnhancedShading || blackThemeEffectsDisabled,
                enabled = !blackThemeEffectsDisabled,
                onDisabledClick = onBlackThemeEffectClick,
                onCheckedChange = onUseEnhancedShading,
            )
        }
        GroupedListItem(position = GroupPosition.LAST) {
            AppearanceToggleItem(
                title = stringResource(R.string.settings_progressive_blur),
                subtitle = stringResource(R.string.settings_progressive_blur_desc),
                checked = progressiveBlurEnabled,
                leadingIcon = Icons.Default.BlurOn,
                onCheckedChange = onProgressiveBlurEnabled,
            )
        }
    }
}

@Composable
private fun AppearanceStudioControls(
    themeMode: AppThemeMode,
    colorSource: AppColorSource,
    savedCustomSeedHexes: List<String>,
    activeCustomSeedHex: String,
    themePaletteStyle: ThemePaletteStyle,
    onThemeMode: (AppThemeMode) -> Unit,
    onColorSource: (AppColorSource) -> Unit,
    onPaletteStyle: (ThemePaletteStyle) -> Unit,
    onSelectCustomSeedHex: (String) -> Unit,
    onCustomHexLongPress: (String) -> Unit,
    onAddCustomHexClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ThemeModeSegmentedRow(
            selected = themeMode,
            onSelect = onThemeMode,
        )
        ThemeAccentRow(
            colorSource = colorSource,
            activeCustomSeedHex = activeCustomSeedHex,
            savedCustomSeedHexes = savedCustomSeedHexes,
            onSelectPreset = onColorSource,
            onSelectCustomHex = onSelectCustomSeedHex,
            onCustomHexLongPress = onCustomHexLongPress,
            onAddCustomHexClick = onAddCustomHexClick,
        )
        AppearanceStudioSection(title = stringResource(R.string.settings_palette_style)) {
            ThemePaletteStyleRow(
                selected = themePaletteStyle,
                enabled = colorSource.supportsPaletteStyle,
                onSelect = onPaletteStyle,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
        ThemePreviewPanel(colorSource = colorSource)
    }
}

@Composable
private fun ThemeModeSegmentedRow(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
) {
    val colors =
        ToggleButtonDefaults.toggleButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    @Suppress("DEPRECATION")
    ButtonGroup(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        themePickerOrder.forEachIndexed { index, mode ->
            ToggleButton(
                checked = selected == mode,
                onCheckedChange = { checked -> if (checked) onSelect(mode) },
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        themePickerOrder.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                colors = colors,
            ) {
                Text(
                    text = themeModeLabel(mode),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AppearanceStudioSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        content()
    }
}

@Composable
private fun AppearanceToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onDisabledClick: (() -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else 0.38f
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .tapSoundCombinedClickable(onClick = {
                    if (enabled) {
                        onCheckedChange(!checked)
                    } else {
                        onDisabledClick?.invoke()
                    }
                })
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
            Spacer(Modifier.width(16.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(16.dp))
        Box {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = if (enabled) onCheckedChange else null,
            )
            if (!enabled && onDisabledClick != null) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clip(MaterialTheme.shapes.extraExtraLarge)
                            .tapSoundCombinedClickable(onClick = { onDisabledClick() }),
                )
            }
        }
    }
}

private val themePickerOrder =
    listOf(
        AppThemeMode.SYSTEM,
        AppThemeMode.LIGHT,
        AppThemeMode.DARK,
        AppThemeMode.BLACK,
    )

@Composable
private fun themeModeLabel(mode: AppThemeMode): String =
    stringResource(
        when (mode) {
            AppThemeMode.SYSTEM -> R.string.theme_system
            AppThemeMode.LIGHT -> R.string.theme_light
            AppThemeMode.DARK -> R.string.theme_dark
            AppThemeMode.BLACK -> R.string.theme_black
        },
    )

@Composable
private fun ThemePreviewPanel(colorSource: AppColorSource) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_theme_preview_named, colorSourceDisplayName(colorSource)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        PreviewSubsection(title = stringResource(R.string.settings_theme_preview_surface_ladder)) {
            SurfaceLadderStrip(scheme = scheme)
        }
        PreviewSubsection(title = stringResource(R.string.settings_theme_preview_accent_containers)) {
            AccentContainersStrip(scheme = scheme)
        }
    }
}

@Composable
private fun PreviewSubsection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        content()
    }
}

@Composable
private fun SurfaceLadderStrip(scheme: ColorScheme) {
    val swatches =
        listOf(
            scheme.surfaceContainerLowest to stringResource(R.string.settings_theme_preview_label_lowest),
            scheme.surface to stringResource(R.string.settings_theme_preview_label_surface),
            scheme.surfaceContainerLow to stringResource(R.string.settings_theme_preview_label_low),
            scheme.surfaceContainer to stringResource(R.string.settings_theme_preview_label_base),
            scheme.surfaceContainerHigh to stringResource(R.string.settings_theme_preview_label_high),
            scheme.surfaceContainerHighest to stringResource(R.string.settings_theme_preview_label_highest),
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(MaterialTheme.shapes.small)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
    ) {
        swatches.forEach { (color, label) ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = contrastingTextColor(color),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AccentContainersStrip(scheme: ColorScheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AccentChip(
            modifier = Modifier.weight(1f),
            container = scheme.primaryContainer,
            onContainer = scheme.onPrimaryContainer,
            label = stringResource(R.string.settings_theme_preview_label_primary),
        )
        AccentChip(
            modifier = Modifier.weight(1f),
            container = scheme.secondaryContainer,
            onContainer = scheme.onSecondaryContainer,
            label = stringResource(R.string.settings_theme_preview_label_secondary),
        )
        AccentChip(
            modifier = Modifier.weight(1f),
            container = scheme.tertiaryContainer,
            onContainer = scheme.onTertiaryContainer,
            label = stringResource(R.string.settings_theme_preview_label_tertiary),
        )
    }
}

@Composable
private fun AccentChip(
    modifier: Modifier,
    container: Color,
    onContainer: Color,
    label: String,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = container,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = onContainer.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.settings_theme_preview_sample),
                style = MaterialTheme.typography.titleMedium,
                color = onContainer,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun contrastingTextColor(background: Color): Color =
    if (ColorUtils.calculateLuminance(background.toArgb()) > 0.5) {
        Color.Black.copy(alpha = 0.82f)
    } else {
        Color.White.copy(alpha = 0.9f)
    }

@Composable
private fun colorSourceDisplayName(source: AppColorSource): String =
    stringResource(
        when (source.migrated()) {
            AppColorSource.MATERIAL_YOU -> R.string.theme_material_you
            AppColorSource.DEFAULT -> R.string.theme_color_forest
            AppColorSource.CURATED_EMBER -> R.string.theme_color_ember
            AppColorSource.CURATED_GROVE -> R.string.theme_color_grove
            AppColorSource.CURATED_HONEY -> R.string.theme_color_honey
            AppColorSource.CURATED_OCEAN -> R.string.theme_color_ocean
            AppColorSource.CURATED_IRIS -> R.string.theme_color_iris
            AppColorSource.CURATED_DUSK -> R.string.theme_color_dusk
            AppColorSource.CURATED_BERRY -> R.string.theme_color_berry
            AppColorSource.CUSTOM -> R.string.theme_color_custom
            else -> R.string.theme_color_custom
        },
    )
