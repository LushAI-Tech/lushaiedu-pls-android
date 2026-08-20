package com.lushaiedupls.ui.student.linkparent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.ParentLinkStatus
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentLinkParentViewModel(
    private val studentRepository: StudentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentLinkParentUiState(isLoadingParents = true))
    val uiState: StateFlow<StudentLinkParentUiState> = _uiState.asStateFlow()
    private var countdownJob: Job? = null

    init {
        refreshParents()
        issueToken()
    }

    fun refreshParents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingParents = true, errorMessage = null) }
            when (val result = studentRepository.myParents()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoadingParents = false,
                        parents = result.data.filter { link -> link.status == ParentLinkStatus.ACTIVE },
                    )
                }
                else -> _uiState.update {
                    it.copy(isLoadingParents = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun issueToken() {
        viewModelScope.launch {
            _uiState.update { it.copy(isIssuing = true, errorMessage = null) }
            when (val result = studentRepository.issueParentLinkToken()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isIssuing = false,
                            token = result.data,
                            remainingSeconds = result.data.expires_in_seconds,
                        )
                    }
                    startCountdown(result.data.expires_in_seconds)
                }
                else -> _uiState.update {
                    it.copy(isIssuing = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun revoke(linkId: String) {
        viewModelScope.launch {
            when (studentRepository.revokeParentLink(linkId)) {
                is NetworkResult.Success -> refreshParents()
                else -> Unit
            }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(1_000)
                left -= 1
                _uiState.update { it.copy(remainingSeconds = left) }
            }
        }
    }

    companion object {
        fun provideFactory(studentRepository: StudentRepository): ViewModelProvider.Factory =
            viewModelFactory { StudentLinkParentViewModel(studentRepository) }
    }
}
