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
    val totalStudents: Int = 0,
    val totalTeachers: Int = 0,
    val totalParents: Int = 0,
    val totalClasses: Int = 0,
    val attendance: AttendanceTotals = AttendanceTotals(),
    val presentPct: Int = 0,
    val isLoading: Boolean = false,
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val month = YearMonth.now().toString()
            coroutineScope {
                val overviewDeferred = async { adminRepository.overview(month) }
                val unreadDeferred = async { adminRepository.unreadCount() }
                when (val result = overviewDeferred.await()) {
                    is NetworkResult.Success -> {
                        val overview = result.data
                        unreadDeferred.await()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                displayName = userSessionStore.getDisplayName(),
                                monthLabel = overview.month,
                                totalStudents = overview.total_students,
                                totalTeachers = overview.total_teachers,
                                totalParents = overview.total_parents,
                                totalClasses = overview.total_classes,
                                attendance = overview.attendance,
                                presentPct = overview.attendance.present_pct_all.roundToInt(),
                                errorMessage = null,
                            )
                        }
                    }
                    else -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.userMessage())
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
