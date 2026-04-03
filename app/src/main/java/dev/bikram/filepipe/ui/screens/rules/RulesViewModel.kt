package dev.bikram.filepipe.ui.screens.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bikram.filepipe.data.preferences.SwipeAction
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.data.repository.FileOperationRepository
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.domain.model.PreviewFileResult
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RunProgress
import dev.bikram.filepipe.domain.model.TriggerType
import dev.bikram.filepipe.domain.usecase.SimulateRuleUseCase
import dev.bikram.filepipe.domain.usecase.ExecuteRulesUseCase
import dev.bikram.filepipe.domain.usecase.RulesAutoExportTrigger
import dev.bikram.filepipe.domain.usecase.ScheduleRulesUseCase
import dev.bikram.filepipe.shortcuts.AppShortcutsManager
import dev.bikram.filepipe.shortcuts.PendingShortcutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeleteUndoEvent(val rules: List<Rule>)

data class PreviewState(
    val ruleName: String,
    val isLoading: Boolean = true,
    val results: List<PreviewFileResult> = emptyList()
)

sealed interface RulesRunNavigation {
    data class HistoryDetail(val historyId: Long) : RulesRunNavigation
    data object HistoryList : RulesRunNavigation
}

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val scheduleRulesUseCase: ScheduleRulesUseCase,
    private val executeRulesUseCase: ExecuteRulesUseCase,
    private val simulateRuleUseCase: SimulateRuleUseCase,
    private val rulesAutoExportTrigger: RulesAutoExportTrigger,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appShortcutsManager: AppShortcutsManager,
    private val pendingShortcutRepository: PendingShortcutRepository,
    private val fileOperationRepository: FileOperationRepository
) : ViewModel() {

    val rules: StateFlow<List<Rule>> = ruleRepository.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _staleRuleIds = MutableStateFlow<Set<Long>>(emptySet())
    /**
     * Stale folder access is recomputed on IO only when folder URIs (per rule) change, not on every
     * rules DB emission (e.g. name/toggle-only updates reuse the last result).
     */
    val staleRuleIds: StateFlow<Set<Long>> = _staleRuleIds.asStateFlow()

    init {
        viewModelScope.launch {
            var lastFolderSignature: String? = null
            rules.collect { ruleList ->
                val signature = folderPathsSignature(ruleList)
                if (signature != lastFolderSignature) {
                    lastFolderSignature = signature
                    _staleRuleIds.value = withContext(Dispatchers.IO) {
                        computeStaleRuleIds(ruleList)
                    }
                }
            }
        }
        // Keep dynamic shortcuts up to date whenever rules change
        rules.onEach { appShortcutsManager.updateShortcuts(it) }.launchIn(viewModelScope)

        // Handle shortcut taps from launcher
        pendingShortcutRepository.pendingRuleId.onEach { ruleId ->
            val rule = rules.value.find { it.id == ruleId }
            if (rule != null) runRule(rule)
        }.launchIn(viewModelScope)
    }

    val swipeStartToEnd: StateFlow<SwipeAction> = userPreferencesRepository.preferencesFlow
        .map { it.swipeStartToEnd }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SwipeAction.EDIT)

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

    private val _deleteUndoEvent = MutableSharedFlow<DeleteUndoEvent>(extraBufferCapacity = 1)
    val deleteUndoEvent = _deleteUndoEvent.asSharedFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() { _userMessage.value = null }

    private val _previewState = MutableStateFlow<PreviewState?>(null)
    val previewState: StateFlow<PreviewState?> = _previewState.asStateFlow()

    fun startPreview(rule: Rule) = viewModelScope.launch {
        _previewState.value = PreviewState(ruleName = rule.name, isLoading = true)
        val results = simulateRuleUseCase(rule)
        _previewState.value = PreviewState(ruleName = rule.name, isLoading = false, results = results)
    }

    fun dismissPreview() { _previewState.value = null }

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

        val result = results.firstOrNull()
        if (result != null) {
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

    private fun folderPathsSignature(ruleList: List<Rule>): String =
        ruleList.sortedBy { it.id }.joinToString("\u0000") { rule ->
            buildString {
                append(rule.id)
                append('\u0001')
                rule.sourceFolderPaths.sorted().forEach { path ->
                    append(path)
                    append('\u0002')
                }
                append('\u0001')
                append(rule.destinationFolderPath)
            }
        }

    private fun computeStaleRuleIds(ruleList: List<Rule>): Set<Long> =
        ruleList.filter { rule ->
            val allPaths = rule.sourceFolderPaths + listOfNotNull(rule.destinationFolderPath.takeIf { it.isNotBlank() })
            allPaths.any { path ->
                !path.startsWith("content://") || !fileOperationRepository.isAccessible(path)
            }
        }.map { it.id }.toSet()
}
