package com.lushaiedupls.ui.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.data.session.UserSessionStore
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TeacherHomeUiState(
            displayName = userSessionStore.getDisplayName(),
            isLoading = true,
        ),
    )
    val uiState: StateFlow<TeacherHomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onClassSelected(classLabel: String) {
        val classId = _uiState.value.classIdsByLabel[classLabel]
        _uiState.update { it.copy(selectedClass = classLabel, selectedClassId = classId) }
        refresh(classId)
    }

    fun refresh(classId: String? = _uiState.value.selectedClassId) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val unitsDeferred = async { teacherRepository.teachingUnits() }
                val unitsResult = unitsDeferred.await()
                val chips = (unitsResult as? NetworkResult.Success)
                    ?.data
                    ?.let(TeacherUiMappers::classChips)
                    .orEmpty()
                val labels = chips.map { it.label }
                val idsByLabel = chips.associate { it.label to it.classId }
                val resolvedClassId = classId
                    ?: _uiState.value.selectedClassId
                    ?: chips.firstOrNull()?.classId
                val overviewResult = teacherRepository.overview(classId = resolvedClassId)
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
                                errorMessage = null,
                                displayName = overview.teacher.name.ifBlank {
                                    userSessionStore.getDisplayName()
                                },
                                notificationCount = overview.unread_notifications,
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
                        it.copy(isLoading = false, errorMessage = overviewResult.userMessage())
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            teacherRepository: TeacherRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherHomeViewModel(userSessionStore, teacherRepository)
        }
    }
}
