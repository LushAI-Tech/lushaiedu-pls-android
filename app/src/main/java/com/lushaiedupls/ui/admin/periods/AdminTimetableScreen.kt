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
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.teacher.overlays.SessionSubjectOption
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
    val institutionId: String = "",
)

data class AdminTimetableUiState(
    val institutions: List<InstitutionOut> = emptyList(),
    val selectedInstitutionId: String? = null,
    val classes: List<AdminTimetableClass> = emptyList(),
    val selectedClassId: String? = null,
    val sessionSubjects: List<SessionSubjectOption> = emptyList(),
    val isLoadingSubjects: Boolean = false,
    val timetable: TeacherTeachingTimetable? = null,
    val teachingUnits: List<TeachingUnitOut> = emptyList(),
    val rawWeekView: WeekView? = null,
    val rawPeriods: List<PeriodOut> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class AdminTimetableViewModel(
    private val adminRepository: AdminRepository,
    private val initialInstitutionId: String? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminTimetableUiState(isLoading = true))
    val uiState: StateFlow<AdminTimetableUiState> = _uiState.asStateFlow()

    private val dayOrder = listOf(
        DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED, DayOfWeek.THU,
        DayOfWeek.FRI, DayOfWeek.SAT, DayOfWeek.SUN,
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.institutions.isNotEmpty() ||
                _uiState.value.timetable != null
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    errorMessage = null,
                )
            }
            val institutions = when (val result = adminRepository.timetableInstitutions()) {
                is NetworkResult.Success -> result.data
                    .filter { it.is_active }
                    .sortedWith(compareBy({ it.sort_order }, { it.name }))
                else -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.userMessage(),
                        )
                    }
                    return@launch
                }
            }
            val selectedInstitutionId = _uiState.value.selectedInstitutionId
                ?.takeIf { id -> institutions.any { it.id == id } }
                ?: initialInstitutionId?.takeIf { id -> institutions.any { it.id == id } }
                ?: institutions.firstOrNull()?.id
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
                        timetable = TeacherTeachingTimetable(
                            classes = emptyList(),
                            days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"),
                            timeSlots = listOf("—"),
                            cells = emptyMap(),
                        ),
                        errorMessage = "Please select an institution.",
                    )
                }
                return@launch
            }
            loadForInstitution(selectedInstitutionId)
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
        viewModelScope.launch { loadForInstitution(selected.id) }
    }

    private suspend fun loadForInstitution(institutionId: String) {
        val snapshot = loadSnapshot(institutionId)
        if (snapshot == null) return
        val selectedId = _uiState.value.selectedClassId
            ?.takeIf { id -> snapshot.classes.any { it.id == id } }
            ?: snapshot.classes.firstOrNull()?.id
        val week = if (selectedId != null) {
            when (val result = adminRepository.weekTimetable(institutionId, selectedId)) {
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
                timetable = mapTimetable(
                    week = week,
                    periods = snapshot.periods,
                    classes = snapshot.classes,
                    units = snapshot.units,
                    selectedClassId = selectedId,
                ),
            )
        }
    }

    fun selectClass(index: Int) {
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
                when (val result = adminRepository.weekTimetable(institutionId, selected.id)) {
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
                    timetable = mapTimetable(
                        week = week ?: it.rawWeekView,
                        periods = it.rawPeriods,
                        classes = classes,
                        units = it.teachingUnits,
                        selectedClassId = selected.id,
                    ),
                )
            }
        }
    }

    fun prepareSessionSubjects() {
        viewModelScope.launch {
            val classId = _uiState.value.selectedClassId
                ?: _uiState.value.classes.firstOrNull()?.id
            _uiState.update { it.copy(isLoadingSubjects = true) }
            val subjects = loadClassSubjects(
                classId,
                _uiState.value.teachingUnits,
                _uiState.value.selectedInstitutionId,
            )
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
        val active = _uiState.value.teachingUnits.filter { it.status == TeachingUnitStatus.ACTIVE }
        val units = active.filter { classId == null || it.class_id == classId }.ifEmpty { active }
        val unit = units.firstOrNull { it.subject_id == subjectId }
            ?: units.firstOrNull { it.id == subjectId }
            ?: units.firstOrNull { it.subject_name.equals(subjectName, ignoreCase = true) }
        if (period == null || dayOfWeek == null || classId == null || institutionId == null || unit == null) {
            onDone(false)
            return
        }
        val classInstitution = _uiState.value.classes.firstOrNull { it.id == classId }?.institutionId
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
            } + SlotInput.of(
                subjectId = subjectId.ifBlank { unit.subject_id },
                periodId = period.id,
                dayOfWeek = dayOfWeek,
                room = room,
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

    private suspend fun loadSnapshot(institutionId: String): Snapshot? = coroutineScope {
        val classesDeferred = async { adminRepository.timetableClasses(institutionId) }
        val unitsDeferred = async { adminRepository.teachingUnits() }
        val weekDeferred = async { adminRepository.weekTimetable(institutionId) }
        val periodsDeferred = async {
            adminRepository.periods(includeInactive = false, institutionId = institutionId)
        }
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
            _uiState.update {
                it.copy(isLoading = false, isRefreshing = false, errorMessage = message)
            }
            return@coroutineScope null
        }
        Snapshot(classes = classes, units = units, week = week, periods = periods)
    }

    private suspend fun currentWeek(): WeekView {
        val institutionId = _uiState.value.selectedInstitutionId
        if (institutionId.isNullOrBlank()) {
            return _uiState.value.rawWeekView
                ?: WeekView(periods = _uiState.value.rawPeriods, days = emptyMap())
        }
        return when (
            val result = adminRepository.weekTimetable(
                institutionId = institutionId,
                classId = _uiState.value.selectedClassId,
            )
        ) {
            is NetworkResult.Success -> result.data
            else -> _uiState.value.rawWeekView
                ?: WeekView(periods = _uiState.value.rawPeriods, days = emptyMap())
        }
    }

    private fun classUnitIds(classId: String): Set<String> =
        _uiState.value.teachingUnits
            .filter { it.class_id == classId }
            .map { it.id }
            .toSet()

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

    private suspend fun loadClassSubjects(
        classId: String?,
        units: List<TeachingUnitOut>,
        institutionId: String?,
    ): List<SessionSubjectOption> {
        val className = classId?.let { id -> _uiState.value.classes.firstOrNull { it.id == id }?.name }
        val apiSubjects = if (classId != null && !institutionId.isNullOrBlank()) {
            when (val result = adminRepository.timetableClassSubjects(classId, institutionId)) {
                is NetworkResult.Success -> result.data
                else -> emptyList()
            }
        } else {
            emptyList()
        }
        return TimetableSubjectParser.sessionOptions(
            apiSubjects = apiSubjects,
            units = units,
            classId = classId,
            className = className,
        )
    }

    private fun mergeClasses(
        listed: List<ClassOut>,
        units: List<TeachingUnitOut>,
        institutionId: String,
    ): List<AdminTimetableClass> {
        val fromClasses = listed.map {
            AdminTimetableClass(id = it.id, name = it.name, institutionId = it.institution_id)
        }
        val extras = units
            .filter { sameInstitution(it.institution_id, institutionId) }
            .map {
                AdminTimetableClass(
                    id = it.class_id,
                    name = it.class_name,
                    institutionId = it.institution_id.orEmpty().ifBlank { institutionId },
                )
            }
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
        val scopedPeriods = TeacherUiMappers.periodsForInstitution(
            periods,
            classes.firstOrNull { it.id == selectedClassId }?.institutionId,
        )
        // Admin catalog includes all units — treat them as owned so slots stay editable.
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
                ?: state.classes.firstOrNull { it.id == state.selectedClassId }?.institutionId,
        )
    }

    private fun sameInstitution(value: String?, expected: String): Boolean {
        val id = value?.takeIf { it.isNotBlank() } ?: return true
        return id == expected
    }

    private data class Snapshot(
        val classes: List<AdminTimetableClass>,
        val units: List<TeachingUnitOut>,
        val week: WeekView,
        val periods: List<PeriodOut>,
    )

    companion object {
        fun provideFactory(
            adminRepository: AdminRepository,
            initialInstitutionId: String? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            AdminTimetableViewModel(adminRepository, initialInstitutionId)
        }
    }
}

