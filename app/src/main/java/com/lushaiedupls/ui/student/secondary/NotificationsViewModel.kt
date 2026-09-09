package com.lushaiedupls.ui.student.secondary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.AppNotification
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class NotificationsViewModel(
    private val studentRepository: StudentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.notifications.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    errorMessage = null,
                )
            }
            when (val result = studentRepository.notifications()) {
                is NetworkResult.Success -> {
                    val list = StudentUiMappers.notifications(result.data)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = list,
                        )
                    }
                    studentRepository.setUnreadNotificationCount(list.count { it.unread })
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = result.userMessage(),
                    )
                }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            val unread = _uiState.value.notifications.filter { it.unread }
            _uiState.update { state ->
                state.copy(
                    notifications = state.notifications.map { it.copy(unread = false) },
                )
            }
            studentRepository.setUnreadNotificationCount(0)
            unread.forEach { n -> studentRepository.markNotificationRead(n.id) }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            val current = _uiState.value.notifications.find { it.id == id } ?: return@launch
            if (!current.unread) return@launch
            _uiState.update { state ->
                state.copy(
                    notifications = state.notifications.map { n ->
                        if (n.id == id) n.copy(unread = false) else n
                    },
                )
            }
            studentRepository.markNotificationRead(id)
        }
    }

    companion object {
        fun provideFactory(studentRepository: StudentRepository): ViewModelProvider.Factory =
            viewModelFactory { NotificationsViewModel(studentRepository) }
    }
}
