package dev.bikram.filepipe.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable

@Composable
fun <T> reducedMotionAwareSpec(defaultSpec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
    if (LocalReducedMotion.current) {
        tween(durationMillis = 0)
    } else {
        defaultSpec
    }

@Composable
fun reducedMotionEnterTransition(defaultTransition: EnterTransition = fadeIn()): EnterTransition =
    if (LocalReducedMotion.current) {
        EnterTransition.None
    } else {
        defaultTransition
    }

@Composable
fun reducedMotionExitTransition(defaultTransition: ExitTransition = fadeOut()): ExitTransition =
    if (LocalReducedMotion.current) {
        ExitTransition.None
    } else {
        defaultTransition
    }
