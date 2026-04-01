package dev.bikram.filepipe.ui.screens.ruledetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bikram.filepipe.data.repository.FileEntry
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.domain.model.ConflictPolicy
import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RuleIcon
import dev.bikram.filepipe.domain.model.RuleSchedule
import dev.bikram.filepipe.domain.model.RuleTemplate
import dev.bikram.filepipe.domain.usecase.PreviewRuleUseCase
import dev.bikram.filepipe.domain.usecase.RulesAutoExportTrigger
import dev.bikram.filepipe.domain.usecase.ScheduleRulesUseCase
import dev.bikram.filepipe.domain.usecase.ValidateRuleUseCase
import dev.bikram.filepipe.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RuleDetailUiState(
    val id: Long = 0,
    val name: String = "",
    val sourceFolderPaths: List<String> = emptyList(),
    val destinationFolderPath: String = "",
    val fileExtensions: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val schedule: RuleSchedule? = null,
    val conflictPolicy: ConflictPolicy = ConflictPolicy.RENAME_SUFFIX,
    val operationMode: OperationMode = OperationMode.MOVE,
    val scanSubdirectories: Boolean = false,
    val icon: RuleIcon = RuleIcon.DEFAULT,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val errors: List<String> = emptyList(),
    val previewFiles: List<FileEntry>? = null,
    val isPreviewLoading: Boolean = false
)

private data class RuleSnapshot(
    val name: String,
    val sourceFolderPaths: List<String>,
    val destinationFolderPath: String,
    val fileExtensions: List<String>,
    val schedule: RuleSchedule?,
    val conflictPolicy: ConflictPolicy,
    val operationMode: OperationMode,
    val scanSubdirectories: Boolean,
    val icon: RuleIcon
)

private fun RuleDetailUiState.toSnapshot(): RuleSnapshot = RuleSnapshot(
    name = name.trim(),
    sourceFolderPaths = sourceFolderPaths.toList(),
    destinationFolderPath = destinationFolderPath,
    fileExtensions = fileExtensions.toList(),
    schedule = schedule,
    conflictPolicy = conflictPolicy,
    operationMode = operationMode,
    scanSubdirectories = scanSubdirectories,
    icon = icon
)

