package com.lushaiedupls.ui.teacher.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherClassOverviewViewModel(
    private val teacherRepository: TeacherRepository,
    private val groupId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherClassOverviewUiState(isLoading = true))
    val uiState: StateFlow<TeacherClassOverviewUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onSectionSelected(section: TeacherClassSection) {
        _uiState.update { it.copy(section = section) }
    }

    fun markParentsSelected(studentId: String) {
        _uiState.update { current ->
            current.copy(
                students = current.students.map { student ->
                    if (student.id == studentId) {
                        student.copy(hasParentsSelected = true)
                    } else {
                        student
                    }
                },
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val unitDeferred = async { teacherRepository.teachingUnit(groupId) }
                val membersDeferred = async { teacherRepository.members(groupId) }
                val summaryDeferred = async { teacherRepository.unitSummary(groupId) }
                val unitResult = unitDeferred.await()
                val membersResult = membersDeferred.await()
                val summaryResult = summaryDeferred.await()
                when (unitResult) {
                    is NetworkResult.Success -> {
                        val members = (membersResult as? NetworkResult.Success)?.data.orEmpty()
                        val summary = (summaryResult as? NetworkResult.Success)?.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                overview = TeacherUiMappers.classOverview(
                                    unit = unitResult.data,
                                    summary = summary,
                                    memberCount = members.size,
                                ),
                                students = TeacherUiMappers.students(members),
                            )
                        }
                    }
                    else -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = unitResult.userMessage())
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            teacherRepository: TeacherRepository,
            groupId: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherClassOverviewViewModel(teacherRepository, groupId)
        }
    }
}
