package dev.bikram.filepipe.ui.feedback

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
 * Haptic when swipe passes the dismiss threshold. Uses a stronger [View.performHapticFeedback]
 * than a bare tick, plus a heavier vibrator pulse when hardware supports it.
 */
fun View.performSwipeThresholdHaptic() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    } ?: return
    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
}
