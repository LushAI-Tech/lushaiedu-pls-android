package com.lushaiedupls.ui.teacher.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.mock.TeacherOverviewDashboard
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.teacher.overlays.AttendancePeriodOption
import java.time.YearMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private data class CacheKey(val month: YearMonth, val unitId: String?)

    private data class CacheEntry(
        val dashboard: TeacherOverviewDashboard,
        val attendanceClasses: List<String>,
        val selectedAttendanceClass: String,
        val selectedUnitId: String?,
        val unitIdsByLabel: Map<String, String>,
    )

    private val overviewCache = linkedMapOf<CacheKey, CacheEntry>()
    private var cachedTeachingUnits: List<TeachingUnitOut>? = null
    private var refreshJob: Job? = null
    private var prefetchJob: Job? = null

    init {
        refresh(forceNetwork = true)
    }

    fun onSectionSelected(section: TeacherOverviewSection) {
        _uiState.update { it.copy(section = section) }
    }

    fun onAttendanceClassSelected(classLabel: String) {
        val unitId = _uiState.value.unitIdsByLabel[classLabel]
        _uiState.update {
            it.copy(
                selectedAttendanceClass = classLabel,
                selectedUnitId = unitId,
                selectedAttendanceDay = null,
            )
        }
        loadMonth(
            month = _uiState.value.attendanceMonth,
            unitId = unitId,
            forceNetwork = false,
            asPullRefresh = false,
            showSkeleton = false,
        )
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
            prefetchAdjacentMonths(month, cached.selectedUnitId)
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
                prefetchAdjacentMonths(month, cached.selectedUnitId)
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

            // Ignore stale responses after a newer month/class selection.
            if (_uiState.value.attendanceMonth != month) return@launch

            if (entry != null) {
                applyCacheEntry(month, entry)
                prefetchAdjacentMonths(month, entry.selectedUnitId)
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

    /**
     * Quietly loads previous/next months for the selected class so month
     * navigation can hit the cache without a content loading state.
     */
    private fun prefetchAdjacentMonths(center: YearMonth, unitId: String?) {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            val neighbors = listOf(center.minusMonths(1), center.plusMonths(1))
            coroutineScope {
                neighbors.map { month ->
                    async {
                        val key = CacheKey(month, unitId)
                        if (overviewCache.containsKey(key)) return@async
                        fetchMonthEntry(
                            month = month,
                            unitId = unitId,
                            forceNetworkUnits = false,
                            bypassCache = false,
                        )
                    }
                }.awaitAll()
            }
        }
    }

    private suspend fun fetchMonthEntry(
        month: YearMonth,
        unitId: String?,
        forceNetworkUnits: Boolean,
        bypassCache: Boolean,
    ): CacheEntry? {
        val resolvedUnits = loadTeachingUnits(forceNetworkUnits)
        val groups = TeacherUiMappers.groups(resolvedUnits)
        val labels = groups.map { it.title }
        val idsByLabel = groups.associate { it.title to it.id }
        val resolvedUnitId = unitId
            ?: _uiState.value.selectedUnitId
            ?: groups.firstOrNull()?.id
        val resolvedLabel = groups.find { it.id == resolvedUnitId }?.title
            ?: labels.firstOrNull().orEmpty()
        val resolvedKey = CacheKey(month, resolvedUnitId)

        if (!bypassCache) {
            overviewCache[resolvedKey]?.let { return it }
        }

        return coroutineScope {
            val monthKey = month.toString()
            val overviewDeferred = async { teacherRepository.overview(month = monthKey) }
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
                        attendanceClasses = labels,
                        selectedAttendanceClass = resolvedLabel,
                        selectedUnitId = resolvedUnitId,
                        unitIdsByLabel = idsByLabel,
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

    private suspend fun loadTeachingUnits(forceNetwork: Boolean): List<TeachingUnitOut> {
        if (!forceNetwork) {
            cachedTeachingUnits?.let { return it }
        }
        return when (val result = teacherRepository.teachingUnits()) {
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
                attendanceClasses = entry.attendanceClasses,
                selectedAttendanceClass = entry.selectedAttendanceClass,
                selectedUnitId = entry.selectedUnitId,
                unitIdsByLabel = entry.unitIdsByLabel,
                attendanceMonth = month,
                selectedAttendanceDay = if (clearSelectedDay) null else it.selectedAttendanceDay,
            )
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
        private const val MAX_CACHE_ENTRIES = 18

        fun provideFactory(
            teacherRepository: TeacherRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherOverviewViewModel(teacherRepository)
        }
    }
}
