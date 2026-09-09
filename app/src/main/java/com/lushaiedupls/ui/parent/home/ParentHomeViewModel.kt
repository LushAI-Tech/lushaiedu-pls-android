package com.lushaiedupls.ui.parent.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.needsAdminApproval
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.data.repository.StudentRepository
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
    private val studentRepository: StudentRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ParentHomeUiState(
            displayName = userSessionStore.getDisplayName(),
            isLoading = true,
        ),
    )
    val uiState: StateFlow<ParentHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            parentRepository.unreadNotificationCount.collect { count ->
                if (count != null) {
                    _uiState.update { it.copy(notificationCount = count) }
                }
            }
        }
        if (studentRepository != null) {
            viewModelScope.launch {
                studentRepository.unreadNotificationCount.collect { count ->
                    if (count != null) {
                        _uiState.update { it.copy(notificationCount = count) }
                    }
                }
            }
        }
        refresh()
    }

    fun selectStudent(studentId: String) {
        _uiState.update { it.copy(selectedStudentId = studentId) }
    }

    fun refresh(asPullRefresh: Boolean = false) {
        viewModelScope.launch {
            val current = _uiState.value
            val keepStatusPanel = current.needsApproval ||
                (current.errorMessage != null && current.children.isEmpty())
            when {
                asPullRefresh && !keepStatusPanel ->
                    _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                keepStatusPanel ->
                    _uiState.update { it.copy(isLoading = true, isRefreshing = false) }
                current.children.isEmpty() ->
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            isRefreshing = false,
                            errorMessage = null,
                            needsApproval = false,
                        )
                    }
                else ->
                    _uiState.update { it.copy(errorMessage = null, isRefreshing = false) }
            }
            val month = YearMonth.now().toString()
            when (val result = parentRepository.overview(month)) {
                is NetworkResult.Success -> {
                    val overview = result.data
                    val children = overview.children
                    val selected = _uiState.value.selectedStudentId
                        ?.takeIf { id -> children.any { it.student.id == id } }
                        ?: children.firstOrNull()?.student?.id
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            displayName = overview.parent.name.ifBlank {
                                userSessionStore.getDisplayName()
                            },
                            monthLabel = overview.month,
                            notificationCount = overview.unread_notifications,
                            totalChildren = overview.total_children.takeIf { count -> count > 0 }
                                ?: children.size,
                            overallAttendance = overview.overall_attendance,
                            pendingFeePaise = overview.total_pending_fee_amount_paise,
                            childrenWithPendingFees = overview.children_with_pending_fees,
                            openFeedbackCount = overview.feedback_counts.unseen,
                            children = children,
                            selectedStudentId = selected,
                            errorMessage = null,
                            needsApproval = false,
                        )
                    }
                }
                else -> {
                    val pending = result.needsAdminApproval()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            needsApproval = pending,
                            errorMessage = if (pending) null else result.userMessage(),
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            parentRepository: ParentRepository,
            studentRepository: StudentRepository? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            ParentHomeViewModel(userSessionStore, parentRepository, studentRepository)
        }
    }
}
