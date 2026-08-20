package com.lushaiedupls.ui.student.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.needsAdminApproval
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentAiHubViewModel(
    private val studentRepository: StudentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentAiHubUiState(isLoading = true))
    val uiState: StateFlow<StudentAiHubUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val statsDeferred = async { studentRepository.progressDashboard() }
                val aiSubjectsDeferred = async { studentRepository.aiSubjects() }
                val unitsDeferred = async { studentRepository.teachingUnits() }
                val statsResult = statsDeferred.await()
                val aiSubjectsResult = aiSubjectsDeferred.await()
                val unitsResult = unitsDeferred.await()

                val stats = when (statsResult) {
                    is NetworkResult.Success -> StudentUiMappers.aiHubStats(statsResult.data)
                    else -> StudentUiMappers.emptyAiHubStats()
                }
                val aiSubjects = (aiSubjectsResult as? NetworkResult.Success)?.data.orEmpty()
                val units = (unitsResult as? NetworkResult.Success)?.data.orEmpty()
                val subjects = when {
                    aiSubjects.isNotEmpty() -> StudentUiMappers.aiSubjects(aiSubjects)
                    units.isNotEmpty() -> StudentUiMappers.teachingUnitSubjects(units)
                    else -> emptyList()
                }
                val error = when {
                    subjects.isNotEmpty() -> null
                    aiSubjectsResult !is NetworkResult.Success &&
                        unitsResult !is NetworkResult.Success -> {
                        unitsResult.userMessage().ifBlank { aiSubjectsResult.userMessage() }
                    }
                    else -> null
                }
                val needsApproval = listOf(aiSubjectsResult, unitsResult, statsResult)
                    .any { it.needsAdminApproval() }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stats = stats,
                        subjects = subjects,
                        needsApproval = needsApproval,
                        errorMessage = if (needsApproval) null else error,
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            studentRepository: StudentRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            StudentAiHubViewModel(studentRepository)
        }
    }
}
