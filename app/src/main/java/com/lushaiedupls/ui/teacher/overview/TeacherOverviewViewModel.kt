package com.lushaiedupls.ui.teacher.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.teacher.overlays.AttendancePeriodOption
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

    fun goToThisAttendanceMonth() {
        val month = YearMonth.now()
        _uiState.update { it.copy(attendanceMonth = month, selectedAttendanceDay = null) }
        refresh(month = month)
    }

    fun selectAttendanceDay(day: Int) {
        val current = _uiState.value
        val unitId = current.selectedUnitId
        if (unitId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    selectedAttendanceDay = day,
                    setupErrorMessage = "Select a class first.",
                )
            }
            return
        }
        val dateLabel = "%04d-%02d-%02d".format(
            current.attendanceMonth.year,
            current.attendanceMonth.monthValue,
            day,
        )
        _uiState.update {
            it.copy(
                selectedAttendanceDay = day,
                setupDateLabel = dateLabel,
                setupScheduledPeriods = emptyList(),
                setupInstitutePeriods = emptyList(),
                isLoadingSetupPeriods = true,
                setupErrorMessage = null,
            )
        }
        loadSetupPeriods(unitId, dateLabel)
    }

    fun dismissAttendanceSetup() {
        _uiState.update {
            it.copy(
                setupDateLabel = null,
                setupScheduledPeriods = emptyList(),
                setupInstitutePeriods = emptyList(),
                isLoadingSetupPeriods = false,
                setupErrorMessage = null,
            )
        }
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

    private fun loadSetupPeriods(unitId: String, dateLabel: String) {
        viewModelScope.launch {
            coroutineScope {
                val dayDeferred = async { teacherRepository.unitDay(unitId, dateLabel) }
                val periodsDeferred = async { teacherRepository.periods() }
                val dayResult = dayDeferred.await()
                val periodsResult = periodsDeferred.await()

                val institutePeriods = when (periodsResult) {
                    is NetworkResult.Success -> periodsResult.data
                        .filter { it.is_active }
                        .sortedBy { it.sort_order }
                        .map { period ->
                            val timeLabel = TeacherUiMappers.periodTimeLabel(
                                period.start_time,
                                period.end_time,
                            )
                            AttendancePeriodOption(
                                periodId = period.id,
                                label = timeLabel,
                                // Docs: extra_label distinguishes extras on the same day (max 30).
                                extraLabel = period.name.trim().take(30).ifBlank {
                                    timeLabel.take(30)
                                },
                            )
                        }
                    else -> emptyList()
                }

                val scheduledPeriods = when (dayResult) {
                    is NetworkResult.Success -> dayResult.data.periods.map { period ->
                        AttendancePeriodOption(
                            periodId = period.period_id,
                            label = TeacherUiMappers.periodTimeLabel(
                                period.start_time,
                                period.end_time,
                            ),
                            extraLabel = period.period_name.trim().take(30),
                            isMarked = period.is_marked,
                        )
                    }
                    else -> emptyList()
                }

                val error = when {
                    dayResult !is NetworkResult.Success &&
                        periodsResult !is NetworkResult.Success ->
                        dayResult.userMessage().ifBlank { periodsResult.userMessage() }
                    scheduledPeriods.isEmpty() && institutePeriods.isEmpty() ->
                        "No periods available."
                    else -> null
                }

                _uiState.update {
                    it.copy(
                        isLoadingSetupPeriods = false,
                        setupScheduledPeriods = scheduledPeriods,
                        setupInstitutePeriods = institutePeriods,
                        setupErrorMessage = error,
                    )
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
