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
) : ViewModel() {

    private val allowMultiSelect = userSessionStore.getRole() == UserRole.Teacher

    private val _uiState = MutableStateFlow(
        SelectClassUiState(
            allowMultiSelect = allowMultiSelect,
            isLoading = true,
            selectedClassIds = userSessionStore.getClassIds().toSet(),
        ),
    )
    val uiState: StateFlow<SelectClassUiState> = _uiState.asStateFlow()

    init {
        loadClasses()
    }

    fun loadClasses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = studentRepository.classes()) {
                is NetworkResult.Success -> {
                    val options = result.data
                        .filter { it.is_active }
                        .sortedBy { it.sort_order }
                        .map { ClassOption(it.id, it.name) }
                    val selected = _uiState.value.selectedClassIds.ifEmpty {
                        if (!allowMultiSelect && options.isNotEmpty()) setOf(options.first().id) else emptySet()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            classes = options,
                            selectedClassIds = selected.filter { id -> options.any { o -> o.id == id } }.toSet(),
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
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
            userSessionStore.setClassIds(selected.toList())
            true
        }
    }

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            studentRepository: StudentRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            SelectClassViewModel(userSessionStore, studentRepository)
        }
    }
}
