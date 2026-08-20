package com.lushaiedupls.ui.parent.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.viewModelFactory
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ParentHomeViewModel(
    private val userSessionStore: UserSessionStore,
    private val parentRepository: ParentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ParentHomeUiState(
            displayName = userSessionStore.getDisplayName(),
            isLoading = true,
        ),
    )
    val uiState: StateFlow<ParentHomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectStudent(studentId: String) {
        _uiState.update { it.copy(selectedStudentId = studentId) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val month = YearMonth.now().toString()
            when (val result = parentRepository.overview(month)) {
                is NetworkResult.Success -> {
                    val children = result.data.children
                    val selected = _uiState.value.selectedStudentId
                        ?.takeIf { id -> children.any { it.student.id == id } }
                        ?: children.firstOrNull()?.student?.id
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            displayName = result.data.parent.name.ifBlank {
                                userSessionStore.getDisplayName()
                            },
                            monthLabel = result.data.month,
                            notificationCount = result.data.unread_notifications,
                            children = children,
                            selectedStudentId = selected,
                            errorMessage = null,
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.userMessage(),
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            parentRepository: ParentRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            ParentHomeViewModel(userSessionStore, parentRepository)
        }
    }
}
