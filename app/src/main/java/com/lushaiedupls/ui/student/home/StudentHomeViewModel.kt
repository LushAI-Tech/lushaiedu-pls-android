package com.lushaiedupls.ui.student.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.needsAdminApproval
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentHomeViewModel(
    private val userSessionStore: UserSessionStore,
    private val studentRepository: StudentRepository,
) : ViewModel() {

    private val cachedOverview = studentRepository.getCachedOverview()

    private val _uiState = MutableStateFlow(
        StudentHomeUiState(
            displayName = cachedOverview?.student?.name?.ifBlank { userSessionStore.getDisplayName() }
                ?: userSessionStore.getDisplayName(),
            isLoading = cachedOverview == null,
            notificationCount = cachedOverview?.unread_notifications ?: 0,
            overviewMetrics = cachedOverview?.let(StudentUiMappers::overviewMetrics).orEmpty(),
            sessionSummary = cachedOverview?.let(StudentUiMappers::sessionSummary),
            attendancePreview = cachedOverview?.let(StudentUiMappers::attendancePreview).orEmpty(),
        ),
    )
    val uiState: StateFlow<StudentHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            studentRepository.unreadNotificationCount.collect { count ->
                if (count != null) {
                    _uiState.update { it.copy(notificationCount = count) }
                }
            }
        }
        refresh()
        prefetchAiLearn()
    }

    private fun prefetchAiLearn() {
        viewModelScope.launch(Dispatchers.IO) {
            studentRepository.prefetchAiLearn()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.overviewMetrics.isNotEmpty()
            _uiState.update {
                if (hasContent) {
                    it.copy(isRefreshing = true, errorMessage = null)
                } else {
                    it.copy(isLoading = true, errorMessage = null)
                }
            }
            when (val result = studentRepository.overview()) {
                is NetworkResult.Success -> {
                    val overview = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            needsApproval = false,
                            errorMessage = null,
                            displayName = overview.student.name.ifBlank {
                                userSessionStore.getDisplayName()
                            },
                            notificationCount = overview.unread_notifications,
                            overviewMetrics = StudentUiMappers.overviewMetrics(overview),
                            sessionSummary = StudentUiMappers.sessionSummary(overview),
                            attendancePreview = StudentUiMappers.attendancePreview(overview),
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        needsApproval = result.needsAdminApproval(),
                        errorMessage = if (_uiState.value.overviewMetrics.isEmpty()) result.userMessage() else null,
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            studentRepository: StudentRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            StudentHomeViewModel(userSessionStore, studentRepository)
        }
    }
}
