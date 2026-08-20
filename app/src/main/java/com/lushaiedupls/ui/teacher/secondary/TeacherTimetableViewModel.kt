package com.lushaiedupls.ui.teacher.secondary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.mock.TeacherTeachingTimetable
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherTimetableUiState(
    val timetable: TeacherTeachingTimetable? = null,
    val subjects: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class TeacherTimetableViewModel(
    private val teacherRepository: TeacherRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherTimetableUiState(isLoading = true))
    val uiState: StateFlow<TeacherTimetableUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = teacherRepository.timetable()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        timetable = TeacherUiMappers.teachingTimetable(result.data),
                        subjects = TeacherUiMappers.timetableSubjects(result.data),
                    )
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            teacherRepository: TeacherRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherTimetableViewModel(teacherRepository)
        }
    }
}
