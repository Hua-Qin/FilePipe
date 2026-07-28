package dev.bikram.filepipe.ui.screens.historydetail

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bikram.filepipe.data.repository.RunHistoryRepository
import dev.bikram.filepipe.domain.model.FileMoved
import dev.bikram.filepipe.domain.model.RunHistory
import dev.bikram.filepipe.domain.usecase.UndoRunUseCase
import dev.bikram.filepipe.shortcuts.PendingHistoryUndoRequest
import dev.bikram.filepipe.shortcuts.PendingShortcutRepository
import dev.bikram.filepipe.ui.feedback.toUserMessage
import dev.bikram.filepipe.ui.navigation.Screen
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val runHistoryRepository: RunHistoryRepository,
        private val undoRunUseCase: UndoRunUseCase,
        private val pendingShortcutRepository: PendingShortcutRepository,
        @param:ApplicationContext private val appContext: Context,
    ) : ViewModel() {
        private val historyId: Long = savedStateHandle[Screen.HistoryDetail.ARG_HISTORY_ID] ?: 0L

        private val _history = MutableStateFlow<RunHistory?>(null)
        val history: StateFlow<RunHistory?> = _history.asStateFlow()

        val isUndoing: StateFlow<Boolean> =
            undoRunUseCase.activeUndoProgress
                .map { it.containsKey(historyId) }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    undoRunUseCase.isUndoInProgress(historyId),
                )

        val undoProgress: StateFlow<Float?> =
            undoRunUseCase.activeUndoProgress
                .map { it[historyId] }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    undoRunUseCase.getUndoProgress(historyId),
                )

        val pendingHistoryUndoRequest: StateFlow<PendingHistoryUndoRequest?> =
            pendingShortcutRepository.pendingHistoryUndoRequest

        val files: StateFlow<List<FileMoved>> =
            runHistoryRepository
                .getFilesForRun(historyId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        // One-shot snackbar messages: a Channel so each is delivered exactly once.
        private val _userMessages = Channel<String>(Channel.BUFFERED)
        val userMessages: Flow<String> = _userMessages.receiveAsFlow()

        init {
            viewModelScope.launch {
                _history.value = runHistoryRepository.getHistoryById(historyId)
            }
        }

        fun undoRun() {
            if (undoRunUseCase.isUndoInProgress(historyId)) return
            launchUndo()
        }

        fun undoRunFromNotification(request: PendingHistoryUndoRequest) {
            if (request.historyId != historyId) return
            if (undoRunUseCase.isUndoInProgress(historyId)) return
            if (!pendingShortcutRepository.consumePendingHistoryUndo(request)) {
                return
            }
            launchUndo(notificationId = request.notificationId)
        }

        private fun launchUndo(notificationId: Int? = null) {
            viewModelScope.launch {
                if (undoRunUseCase.isUndoInProgress(historyId)) return@launch
                try {
                    val result =
                        withContext(NonCancellable) {
                            undoRunUseCase(historyId)
                        }
                    result.toUserMessage(appContext)?.let { message ->
                        _userMessages.trySend(message)
                    }
                    _history.value = runHistoryRepository.getHistoryById(historyId)
                } finally {
                    notificationId?.let { completedNotificationId ->
                        NotificationManagerCompat.from(appContext).cancel(completedNotificationId)
                    }
                }
            }
        }
    }
