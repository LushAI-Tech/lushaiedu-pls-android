package com.lushaiedupls.ui.parent.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.AppNotification
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Notifications ViewModel for the Parent org.
 *
 * Fetches notifications via [StudentRepository.notifications] (same API endpoint as student)
 * but updates [ParentRepository.setUnreadNotificationCount] to keep the parent's badge in sync.
 */
data class ParentNotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class ParentNotificationsViewModel(
    private val studentRepository: StudentRepository,
    private val parentRepository: ParentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentNotificationsUiState(isLoading = true))
    val uiState: StateFlow<ParentNotificationsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = studentRepository.notifications()) {
                is NetworkResult.Success -> {
                    val list = StudentUiMappers.notifications(result.data)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notifications = list,
                        )
                    }
                    // Sync unread count badge into the parent repository
                    parentRepository.setUnreadNotificationCount(list.count { it.unread })
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
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
            // Sync badge to zero
            parentRepository.setUnreadNotificationCount(0)
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
            // Only decrement if it was truly unread
            parentRepository.decrementUnreadNotificationCount()
            studentRepository.markNotificationRead(id)
        }
    }

    companion object {
        fun provideFactory(
            studentRepository: StudentRepository,
            parentRepository: ParentRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            ParentNotificationsViewModel(studentRepository, parentRepository)
        }
    }
}