@HiltViewModel
class RuleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ruleRepository: RuleRepository,
    private val scheduleRulesUseCase: ScheduleRulesUseCase,
    private val validateRuleUseCase: ValidateRuleUseCase,
    private val previewRuleUseCase: PreviewRuleUseCase,
    private val rulesAutoExportTrigger: RulesAutoExportTrigger
) : ViewModel() {

    private val ruleId: Long = savedStateHandle[Screen.RuleDetail.ARG_RULE_ID] ?: Screen.RuleDetail.NEW_RULE_ID
    val isNewRule = ruleId == Screen.RuleDetail.NEW_RULE_ID

    private val _uiState = MutableStateFlow(RuleDetailUiState())
    val uiState: StateFlow<RuleDetailUiState> = _uiState.asStateFlow()

    private val _baseline = MutableStateFlow<RuleSnapshot?>(null)

    val isDirty: StateFlow<Boolean> = combine(_uiState, _baseline) { state, baseline ->
        baseline != null && state.toSnapshot() != baseline
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        if (!isNewRule) {
            loadRule()
        } else {
            _uiState.update { it.copy(isLoading = false) }
            _baseline.value = _uiState.value.toSnapshot()
        }
    }

    private fun loadRule() = viewModelScope.launch {
        val rule = ruleRepository.getRuleById(ruleId)
        if (rule != null) {
            _uiState.update {
                it.copy(
                    id = rule.id,
                    name = rule.name,
                    sourceFolderPaths = rule.sourceFolderPaths,
                    destinationFolderPath = rule.destinationFolderPath,
                    fileExtensions = rule.fileExtensions,
                    isEnabled = rule.isEnabled,
                    schedule = rule.schedule,
                    conflictPolicy = rule.conflictPolicy,
                    operationMode = rule.operationMode,
                    scanSubdirectories = rule.scanSubdirectories,
                    icon = rule.icon,
                    isLoading = false
                )
            }
            _baseline.value = _uiState.value.toSnapshot()
        } else {
            _uiState.update { it.copy(isLoading = false) }
            _baseline.value = _uiState.value.toSnapshot()
        }
    }

    fun setName(name: String) = _uiState.update { it.copy(name = name, errors = emptyList()) }

    fun addSourceFolder(path: String) = _uiState.update {
        if (path in it.sourceFolderPaths) it
        else it.copy(sourceFolderPaths = it.sourceFolderPaths + path)
    }

    fun removeSourceFolder(path: String) = _uiState.update {
        it.copy(sourceFolderPaths = it.sourceFolderPaths - path)
    }

    fun replaceSourceFolder(previousPath: String, newPath: String) = _uiState.update { state ->
        if (previousPath !in state.sourceFolderPaths) state
        else {
            val withoutPrevious = state.sourceFolderPaths - previousPath
            val nextPaths =
                if (newPath in withoutPrevious) withoutPrevious
                else withoutPrevious + newPath
            state.copy(sourceFolderPaths = nextPaths)
        }
    }

    fun setDestination(path: String) = _uiState.update { it.copy(destinationFolderPath = path) }

    fun addExtension(ext: String) = _uiState.update {
        val normalized = ext.lowercase().let { e -> if (e.startsWith(".")) e else ".$e" }
        if (normalized in it.fileExtensions) it
        else it.copy(fileExtensions = it.fileExtensions + normalized)
    }

    fun addExtensions(exts: List<String>) {
        exts.forEach { addExtension(it) }
    }

    fun removeExtension(ext: String) = _uiState.update {
        it.copy(fileExtensions = it.fileExtensions - ext)
    }

    fun setSchedule(schedule: RuleSchedule?) = _uiState.update { it.copy(schedule = schedule) }

    fun setConflictPolicy(policy: ConflictPolicy) = _uiState.update { it.copy(conflictPolicy = policy) }

    fun setOperationMode(mode: OperationMode) = _uiState.update { it.copy(operationMode = mode) }

    fun setScanSubdirectories(enabled: Boolean) = _uiState.update { it.copy(scanSubdirectories = enabled) }

    fun setIcon(icon: RuleIcon) = _uiState.update { it.copy(icon = icon) }

    fun applyTemplate(template: RuleTemplate) {
        _uiState.update { state ->
            val mergedSources = (state.sourceFolderPaths + template.suggestedSourcePaths).distinct()
            state.copy(
                name = if (state.name.isBlank()) template.name else state.name,
                fileExtensions = template.extensions,
                operationMode = template.operationMode,
                scanSubdirectories = template.scanSubdirectories,
                sourceFolderPaths = mergedSources,
                icon = template.suggestedIcon
            )
        }
    }

    fun dismissPreview() = _uiState.update { it.copy(previewFiles = null) }

    fun loadPreview() = viewModelScope.launch {
        val state = _uiState.value
        if (state.sourceFolderPaths.isEmpty() || state.fileExtensions.isEmpty()) return@launch
        _uiState.update { it.copy(isPreviewLoading = true, previewFiles = null) }
        val rule = buildRuleFromState(state)
        val files = previewRuleUseCase(rule)
        _uiState.update { it.copy(previewFiles = files, isPreviewLoading = false) }
    }

    fun save() = viewModelScope.launch {
        val state = _uiState.value
        val rule = buildRuleFromState(state)

        when (val result = validateRuleUseCase(rule)) {
            is ValidateRuleUseCase.Result.Invalid -> {
                _uiState.update { it.copy(errors = result.errors) }
                return@launch
            }
            is ValidateRuleUseCase.Result.Valid -> {}
        }

        val savedId = ruleRepository.saveRule(rule)
        val savedRule = rule.copy(id = savedId)

        if (savedRule.isEnabled && savedRule.schedule != null) {
            scheduleRulesUseCase.scheduleRule(savedRule)
        } else {
            scheduleRulesUseCase.cancelRule(savedRule)
        }

        _uiState.update {
            it.copy(id = savedId, errors = emptyList())
        }
        _baseline.value = _uiState.value.toSnapshot()
        rulesAutoExportTrigger.maybeExportAfterRuleChange()
        _uiState.update { it.copy(isSaved = true) }
    }

    private fun buildRuleFromState(state: RuleDetailUiState) = Rule(
        id = state.id,
        name = state.name.trim(),
        sourceFolderPaths = state.sourceFolderPaths,
        destinationFolderPath = state.destinationFolderPath,
        fileExtensions = state.fileExtensions,
        isEnabled = state.isEnabled,
        schedule = state.schedule,
        conflictPolicy = state.conflictPolicy,
        operationMode = state.operationMode,
        scanSubdirectories = state.scanSubdirectories,
        icon = state.icon
    )
}
