package com.lushaiedupls.ui.admin.periods

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.mock.TeacherTeachingTimetable
import com.lushaiedupls.data.mock.TeacherTimetableCell
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.DayOfWeek
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.SlotInput
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitStatus
import com.lushaiedupls.data.remote.dto.WeekSlot
import com.lushaiedupls.data.remote.dto.WeekView
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.teacher.secondary.TeacherTimetableScreen
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminTimetableClass(
    val id: String,
    val name: String,
)

data class AdminTimetableUiState(
    val classes: List<AdminTimetableClass> = emptyList(),
    val selectedClassId: String? = null,
    val subjects: List<String> = emptyList(),
    val timetable: TeacherTeachingTimetable? = null,
    val teachingUnits: List<TeachingUnitOut> = emptyList(),
    val rawWeekView: WeekView? = null,
    val rawPeriods: List<PeriodOut> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class AdminTimetableViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminTimetableUiState(isLoading = true))
    val uiState: StateFlow<AdminTimetableUiState> = _uiState.asStateFlow()

    private val dayOrder = listOf(
        DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED, DayOfWeek.THU, DayOfWeek.FRI, DayOfWeek.SAT,
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val snapshot = loadSnapshot()
            if (snapshot == null) return@launch
            val selectedId = _uiState.value.selectedClassId
                ?.takeIf { id -> snapshot.classes.any { it.id == id } }
                ?: snapshot.classes.firstOrNull()?.id
            _uiState.update {
                it.copy(
                    isLoading = false,
                    classes = snapshot.classes,
                    selectedClassId = selectedId,
                    teachingUnits = snapshot.units,
                    rawWeekView = snapshot.week,
                    rawPeriods = snapshot.periods,
                    subjects = subjectsFor(snapshot.units, selectedId),
                    timetable = mapTimetable(
                        week = snapshot.week,
                        periods = snapshot.periods,
                        classes = snapshot.classes,
                        units = snapshot.units,
                        selectedClassId = selectedId,
                    ),
                )
            }
        }
    }

    fun selectClass(index: Int) {
        val classes = _uiState.value.classes
        val selected = classes.getOrNull(index) ?: return
        if (selected.id == _uiState.value.selectedClassId) return
        val state = _uiState.value
        _uiState.update {
            it.copy(
                selectedClassId = selected.id,
                subjects = subjectsFor(state.teachingUnits, selected.id),
                timetable = mapTimetable(
                    week = state.rawWeekView,
                    periods = state.rawPeriods,
                    classes = classes,
                    units = state.teachingUnits,
                    selectedClassId = selected.id,
                ),
            )
        }
    }

    fun saveSlot(
        timeIndex: Int,
        dayIndex: Int,
        subjectName: String,
        room: String,
        onDone: (Boolean) -> Unit = {},
    ) {
        val period = _uiState.value.rawPeriods.getOrNull(timeIndex)
        val dayOfWeek = dayOrder.getOrNull(dayIndex)
        val classId = _uiState.value.selectedClassId
        val unit = _uiState.value.teachingUnits.firstOrNull { candidate ->
            candidate.class_id == classId &&
                candidate.status == TeachingUnitStatus.ACTIVE &&
                candidate.subject_name.equals(subjectName, ignoreCase = true)
        }
        if (period == null || dayOfWeek == null || classId == null || unit == null) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val week = currentWeek()
            val classUnitIds = classUnitIds(classId)
            val occupants = flattenSlots(week).filter { slot ->
                slot.period_id == period.id &&
                    slot.day_of_week == dayOfWeek &&
                    slot.teaching_unit_id in classUnitIds &&
                    slot.teaching_unit_id != unit.id
            }
            for (occupant in occupants.distinctBy { it.teaching_unit_id }) {
                val remaining = slotsOf(week, occupant.teaching_unit_id).filterNot {
                    it.period_id == period.id && it.day_of_week == dayOfWeek
                }
                when (val result = adminRepository.setSlots(occupant.teaching_unit_id, remaining)) {
                    is NetworkResult.Success -> Unit
                    else -> {
                        _uiState.update {
                            it.copy(isSaving = false, errorMessage = result.userMessage())
                        }
                        onDone(false)
                        return@launch
                    }
                }
            }
            val updated = slotsOf(week, unit.id).filterNot {
                it.period_id == period.id && it.day_of_week == dayOfWeek
            } + SlotInput(
                period_id = period.id,
                day_of_week = dayOfWeek,
                room = room.trim().ifBlank { null },
            )
            when (val result = adminRepository.setSlots(unit.id, updated)) {
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
        val period = _uiState.value.rawPeriods.getOrNull(timeIndex)
        val dayOfWeek = dayOrder.getOrNull(dayIndex)
        val classId = _uiState.value.selectedClassId
        if (period == null || dayOfWeek == null || classId == null) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val week = currentWeek()
            val occupants = flattenSlots(week).filter { slot ->
                slot.period_id == period.id &&
                    slot.day_of_week == dayOfWeek &&
                    slot.teaching_unit_id in classUnitIds(classId)
            }
            if (occupants.isEmpty()) {
                _uiState.update { it.copy(isSaving = false) }
                onDone(true)
                return@launch
            }
            for (occupant in occupants.distinctBy { it.teaching_unit_id }) {
                val remaining = slotsOf(week, occupant.teaching_unit_id).filterNot {
                    it.period_id == period.id && it.day_of_week == dayOfWeek
                }
                when (val result = adminRepository.setSlots(occupant.teaching_unit_id, remaining)) {
                    is NetworkResult.Success -> Unit
                    else -> {
                        _uiState.update {
                            it.copy(isSaving = false, errorMessage = result.userMessage())
                        }
                        onDone(false)
                        return@launch
                    }
                }
            }
            refresh()
            onDone(true)
        }
    }

    private suspend fun loadSnapshot(): Snapshot? = coroutineScope {
        val classesDeferred = async { adminRepository.listClasses(includeInactive = false) }
        val unitsDeferred = async { adminRepository.teachingUnits() }
        val weekDeferred = async { adminRepository.timetable() }
        val periodsDeferred = async { adminRepository.periods(includeInactive = false) }
        val classesResult = classesDeferred.await()
        val unitsResult = unitsDeferred.await()
        val weekResult = weekDeferred.await()
        val periodsResult = periodsDeferred.await()

        val units = (unitsResult as? NetworkResult.Success)?.data.orEmpty()
            .filter { it.status == TeachingUnitStatus.ACTIVE }
        val listedClasses = (classesResult as? NetworkResult.Success)?.data.orEmpty()
            .filter { it.is_active }
            .sortedBy { it.sort_order }
        val classes = mergeClasses(listedClasses, units)
        val listedPeriods = (periodsResult as? NetworkResult.Success)?.data.orEmpty()
            .filter { it.is_active }
            .sortedBy { it.sort_order }
        val week = when (weekResult) {
            is NetworkResult.Success -> weekResult.data
            else -> WeekView(periods = listedPeriods, days = emptyMap())
        }
        val periods = week.periods.filter { it.is_active }.sortedBy { it.sort_order }
            .ifEmpty { listedPeriods }
        if (weekResult !is NetworkResult.Success &&
            periodsResult !is NetworkResult.Success &&
            classesResult !is NetworkResult.Success &&
            unitsResult !is NetworkResult.Success
        ) {
            val message = listOf(weekResult, periodsResult, classesResult, unitsResult)
                .firstOrNull { it !is NetworkResult.Success }
                ?.userMessage()
                .orEmpty()
            _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            return@coroutineScope null
        }
        Snapshot(classes = classes, units = units, week = week, periods = periods)
    }

    private suspend fun currentWeek(): WeekView {
        return when (val result = adminRepository.timetable()) {
            is NetworkResult.Success -> result.data
            else -> _uiState.value.rawWeekView ?: WeekView(periods = _uiState.value.rawPeriods, days = emptyMap())
        }
    }

    private fun classUnitIds(classId: String): Set<String> =
        _uiState.value.teachingUnits
            .filter { it.class_id == classId }
            .map { it.id }
            .toSet()

    private fun flattenSlots(week: WeekView): List<WeekSlot> =
        week.days.values.flatten()

    private fun slotsOf(week: WeekView, unitId: String): List<SlotInput> =
        flattenSlots(week)
            .filter { it.teaching_unit_id == unitId }
            .distinctBy { it.slot_id }
            .map { slot ->
                SlotInput(
                    period_id = slot.period_id,
                    day_of_week = slot.day_of_week,
                    room = slot.room,
                )
            }

    private fun subjectsFor(units: List<TeachingUnitOut>, classId: String?): List<String> =
        units.filter { it.class_id == classId }
            .map { it.subject_name }
            .distinct()
            .sorted()

    private fun mergeClasses(
        listed: List<ClassOut>,
        units: List<TeachingUnitOut>,
    ): List<AdminTimetableClass> {
        val fromClasses = listed.map { AdminTimetableClass(id = it.id, name = it.name) }
        val extras = units
            .map { AdminTimetableClass(id = it.class_id, name = it.class_name) }
            .distinctBy { it.id }
            .filter { extra -> fromClasses.none { it.id == extra.id } }
        return fromClasses + extras
    }

    private fun mapTimetable(
        week: WeekView?,
        periods: List<PeriodOut>,
        classes: List<AdminTimetableClass>,
        units: List<TeachingUnitOut>,
        selectedClassId: String?,
    ): TeacherTeachingTimetable {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val timeSlots = periods.map { TeacherUiMappers.periodTimeLabel(it.start_time, it.end_time) }
        val unitIds = units.filter { it.class_id == selectedClassId }.map { it.id }.toSet()
        val cells = mutableMapOf<Pair<Int, Int>, TeacherTimetableCell>()
        val slots = week?.let { flattenSlots(it) }.orEmpty()
        periods.forEachIndexed { timeIndex, period ->
            dayOrder.forEachIndexed { dayIndex, day ->
                val slot = slots.firstOrNull { candidate ->
                    candidate.period_id == period.id &&
                        candidate.day_of_week == day &&
                        candidate.teaching_unit_id in unitIds
                } ?: return@forEachIndexed
                cells[timeIndex to dayIndex] = TeacherTimetableCell(
                    subject = slot.subject_name,
                    detail = slot.room.orEmpty().ifBlank {
                        slot.teacher_name.orEmpty()
                    },
                )
            }
        }
        return TeacherTeachingTimetable(
            classes = classes.map { it.name }.ifEmpty { listOf("All classes") },
            days = days,
            timeSlots = timeSlots.ifEmpty { listOf("—") },
            cells = cells,
        )
    }

    private data class Snapshot(
        val classes: List<AdminTimetableClass>,
        val units: List<TeachingUnitOut>,
        val week: WeekView,
        val periods: List<PeriodOut>,
    )

    companion object {
        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminTimetableViewModel(adminRepository) }
    }
}

@Composable
fun AdminTimetableRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminTimetableViewModel = viewModel(
        factory = AdminTimetableViewModel.provideFactory(adminRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val title = stringResource(R.string.teacher_set_timetable_title)
    when {
        uiState.isLoading && uiState.timetable == null && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.Timetable, title = title, modifier = modifier)
        uiState.errorMessage != null && uiState.timetable == null -> LoadErrorPanel(
            screenTitle = title,
            message = uiState.errorMessage.orEmpty(),
            onRetry = viewModel::refresh,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> TeacherTimetableScreen(
            timetable = uiState.timetable ?: TeacherTeachingTimetable(
                classes = emptyList(),
                days = emptyList(),
                timeSlots = emptyList(),
                cells = emptyMap(),
            ),
            subjects = uiState.subjects,
            editable = true,
            onSelectClass = viewModel::selectClass,
            onSaveSlot = viewModel::saveSlot,
            onClearSlot = viewModel::clearSlot,
            onBack = onBack,
            modifier = modifier,
        )
    }
}
