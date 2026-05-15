package dev.bikram.filepipe.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.bikram.filepipe.ui.feedback.LocalHapticEnabled
import dev.bikram.filepipe.ui.feedback.performLongPressHaptic
import dev.bikram.filepipe.ui.feedback.rememberPlayTapSound
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TOOLTIP_DISMISS_MILLIS = 5_000L

@Composable
private fun FilePipeLongPressLabelTooltip(
    label: String?,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (label == null) {
        content()
        return
    }

    val tooltipState = rememberTooltipState(isPersistent = true)
    val hapticEnabled = LocalHapticEnabled.current
    val view = LocalView.current

    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(
                TooltipAnchorPosition.Above,
            ),
        tooltip = {
            PlainTooltip {
                Box(
                    modifier = Modifier.heightIn(min = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label)
                }
            }
        },
        state = tooltipState,
        modifier =
            Modifier.showLabelTooltipOnLongPress(
                enabled = enabled,
                tooltipState = tooltipState,
                hapticEnabled = hapticEnabled,
                view = view,
            ),
        enableUserInput = false,
    ) {
        content()
    }
}

private fun Modifier.showLabelTooltipOnLongPress(
    enabled: Boolean,
    tooltipState: TooltipState,
    hapticEnabled: Boolean,
    view: android.view.View,
): Modifier {
    if (!enabled) return this

    return pointerInput(tooltipState, hapticEnabled, view) {
        coroutineScope {
            awaitEachGesture {
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                val releasedBeforeLongPress =
                    withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        waitForUpOrCancellation(PointerEventPass.Initial)
                    }
                if (releasedBeforeLongPress == null) {
                    if (hapticEnabled) {
                        view.performLongPressHaptic()
                    }
                    launch { tooltipState.show() }

                    var released = false
                    while (!released) {
                        val pointerEvent = awaitPointerEvent(PointerEventPass.Initial)
                        val activePointerChange =
                            pointerEvent.changes.firstOrNull { pointerChange ->
                                pointerChange.id == firstDown.id
                            }
                        activePointerChange?.consume()
                        released = activePointerChange?.pressed != true
                    }

                    launch {
                        delay(TOOLTIP_DISMISS_MILLIS)
                        tooltipState.dismiss()
                    }
                }
            }
        }
    }
}

