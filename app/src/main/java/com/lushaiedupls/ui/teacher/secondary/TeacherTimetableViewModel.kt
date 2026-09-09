package com.lushaiedupls.ui.teacher.secondary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.mapper.TimetableSubjectParser
import com.lushaiedupls.data.mock.TeacherTeachingTimetable
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.DayOfWeek
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.SlotInput
import com.lushaiedupls.data.remote.dto.toSlotInput
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitStatus
import com.lushaiedupls.data.remote.dto.WeekSlot
import com.lushaiedupls.data.remote.dto.WeekView
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.teacher.overlays.SessionSubjectOption
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherTimetableUiState(
    val timetable: TeacherTeachingTimetable? = null,
    val institutions: List<InstitutionOut> = emptyList(),
    val selectedInstitutionId: String? = null,
    val classes: List<ClassOut> = emptyList(),
    val selectedClassId: String? = null,
    val teachingUnits: List<TeachingUnitOut> = emptyList(),
    val sessionSubjects: List<SessionSubjectOption> = emptyList(),
    val isLoadingSubjects: Boolean = false,
    val rawWeekView: WeekView? = null,
    val rawPeriods: List<PeriodOut> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class TeacherTimetableViewModel(
    private val teacherRepository: TeacherRepository,
    private val editable: Boolean = false,
    private val initialInstitutionId: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherTimetableUiState(isLoading = true))
    val uiState: StateFlow<TeacherTimetableUiState> = _uiState.asStateFlow()

    private val dayOrder = listOf(
        DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED, DayOfWeek.THU,
        DayOfWeek.FRI, DayOfWeek.SAT, DayOfWeek.SUN,
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.timetable != null ||
                _uiState.value.institutions.isNotEmpty()
            _uiState.update {
                if (hasContent) {
                    it.copy(isRefreshing = true, isLoading = false, errorMessage = null)
                } else {
                    it.copy(isLoading = true, isRefreshing = false, errorMessage = null)
                }
            }
            val institutions = loadInstitutions()
            if (institutions == null) {
                _uiState.update { it.copy(isRefreshing = false) }
                return@launch
            }
            val selectedInstitutionId = resolveInstitutionId(institutions)
            _uiState.update {
                it.copy(institutions = institutions, selectedInstitutionId = selectedInstitutionId)
            }
            if (selectedInstitutionId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        classes = emptyList(),
                        selectedClassId = null,
                        timetable = emptyTimetable(),
                        errorMessage = "Please select an institution.",
                    )
                }
                return@launch
            }
            if (editable) {
                loadForInstitution(selectedInstitutionId)
            } else {
                refreshMyTimetable(selectedInstitutionId)
            }
        }
    }

    fun selectInstitution(index: Int) {
        val selected = _uiState.value.institutions.getOrNull(index) ?: return
        if (selected.id == _uiState.value.selectedInstitutionId) return
        _uiState.update {
            it.copy(
                selectedInstitutionId = selected.id,
                selectedClassId = null,
                classes = emptyList(),
                sessionSubjects = emptyList(),
                rawWeekView = null,
                rawPeriods = emptyList(),
                timetable = null,
                isLoading = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            if (editable) {
                loadForInstitution(selected.id)
            } else {
                refreshMyTimetable(selected.id)
            }
        }
    }

    fun selectClass(index: Int) {
        if (!editable) return
        val classes = _uiState.value.classes
        val selected = classes.getOrNull(index) ?: return
        if (selected.id == _uiState.value.selectedClassId) return
        val institutionId = _uiState.value.selectedInstitutionId
        _uiState.update {
            it.copy(
                selectedClassId = selected.id,
                sessionSubjects = emptyList(),
                isLoadingSubjects = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            val week = if (!institutionId.isNullOrBlank()) {
                when (val result = teacherRepository.weekTimetable(institutionId, selected.id)) {
                    is NetworkResult.Success -> result.data
                    else -> _uiState.value.rawWeekView
                }
            } else {
                _uiState.value.rawWeekView
            }
            val subjects = loadClassSubjects(
                selected.id,
                _uiState.value.teachingUnits,
                institutionId,
            )
            _uiState.update {
                it.copy(
                    rawWeekView = week ?: it.rawWeekView,
                    sessionSubjects = subjects,
                    isLoadingSubjects = false,
                    timetable = mapClassTimetable(
                        week = week ?: it.rawWeekView,
                        periods = it.rawPeriods,
                        classes = classes,
                        units = it.teachingUnits,
                        selectedClassId = selected.id,
                    ),
                    errorMessage = if (week == null && institutionId != null) {
                        it.errorMessage
                    } else {
                        null
                    },
                )
            }
        }
    }

    fun prepareSessionSubjects() {
        viewModelScope.launch {
            val classId = _uiState.value.selectedClassId
                ?: _uiState.value.classes.firstOrNull()?.id
            val institutionId = _uiState.value.selectedInstitutionId
            _uiState.update { it.copy(isLoadingSubjects = true) }
            val subjects = loadClassSubjects(classId, _uiState.value.teachingUnits, institutionId)
            _uiState.update {
                it.copy(
                    selectedClassId = classId ?: it.selectedClassId,
                    sessionSubjects = subjects,
                    isLoadingSubjects = false,
                )
            }
        }
    }

    fun saveSlot(
        timeIndex: Int,
        dayIndex: Int,
        subjectId: String,
        subjectName: String,
        room: String,
        onDone: (Boolean) -> Unit = {},
    ) {
        val period = displayPeriods().getOrNull(timeIndex)
        val dayOfWeek = dayOrder.getOrNull(dayIndex)
        val classId = _uiState.value.selectedClassId
        val institutionId = _uiState.value.selectedInstitutionId
        val unit = unitForSubject(classId, subjectId, subjectName)
        if (period == null || dayOfWeek == null || classId == null || institutionId == null || unit == null) {
            onDone(false)
            return
        }
        val classInstitution = _uiState.value.classes.firstOrNull { it.id == classId }?.institution_id
        if (!sameInstitution(period.institution_id, institutionId) ||
            !sameInstitution(unit.institution_id, institutionId) ||
            !sameInstitution(classInstitution, institutionId)
        ) {
            _uiState.update {
                it.copy(errorMessage = "Period and class must belong to the selected institution.")
            }
            onDone(false)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val week = currentWeek()
            val myUnitIds = myUnitIds()
            val occupant = flattenSlots(week).firstOrNull { slot ->
                slot.period_id == period.id && slot.day_of_week == dayOfWeek
            }
            if (occupant != null && occupant.teaching_unit_id !in myUnitIds) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "This slot is taken by ${occupant.teacher_name ?: "another teacher"}.",
                    )
                }
                onDone(false)
                return@launch
            }
            // Full-replace only this teacher's own teaching unit (never another teacher's unit).
            val updated = slotsOf(week, unit.id).filterNot {
                it.period_id == period.id && it.day_of_week == dayOfWeek
            } + SlotInput.of(
                subjectId = subjectId.ifBlank { unit.subject_id },
                periodId = period.id,
                dayOfWeek = dayOfWeek,
                room = room,
            )
            when (val result = teacherRepository.setSlots(unit.id, updated)) {
                is NetworkResult.Success -> {
                    reloadOccupancy()
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
        val period = displayPeriods().getOrNull(timeIndex)
        val dayOfWeek = dayOrder.getOrNull(dayIndex)
        val classId = _uiState.value.selectedClassId
        if (period == null || dayOfWeek == null || classId == null) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val week = currentWeek()
            val myUnitIds = myUnitIds()
            val occupant = flattenSlots(week).firstOrNull { slot ->
                slot.period_id == period.id && slot.day_of_week == dayOfWeek
            }
            if (occupant == null) {
                _uiState.update { it.copy(isSaving = false) }
                onDone(true)
                return@launch
            }
            if (occupant.teaching_unit_id !in myUnitIds) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "You can only clear your own slots.",
                    )
                }
                onDone(false)
                return@launch
            }
            val remaining = slotsOf(week, occupant.teaching_unit_id).filterNot {
                it.period_id == period.id && it.day_of_week == dayOfWeek
            }
            when (val result = teacherRepository.setSlots(occupant.teaching_unit_id, remaining)) {
                is NetworkResult.Success -> {
                    reloadOccupancy()
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

    private suspend fun loadForInstitution(institutionId: String) {
        val snapshot = loadSetSnapshot(institutionId) ?: return
        val selectedId = _uiState.value.selectedClassId
            ?.takeIf { id -> snapshot.classes.any { it.id == id } }
            ?: snapshot.classes.firstOrNull()?.id
        val week = if (selectedId != null) {
            when (val result = teacherRepository.weekTimetable(institutionId, selectedId)) {
                is NetworkResult.Success -> result.data
                else -> snapshot.week
            }
        } else {
            snapshot.week
        }
        val subjects = loadClassSubjects(selectedId, snapshot.units, institutionId)
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                classes = snapshot.classes,
                selectedClassId = selectedId,
                teachingUnits = snapshot.units,
                rawWeekView = week,
                rawPeriods = snapshot.periods,
                sessionSubjects = subjects,
                isLoadingSubjects = false,
                timetable = mapClassTimetable(
                    week = week,
                    periods = snapshot.periods,
                    classes = snapshot.classes,
                    units = snapshot.units,
                    selectedClassId = selectedId,
                ),
            )
        }
    }

    private fun refreshMyTimetable(institutionId: String) {
        viewModelScope.launch {
            val keepContent = _uiState.value.isRefreshing || _uiState.value.timetable != null
            if (!keepContent) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            when (val unitsResult = teacherRepository.teachingUnits()) {
                is NetworkResult.Success -> {
                    val units = unitsResult.data
                        .filter { it.status == TeachingUnitStatus.ACTIVE }
                        .filter { sameInstitution(it.institution_id, institutionId) }
                    when (val result = teacherRepository.timetable(institutionId = institutionId)) {
                        is NetworkResult.Success -> {
                            val periods = result.data.periods.ifEmpty {
                                (teacherRepository.periods(institutionId) as? NetworkResult.Success)
                                    ?.data
                                    .orEmpty()
                            }.filter { it.is_active }.sortedBy { it.sort_order }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    teachingUnits = units,
                                    rawWeekView = result.data,
                                    rawPeriods = periods,
                                    timetable = TeacherUiMappers.teachingTimetable(result.data),
                                )
                            }
                        }
                        else -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = result.userMessage(),
                            )
                        }
                    }
                }
                else -> {
                    when (val result = teacherRepository.timetable(institutionId = institutionId)) {
                        is NetworkResult.Success -> {
                            val periods = result.data.periods.ifEmpty {
                                (teacherRepository.periods(institutionId) as? NetworkResult.Success)
                                    ?.data
                                    .orEmpty()
                            }.filter { it.is_active }.sortedBy { it.sort_order }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    rawWeekView = result.data,
                                    rawPeriods = periods,
                                    timetable = TeacherUiMappers.teachingTimetable(result.data),
                                )
                            }
                        }
                        else -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = result.userMessage(),
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadInstitutions(): List<InstitutionOut>? {
        return when (val result = teacherRepository.institutions()) {
            is NetworkResult.Success -> result.data
                .filter { it.is_active }
                .sortedWith(compareBy({ it.sort_order }, { it.name }))
            else -> {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, errorMessage = result.userMessage())
                }
                null
            }
        }
    }

    private fun resolveInstitutionId(institutions: List<InstitutionOut>): String? {
        val current = _uiState.value.selectedInstitutionId
        return current?.takeIf { id -> institutions.any { it.id == id } }
            ?: initialInstitutionId?.takeIf { id -> institutions.any { it.id == id } }
            ?: institutions.firstOrNull()?.id
    }

    private fun emptyTimetable() = TeacherTeachingTimetable(
        classes = emptyList(),
        days = emptyList(),
        timeSlots = emptyList(),
        cells = emptyMap(),
    )

    private suspend fun loadSetSnapshot(institutionId: String): SetSnapshot? = coroutineScope {
        val classesDeferred = async { teacherRepository.timetableClasses(institutionId) }
        val unitsDeferred = async { teacherRepository.teachingUnits() }
        val weekDeferred = async { teacherRepository.weekTimetable(institutionId) }
        val periodsDeferred = async { teacherRepository.periods(institutionId) }
        val classesResult = classesDeferred.await()
        val unitsResult = unitsDeferred.await()
        val weekResult = weekDeferred.await()
        val periodsResult = periodsDeferred.await()

        val units = (unitsResult as? NetworkResult.Success)?.data.orEmpty()
            .filter { it.status == TeachingUnitStatus.ACTIVE }
            .filter { sameInstitution(it.institution_id, institutionId) }
        val listedClasses = (classesResult as? NetworkResult.Success)?.data.orEmpty()
            .filter { it.is_active }
            .sortedBy { it.sort_order }
        val classes = mergeClasses(listedClasses, units, institutionId)
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
            _uiState.update { it.copy(isLoading = false, errorMessage = message, isRefreshing = false) }
            return@coroutineScope null
        }
        SetSnapshot(classes = classes, units = units, week = week, periods = periods)
    }

    private fun loadClassSubjects(
        classId: String?,
        units: List<TeachingUnitOut>,
        institutionId: String?,
    ): List<SessionSubjectOption> {
        val className = classId?.let { id -> _uiState.value.classes.firstOrNull { it.id == id }?.name }
        val assigned = units.filter { unit ->
            unit.status == TeachingUnitStatus.ACTIVE &&
                (institutionId.isNullOrBlank() || sameInstitution(unit.institution_id, institutionId))
        }
        return TimetableSubjectParser.subjectsFromUnits(
            units = assigned,
            classId = classId,
            className = className,
        )
    }

    private fun unitForSubject(
        classId: String?,
        subjectId: String,
        subjectName: String,
    ): TeachingUnitOut? {
        val active = _uiState.value.teachingUnits.filter { it.status == TeachingUnitStatus.ACTIVE }
        val units = active.filter { classId == null || it.class_id == classId }.ifEmpty { active }
        return units.firstOrNull { it.subject_id == subjectId }
            ?: units.firstOrNull { it.id == subjectId }
            ?: units.firstOrNull { it.subject_name.equals(subjectName, ignoreCase = true) }
    }

    private fun myUnitIds(): Set<String> =
        _uiState.value.teachingUnits.map { it.id }.toSet()

    private suspend fun reloadOccupancy() {
        val institutionId = _uiState.value.selectedInstitutionId ?: return
        val classId = _uiState.value.selectedClassId
        val week = when (val result = teacherRepository.weekTimetable(institutionId, classId)) {
            is NetworkResult.Success -> result.data
            else -> {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
                return
            }
        }
        val periods = week.periods.filter { it.is_active }.sortedBy { it.sort_order }
            .ifEmpty { _uiState.value.rawPeriods }
        _uiState.update {
            it.copy(
                isSaving = false,
                rawWeekView = week,
                rawPeriods = periods,
                timetable = mapClassTimetable(
                    week = week,
                    periods = periods,
                    classes = it.classes,
                    units = it.teachingUnits,
                    selectedClassId = classId,
                ),
                errorMessage = null,
            )
        }
    }

    private fun unitById(unitId: String): TeachingUnitOut? =
        _uiState.value.teachingUnits.firstOrNull { it.id == unitId }

    private fun flattenSlots(week: WeekView): List<WeekSlot> =
        week.days.values.flatten()

    private fun slotsOf(week: WeekView, unitId: String): List<SlotInput> {
        val fallbackSubjectId = unitById(unitId)?.subject_id
        return flattenSlots(week)
            .filter { it.teaching_unit_id == unitId }
            .distinctBy { it.slot_id }
            .map { it.toSlotInput(fallbackSubjectId = fallbackSubjectId) }
    }

    private suspend fun currentWeek(): WeekView {
        val institutionId = _uiState.value.selectedInstitutionId
        if (institutionId.isNullOrBlank()) {
            return _uiState.value.rawWeekView
                ?: WeekView(periods = _uiState.value.rawPeriods, days = emptyMap())
        }
        val classId = _uiState.value.selectedClassId.takeIf { editable }
        return when (
            val result = if (editable) {
                teacherRepository.weekTimetable(institutionId, classId)
            } else {
                teacherRepository.timetable(institutionId = institutionId)
            }
        ) {
            is NetworkResult.Success -> result.data
            else -> _uiState.value.rawWeekView
                ?: WeekView(periods = _uiState.value.rawPeriods, days = emptyMap())
        }
    }

    private fun mergeClasses(
        listed: List<ClassOut>,
        units: List<TeachingUnitOut>,
        institutionId: String,
    ): List<ClassOut> {
        val extras = units
            .filter { sameInstitution(it.institution_id, institutionId) }
            .map {
                ClassOut(
                    id = it.class_id,
                    name = it.class_name,
                    sort_order = Int.MAX_VALUE,
                    is_active = true,
                    institution_id = it.institution_id.orEmpty().ifBlank { institutionId },
                    institution_name = it.institution_name,
                )
            }
            .distinctBy { it.id }
            .filter { extra -> listed.none { it.id == extra.id } }
        return listed + extras
    }

    private fun mapClassTimetable(
        week: WeekView?,
        periods: List<PeriodOut>,
        classes: List<ClassOut>,
        units: List<TeachingUnitOut>,
        selectedClassId: String?,
    ): TeacherTeachingTimetable {
        val institutionId = classes.firstOrNull { it.id == selectedClassId }?.institution_id
            ?: _uiState.value.selectedInstitutionId
        val scopedPeriods = TeacherUiMappers.periodsForInstitution(periods, institutionId)
        // Ownership only — do not hide other teachers' slots from /timetable/week.
        val myUnitIds = units.map { it.id }.toSet()
        return TeacherUiMappers.classSetTimetable(
            week = week,
            periods = scopedPeriods,
            classLabels = classes.map { it.name },
            myUnitIds = myUnitIds,
        )
    }

    private fun displayPeriods(): List<PeriodOut> {
        val state = _uiState.value
        return TeacherUiMappers.periodsForInstitution(
            state.rawPeriods,
            state.selectedInstitutionId
                ?: state.classes.firstOrNull { it.id == state.selectedClassId }?.institution_id,
        )
    }

    private fun sameInstitution(value: String?, expected: String): Boolean {
        val id = value?.takeIf { it.isNotBlank() } ?: return true
        return id == expected
    }

    private data class SetSnapshot(
        val classes: List<ClassOut>,
        val units: List<TeachingUnitOut>,
        val week: WeekView,
        val periods: List<PeriodOut>,
    )

    companion object {
        fun provideFactory(
            teacherRepository: TeacherRepository,
            editable: Boolean = false,
            initialInstitutionId: String? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherTimetableViewModel(teacherRepository, editable, initialInstitutionId)
        }
    }
}
