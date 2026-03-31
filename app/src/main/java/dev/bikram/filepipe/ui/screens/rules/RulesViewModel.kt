package dev.bikram.filepipe.ui.screens.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RunProgress
import dev.bikram.filepipe.domain.model.TriggerType
import dev.bikram.filepipe.domain.usecase.ExecuteRulesUseCase
import dev.bikram.filepipe.domain.usecase.RulesAutoExportTrigger
import dev.bikram.filepipe.domain.usecase.ScheduleRulesUseCase
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

sealed interface RulesRunNavigation {
    data class HistoryDetail(val historyId: Long) : RulesRunNavigation
    data object HistoryList : RulesRunNavigation
}

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val scheduleRulesUseCase: ScheduleRulesUseCase,
    private val executeRulesUseCase: ExecuteRulesUseCase,
    private val rulesAutoExportTrigger: RulesAutoExportTrigger
) : ViewModel() {

    val rules: StateFlow<List<Rule>> = ruleRepository.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedRuleIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedRuleIds: StateFlow<Set<Long>> = _selectedRuleIds.asStateFlow()

    private val _progressMap = MutableStateFlow<Map<Long, RunProgress>>(emptyMap())
    val progressMap: StateFlow<Map<Long, RunProgress>> = _progressMap.asStateFlow()

    val isRunning: StateFlow<Boolean> = _progressMap
        .map { progressMap -> progressMap.values.any { !it.isComplete } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _navigateAfterRun = MutableSharedFlow<RulesRunNavigation>(extraBufferCapacity = 1)
    val navigateAfterRun = _navigateAfterRun.asSharedFlow()

    fun toggleSelection(ruleId: Long) {
        _selectedRuleIds.update { current ->
            if (ruleId in current) current - ruleId else current + ruleId
        }
    }

    fun clearSelection() {
        _selectedRuleIds.value = emptySet()
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

        when {
            results.size == 1 -> _navigateAfterRun.emit(
                RulesRunNavigation.HistoryDetail(results.first().historyId)
            )
            results.isNotEmpty() -> _navigateAfterRun.emit(RulesRunNavigation.HistoryList)
        }
        clearProgress()
    }
}
