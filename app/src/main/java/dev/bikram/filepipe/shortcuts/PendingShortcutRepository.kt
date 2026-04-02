package dev.bikram.filepipe.shortcuts

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingShortcutRepository @Inject constructor() {
    private val _pendingRuleId = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val pendingRuleId: SharedFlow<Long> = _pendingRuleId.asSharedFlow()

    fun requestRunRule(ruleId: Long) {
        _pendingRuleId.tryEmit(ruleId)
    }
}
