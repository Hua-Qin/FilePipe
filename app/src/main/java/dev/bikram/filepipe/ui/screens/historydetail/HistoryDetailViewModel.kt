package dev.bikram.filepipe.ui.screens.historydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.domain.model.FileMoved
import dev.bikram.filepipe.domain.model.RunHistory
import dev.bikram.filepipe.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runHistoryRepository: RunHistoryRepository
) : ViewModel() {

    private val historyId: Long = savedStateHandle[Screen.HistoryDetail.ARG_HISTORY_ID] ?: 0L

    private val _history = MutableStateFlow<RunHistory?>(null)
    val history: StateFlow<RunHistory?> = _history.asStateFlow()

    val files: StateFlow<List<FileMoved>> = runHistoryRepository.getFilesForRun(historyId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _history.value = runHistoryRepository.getHistoryById(historyId)
        }
    }
}
