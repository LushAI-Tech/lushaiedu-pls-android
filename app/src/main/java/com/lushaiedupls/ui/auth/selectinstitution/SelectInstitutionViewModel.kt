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

    private val _uiState = MutableStateFlow(
        SelectInstitutionUiState(
            isLoading = true,
            isTeacher = userSessionStore.getRole() == UserRole.Teacher,
            selectedInstitutionId = userSessionStore.getInstitutionId(),
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
                    val selected = _uiState.value.selectedInstitutionId
                        ?.takeIf { id -> options.any { it.id == id } }
                        ?: options.singleOrNull()?.id
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            institutions = options,
                            selectedInstitutionId = selected,
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
        _uiState.update { it.copy(selectedInstitutionId = institutionId, errorMessage = null) }
    }

    fun validateAndSave(): Boolean {
        val selected = _uiState.value.selectedInstitutionId
        return if (selected.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Please select an institution.") }
            false
        } else {
            val previous = userSessionStore.getInstitutionId()
            userSessionStore.setInstitutionId(selected)
            if (previous != selected) {
                userSessionStore.setClassIds(emptyList())
                userSessionStore.setSubjectIds(emptyList())
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
