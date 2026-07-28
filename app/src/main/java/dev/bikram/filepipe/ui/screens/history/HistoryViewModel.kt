package dev.bikram.filepipe.ui.screens.history

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.preferences.AppPreferences
import dev.bikram.filepipe.data.preferences.UserPreferencesRepository
import dev.bikram.filepipe.data.repository.RuleRepository
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.domain.model.HistorySortDirection
import dev.bikram.filepipe.domain.model.HistorySortKey
import dev.bikram.filepipe.domain.model.HistoryStatusFilter
import dev.bikram.filepipe.domain.model.RunHistory
import dev.bikram.filepipe.domain.model.RunStatus
import dev.bikram.filepipe.domain.model.isEffectivelyUndone
import dev.bikram.filepipe.domain.model.isNoChangesRun
import dev.bikram.filepipe.domain.usecase.RulesAutoExportTrigger
import dev.bikram.filepipe.domain.usecase.ScheduleRulesUseCase
import dev.bikram.filepipe.domain.usecase.UndoRunUseCase
import dev.bikram.filepipe.ui.feedback.toUserMessage
import dev.bikram.filepipe.ui.navigation.Screen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

enum class HistoryStatusSection {
    SUCCESS,
    FAILED,
    PARTIAL,
    NO_CHANGES,
    IN_PROGRESS,
    CANCELLED,
    PARTIAL_UNDONE,
    UNDONE,
}

sealed interface HistoryItem {
    data class Entry(
        val history: RunHistory,
    ) : HistoryItem

    data class DateHeader(
        val label: String,
    ) : HistoryItem

    data class RuleHeader(
        val ruleName: String,
        val count: Int,
    ) : HistoryItem

    data class StatusHeader(
        val section: HistoryStatusSection,
        val count: Int,
    ) : HistoryItem
}

enum class HistoryViewMode { BY_DATE, BY_RULE, BY_STATUS }

enum class HistorySection { RUNS, TRASH }

private data class HistoryGroupCounts(
    val rules: Map<String, Int> = emptyMap(),
    val statuses: Map<Int, Int> = emptyMap(),
)

