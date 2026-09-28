package com.lushaiedupls.ui.admin.roll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.common.reloadUiFlags
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminRollNumbersUiState(
    val institutions: List<InstitutionOut> = emptyList(),
    val classes: List<ClassOut> = emptyList(),
    val units: List<TeachingUnitOut> = emptyList(),
    val selectedInstitutionId: String? = null,
    val selectedClassId: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class AdminRollNumbersViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminRollNumbersUiState(isLoading = true))
    val uiState: StateFlow<AdminRollNumbersUiState> = _uiState.asStateFlow()

    private var allUnits: List<TeachingUnitOut> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.institutions.isNotEmpty() ||
                _uiState.value.units.isNotEmpty()
            val (loading, refreshing) = reloadUiFlags(_uiState.value.isLoading, hasContent)
            _uiState.update {
                it.copy(isLoading = loading, isRefreshing = refreshing, errorMessage = null)
            }

            val institutions = when (
                val result = adminRepository.listInstitutions(includeInactive = false)
            ) {
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

            val institutionId = _uiState.value.selectedInstitutionId
                ?.takeIf { id -> institutions.any { it.id == id } }
                ?: institutions.firstOrNull()?.id

            val unitsResult = adminRepository.teachingUnits(forceRefresh = true)
            allUnits = when (unitsResult) {
                is NetworkResult.Success -> unitsResult.data
                else -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            institutions = institutions,
                            selectedInstitutionId = institutionId,
                            errorMessage = unitsResult.userMessage(),
                        )
                    }
                    return@launch
                }
            }

            val classes = loadClasses(institutionId)
            val classId = _uiState.value.selectedClassId
                ?.takeIf { id -> classes.any { it.id == id } }
                ?: classes.firstOrNull()?.id

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    institutions = institutions,
                    classes = classes,
                    selectedInstitutionId = institutionId,
                    selectedClassId = classId,
                    units = unitsForClass(classId),
                    errorMessage = null,
                )
            }
        }
    }

    fun selectInstitution(institutionId: String) {
        if (institutionId == _uiState.value.selectedInstitutionId) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedInstitutionId = institutionId,
                    selectedClassId = null,
                    classes = emptyList(),
                    units = emptyList(),
                    errorMessage = null,
                )
            }
            val classes = loadClasses(institutionId)
            val classId = classes.firstOrNull()?.id
            _uiState.update {
                it.copy(
                    classes = classes,
                    selectedClassId = classId,
                    units = unitsForClass(classId),
                )
            }
        }
    }

    fun selectClass(classId: String) {
        if (classId == _uiState.value.selectedClassId) return
        _uiState.update {
            it.copy(
                selectedClassId = classId,
                units = unitsForClass(classId),
                errorMessage = null,
            )
        }
    }

    private suspend fun loadClasses(institutionId: String?): List<ClassOut> {
        if (institutionId.isNullOrBlank()) return emptyList()
        return when (
            val result = adminRepository.listClasses(
                includeInactive = false,
                institutionId = institutionId,
            )
        ) {
            is NetworkResult.Success -> result.data
                .filter { it.is_active }
                .sortedWith(compareBy({ it.sort_order }, { it.name }))
            else -> emptyList()
        }
    }

    private fun unitsForClass(classId: String?): List<TeachingUnitOut> {
        if (classId.isNullOrBlank()) return emptyList()
        return allUnits
            .filter { it.class_id == classId }
            .sortedBy { it.subject_name.lowercase() }
    }

    companion object {
        fun provideFactory(adminRepository: AdminRepository) = viewModelFactory {
            AdminRollNumbersViewModel(adminRepository)
        }
    }
}
