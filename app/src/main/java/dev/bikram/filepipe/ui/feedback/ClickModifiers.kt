package dev.bikram.filepipe.ui.feedback

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

fun Modifier.appClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: androidx.compose.foundation.Indication? = null,
    onClick: () -> Unit,
): Modifier =
    clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = indication,
        onClick = onClick,
    )

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.appCombinedClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: androidx.compose.foundation.Indication? = null,
): Modifier =
    composed {
        val hapticEnabled = LocalHapticEnabled.current
        val view = LocalView.current
        combinedClickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            role = role,
            interactionSource = interactionSource,
            indication = indication,
            onClick = onClick,
            onLongClick =
                if (onLongClick != null) {
                    {
                        if (hapticEnabled) view.performLongPressHaptic()
                        onLongClick()
                    }
                } else {
                    null
                },
        )
    }
