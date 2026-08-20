package com.lushaiedupls.ui.teacher.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.viewModelFactory
import java.time.YearMonth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherOverviewViewModel(
    private val teacherRepository: TeacherRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TeacherOverviewUiState(attendanceMonth = YearMonth.now(), isLoading = true),
    )
    val uiState: StateFlow<TeacherOverviewUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onSectionSelected(section: TeacherOverviewSection) {
        _uiState.update { it.copy(section = section) }
    }

    fun onAttendanceClassSelected(classLabel: String) {
        val unitId = _uiState.value.unitIdsByLabel[classLabel]
        _uiState.update {
            it.copy(selectedAttendanceClass = classLabel, selectedUnitId = unitId)
        }
        refresh(unitId = unitId)
    }

    fun previousAttendanceMonth() {
        val month = _uiState.value.attendanceMonth.minusMonths(1)
        _uiState.update { it.copy(attendanceMonth = month, selectedAttendanceDay = null) }
        refresh(month = month)
    }

    fun nextAttendanceMonth() {
        val month = _uiState.value.attendanceMonth.plusMonths(1)
        _uiState.update { it.copy(attendanceMonth = month, selectedAttendanceDay = null) }
        refresh(month = month)
    }

    fun selectAttendanceDay(day: Int) {
        _uiState.update { it.copy(selectedAttendanceDay = day) }
    }

    fun refresh(
        month: YearMonth = _uiState.value.attendanceMonth,
        unitId: String? = _uiState.value.selectedUnitId,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val monthKey = month.toString()
                val overviewDeferred = async { teacherRepository.overview(month = monthKey) }
                val unitsDeferred = async { teacherRepository.teachingUnits() }
                val overviewResult = overviewDeferred.await()
                val unitsResult = unitsDeferred.await()
                val units = (unitsResult as? NetworkResult.Success)?.data.orEmpty()
                val groups = TeacherUiMappers.groups(units)
                val labels = groups.map { it.title }
                val idsByLabel = groups.associate { it.title to it.id }
                val resolvedUnitId = unitId
                    ?: _uiState.value.selectedUnitId
                    ?: groups.firstOrNull()?.id
                val resolvedLabel = groups.find { it.id == resolvedUnitId }?.title
                    ?: labels.firstOrNull().orEmpty()
                val unitSummary = if (resolvedUnitId != null) {
                    teacherRepository.unitSummary(resolvedUnitId, monthKey)
                } else {
                    null
                }
                when (overviewResult) {
                    is NetworkResult.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            dashboard = TeacherUiMappers.overviewDashboard(
                                overviewResult.data,
                                (unitSummary as? NetworkResult.Success)?.data,
                            ),
                            attendanceClasses = labels,
                            selectedAttendanceClass = resolvedLabel,
                            selectedUnitId = resolvedUnitId,
                            unitIdsByLabel = idsByLabel,
                            attendanceMonth = month,
                        )
                    }
                    else -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = overviewResult.userMessage())
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            teacherRepository: TeacherRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherOverviewViewModel(teacherRepository)
        }
    }
}
