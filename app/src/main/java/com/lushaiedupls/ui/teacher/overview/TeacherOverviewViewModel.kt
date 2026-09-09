package com.lushaiedupls.ui.teacher.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.mock.TeacherOverviewDashboard
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitStatus
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.teacher.overlays.AttendancePeriodOption
import java.time.YearMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherOverviewViewModel(
    private val teacherRepository: TeacherRepository,
    private val userSessionStore: UserSessionStore? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TeacherOverviewUiState(attendanceMonth = YearMonth.now(), isLoading = true),
    )
    val uiState: StateFlow<TeacherOverviewUiState> = _uiState.asStateFlow()

    private data class CacheKey(val month: YearMonth, val unitId: String?)

    private data class CacheEntry(
        val dashboard: TeacherOverviewDashboard,
    )

    private val overviewCache = linkedMapOf<CacheKey, CacheEntry>()
    private var cachedTeachingUnits: List<TeachingUnitOut>? = null
    private var cachedInstitutions: List<InstitutionOut>? = null
    private var refreshJob: Job? = null
    private var pickerJob: Job? = null

    init {
        refresh(forceNetwork = false)
    }

    fun onSectionSelected(section: TeacherOverviewSection) {
        _uiState.update { it.copy(section = section) }
    }

    fun onInstitutionSelected(index: Int) {
        val institutionId = _uiState.value.institutionIds.getOrNull(index) ?: return
        if (institutionId == _uiState.value.selectedInstitutionId) return
        userSessionStore?.setInstitutionId(institutionId)
        overviewCache.clear()
        pickerJob?.cancel()
        pickerJob = viewModelScope.launch {
            val unitId = syncAttendancePicker(
                institutionId = institutionId,
                classId = null,
                subjectId = null,
                forceReloadInstitutions = true,
            )
            _uiState.update { it.copy(selectedAttendanceDay = null) }
            loadMonth(
                month = _uiState.value.attendanceMonth,
                unitId = unitId,
                forceNetwork = true,
                asPullRefresh = false,
                showSkeleton = true,
            )
        }
    }

    fun onAttendanceClassSelected(classLabel: String) {
        val classId = _uiState.value.classIdsByLabel[classLabel] ?: return
        if (classId == _uiState.value.selectedClassId) return
        pickerJob?.cancel()
        pickerJob = viewModelScope.launch {
            val unitId = syncAttendancePicker(
                institutionId = _uiState.value.selectedInstitutionId,
                classId = classId,
                subjectId = null,
            )
            _uiState.update { it.copy(selectedAttendanceDay = null) }
            loadMonth(
                month = _uiState.value.attendanceMonth,
                unitId = unitId,
                forceNetwork = false,
                asPullRefresh = false,
                showSkeleton = false,
            )
        }
    }

    fun onAttendanceSubjectSelected(subjectLabel: String) {
        val subjectId = _uiState.value.subjectIdsByLabel[subjectLabel] ?: return
        if (subjectId == _uiState.value.selectedSubjectId) return
        pickerJob?.cancel()
        pickerJob = viewModelScope.launch {
            val unitId = syncAttendancePicker(
                institutionId = _uiState.value.selectedInstitutionId,
                classId = _uiState.value.selectedClassId,
                subjectId = subjectId,
            )
            _uiState.update { it.copy(selectedAttendanceDay = null) }
            loadMonth(
                month = _uiState.value.attendanceMonth,
                unitId = unitId,
                forceNetwork = false,
                asPullRefresh = false,
                showSkeleton = false,
            )
        }
    }

    fun previousAttendanceMonth() {
        showMonth(_uiState.value.attendanceMonth.minusMonths(1))
    }

    fun nextAttendanceMonth() {
        showMonth(_uiState.value.attendanceMonth.plusMonths(1))
    }

    fun selectAttendanceMonth(month: YearMonth) {
        showMonth(month)
    }

    fun selectAttendanceDay(day: Int) {
        val current = _uiState.value
        val unitId = current.selectedUnitId
        if (unitId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    selectedAttendanceDay = day,
                    setupErrorMessage = "Select a class and subject first.",
                )
            }
            return
        }
        if (current.selectedInstitutionId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    selectedAttendanceDay = day,
                    setupErrorMessage = "Select an institution first.",
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
        loadSetupPeriods(unitId, dateLabel, current.selectedInstitutionId)
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

    fun pullToRefresh() {
        loadMonth(
            month = _uiState.value.attendanceMonth,
            unitId = _uiState.value.selectedUnitId,
            forceNetwork = true,
            asPullRefresh = true,
            showSkeleton = false,
        )
    }

    fun refresh(
        month: YearMonth = _uiState.value.attendanceMonth,
        unitId: String? = _uiState.value.selectedUnitId,
        forceNetwork: Boolean = true,
    ) {
        loadMonth(
            month = month,
            unitId = unitId,
            forceNetwork = forceNetwork,
            asPullRefresh = false,
            showSkeleton = _uiState.value.dashboard == null,
        )
    }

    private fun showMonth(month: YearMonth) {
        val unitId = _uiState.value.selectedUnitId
        val cached = overviewCache[CacheKey(month, unitId)]
        if (cached != null) {
            refreshJob?.cancel()
            applyCacheEntry(month, cached, clearSelectedDay = true)
            return
        }
        loadMonth(
            month = month,
            unitId = unitId,
            forceNetwork = false,
            asPullRefresh = false,
            showSkeleton = true,
        )
    }

    private fun loadMonth(
        month: YearMonth,
        unitId: String?,
        forceNetwork: Boolean,
        asPullRefresh: Boolean,
        showSkeleton: Boolean,
    ) {
        val cacheKey = CacheKey(month, unitId)
        if (!forceNetwork) {
            overviewCache[cacheKey]?.let { cached ->
                refreshJob?.cancel()
                applyCacheEntry(
                    month = month,
                    entry = cached,
                    clearSelectedDay = showSkeleton,
                )
                return
            }
        }

        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { state ->
                when {
                    asPullRefresh -> state.copy(
                        isRefreshing = true,
                        isLoading = false,
                        errorMessage = null,
                        attendanceMonth = month,
                    )
                    showSkeleton -> state.copy(
                        dashboard = null,
                        isLoading = true,
                        isRefreshing = false,
                        errorMessage = null,
                        attendanceMonth = month,
                        selectedAttendanceDay = null,
                    )
                    else -> state.copy(
                        isLoading = state.dashboard == null,
                        isRefreshing = false,
                        errorMessage = null,
                        attendanceMonth = month,
                    )
                }
            }

            val entry = fetchMonthEntry(
                month = month,
                unitId = unitId,
                forceNetworkUnits = forceNetwork,
                bypassCache = forceNetwork,
            )

            if (_uiState.value.attendanceMonth != month) return@launch

            if (entry != null) {
                applyCacheEntry(month, entry)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = it.errorMessage ?: "Unable to load overview.",
                    )
                }
            }
        }
    }

    private suspend fun fetchMonthEntry(
        month: YearMonth,
        unitId: String?,
        forceNetworkUnits: Boolean,
        bypassCache: Boolean,
    ): CacheEntry? {
        if (forceNetworkUnits) {
            loadInstitutions(forceNetwork = true)
        }
        loadTeachingUnits(forceNetworkUnits)

        val resolvedUnitId = unitId
            ?: syncAttendancePicker(
                institutionId = _uiState.value.selectedInstitutionId,
                classId = _uiState.value.selectedClassId,
                subjectId = _uiState.value.selectedSubjectId,
            )
            ?: _uiState.value.selectedUnitId
        val resolvedKey = CacheKey(month, resolvedUnitId)

        if (!bypassCache) {
            overviewCache[resolvedKey]?.let { return it }
        }

        return coroutineScope {
            val monthKey = month.toString()
            val overviewDeferred = async {
                teacherRepository.overview(month = monthKey, forceRefresh = bypassCache)
            }
            val unitSummaryDeferred = async {
                if (resolvedUnitId != null) {
                    teacherRepository.unitSummary(resolvedUnitId, monthKey)
                } else {
                    null
                }
            }
            val overviewResult = overviewDeferred.await()
            val unitSummary = unitSummaryDeferred.await()

            when (overviewResult) {
                is NetworkResult.Success -> {
                    val entry = CacheEntry(
                        dashboard = TeacherUiMappers.overviewDashboard(
                            overviewResult.data,
                            (unitSummary as? NetworkResult.Success)?.data,
                        ),
                    )
                    putCache(resolvedKey, entry)
                    entry
                }
                else -> {
                    if (_uiState.value.attendanceMonth == month) {
                        _uiState.update {
                            it.copy(errorMessage = overviewResult.userMessage())
                        }
                    }
                    null
                }
            }
        }
    }

    /**
     * Institution → teacher-assigned class/subject pickers → teaching_unit_id.
     * Only shows classes/subjects from GET /teaching-units (this teacher's assignments).
     * Attendance APIs always use the resolved [selectedUnitId].
     */
    private suspend fun syncAttendancePicker(
        institutionId: String?,
        classId: String?,
        subjectId: String?,
        forceReloadInstitutions: Boolean = false,
    ): String? {
        val institutions = loadInstitutions(forceNetwork = forceReloadInstitutions)
        val institutionNames = institutions.map { it.name }
        val institutionIds = institutions.map { it.id }
        val allUnits = loadTeachingUnits(forceNetwork = false)
        val resolvedInstitutionId = institutionId
            ?.takeIf { id -> institutions.any { it.id == id } }
            ?: userSessionStore?.getInstitutionId()?.takeIf { id ->
                institutions.any { it.id == id }
            }
            ?: institutions.firstOrNull()?.id

        if (institutions.isEmpty()) {
            _uiState.update {
                it.copy(
                    institutions = emptyList(),
                    institutionIds = emptyList(),
                    selectedInstitutionId = null,
                    attendancePickerError = "No institutions are available yet.",
                    selectedUnitId = null,
                )
            }
            return null
        }

        if (resolvedInstitutionId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    institutions = institutionNames,
                    institutionIds = institutionIds,
                    selectedInstitutionId = null,
                    attendancePickerError = "Please select an institution.",
                    selectedUnitId = null,
                )
            }
            return null
        }
        userSessionStore?.setInstitutionId(resolvedInstitutionId)

        val scopedUnits = TeacherUiMappers.unitsForInstitution(allUnits, resolvedInstitutionId)
            .filter { it.status == TeachingUnitStatus.ACTIVE }
        val classChips = TeacherUiMappers.classChips(scopedUnits)
        val classLabels = classChips.map { it.label }
        val classIdsByLabel = classChips.associate { it.label to it.classId }

        val resolvedClassId = classId?.takeIf { id -> classChips.any { it.classId == id } }
            ?: _uiState.value.selectedClassId?.takeIf { id -> classChips.any { it.classId == id } }
            ?: classChips.firstOrNull()?.classId
        val resolvedClassLabel = classChips.firstOrNull { it.classId == resolvedClassId }?.label
            .orEmpty()

        if (resolvedClassId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    institutions = institutionNames,
                    institutionIds = institutionIds,
                    selectedInstitutionId = resolvedInstitutionId,
                    attendanceClasses = emptyList(),
                    classIdsByLabel = emptyMap(),
                    selectedAttendanceClass = "",
                    selectedClassId = null,
                    attendanceSubjects = emptyList(),
                    subjectIdsByLabel = emptyMap(),
                    selectedAttendanceSubject = "",
                    selectedSubjectId = null,
                    selectedUnitId = null,
                    attendancePickerError = "No assigned classes for this institution.",
                )
            }
            return null
        }

        val classUnits = scopedUnits.filter { it.class_id == resolvedClassId }
        val subjectOptions = classUnits
            .mapNotNull { unit ->
                val id = unit.subject_id.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val name = unit.subject_name.trim().ifBlank { return@mapNotNull null }
                id to name
            }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
        val subjectLabels = subjectOptions.map { it.second }
        val subjectIdsByLabel = subjectOptions.associate { (id, name) -> name to id }

        val resolvedSubjectId = subjectId?.takeIf { id -> subjectOptions.any { it.first == id } }
            ?: _uiState.value.selectedSubjectId?.takeIf { id -> subjectOptions.any { it.first == id } }
            ?: subjectOptions.firstOrNull()?.first
        val resolvedSubjectLabel = subjectOptions.firstOrNull { it.first == resolvedSubjectId }?.second
            .orEmpty()

        val resolvedUnitId = if (!resolvedSubjectId.isNullOrBlank()) {
            TeacherUiMappers.resolveTeachingUnitId(
                units = scopedUnits,
                classId = resolvedClassId,
                subjectId = resolvedSubjectId,
                institutionId = resolvedInstitutionId,
            )
        } else {
            null
        }

        val pickerError = when {
            subjectOptions.isEmpty() ->
                "No assigned subjects for this class."
            resolvedSubjectId.isNullOrBlank() ->
                "Select a subject for this class."
            resolvedUnitId.isNullOrBlank() ->
                "No teaching unit found for this class and subject."
            else -> null
        }

        _uiState.update {
            it.copy(
                institutions = institutionNames,
                institutionIds = institutionIds,
                selectedInstitutionId = resolvedInstitutionId,
                attendanceClasses = classLabels,
                classIdsByLabel = classIdsByLabel,
                selectedAttendanceClass = resolvedClassLabel,
                selectedClassId = resolvedClassId,
                attendanceSubjects = subjectLabels,
                subjectIdsByLabel = subjectIdsByLabel,
                selectedAttendanceSubject = resolvedSubjectLabel,
                selectedSubjectId = resolvedSubjectId,
                selectedUnitId = resolvedUnitId,
                attendancePickerError = pickerError,
            )
        }
        return resolvedUnitId
    }

    private suspend fun loadInstitutions(forceNetwork: Boolean = false): List<InstitutionOut> {
        if (!forceNetwork) {
            cachedInstitutions?.let { return it }
        }
        return when (val result = teacherRepository.institutions(forceNetwork)) {
            is NetworkResult.Success -> {
                val institutions = result.data
                    .filter { it.is_active }
                    .sortedWith(compareBy({ it.sort_order }, { it.name }))
                cachedInstitutions = institutions
                institutions
            }
            else -> cachedInstitutions.orEmpty()
        }
    }

    private suspend fun loadTeachingUnits(forceNetwork: Boolean): List<TeachingUnitOut> {
        if (!forceNetwork) {
            cachedTeachingUnits?.let { return it }
        }
        return when (val result = teacherRepository.teachingUnits(forceNetwork)) {
            is NetworkResult.Success -> {
                cachedTeachingUnits = result.data
                result.data
            }
            else -> cachedTeachingUnits.orEmpty()
        }
    }

    private fun putCache(key: CacheKey, entry: CacheEntry) {
        overviewCache.remove(key)
        overviewCache[key] = entry
        while (overviewCache.size > MAX_CACHE_ENTRIES) {
            val oldest = overviewCache.keys.firstOrNull() ?: break
            overviewCache.remove(oldest)
        }
    }

    private fun applyCacheEntry(
        month: YearMonth,
        entry: CacheEntry,
        clearSelectedDay: Boolean = false,
    ) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessage = null,
                dashboard = entry.dashboard,
                attendanceMonth = month,
                selectedAttendanceDay = if (clearSelectedDay) null else it.selectedAttendanceDay,
            )
        }
    }

    private fun loadSetupPeriods(
        unitId: String,
        dateLabel: String,
        institutionId: String,
    ) {
        viewModelScope.launch {
            coroutineScope {
                val dayDeferred = async { teacherRepository.unitDay(unitId, dateLabel) }
                val periodsDeferred = async {
                    teacherRepository.periods(institutionId)
                }
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
        private const val MAX_CACHE_ENTRIES = 18

        fun provideFactory(
            teacherRepository: TeacherRepository,
            userSessionStore: UserSessionStore? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherOverviewViewModel(teacherRepository, userSessionStore)
        }
    }
}
