package dev.bikram.filepipe.shortcuts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingShortcutRepository
    @Inject
    constructor() {
        private val _pendingRuleId = MutableStateFlow<Long?>(null)
        val pendingRuleId: StateFlow<Long?> = _pendingRuleId.asStateFlow()

        private val _pendingHistoryDetailId = MutableStateFlow<Long?>(null)
        val pendingHistoryDetailId: StateFlow<Long?> = _pendingHistoryDetailId.asStateFlow()

        private val _pendingOpenHistory = MutableStateFlow(false)
        val pendingOpenHistory: StateFlow<Boolean> = _pendingOpenHistory.asStateFlow()

        private val _pendingOpenSettingsForUpdates = MutableStateFlow(false)
        val pendingOpenSettingsForUpdates: StateFlow<Boolean> = _pendingOpenSettingsForUpdates.asStateFlow()

        fun requestRunRule(ruleId: Long) {
            _pendingRuleId.value = ruleId
        }

        fun clearPendingRule() {
            _pendingRuleId.value = null
        }

        fun requestOpenHistoryDetail(historyId: Long) {
            _pendingHistoryDetailId.value = historyId
        }

        fun clearPendingHistoryDetail() {
            _pendingHistoryDetailId.value = null
        }

        fun requestOpenHistory() {
            _pendingOpenHistory.value = true
        }

        fun clearPendingOpenHistory() {
            _pendingOpenHistory.value = false
        }

        fun requestOpenSettingsForUpdates() {
            _pendingOpenSettingsForUpdates.value = true
        }

        fun clearPendingOpenSettingsForUpdates() {
            _pendingOpenSettingsForUpdates.value = false
        }

        companion object {
            const val EXTRA_OPEN_HISTORY = "extra_open_history"
            const val EXTRA_OPEN_HISTORY_DETAIL_ID = "extra_open_history_detail_id"
            const val EXTRA_OPEN_SETTINGS_UPDATES = "extra_open_settings_updates"
        }
    }