@Composable
fun AdminTimetableRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    initialInstitutionId: String? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminTimetableViewModel = viewModel(
        factory = AdminTimetableViewModel.provideFactory(adminRepository, initialInstitutionId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val title = stringResource(R.string.teacher_set_timetable_title)
    when {
        uiState.isLoading && uiState.timetable == null && uiState.institutions.isEmpty() &&
            uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.Timetable, title = title, modifier = modifier)
        uiState.errorMessage != null && uiState.timetable == null && uiState.institutions.isEmpty() ->
            LoadErrorPanel(
                screenTitle = title,
                message = uiState.errorMessage.orEmpty(),
                onRetry = viewModel::refresh,
                isRetrying = uiState.isLoading || uiState.isRefreshing,
                modifier = modifier,
            )
        else -> TeacherTimetableScreen(
            timetable = uiState.timetable ?: TeacherTeachingTimetable(
                classes = emptyList(),
                days = emptyList(),
                timeSlots = emptyList(),
                cells = emptyMap(),
            ),
            institutions = uiState.institutions.map { it.name },
            selectedInstitutionIndex = uiState.institutions
                .indexOfFirst { it.id == uiState.selectedInstitutionId }
                .coerceAtLeast(0),
            onSelectInstitution = viewModel::selectInstitution,
            subjects = uiState.sessionSubjects,
            isLoadingSubjects = uiState.isLoadingSubjects,
            editable = true,
            selectedClassIndex = uiState.classes
                .indexOfFirst { it.id == uiState.selectedClassId }
                .coerceAtLeast(0),
            onSelectClass = viewModel::selectClass,
            onLoadSessionSubjects = viewModel::prepareSessionSubjects,
            onSaveSlot = viewModel::saveSlot,
            onClearSlot = viewModel::clearSlot,
            onBack = onBack,
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = modifier,
        )
    }
}
