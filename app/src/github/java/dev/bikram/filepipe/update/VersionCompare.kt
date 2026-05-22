package dev.bikram.filepipe.update

/**
 * Compares two dotted version strings (e.g. "1.5.0" vs "1.10.1").
 * Returns negative if [left] is older than [right], zero if equal, positive if [left] is newer.
 * Non-numeric segments sort after numeric ones; missing segments treated as 0.
 */
internal fun compareVersionNames(
    left: String,
    right: String,
): Int {
    val leftParts = left.trim().removePrefix("v").split('.', '-', limit = 10)
    val rightParts = right.trim().removePrefix("v").split('.', '-', limit = 10)
    val maxLen = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until maxLen) {
        val leftToken = leftParts.getOrNull(index).orEmpty()
        val rightToken = rightParts.getOrNull(index).orEmpty()
        val leftNum = leftToken.toIntOrNull()
        val rightNum = rightToken.toIntOrNull()
        val cmp =
            when {
                leftNum != null && rightNum != null -> leftNum.compareTo(rightNum)
                leftNum != null -> -1
                rightNum != null -> 1
                else -> leftToken.compareTo(rightToken)
            }
        if (cmp != 0) return cmp
    }
    return 0
}

/** True if [remote] is strictly newer than [installed] per [compareVersionNames]. */
internal fun isRemoteVersionNewer(
    remote: String,
    installed: String,
): Boolean = compareVersionNames(remote, installed) > 0
