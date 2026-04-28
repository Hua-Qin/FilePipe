package dev.bikram.filepipe.ui.feedback

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tapSoundCombinedClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    role: Role? = null,
): Modifier =
    composed {
        val playTap = LocalTapSound.current
        val hapticEnabled = LocalHapticEnabled.current
        val view = LocalView.current
        combinedClickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            role = role,
            onClick = {
                playTap()
                onClick()
            },
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
