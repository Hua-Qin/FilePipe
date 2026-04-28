package dev.bikram.filepipe.ui.components

/**
 * Shared swipe-to-reveal tuning for list cards (rules, history).
 *
 * [DeliberateSwipeRevealCard] uses [COMMIT_THRESHOLD_FRACTION]: the user must drag at least that
 * fraction of the card width before the action runs on finger up.
 *
 * Note: Material3 [androidx.compose.material3.SwipeToDismissBox] often ignores custom
 * [positionalThreshold] in 1.4+, so we use [DeliberateSwipeRevealCard] instead.
 */
object SwipeDismissCardDefaults {
    /** Fraction of card width (0–1) required before swipe commits on release. */
    const val COMMIT_THRESHOLD_FRACTION = 0.45f
}
