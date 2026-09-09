package com.lushaiedupls.ui.parent.fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.FeeHistoryMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.FeeHistoryResponse
import com.lushaiedupls.data.remote.dto.LinkedStudentOut
import com.lushaiedupls.data.remote.needsAdminApproval
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentFeesUiState(
    val children: List<LinkedStudentOut> = emptyList(),
    val selectedStudentId: String? = null,
    val history: FeeHistoryResponse? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val needsApproval: Boolean = false,
)

class ParentFeesViewModel(
    private val parentRepository: ParentRepository,
    initialStudentId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ParentFeesUiState(selectedStudentId = initialStudentId, isLoading = true),
    )
    val uiState: StateFlow<ParentFeesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectStudent(studentId: String) {
        if (_uiState.value.selectedStudentId == studentId) return
        _uiState.update { it.copy(selectedStudentId = studentId) }
        loadHistory(asPullRefresh = false)
    }

    fun refresh(asPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (asPullRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true, isRefreshing = false) }
            }
            when (val result = parentRepository.linkedStudents()) {
                is NetworkResult.Success -> {
                    val children = result.data
                    val selected = _uiState.value.selectedStudentId
                        ?.takeIf { id -> children.any { it.student.id == id } }
                        ?: children.firstOrNull()?.student?.id
                    _uiState.update {
                        it.copy(
                            children = children,
                            selectedStudentId = selected,
                            needsApproval = false,
                            errorMessage = null,
                        )
                    }
                    loadHistory(asPullRefresh = asPullRefresh)
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

    private fun loadHistory(asPullRefresh: Boolean = false) {
        val studentId = _uiState.value.selectedStudentId
        if (studentId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    history = null,
                    errorMessage = null,
                    needsApproval = false,
                )
            }
            return
        }
        viewModelScope.launch {
            if (!asPullRefresh) {
                _uiState.update { it.copy(isLoading = true) }
            }
            when (val result = parentRepository.studentFeeHistory(studentId, month = "all")) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        history = FeeHistoryMappers.subjectFeeHistoryOnly(result.data),
                        errorMessage = null,
                        needsApproval = false,
                    )
                }
                else -> {
                    val pending = result.needsAdminApproval()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            history = if (asPullRefresh) it.history else null,
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
            parentRepository: ParentRepository,
            studentId: String?,
        ): ViewModelProvider.Factory = viewModelFactory {
            ParentFeesViewModel(parentRepository, studentId)
        }
    }
}
