package com.lushaiedupls.ui.parent.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.student.secondary.TimetableUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ParentChildTimetableViewModel(
    private val parentRepository: ParentRepository,
    private val studentId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimetableUiState(isLoading = true))
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = parentRepository.linkedTimetable()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        timetable = StudentUiMappers.weeklyTimetable(result.data),
                    )
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.userMessage(),
                        timetable = null,
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            parentRepository: ParentRepository,
            studentId: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            ParentChildTimetableViewModel(parentRepository, studentId)
        }
    }
}
