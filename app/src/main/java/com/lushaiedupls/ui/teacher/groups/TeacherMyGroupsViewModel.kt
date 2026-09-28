package com.lushaiedupls.ui.teacher.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.mock.TeacherGroup
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.reloadUiFlags
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherMyGroupsViewModel(
    private val teacherRepository: TeacherRepository,
    private val userSessionStore: UserSessionStore? = null,
) : ViewModel() {

    private var allGroups: List<TeacherGroup> = emptyList()

    private val _uiState = MutableStateFlow(TeacherMyGroupsUiState(isLoading = true))
    val uiState: StateFlow<TeacherMyGroupsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = allGroups.isNotEmpty() || _uiState.value.groups.isNotEmpty()
            val (loading, refreshing) = reloadUiFlags(_uiState.value.isLoading, hasContent)
            _uiState.update {
                it.copy(
                    isRefreshing = refreshing,
                    isLoading = loading,
                    errorMessage = null,
                )
            }
            when (val result = teacherRepository.teachingUnits()) {
                is NetworkResult.Success -> {
                    val units = result.data
                    allGroups = TeacherUiMappers.groups(units)
                    val chips = TeacherUiMappers.institutionChips(units)
                    val institutionIds = chips.map { it.id }
                    val institutionNames = chips.map { it.name }
                    val selectedInstitutionId = _uiState.value.selectedInstitutionId
                        ?.takeIf { id -> institutionIds.contains(id) }
                        ?: userSessionStore?.getInstitutionId()?.takeIf { id ->
                            institutionIds.contains(id)
                        }
                        ?: institutionIds.firstOrNull()
                    if (selectedInstitutionId != null) {
                        userSessionStore?.setInstitutionId(selectedInstitutionId)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            groups = groupsForInstitution(allGroups, selectedInstitutionId),
                            institutions = institutionNames,
                            institutionIds = institutionIds,
                            selectedInstitutionId = selectedInstitutionId,
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

    fun selectInstitution(index: Int) {
        val institutionId = _uiState.value.institutionIds.getOrNull(index) ?: return
        if (institutionId == _uiState.value.selectedInstitutionId) return
        userSessionStore?.setInstitutionId(institutionId)
        _uiState.update {
            it.copy(
                selectedInstitutionId = institutionId,
                groups = groupsForInstitution(allGroups, institutionId),
            )
        }
    }

    fun syncSelectedInstitution() {
        val ids = _uiState.value.institutionIds
        if (ids.isEmpty()) return
        val stored = userSessionStore?.getInstitutionId()?.takeIf { it in ids } ?: return
        if (stored == _uiState.value.selectedInstitutionId) return
        _uiState.update {
            it.copy(
                selectedInstitutionId = stored,
                groups = groupsForInstitution(allGroups, stored),
            )
        }
    }

    companion object {
        fun provideFactory(
            teacherRepository: TeacherRepository,
            userSessionStore: UserSessionStore? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherMyGroupsViewModel(teacherRepository, userSessionStore)
        }

        fun groupsForInstitution(
            groups: List<TeacherGroup>,
            institutionId: String?,
        ): List<TeacherGroup> {
            if (institutionId.isNullOrBlank()) return groups
            val scoped = groups.filter { group ->
                group.institutionId.isBlank() || group.institutionId == institutionId
            }
            return scoped
        }
    }
}