data class HistoryUiState(
    val statusFilter: HistoryStatusFilter = HistoryStatusFilter.ALL,
    val viewMode: HistoryViewMode = HistoryViewMode.BY_DATE,
    val sortKey: HistorySortKey = HistorySortKey.LAST_RAN,
    val sortDirection: HistorySortDirection = HistorySortDirection.DESCENDING,
) {
    val isFilterActive: Boolean
        get() = statusFilter != HistoryStatusFilter.ALL || viewMode != HistoryViewMode.BY_DATE
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val runHistoryRepository: RunHistoryRepository,
        private val ruleRepository: RuleRepository,
        private val scheduleRulesUseCase: ScheduleRulesUseCase,
        private val rulesAutoExportTrigger: RulesAutoExportTrigger,
        private val undoRunUseCase: UndoRunUseCase,
        private val userPreferencesRepository: UserPreferencesRepository,
        @param:ApplicationContext private val appContext: Context,
    ) : ViewModel() {
        val filterRuleId: Long? =
            savedStateHandle
                .get<Long>(Screen.HistoryForRule.ARG_RULE_ID)
                ?.takeIf { it > 0 }

        private val _statusFilter = MutableStateFlow(HistoryStatusFilter.ALL)
        private val _viewMode = MutableStateFlow(HistoryViewMode.BY_DATE)
        private val _section = MutableStateFlow(HistorySection.RUNS)
        val section: StateFlow<HistorySection> = _section.asStateFlow()

        private val historySortPreferencesFlow =
            userPreferencesRepository.preferencesFlow
                .map { prefs -> prefs.historySortKey to prefs.historySortDirection }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    AppPreferences.DEFAULT.historySortKey to AppPreferences.DEFAULT.historySortDirection,
                )

        val uiState: StateFlow<HistoryUiState> =
            combine(
                _statusFilter,
                _viewMode,
                historySortPreferencesFlow,
            ) { status, mode, sortParams ->
                val (sortKey, sortDir) = sortParams
                HistoryUiState(
                    statusFilter = status,
                    viewMode = mode,
                    sortKey = sortKey,
                    sortDirection = sortDir,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

        val hasAnyHistory: StateFlow<Boolean> =
            runHistoryRepository
                .observeHasAnyHistory()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        val trashedRules =
            ruleRepository
                .getTrashedRules()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val availableStatusFilters: StateFlow<Set<HistoryStatusFilter>> =
            runHistoryRepository
                .observeAvailableHistoryStatusFilters(filterRuleId)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    setOf(HistoryStatusFilter.ALL),
                )

        val historyPagingFlow: Flow<PagingData<HistoryItem>> =
            combine(
                _statusFilter,
                _viewMode,
                historySortPreferencesFlow,
            ) { status, mode, sortParams ->
                Triple(status, mode, sortParams)
            }.flatMapLatest { (status, mode, sortParams) ->
                val (sortKey, sortDirection) = sortParams
                val groupingCounts =
                    when (mode) {
                        HistoryViewMode.BY_DATE -> {
                            flowOf(HistoryGroupCounts())
                        }

                        HistoryViewMode.BY_RULE -> {
                            runHistoryRepository
                                .observeHistoryRuleCounts(filterRuleId, status)
                                .map { counts -> HistoryGroupCounts(rules = counts) }
                        }

                        HistoryViewMode.BY_STATUS -> {
                            runHistoryRepository
                                .observeHistoryStatusSectionCounts(filterRuleId, status)
                                .map { counts -> HistoryGroupCounts(statuses = counts) }
                        }
                    }
                groupingCounts.flatMapLatest { counts ->
                    runHistoryRepository
                        .getHistoryPaged(
                            ruleId = filterRuleId,
                            statusFilter = status,
                            groupByRule = mode == HistoryViewMode.BY_RULE,
                            groupByStatus = mode == HistoryViewMode.BY_STATUS,
                            sortKey = sortKey,
                            sortDirection = sortDirection,
                        ).map { pagingData ->
                            pagingData
                                .map { history -> HistoryItem.Entry(history) as HistoryItem }
                                .insertSeparators { before, after ->
                                    val afterEntry = after as? HistoryItem.Entry ?: return@insertSeparators null
                                    val beforeEntry = before as? HistoryItem.Entry
                                    when (mode) {
                                        HistoryViewMode.BY_DATE -> {
                                            if (beforeEntry == null ||
                                                !isSameDay(beforeEntry.history.startedAt, afterEntry.history.startedAt)
                                            ) {
                                                HistoryItem.DateHeader(formatDateLabel(afterEntry.history.startedAt))
                                            } else {
                                                null
                                            }
                                        }

                                        HistoryViewMode.BY_RULE -> {
                                            if (beforeEntry?.history?.ruleName != afterEntry.history.ruleName) {
                                                HistoryItem.RuleHeader(
                                                    ruleName = afterEntry.history.ruleName,
                                                    count = counts.rules[afterEntry.history.ruleName] ?: 0,
                                                )
                                            } else {
                                                null
                                            }
                                        }

                                        HistoryViewMode.BY_STATUS -> {
                                            val afterSection = historyStatusSection(afterEntry.history)
                                            if (beforeEntry == null || historyStatusSection(beforeEntry.history) != afterSection) {
                                                HistoryItem.StatusHeader(
                                                    section = afterSection,
                                                    count = counts.statuses[afterSection.ordinal] ?: 0,
                                                )
                                            } else {
                                                null
                                            }
                                        }
                                    }
                                }
                        }
                }
            }.cachedIn(viewModelScope)

        val visibleRunIds: StateFlow<List<Long>> =
            combine(
                _statusFilter,
                _viewMode,
                _section,
                historySortPreferencesFlow,
            ) { status, mode, section, sortParams ->
                Triple(status, mode, section) to sortParams
            }.flatMapLatest { (selection, sortParams) ->
                val (status, mode, section) = selection
                val (sortKey, sortDirection) = sortParams
                if (section == HistorySection.TRASH) {
                    flowOf(emptyList())
                } else {
                    runHistoryRepository.observeVisibleHistoryIds(
                        ruleId = filterRuleId,
                        statusFilter = status,
                        groupByRule = mode == HistoryViewMode.BY_RULE,
                        groupByStatus = mode == HistoryViewMode.BY_STATUS,
                        sortKey = sortKey,
                        sortDirection = sortDirection,
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        // One-shot snackbar messages: a Channel so each is delivered exactly once (no rotation
        // replay, no conflation of identical/rapid messages).
        private val _userMessages = Channel<String>(Channel.BUFFERED)
        val userMessages: Flow<String> = _userMessages.receiveAsFlow()

        private fun postUserMessage(message: String) {
            _userMessages.trySend(message)
        }

        fun setStatusFilter(status: HistoryStatusFilter) {
            _statusFilter.value = status
        }

        fun setViewMode(mode: HistoryViewMode) {
            _viewMode.value = mode
        }

        fun setSort(
            key: HistorySortKey,
            direction: HistorySortDirection,
        ) = viewModelScope.launch {
            userPreferencesRepository.setHistorySort(key, direction)
        }

        fun setSection(section: HistorySection) {
            _section.value = section
        }

        fun clearFilters() {
            _statusFilter.value = HistoryStatusFilter.ALL
            _viewMode.value = HistoryViewMode.BY_DATE
        }

        fun clearAllHistory() =
            viewModelScope.launch {
                runHistoryRepository.clearAllHistory()
            }

        fun deleteHistoryEntry(historyId: Long) =
            viewModelScope.launch {
                runHistoryRepository.deleteHistoryById(historyId)
            }

        fun restoreRule(ruleId: Long) =
            viewModelScope.launch {
                val rule = trashedRules.value.firstOrNull { it.id == ruleId }
                ruleRepository.restoreRuleFromTrash(ruleId)
                if (rule?.isEnabled == true && rule.schedule != null) {
                    scheduleRulesUseCase.scheduleRule(rule)
                }
                rulesAutoExportTrigger.maybeExportAfterRuleChange()
            }

        fun deleteRuleForever(ruleId: Long) =
            viewModelScope.launch {
                ruleRepository.deleteRuleForever(ruleId)
                rulesAutoExportTrigger.maybeExportAfterRuleChange()
            }

        fun emptyTrashForever() =
            viewModelScope.launch {
                ruleRepository.emptyTrashForever()
                rulesAutoExportTrigger.maybeExportAfterRuleChange()
            }

        fun undoRun(historyId: Long) =
            viewModelScope.launch {
                if (undoRunUseCase.isUndoInProgress(historyId)) return@launch
                val result = undoRunUseCase(historyId)
                result.toUserMessage(appContext)?.let { message ->
                    postUserMessage(message)
                }
            }

        private fun formatDateLabel(timestampMs: Long): String {
            val zone = ZoneId.systemDefault()
            val day = Instant.ofEpochMilli(timestampMs).atZone(zone).toLocalDate()
            val today = LocalDate.now(zone)
            return when (day) {
                today -> appContext.getString(R.string.history_date_today)
                today.minusDays(1) -> appContext.getString(R.string.history_date_yesterday)
                else -> day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            }
        }

        private fun isSameDay(
            t1: Long,
            t2: Long,
        ): Boolean {
            val zone = ZoneId.systemDefault()
            return Instant.ofEpochMilli(t1).atZone(zone).toLocalDate() ==
                Instant.ofEpochMilli(t2).atZone(zone).toLocalDate()
        }
    }

private fun historyStatusSection(history: RunHistory): HistoryStatusSection =
    when {
        history.isEffectivelyUndone() -> HistoryStatusSection.UNDONE
        history.isNoChangesRun() -> HistoryStatusSection.NO_CHANGES
        history.status == RunStatus.IN_PROGRESS -> HistoryStatusSection.IN_PROGRESS
        history.status == RunStatus.CANCELLED -> HistoryStatusSection.CANCELLED
        history.status == RunStatus.FAILED -> HistoryStatusSection.FAILED
        history.status == RunStatus.PARTIAL_FAILURE -> HistoryStatusSection.PARTIAL
        history.status == RunStatus.PARTIAL_UNDONE -> HistoryStatusSection.PARTIAL_UNDONE
        else -> HistoryStatusSection.SUCCESS
    }
