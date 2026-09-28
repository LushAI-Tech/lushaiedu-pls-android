package com.lushaiedupls.ui.auth.selectclass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.selectrole.UserRole
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SelectClassViewModel(
    private val userSessionStore: UserSessionStore,
    private val studentRepository: StudentRepository,
    private val institutionId: String,
) : ViewModel() {

    private val allowMultiSelect = userSessionStore.getRole() == UserRole.Teacher

    private val _uiState = MutableStateFlow(
        SelectClassUiState(
            allowMultiSelect = allowMultiSelect,
            isLoading = true,
            selectedClassIds = userSessionStore.getClassInstitutionIds()
                .filter { it.value == institutionId }
                .keys
                .ifEmpty {
                    if (userSessionStore.getInstitutionIds().size <= 1) {
                        userSessionStore.getClassIds().toSet()
                    } else {
                        emptySet()
                    }
                },
            showInstitutionContext = userSessionStore.getRole() == UserRole.Teacher &&
                userSessionStore.getInstitutionIds().size > 1,
        ),
    )
    val uiState: StateFlow<SelectClassUiState> = _uiState.asStateFlow()

    init {
        loadClasses()
    }

    fun loadClasses() {
        if (institutionId.isBlank()) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Please select an institution first.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val institutionName = when (val institutions = studentRepository.institutions()) {
                is NetworkResult.Success ->
                    institutions.data.firstOrNull { it.id == institutionId }?.name.orEmpty()
                else -> ""
            }
            when (val result = studentRepository.classes(institutionId)) {
                is NetworkResult.Success -> {
                    val options = result.data
                        .filter { it.is_active }
                        .sortedBy { it.sort_order }
                        .map { item ->
                            ClassOption(
                                id = item.id,
                                name = item.name,
                                institutionId = item.institution_id.ifBlank { institutionId },
                                institutionName = item.institution_name
                                    ?.takeIf { it.isNotBlank() }
                                    ?: institutionName,
                            )
                        }
                    val selected = _uiState.value.selectedClassIds.ifEmpty {
                        if (!allowMultiSelect && options.isNotEmpty()) setOf(options.first().id) else emptySet()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            classes = options,
                            institutionName = institutionName.ifBlank {
                                options.firstOrNull()?.institutionName.orEmpty()
                            },
                            selectedClassIds = selected.filter { id -> options.any { o -> o.id == id } }.toSet(),
                            errorMessage = if (options.isEmpty()) {
                                "No classes are available yet."
                            } else {
                                null
                            },
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        institutionName = institutionName,
                        errorMessage = result.userMessage(),
                    )
                }
            }
        }
    }

    fun onClassSelected(classId: String) {
        _uiState.update { state ->
            val next = if (state.allowMultiSelect) {
                if (classId in state.selectedClassIds) state.selectedClassIds - classId
                else state.selectedClassIds + classId
            } else {
                setOf(classId)
            }
            state.copy(selectedClassIds = next, errorMessage = null)
        }
    }

    fun validateAndSave(): Boolean {
        val selected = _uiState.value.selectedClassIds
        val selectedOptions = _uiState.value.classes.filter { it.id in selected }
        return if (selected.isEmpty()) {
            _uiState.update {
                it.copy(
                    errorMessage = if (it.allowMultiSelect) {
                        "Please select at least one class."
                    } else {
                        "Please select a class."
                    },
                )
            }
            false
        } else {
            val map = userSessionStore.getClassInstitutionIds().toMutableMap()
            map.entries.removeIf { it.value == institutionId }
            selectedOptions.forEach { option ->
                map[option.id] = option.institutionId.ifBlank { institutionId }
            }
            userSessionStore.setClassIds(map.keys.toList())
            userSessionStore.setClassInstitutionIds(map)
            val keepClasses = selected.toSet()
            userSessionStore.setPendingTeacherAssignments(
                userSessionStore.getPendingTeacherAssignments().filter { item ->
                    item.institutionId != institutionId || item.classId in keepClasses
                },
            )
            true
        }
    }

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            studentRepository: StudentRepository,
            institutionId: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            SelectClassViewModel(userSessionStore, studentRepository, institutionId)
        }
    }
}
