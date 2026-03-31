package dev.bikram.filepipe.ui.screens.ruledetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RuleSchedule
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
    val sourceFolderUris: List<String> = emptyList(),
    val destinationFolderUri: String = "",
    val fileExtensions: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val schedule: RuleSchedule? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val errors: List<String> = emptyList()
)

private data class RuleSnapshot(
    val name: String,
    val sourceFolderUris: List<String>,
    val destinationFolderUri: String,
    val fileExtensions: List<String>,
    val schedule: RuleSchedule?
)

private fun RuleDetailUiState.toSnapshot(): RuleSnapshot = RuleSnapshot(
    name = name.trim(),
    sourceFolderUris = sourceFolderUris.toList(),
    destinationFolderUri = destinationFolderUri,
    fileExtensions = fileExtensions.toList(),
    schedule = schedule
)

@HiltViewModel
class RuleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ruleRepository: RuleRepository,
    private val scheduleRulesUseCase: ScheduleRulesUseCase,
    private val validateRuleUseCase: ValidateRuleUseCase,
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
                    sourceFolderUris = rule.sourceFolderUris,
                    destinationFolderUri = rule.destinationFolderUri,
                    fileExtensions = rule.fileExtensions,
                    isEnabled = rule.isEnabled,
                    schedule = rule.schedule,
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

    fun addSourceFolder(uri: String) = _uiState.update {
        if (uri in it.sourceFolderUris) it
        else it.copy(sourceFolderUris = it.sourceFolderUris + uri)
    }

    fun removeSourceFolder(uri: String) = _uiState.update {
        it.copy(sourceFolderUris = it.sourceFolderUris - uri)
    }

    fun setDestination(uri: String) = _uiState.update { it.copy(destinationFolderUri = uri) }

    fun addExtension(ext: String) = _uiState.update {
        val normalized = ext.lowercase().let { extension -> if (extension.startsWith(".")) extension else ".$extension" }
        if (normalized in it.fileExtensions) it
        else it.copy(fileExtensions = it.fileExtensions + normalized)
    }

    fun removeExtension(ext: String) = _uiState.update {
        it.copy(fileExtensions = it.fileExtensions - ext)
    }

    fun setSchedule(schedule: RuleSchedule?) = _uiState.update { it.copy(schedule = schedule) }

    fun save() = viewModelScope.launch {
        val state = _uiState.value
        val rule = Rule(
            id = state.id,
            name = state.name.trim(),
            sourceFolderUris = state.sourceFolderUris,
            destinationFolderUri = state.destinationFolderUri,
            fileExtensions = state.fileExtensions,
            isEnabled = state.isEnabled,
            schedule = state.schedule
        )

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
}
