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

    private val initialAiSubjects = studentRepository.getCachedAiSubjects()
    private val initialUnits = studentRepository.getCachedTeachingUnits()
    private val initialDashboard = studentRepository.getCachedProgressDashboard()
    private val initialSubjects = when {
        !initialAiSubjects.isNullOrEmpty() -> StudentUiMappers.aiSubjects(initialAiSubjects)
        !initialUnits.isNullOrEmpty() -> StudentUiMappers.teachingUnitSubjects(initialUnits)
        else -> emptyList()
    }
    private val initialStats = initialDashboard?.let(StudentUiMappers::aiHubStats)
        ?: StudentUiMappers.emptyAiHubStats()

    private val _uiState = MutableStateFlow(
        StudentAiHubUiState(
            isLoading = initialSubjects.isEmpty(),
            subjects = initialSubjects,
            stats = initialStats,
        ),
    )
    val uiState: StateFlow<StudentAiHubUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (_uiState.value.subjects.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            coroutineScope {
                val statsDeferred = async { studentRepository.progressDashboard() }
                val aiSubjectsDeferred = async { studentRepository.aiSubjects() }
                val unitsDeferred = async { studentRepository.teachingUnits() }

                // Process subjects as soon as available
                launch {
                    val aiSubjectsResult = aiSubjectsDeferred.await()
                    val unitsResult = unitsDeferred.await()
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
                    val needsApproval = listOf(aiSubjectsResult, unitsResult).any { it.needsAdminApproval() }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            subjects = subjects.ifEmpty { it.subjects },
                            needsApproval = needsApproval,
                            errorMessage = if (needsApproval) null else error,
                        )
                    }
                    val firstSubjectId = subjects.firstOrNull()?.id
                    if (!firstSubjectId.isNullOrBlank()) {
                        val chResult = studentRepository.chapters(firstSubjectId)
                        if (chResult is NetworkResult.Success) {
                            val activeChapterIds = chResult.data.filter { it.is_active }.map { it.id }
                            if (activeChapterIds.isNotEmpty()) {
                                studentRepository.prefetchAiChat(activeChapterIds)
                            }
                        }
                    }
                }

                // Process stats as soon as available
                launch {
                    val statsResult = statsDeferred.await()
                    if (statsResult is NetworkResult.Success) {
                        _uiState.update {
                            it.copy(stats = StudentUiMappers.aiHubStats(statsResult.data))
                        }
                    }
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
