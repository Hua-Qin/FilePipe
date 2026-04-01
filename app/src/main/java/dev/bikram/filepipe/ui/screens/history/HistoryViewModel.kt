package dev.bikram.filepipe.ui.screens.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.domain.model.RunHistory
import dev.bikram.filepipe.domain.usecase.UndoRunUseCase
import dev.bikram.filepipe.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HistoryGroup(
    val dateLabel: String,
    val entries: List<RunHistory>
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runHistoryRepository: RunHistoryRepository,
    private val undoRunUseCase: UndoRunUseCase
) : ViewModel() {

    val filterRuleId: Long? = savedStateHandle.get<Long>(Screen.HistoryForRule.ARG_RULE_ID)
        ?.takeIf { it > 0 }

    val historyGroups: StateFlow<List<HistoryGroup>> = (
        if (filterRuleId != null) runHistoryRepository.getHistoryForRule(filterRuleId)
        else runHistoryRepository.getAllHistory()
    )
        .map { list -> groupByDate(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() { _userMessage.value = null }

    private fun groupByDate(history: List<RunHistory>): List<HistoryGroup> {
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val now = System.currentTimeMillis()
        val todayKey = dayFormat.format(Date(now))
        val yesterdayKey = dayFormat.format(Date(now - 86_400_000L))
        val labelFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        return history
            .groupBy { dayFormat.format(Date(it.startedAt)) }
            .entries
            .sortedByDescending { it.key }
            .map { (key, entries) ->
                val label = when (key) {
                    todayKey -> "Today"
                    yesterdayKey -> "Yesterday"
                    else -> labelFormat.format(SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(key)!!)
                }
                HistoryGroup(label, entries.sortedByDescending { it.startedAt })
            }
    }

    fun clearAllHistory() = viewModelScope.launch {
        runHistoryRepository.clearAllHistory()
    }

    fun deleteHistoryEntry(historyId: Long) = viewModelScope.launch {
        runHistoryRepository.deleteHistoryById(historyId)
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
