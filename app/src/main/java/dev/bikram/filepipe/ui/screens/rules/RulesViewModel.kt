package dev.bikram.filepipe.ui.screens.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bikram.filepipe.data.preferences.SwipeAction
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RunProgress
import dev.bikram.filepipe.domain.model.TriggerType
import dev.bikram.filepipe.domain.usecase.ExecuteRulesUseCase
import dev.bikram.filepipe.domain.usecase.RulesAutoExportTrigger
import dev.bikram.filepipe.domain.usecase.ScheduleRulesUseCase
import dev.bikram.filepipe.domain.usecase.UndoRunUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UndoSnackbarEvent(
    val historyId: Long,
    val ruleName: String,
    val filesMoved: Int
)

data class DeleteUndoEvent(val rules: List<Rule>)

sealed interface RulesRunNavigation {
    data class HistoryDetail(val historyId: Long) : RulesRunNavigation
    data object HistoryList : RulesRunNavigation
}

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val scheduleRulesUseCase: ScheduleRulesUseCase,
    private val executeRulesUseCase: ExecuteRulesUseCase,
    private val rulesAutoExportTrigger: RulesAutoExportTrigger,
    private val undoRunUseCase: UndoRunUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val rules: StateFlow<List<Rule>> = ruleRepository.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val swipeStartToEnd: StateFlow<SwipeAction> = userPreferencesRepository.preferencesFlow
        .map { it.swipeStartToEnd }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SwipeAction.DUPLICATE)

    val swipeEndToStart: StateFlow<SwipeAction> = userPreferencesRepository.preferencesFlow
        .map { it.swipeEndToStart }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SwipeAction.DELETE)

    private val _selectedRuleIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedRuleIds: StateFlow<Set<Long>> = _selectedRuleIds.asStateFlow()

    private val _progressMap = MutableStateFlow<Map<Long, RunProgress>>(emptyMap())
    val progressMap: StateFlow<Map<Long, RunProgress>> = _progressMap.asStateFlow()

    val isRunning: StateFlow<Boolean> = _progressMap
        .map { progressMap -> progressMap.values.any { !it.isComplete } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _navigateAfterRun = MutableSharedFlow<RulesRunNavigation>(extraBufferCapacity = 1)
    val navigateAfterRun = _navigateAfterRun.asSharedFlow()

    private val _undoEvent = MutableSharedFlow<UndoSnackbarEvent>(extraBufferCapacity = 1)
    val undoEvent = _undoEvent.asSharedFlow()

    private val _deleteUndoEvent = MutableSharedFlow<DeleteUndoEvent>(extraBufferCapacity = 1)
    val deleteUndoEvent = _deleteUndoEvent.asSharedFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() { _userMessage.value = null }

    // Compact / expanded view mode
    // isCompactMode = global default; cardModeOverrides = cards that flip the global default
    private val _isCompactMode = MutableStateFlow(false)
    val isCompactMode: StateFlow<Boolean> = _isCompactMode.asStateFlow()

    private val _cardModeOverrides = MutableStateFlow<Set<Long>>(emptySet())
    val cardModeOverrides: StateFlow<Set<Long>> = _cardModeOverrides.asStateFlow()

    fun isCardExpanded(ruleId: Long, compact: Boolean, overrides: Set<Long>): Boolean =
        if (compact) ruleId in overrides else ruleId !in overrides

    fun toggleCardExpansion(ruleId: Long) {
        _cardModeOverrides.update { if (ruleId in it) it - ruleId else it + ruleId }
    }

    fun toggleGlobalViewMode() {
        _isCompactMode.update { !it }
        _cardModeOverrides.value = emptySet()
    }

    fun toggleSelection(ruleId: Long) {
        _selectedRuleIds.update { current ->
            if (ruleId in current) current - ruleId else current + ruleId
        }
    }

    fun clearSelection() {
        _selectedRuleIds.value = emptySet()
    }

    fun selectAll() {
        _selectedRuleIds.value = rules.value.map { it.id }.toSet()
    }

    fun deleteSelected() = viewModelScope.launch {
        val toDelete = rules.value.filter { it.id in _selectedRuleIds.value }
        toDelete.forEach { rule ->
            scheduleRulesUseCase.cancelRule(rule)
            ruleRepository.deleteRule(rule.id)
        }
        rulesAutoExportTrigger.maybeExportAfterRuleChange()
        clearSelection()
        if (toDelete.isNotEmpty()) _deleteUndoEvent.emit(DeleteUndoEvent(toDelete))
    }

    fun clearProgress() {
        _progressMap.value = emptyMap()
        _selectedRuleIds.value = emptySet()
    }

    fun toggleEnabled(rule: Rule, enabled: Boolean) = viewModelScope.launch {
        val updated = rule.copy(isEnabled = enabled)
        ruleRepository.updateRule(updated)
        if (enabled) {
            scheduleRulesUseCase.scheduleRule(updated)
        } else {
            scheduleRulesUseCase.cancelRule(updated)
        }
        rulesAutoExportTrigger.maybeExportAfterRuleChange()
    }

    fun deleteRule(rule: Rule) = viewModelScope.launch {
        scheduleRulesUseCase.cancelRule(rule)
        ruleRepository.deleteRule(rule.id)
        rulesAutoExportTrigger.maybeExportAfterRuleChange()
        _deleteUndoEvent.emit(DeleteUndoEvent(listOf(rule)))
    }

    fun undoDelete(rules: List<Rule>) = viewModelScope.launch {
        rules.forEach { rule ->
            ruleRepository.saveRule(rule)
            if (rule.isEnabled && rule.schedule != null) {
                scheduleRulesUseCase.scheduleRule(rule)
            }
        }
        rulesAutoExportTrigger.maybeExportAfterRuleChange()
    }

    fun runSelected() = viewModelScope.launch {
        val selected = rules.value.filter { it.id in _selectedRuleIds.value && it.isEnabled }
        if (selected.isEmpty()) return@launch

        _progressMap.update {
            selected.associate { rule ->
                rule.id to RunProgress(
                    ruleId = rule.id,
                    ruleName = rule.name,
                    progress = 0f
                )
            }
        }

        val results = executeRulesUseCase(
            rules = selected,
            triggerType = TriggerType.MANUAL,
            onProgress = { progress ->
                _progressMap.update { current -> current + (progress.ruleId to progress) }
            }
        )

        when {
            results.size == 1 -> _navigateAfterRun.emit(
                RulesRunNavigation.HistoryDetail(results.first().historyId)
            )
            results.isNotEmpty() -> _navigateAfterRun.emit(RulesRunNavigation.HistoryList)
        }
        clearProgress()
    }

    fun runRule(rule: Rule) = viewModelScope.launch {
        if (!rule.isEnabled) return@launch

        _progressMap.value = mapOf(
            rule.id to RunProgress(
                ruleId = rule.id,
                ruleName = rule.name,
                progress = 0f
            )
        )

        val results = executeRulesUseCase(
            rules = listOf(rule),
            triggerType = TriggerType.MANUAL,
            onProgress = { progress ->
                _progressMap.update { current -> current + (progress.ruleId to progress) }
            }
        )

        clearProgress()

        // If files were moved, emit undo snackbar event; otherwise navigate to detail
        val result = results.firstOrNull()
        if (result != null && result.totalMoved > 0) {
            _undoEvent.emit(UndoSnackbarEvent(result.historyId, rule.name, result.totalMoved))
        } else if (result != null) {
            _navigateAfterRun.emit(RulesRunNavigation.HistoryDetail(result.historyId))
        }
    }

    fun duplicateRule(rule: Rule) = viewModelScope.launch {
        val copy = rule.copy(
            id = 0,
            name = "${rule.name} (copy)",
            isEnabled = false,
            schedule = null
        )
        ruleRepository.saveRule(copy)
        rulesAutoExportTrigger.maybeExportAfterRuleChange()
        _userMessage.value = "\"${copy.name}\" created"
    }

    fun undoRun(historyId: Long) = viewModelScope.launch {
        val result = undoRunUseCase(historyId)
        _userMessage.value = when {
            result.totalFailed == 0 -> "Undone: ${result.totalReversed} file(s) restored"
            result.totalReversed == 0 -> "Undo failed: ${result.errors.firstOrNull() ?: "unknown error"}"
            else -> "Partial undo: ${result.totalReversed} restored, ${result.totalFailed} failed"
        }
    }
}
