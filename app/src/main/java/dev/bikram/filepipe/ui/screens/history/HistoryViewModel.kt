package dev.bikram.filepipe.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.domain.model.RunHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val runHistoryRepository: RunHistoryRepository
) : ViewModel() {

    val historyGroups: StateFlow<List<HistoryGroup>> = runHistoryRepository.getAllHistory()
        .map { list -> groupByDate(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
}
