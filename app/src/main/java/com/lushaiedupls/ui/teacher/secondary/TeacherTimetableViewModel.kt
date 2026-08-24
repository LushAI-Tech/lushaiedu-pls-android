package com.lushaiedupls.ui.teacher.secondary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.mock.TeacherTeachingTimetable
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.DayOfWeek
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.SlotInput
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitStatus
import com.lushaiedupls.data.remote.dto.WeekView
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
    val teachingUnits: List<TeachingUnitOut> = emptyList(),
    val selectedTeachingUnitId: String? = null,
    val subjects: List<String> = emptyList(),
    val rawWeekView: WeekView? = null,
    val rawPeriods: List<PeriodOut> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class TeacherTimetableViewModel(
    private val teacherRepository: TeacherRepository,
    private val editable: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherTimetableUiState(isLoading = true))
    val uiState: StateFlow<TeacherTimetableUiState> = _uiState.asStateFlow()

    private val dayOrder = listOf(
        DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED, DayOfWeek.THU, DayOfWeek.FRI, DayOfWeek.SAT,
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val unitsResult = teacherRepository.teachingUnits()) {
                is NetworkResult.Success -> {
                    val units = unitsResult.data.filter { it.status == TeachingUnitStatus.ACTIVE }
                    val currentSelectedId = _uiState.value.selectedTeachingUnitId
                    val activeUnitId = currentSelectedId?.takeIf { id -> units.any { it.id == id } }
                        ?: units.firstOrNull()?.id
                    val subjects = units.map { it.subject_name }.distinct()
                    val classesFromUnits = units.map { u ->
                        if (units.count { it.class_name == u.class_name } > 1) {
                            "${u.class_name} (${u.subject_name})"
                        } else {
                            u.class_name
                        }
                    }

                    if (editable && activeUnitId != null) {
                        when (val timetableResult = teacherRepository.timetable(teachingUnitId = activeUnitId)) {
                            is NetworkResult.Success -> {
                                val periods = timetableResult.data.periods.ifEmpty {
                                    (teacherRepository.periods() as? NetworkResult.Success)?.data.orEmpty()
                                }.filter { it.is_active }.sortedBy { it.sort_order }

                                val mapped = TeacherUiMappers.teachingTimetable(timetableResult.data)
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        teachingUnits = units,
                                        selectedTeachingUnitId = activeUnitId,
                                        rawWeekView = timetableResult.data,
                                        rawPeriods = periods,
                                        timetable = mapped.copy(
                                            classes = classesFromUnits.ifEmpty { mapped.classes },
                                        ),
                                        subjects = subjects.ifEmpty { TeacherUiMappers.timetableSubjects(timetableResult.data) },
                                    )
                                }
                            }
                            else -> {
                                val fallbackPeriods = (teacherRepository.periods() as? NetworkResult.Success)?.data.orEmpty()
                                    .filter { it.is_active }.sortedBy { it.sort_order }
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        teachingUnits = units,
                                        selectedTeachingUnitId = activeUnitId,
                                        rawPeriods = fallbackPeriods,
                                        timetable = TeacherTeachingTimetable(
                                            classes = classesFromUnits,
                                            days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"),
                                            timeSlots = fallbackPeriods.map { p -> TeacherUiMappers.periodTimeLabel(p.start_time, p.end_time) },
                                            cells = emptyMap(),
                                        ),
                                        subjects = subjects,
                                    )
                                }
                            }
                        }
                    } else {
                        // Global timetable (My Timetable view)
                        when (val result = teacherRepository.timetable()) {
                            is NetworkResult.Success -> {
                                val periods = result.data.periods.ifEmpty {
                                    (teacherRepository.periods() as? NetworkResult.Success)?.data.orEmpty()
                                }.filter { it.is_active }.sortedBy { it.sort_order }

                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        teachingUnits = units,
                                        rawWeekView = result.data,
                                        rawPeriods = periods,
                                        timetable = TeacherUiMappers.teachingTimetable(result.data),
                                        subjects = subjects.ifEmpty { TeacherUiMappers.timetableSubjects(result.data) },
                                    )
                                }
                            }
                            else -> _uiState.update {
                                it.copy(isLoading = false, errorMessage = result.userMessage())
                            }
                        }
                    }
                }
                else -> {
                    when (val result = teacherRepository.timetable()) {
                        is NetworkResult.Success -> {
                            val periods = result.data.periods.ifEmpty {
                                (teacherRepository.periods() as? NetworkResult.Success)?.data.orEmpty()
                            }.filter { it.is_active }.sortedBy { it.sort_order }

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    rawWeekView = result.data,
                                    rawPeriods = periods,
                                    timetable = TeacherUiMappers.teachingTimetable(result.data),
                                    subjects = TeacherUiMappers.timetableSubjects(result.data),
                                )
                            }
                        }
                        else -> _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.userMessage())
                        }
                    }
                }
            }
        }
    }

    fun selectClass(index: Int) {
        val units = _uiState.value.teachingUnits
        val unit = units.getOrNull(index) ?: return
        if (unit.id == _uiState.value.selectedTeachingUnitId) return
        _uiState.update { it.copy(selectedTeachingUnitId = unit.id, isLoading = true) }
        viewModelScope.launch {
            when (val result = teacherRepository.timetable(teachingUnitId = unit.id)) {
                is NetworkResult.Success -> {
                    val classesFromUnits = units.map { u ->
                        if (units.count { it.class_name == u.class_name } > 1) {
                            "${u.class_name} (${u.subject_name})"
                        } else {
                            u.class_name
                        }
                    }
                    val periods = result.data.periods.ifEmpty {
                        (teacherRepository.periods() as? NetworkResult.Success)?.data.orEmpty()
                    }.filter { it.is_active }.sortedBy { it.sort_order }

                    val mapped = TeacherUiMappers.teachingTimetable(result.data)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            rawWeekView = result.data,
                            rawPeriods = periods,
                            timetable = mapped.copy(classes = classesFromUnits.ifEmpty { mapped.classes }),
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun saveSlot(
        timeIndex: Int,
        dayIndex: Int,
        subjectName: String,
        room: String,
        onDone: (Boolean) -> Unit = {},
    ) {
        val period = _uiState.value.rawPeriods.getOrNull(timeIndex) ?: run {
            onDone(false)
            return
        }
        val dayOfWeek = dayOrder.getOrNull(dayIndex) ?: run {
            onDone(false)
            return
        }
        val units = _uiState.value.teachingUnits
        val unit = units.find { it.id == _uiState.value.selectedTeachingUnitId }
            ?: units.find { it.subject_name.equals(subjectName, ignoreCase = true) }
            ?: units.firstOrNull() ?: run {
                onDone(false)
                return
            }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val currentResult = teacherRepository.timetable(teachingUnitId = unit.id)
            val currentWeek = (currentResult as? NetworkResult.Success)?.data ?: _uiState.value.rawWeekView

            val currentSlots = currentWeek?.days?.values?.flatten()
                ?.filter { it.teaching_unit_id == unit.id }
                ?.map {
                    SlotInput(
                        period_id = it.period_id,
                        day_of_week = it.day_of_week,
                        room = it.room,
                    )
                }
                .orEmpty()

            val updatedSlots = currentSlots.filterNot {
                it.period_id == period.id && it.day_of_week == dayOfWeek
            } + SlotInput(
                period_id = period.id,
                day_of_week = dayOfWeek,
                room = room.trim().ifBlank { null },
            )

            when (val result = teacherRepository.setSlots(unit.id, updatedSlots)) {
                is NetworkResult.Success -> {
                    refresh()
                    onDone(true)
                }
                else -> {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = result.userMessage())
                    }
                    onDone(false)
                }
            }
        }
    }

    fun clearSlot(
        timeIndex: Int,
        dayIndex: Int,
        onDone: (Boolean) -> Unit = {},
    ) {
        val period = _uiState.value.rawPeriods.getOrNull(timeIndex) ?: run {
            onDone(false)
            return
        }
        val dayOfWeek = dayOrder.getOrNull(dayIndex) ?: run {
            onDone(false)
            return
        }
        val units = _uiState.value.teachingUnits
        val unit = units.find { it.id == _uiState.value.selectedTeachingUnitId }
            ?: units.firstOrNull() ?: run {
                onDone(false)
                return
            }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val currentResult = teacherRepository.timetable(teachingUnitId = unit.id)
            val currentWeek = (currentResult as? NetworkResult.Success)?.data ?: _uiState.value.rawWeekView

            val currentSlots = currentWeek?.days?.values?.flatten()
                ?.filter { it.teaching_unit_id == unit.id }
                ?.map {
                    SlotInput(
                        period_id = it.period_id,
                        day_of_week = it.day_of_week,
                        room = it.room,
                    )
                }
                .orEmpty()

            val updatedSlots = currentSlots.filterNot {
                it.period_id == period.id && it.day_of_week == dayOfWeek
            }

            when (val result = teacherRepository.setSlots(unit.id, updatedSlots)) {
                is NetworkResult.Success -> {
                    refresh()
                    onDone(true)
                }
                else -> {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = result.userMessage())
                    }
                    onDone(false)
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            teacherRepository: TeacherRepository,
            editable: Boolean = false,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherTimetableViewModel(teacherRepository, editable)
        }
    }
}
