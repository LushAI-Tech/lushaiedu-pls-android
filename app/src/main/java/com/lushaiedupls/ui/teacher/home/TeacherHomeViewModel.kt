package com.lushaiedupls.ui.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.reloadUiFlags
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherHomeViewModel(
    private val userSessionStore: UserSessionStore,
    private val teacherRepository: TeacherRepository,
    private val studentRepository: StudentRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TeacherHomeUiState(
            displayName = userSessionStore.getDisplayName(),
            avatarUrl = AuthRepository.normalizeAvatarUrl(userSessionStore.getAvatarUrl()),
            avatarCacheKey = userSessionStore.getAvatarRevision(),
            isLoading = true,
        ),
    )
    val uiState: StateFlow<TeacherHomeUiState> = _uiState.asStateFlow()
    private var cachedUnits: List<TeachingUnitOut> = emptyList()
    private var cachedInstitutions: List<InstitutionOut>? = null

    init {
        viewModelScope.launch {
            teacherRepository.unreadNotificationCount.collect { count ->
                if (count != null) {
                    _uiState.update { it.copy(notificationCount = count) }
                }
            }
        }
        if (studentRepository != null) {
            viewModelScope.launch {
                studentRepository.unreadNotificationCount.collect { count ->
                    if (count != null) {
                        _uiState.update { it.copy(notificationCount = count) }
                    }
                }
            }
        }
        refresh()
    }

    fun onInstitutionSelected(index: Int) {
        val institutionId = _uiState.value.institutionIds.getOrNull(index) ?: return
        if (institutionId == _uiState.value.selectedInstitutionId) return
        userSessionStore.setInstitutionId(institutionId)
        applyUnits(
            units = cachedUnits,
            institutionId = institutionId,
            classId = null,
        )
        refresh(classId = _uiState.value.selectedClassId)
    }

    fun onClassSelected(classLabel: String) {
        val classId = _uiState.value.classIdsByLabel[classLabel]
        _uiState.update { it.copy(selectedClass = classLabel, selectedClassId = classId) }
        refresh(classId)
    }

    fun refresh(
        classId: String? = _uiState.value.selectedClassId,
        forceNetwork: Boolean = false,
    ) {
        viewModelScope.launch {
            val hasContent = _uiState.value.classes.isNotEmpty() ||
                _uiState.value.institutions.isNotEmpty()
            val (loading, refreshing) = reloadUiFlags(_uiState.value.isLoading, hasContent)
            _uiState.update {
                it.copy(
                    isRefreshing = refreshing,
                    isLoading = loading,
                    errorMessage = null,
                )
            }
            coroutineScope {
                val unitsDeferred = async { teacherRepository.teachingUnits(forceNetwork) }
                val unitsResult = unitsDeferred.await()
                val allUnits = (unitsResult as? NetworkResult.Success)?.data.orEmpty()
                cachedUnits = allUnits
                val institutions = loadInstitutions(forceNetwork = forceNetwork)
                val institutionNames = institutions.map { it.name }
                val institutionIds = institutions.map { it.id }
                val selectedInstitutionId = _uiState.value.selectedInstitutionId
                    ?.takeIf { id -> institutions.any { it.id == id } }
                    ?: userSessionStore.getInstitutionId()?.takeIf { id ->
                        institutions.any { it.id == id }
                    }
                    ?: institutions.firstOrNull()?.id
                if (selectedInstitutionId != null) {
                    userSessionStore.setInstitutionId(selectedInstitutionId)
                }
                val scopedUnits = TeacherUiMappers.unitsForInstitution(allUnits, selectedInstitutionId)
                val chips = TeacherUiMappers.classChips(scopedUnits)
                val labels = chips.map { it.label }
                val idsByLabel = chips.associate { it.label to it.classId }
                val resolvedClassId = classId
                    ?.takeIf { id -> chips.any { it.classId == id } }
                    ?: _uiState.value.selectedClassId?.takeIf { id -> chips.any { it.classId == id } }
                    ?: chips.firstOrNull()?.classId
                val overviewResult = teacherRepository.overview(
                    classId = resolvedClassId,
                    forceRefresh = forceNetwork,
                )
                when (overviewResult) {
                    is NetworkResult.Success -> {
                        val overview = overviewResult.data
                        val selectedLabel = chips.find { it.classId == resolvedClassId }?.label
                            ?: labels.firstOrNull().orEmpty()
                        val dashboard = TeacherUiMappers.homeDashboard(
                            overview = overview,
                            classLabel = selectedLabel,
                            classOptions = labels,
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = null,
                                displayName = overview.teacher.name.ifBlank {
                                    userSessionStore.getDisplayName()
                                },
                                avatarUrl = resolveAvatarUrl(overview.teacher.avatar_url),
                                avatarCacheKey = userSessionStore.getAvatarRevision(),
                                notificationCount = overview.unread_notifications,
                                institutions = institutionNames,
                                institutionIds = institutionIds,
                                selectedInstitutionId = selectedInstitutionId,
                                selectedClass = selectedLabel,
                                selectedClassId = resolvedClassId,
                                classes = labels,
                                classIdsByLabel = idsByLabel,
                                groupOutcome = dashboard.groupOutcome,
                                regularAttendance = dashboard.regularAttendance,
                                extraAttendance = dashboard.extraAttendance,
                                topPerformances = dashboard.topPerformances,
                            )
                        }
                    }
                    else -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = overviewResult.userMessage(),
                        )
                    }
                }
            }
        }
    }

    private fun applyUnits(
        units: List<TeachingUnitOut>,
        institutionId: String?,
        classId: String?,
    ) {
        val scopedUnits = TeacherUiMappers.unitsForInstitution(units, institutionId)
        val chips = TeacherUiMappers.classChips(scopedUnits)
        val labels = chips.map { it.label }
        val idsByLabel = chips.associate { it.label to it.classId }
        val resolvedClassId = classId?.takeIf { id -> chips.any { it.classId == id } }
            ?: chips.firstOrNull()?.classId
        val selectedLabel = chips.find { it.classId == resolvedClassId }?.label
            ?: labels.firstOrNull().orEmpty()
        _uiState.update {
            it.copy(
                selectedInstitutionId = institutionId,
                selectedClass = selectedLabel,
                selectedClassId = resolvedClassId,
                classes = labels,
                classIdsByLabel = idsByLabel,
            )
        }
    }

    private suspend fun loadInstitutions(forceNetwork: Boolean = false): List<InstitutionOut> {
        if (!forceNetwork) {
            cachedInstitutions?.let { return it }
        }
        return when (val result = teacherRepository.institutions(forceRefresh = forceNetwork)) {
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

    private fun resolveAvatarUrl(remote: String?): String? {
        val sessionUrl = AuthRepository.normalizeAvatarUrl(userSessionStore.getAvatarUrl())
        val remoteUrl = AuthRepository.normalizeAvatarUrl(remote)
        if (sessionUrl != null && remoteUrl != null && sessionUrl != remoteUrl) {
            return sessionUrl
        }
        val url = remoteUrl ?: sessionUrl
        url?.let { userSessionStore.setAvatarUrl(it) }
        return url
    }

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            teacherRepository: TeacherRepository,
            studentRepository: StudentRepository? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherHomeViewModel(userSessionStore, teacherRepository, studentRepository)
        }
    }
}
