package com.lushaiedupls.ui.auth.selectinstitution

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

class SelectInstitutionViewModel(
    private val userSessionStore: UserSessionStore,
    private val studentRepository: StudentRepository,
) : ViewModel() {

    private val isTeacher = userSessionStore.getRole() == UserRole.Teacher

    private val _uiState = MutableStateFlow(
        SelectInstitutionUiState(
            isLoading = true,
            isTeacher = isTeacher,
            selectedInstitutionIds = userSessionStore.getInstitutionIds().toSet(),
        ),
    )
    val uiState: StateFlow<SelectInstitutionUiState> = _uiState.asStateFlow()

    init {
        loadInstitutions()
    }

    fun loadInstitutions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = studentRepository.institutions()) {
                is NetworkResult.Success -> {
                    val options = result.data
                        .filter { it.is_active }
                        .sortedWith(compareBy({ it.sort_order }, { it.name }))
                        .map { InstitutionOption(it.id, it.name) }
                    val retained = _uiState.value.selectedInstitutionIds
                        .filter { id -> options.any { it.id == id } }
                        .toSet()
                    val selected = when {
                        retained.isNotEmpty() -> retained
                        options.size == 1 -> setOf(options.first().id)
                        else -> emptySet()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            institutions = options,
                            selectedInstitutionIds = selected,
                            errorMessage = if (options.isEmpty()) {
                                "No institutions are available yet."
                            } else {
                                null
                            },
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun onInstitutionSelected(institutionId: String) {
        _uiState.update { state ->
            val next = if (state.isTeacher) {
                if (institutionId in state.selectedInstitutionIds) {
                    state.selectedInstitutionIds - institutionId
                } else {
                    state.selectedInstitutionIds + institutionId
                }
            } else {
                setOf(institutionId)
            }
            state.copy(selectedInstitutionIds = next, errorMessage = null)
        }
    }

    fun validateAndSave(): Boolean {
        val selected = _uiState.value.selectedInstitutionIds
        return if (selected.isEmpty()) {
            _uiState.update {
                it.copy(
                    errorMessage = if (it.isTeacher) {
                        "Please select at least one institution."
                    } else {
                        "Please select an institution."
                    },
                )
            }
            false
        } else {
            val previous = userSessionStore.getInstitutionIds().toSet()
            userSessionStore.setInstitutionIds(selected.toList())
            if (previous != selected) {
                userSessionStore.setClassIds(emptyList())
                userSessionStore.setSubjectIds(emptyList())
                userSessionStore.setClassInstitutionIds(emptyMap())
                userSessionStore.setPendingTeacherAssignments(emptyList())
            }
            true
        }
    }

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            studentRepository: StudentRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            SelectInstitutionViewModel(userSessionStore, studentRepository)
        }
    }
}
