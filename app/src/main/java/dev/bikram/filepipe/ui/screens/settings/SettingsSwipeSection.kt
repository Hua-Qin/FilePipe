package dev.bikram.filepipe.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.preferences.SwipeAction
import dev.bikram.filepipe.data.preferences.materialSymbolName
import dev.bikram.filepipe.ui.common.FilePipeMaterialRoundedSymbol
import dev.bikram.filepipe.ui.components.FilePipeDropdownMenuItem
import dev.bikram.filepipe.ui.feedback.appClickable
import dev.bikram.filepipe.ui.theme.swipeActionAccent

internal enum class SwipeDirectionCue(
    val iconName: String,
) {
    LEFT("arrow_back"),
    RIGHT("arrow_forward"),
}

@Composable
internal fun swipeActionLabel(action: SwipeAction): String =
    when (action) {
        SwipeAction.EDIT -> stringResource(R.string.action_expand_collapse)
        SwipeAction.DELETE -> stringResource(R.string.settings_swipe_action_trash)
        SwipeAction.DUPLICATE -> stringResource(R.string.action_duplicate)
        SwipeAction.PREVIEW -> stringResource(R.string.preview_title)
        SwipeAction.VIEW_HISTORY -> stringResource(R.string.settings_swipe_action_history)
    }

@Composable
internal fun SwipeExecuteOneActionsEditor(
    startTitle: String,
    endTitle: String,
    startAction: SwipeAction,
    endAction: SwipeAction,
    onStartActionChange: (SwipeAction) -> Unit,
    onEndActionChange: (SwipeAction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SwipeExecuteDirectionColumn(
                title = startTitle,
                direction = SwipeDirectionCue.RIGHT,
                action = startAction,
                availableActions = SwipeAction.entries.filter { it != endAction },
                onActionChange = onStartActionChange,
                modifier = Modifier.weight(1f),
            )
            SwipeExecuteDirectionColumn(
                title = endTitle,
                direction = SwipeDirectionCue.LEFT,
                action = endAction,
                availableActions = SwipeAction.entries.filter { it != startAction },
                onActionChange = onEndActionChange,
                modifier = Modifier.weight(1f),
            )
        }
        SwipePanelDivider()
        SwipeHintText(text = stringResource(R.string.settings_swipe_execute_hint))
    }
}

@Composable
internal fun SwipeExecuteDirectionColumn(
    title: String,
    direction: SwipeDirectionCue,
    action: SwipeAction,
    availableActions: List<SwipeAction>,
    onActionChange: (SwipeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        SwipeExecuteDirectionHeader(
            title = title,
            direction = direction,
        )
        SwipeExecuteActionPicker(
            action = action,
            availableActions = availableActions,
            onActionChange = onActionChange,
        )
    }
}

@Composable
internal fun SwipeExecuteDirectionHeader(
    title: String,
    direction: SwipeDirectionCue,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            if (direction == SwipeDirectionCue.LEFT) {
                Arrangement.End
            } else {
                Arrangement.Start
            },
    ) {
        if (direction == SwipeDirectionCue.RIGHT) {
            SwipeDirectionIcon(direction = direction)
            Spacer(Modifier.size(7.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (direction == SwipeDirectionCue.LEFT) {
            Spacer(Modifier.size(7.dp))
            SwipeDirectionIcon(direction = direction)
        }
    }
}

@Composable
internal fun SwipeDirectionIcon(direction: SwipeDirectionCue) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        FilePipeMaterialRoundedSymbol(
            name = direction.iconName,
            size = 15.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            weight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun SwipeExecuteActionPicker(
    action: SwipeAction,
    availableActions: List<SwipeAction>,
    onActionChange: (SwipeAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.medium
    val actionAccent = action.settingsSwipeAccent()
    Box {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(shape)
                    .appClickable(role = Role.Button) { expanded = true },
            shape = shape,
            color = action.settingsSwipeTileColor(),
            border = BorderStroke(1.dp, actionAccent.copy(alpha = 0.55f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SwipeActionIcon(action = action)
                Text(
                    text = swipeActionLabel(action),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                FilePipeMaterialRoundedSymbol(
                    name = "expand_more",
                    size = 17.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            availableActions.forEach { action ->
                FilePipeDropdownMenuItem(
                    text = { Text(swipeActionLabel(action)) },
                    leadingIcon = { SwipeActionIcon(action = action) },
                    onClick = {
                        onActionChange(action)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun SwipeActionIcon(action: SwipeAction) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(action.settingsSwipeIconContainerColor()),
        contentAlignment = Alignment.Center,
    ) {
        FilePipeMaterialRoundedSymbol(
            name = action.materialSymbolName(),
            size = 15.dp,
            tint = action.settingsSwipeIconColor(),
            weight = FontWeight.Medium,
        )
    }
}

internal fun SwipeAction.settingsSwipeTileColor(): Color = settingsSwipeAccent().copy(alpha = 0.14f)

internal fun SwipeAction.settingsSwipeAccent(): Color = swipeActionAccent()

internal fun SwipeAction.settingsSwipeIconContainerColor(): Color = settingsSwipeAccent()

internal fun SwipeAction.settingsSwipeIconColor(): Color = Color.White

@Composable
internal fun SwipePanelDivider() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)),
    )
}

@Composable
internal fun SwipeHintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
