package dev.bikram.filepipe.ui.feedback

import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalTapSound = staticCompositionLocalOf<() -> Unit> { { } }
val LocalHapticEnabled = staticCompositionLocalOf { true }

@Composable
fun rememberPlayTapSound(): () -> Unit = LocalTapSound.current

fun View.playTapSound() {
    if (isShown) {
        playSoundEffect(SoundEffectConstants.CLICK)
    }
}

/**
 * Heavier double-click haptic used for long-press multi-select.
 * Distinct from the swipe-threshold haptic so the user can feel the difference.
 */
fun View.performLongPressHaptic() {
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return
    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
}

/**
 * Haptic when swipe passes the dismiss threshold. Uses a stronger [View.performHapticFeedback]
 * than a bare tick, plus a heavier vibrator pulse when hardware supports it.
 */
fun View.performSwipeThresholdHaptic() {
    performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return
    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
}

fun View.performRejectHaptic() {
    performHapticFeedback(HapticFeedbackConstants.REJECT)
}