@Composable
fun FilePipeIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    tooltipLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val playTap = rememberPlayTapSound()
    FilePipeLongPressLabelTooltip(label = tooltipLabel, enabled = enabled) {
        IconButton(
            onClick = {
                playTap()
                onClick()
            },
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@Composable
fun FilePipeFilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    tooltipLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val playTap = rememberPlayTapSound()
    FilePipeLongPressLabelTooltip(label = tooltipLabel, enabled = enabled) {
        FilledIconButton(
            onClick = {
                playTap()
                onClick()
            },
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@Composable
fun FilePipeFilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    tooltipLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val playTap = rememberPlayTapSound()
    FilePipeLongPressLabelTooltip(label = tooltipLabel, enabled = enabled) {
        FilledTonalIconButton(
            onClick = {
                playTap()
                onClick()
            },
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@Composable
fun FilePipeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val playTap = rememberPlayTapSound()
    androidx.compose.material3.Button(
        onClick = {
            playTap()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun FilePipeTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val playTap = rememberPlayTapSound()
    TextButton(
        onClick = {
            playTap()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun FilePipeOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val playTap = rememberPlayTapSound()
    OutlinedButton(
        onClick = {
            playTap()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun FilePipeFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val playTap = rememberPlayTapSound()
    FilledTonalButton(
        onClick = {
            playTap()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun FilePipeToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapes(),
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ToggleButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val playTap = rememberPlayTapSound()
    ToggleButton(
        checked = checked,
        onCheckedChange = { selected ->
            playTap()
            onCheckedChange(selected)
        },
        modifier = modifier,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun FilePipeFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = androidx.compose.material3.contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    tooltipLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val playTap = rememberPlayTapSound()
    FilePipeLongPressLabelTooltip(label = tooltipLabel, enabled = enabled) {
        FloatingActionButton(
            onClick = {
                if (enabled) {
                    playTap()
                    onClick()
                }
            },
            modifier = modifier,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = elevation,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@Composable
fun FilePipeExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = androidx.compose.material3.contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) {
    val playTap = rememberPlayTapSound()
    ExtendedFloatingActionButton(
        text = text,
        icon = icon,
        onClick = {
            playTap()
            onClick()
        },
        modifier = modifier,
        expanded = expanded,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
    )
}

@Composable
fun FilePipeSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    contentColor: Color = androidx.compose.material3.contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val playTap = rememberPlayTapSound()
    Surface(
        onClick = {
            playTap()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun FilePipeElevatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val playTap = rememberPlayTapSound()
    ElevatedCard(
        onClick = {
            playTap()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun FilePipeOutlinedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.outlinedShape,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = CardDefaults.outlinedCardBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val playTap = rememberPlayTapSound()
    OutlinedCard(
        onClick = {
            playTap()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun FilePipeFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = androidx.compose.material3.FilterChipDefaults.shape,
    colors: androidx.compose.material3.SelectableChipColors =
        androidx.compose.material3.FilterChipDefaults
            .filterChipColors(),
    elevation: androidx.compose.material3.SelectableChipElevation? =
        androidx.compose.material3.FilterChipDefaults
            .filterChipElevation(),
    border: BorderStroke? =
        androidx.compose.material3.FilterChipDefaults
            .filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null,
) {
    val playTap = rememberPlayTapSound()
    FilterChip(
        selected = selected,
        onClick = {
            playTap()
            onClick()
        },
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource,
    )
}

@Composable
fun FilePipeInputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    avatar: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = androidx.compose.material3.InputChipDefaults.shape,
    colors: androidx.compose.material3.SelectableChipColors =
        androidx.compose.material3.InputChipDefaults
            .inputChipColors(),
    elevation: androidx.compose.material3.SelectableChipElevation? =
        androidx.compose.material3.InputChipDefaults
            .inputChipElevation(),
    border: BorderStroke? =
        androidx.compose.material3.InputChipDefaults
            .inputChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null,
) {
    val playTap = rememberPlayTapSound()
    InputChip(
        selected = selected,
        onClick = {
            playTap()
            onClick()
        },
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        avatar = avatar,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource,
    )
}

@Composable
fun FilePipeDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors =
        androidx.compose.material3.MenuDefaults
            .itemColors(),
    contentPadding: PaddingValues = androidx.compose.material3.MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    val playTap = rememberPlayTapSound()
    DropdownMenuItem(
        text = text,
        onClick = {
            playTap()
            onClick()
        },
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )
}

@Composable
fun androidx.compose.material3.SingleChoiceSegmentedButtonRowScope.FilePipeSegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: androidx.compose.material3.SegmentedButtonColors =
        androidx.compose.material3.SegmentedButtonDefaults
            .colors(),
    border: BorderStroke =
        androidx.compose.material3.SegmentedButtonDefaults
            .borderStroke(colors.activeBorderColor),
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable () -> Unit = {
        androidx.compose.material3.SegmentedButtonDefaults
            .Icon(selected)
    },
    label: @Composable () -> Unit,
) {
    val playTap = rememberPlayTapSound()
    SegmentedButton(
        selected = selected,
        onClick = {
            playTap()
            onClick()
        },
        shape = shape,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        border = border,
        interactionSource = interactionSource,
        icon = icon,
        label = label,
    )
}

@Composable
fun FilePipeTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    selectedContentColor: Color = androidx.compose.material3.LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    interactionSource: MutableInteractionSource? = null,
) {
    val playTap = rememberPlayTapSound()
    Tab(
        selected = selected,
        onClick = {
            playTap()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        text = text,
        icon = icon,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        interactionSource = interactionSource,
    )
}
