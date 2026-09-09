package com.lushaiedupls.ui.admin.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.AttendanceTotals
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.viewModelFactory
import java.time.YearMonth
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminHomeUiState(
    val displayName: String = "",
    val monthLabel: String = "",
    val notificationCount: Int = 0,
    val institutions: List<String> = emptyList(),
    val institutionIds: List<String> = emptyList(),
    val selectedInstitutionId: String? = null,
    val totalStudents: Int = 0,
    val totalTeachers: Int = 0,
    val totalParents: Int = 0,
    val totalClasses: Int = 0,
    val attendance: AttendanceTotals = AttendanceTotals(),
    val presentPct: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class AdminHomeViewModel(
    private val userSessionStore: UserSessionStore,
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminHomeUiState(
            displayName = userSessionStore.getDisplayName(),
            isLoading = true,
        ),
    )
    val uiState: StateFlow<AdminHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            adminRepository.unreadNotificationCount.collect { count ->
                if (count != null) _uiState.update { it.copy(notificationCount = count) }
            }
        }
        refresh()
    }

    private fun prefetchClasses(institutionId: String?) {
        viewModelScope.launch {
            adminRepository.prefetchClassesPage(institutionId)
        }
    }

    fun onInstitutionSelected(index: Int) {
        val institutionId = _uiState.value.institutionIds.getOrNull(index) ?: return
        if (institutionId == _uiState.value.selectedInstitutionId) return
        userSessionStore.setInstitutionId(institutionId)
        _uiState.update { it.copy(selectedInstitutionId = institutionId) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val showSkeleton = _uiState.value.institutions.isEmpty() &&
                _uiState.value.totalStudents == 0 &&
                _uiState.value.totalTeachers == 0
            if (showSkeleton) {
                _uiState.update {
                    it.copy(isLoading = true, isRefreshing = false, errorMessage = null)
                }
            } else {
                _uiState.update {
                    it.copy(isRefreshing = true, isLoading = false, errorMessage = null)
                }
            }
            val month = YearMonth.now().toString()
            coroutineScope {
                val institutionsDeferred = async {
                    adminRepository.listInstitutions(includeInactive = false)
                }
                val unreadDeferred = async { adminRepository.unreadCount() }
                val loadedInstitutions = when (val result = institutionsDeferred.await()) {
                    is NetworkResult.Success -> result.data
                        .filter { it.is_active }
                        .sortedWith(compareBy({ it.sort_order }, { it.name }))
                    else -> emptyList()
                }
                val institutionNames = loadedInstitutions.map { it.name }
                    .ifEmpty { _uiState.value.institutions }
                val institutionIds = loadedInstitutions.map { it.id }
                    .ifEmpty { _uiState.value.institutionIds }
                val selectedInstitutionId = _uiState.value.selectedInstitutionId
                    ?.takeIf { id -> institutionIds.contains(id) }
                    ?: userSessionStore.getInstitutionId()?.takeIf { id -> institutionIds.contains(id) }
                    ?: institutionIds.firstOrNull()
                if (selectedInstitutionId != null) {
                    userSessionStore.setInstitutionId(selectedInstitutionId)
                }
                when (val result = adminRepository.overview(month, selectedInstitutionId)) {
                    is NetworkResult.Success -> {
                        val overview = result.data
                        unreadDeferred.await()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                displayName = userSessionStore.getDisplayName(),
                                monthLabel = overview.month,
                                institutions = institutionNames,
                                institutionIds = institutionIds,
                                selectedInstitutionId = selectedInstitutionId,
                                totalStudents = overview.total_students,
                                totalTeachers = overview.total_teachers,
                                totalParents = overview.total_parents,
                                totalClasses = overview.total_classes,
                                attendance = overview.attendance,
                                presentPct = overview.attendance.present_pct_all.roundToInt(),
                                errorMessage = null,
                            )
                        }
                        prefetchClasses(selectedInstitutionId)
                    }
                    else -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            institutions = institutionNames,
                            institutionIds = institutionIds,
                            selectedInstitutionId = selectedInstitutionId,
                            errorMessage = result.userMessage(),
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            adminRepository: AdminRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            AdminHomeViewModel(userSessionStore, adminRepository)
        }
    }
}
