package com.lushaiedupls.ui.parent.fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.FeeLedgerOut
import com.lushaiedupls.data.remote.dto.FeePaymentStatus
import com.lushaiedupls.data.remote.dto.LinkedStudentOut
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
    val rows: List<FeeLedgerOut> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
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
        loadHistory()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = parentRepository.linkedStudents()) {
                is NetworkResult.Success -> {
                    val children = result.data
                    val selected = _uiState.value.selectedStudentId
                        ?.takeIf { id -> children.any { it.student.id == id } }
                        ?: children.firstOrNull()?.student?.id
                    _uiState.update {
                        it.copy(children = children, selectedStudentId = selected)
                    }
                    loadHistory()
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    private fun loadHistory() {
        val studentId = _uiState.value.selectedStudentId
        if (studentId.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, rows = emptyList(), errorMessage = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = parentRepository.studentFeeHistory(studentId, month = "all")) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, rows = sortFeeRows(result.data.rows), errorMessage = null)
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage(), rows = emptyList())
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

private fun sortFeeRows(rows: List<FeeLedgerOut>): List<FeeLedgerOut> =
    rows.sortedWith(
        compareBy<FeeLedgerOut> { it.payment_status == FeePaymentStatus.PAID }
            .thenByDescending { it.month },
    )
