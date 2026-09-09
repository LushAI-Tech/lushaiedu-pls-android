package com.lushaiedupls.ui.student.secondary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.WeeklyTimetable
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
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

data class TimetableUiState(
    val timetable: WeeklyTimetable? = null,
    val teachingUnits: List<TeachingUnitOut> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val needsApproval: Boolean = false,
    val errorMessage: String? = null,
)

class TimetableViewModel(
    private val studentRepository: StudentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimetableUiState(isLoading = true))
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.timetable != null
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    errorMessage = null,
                )
            }
            coroutineScope {
                val unitsDeferred = async { studentRepository.teachingUnits(forceRefresh = true) }
                val timetableDeferred = async { studentRepository.timetable(forceRefresh = true) }

                val unitsResult = unitsDeferred.await()
                val timetableResult = timetableDeferred.await()

                when (timetableResult) {
                    is NetworkResult.Success -> {
                        val units = (unitsResult as? NetworkResult.Success)?.data.orEmpty()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                needsApproval = false,
                                errorMessage = null,
                                teachingUnits = units,
                                timetable = StudentUiMappers.weeklyTimetable(timetableResult.data, units),
                            )
                        }
                    }
                    else -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            needsApproval = timetableResult.needsAdminApproval(),
                            errorMessage = timetableResult.userMessage(),
                            timetable = null,
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(studentRepository: StudentRepository): ViewModelProvider.Factory =
            viewModelFactory { TimetableViewModel(studentRepository) }
    }
}
