package com.lushaiedupls.ui.admin.roll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.MemberOut
import com.lushaiedupls.data.remote.dto.RollNumberAssignment
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.common.reloadUiFlags
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminRollEditUiState(
    val className: String = "",
    val subjectName: String = "",
    val members: List<MemberOut> = emptyList(),
    /** student_id → draft text (empty = blank field). */
    val drafts: Map<String, String> = emptyMap(),
    val autoFilled: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class AdminRollEditViewModel(
    private val adminRepository: AdminRepository,
    private val unitId: String,
    className: String,
    subjectName: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AdminRollEditUiState(
            className = className,
            subjectName = subjectName,
            isLoading = true,
        ),
    )
    val uiState: StateFlow<AdminRollEditUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.members.isNotEmpty()
            val (loading, refreshing) = reloadUiFlags(_uiState.value.isLoading, hasContent)
            _uiState.update {
                it.copy(isLoading = loading, isRefreshing = refreshing, errorMessage = null)
            }
            when (val result = adminRepository.members(unitId)) {
                is NetworkResult.Success -> applyMembers(result.data, keepDrafts = false)
                else -> {
                    val detail = result.userMessage()
                    val needsReload = detail.contains("Not members of this unit", ignoreCase = true)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = if (needsReload) {
                                "Reload the roster"
                            } else {
                                detail
                            },
                        )
                    }
                }
            }
        }
    }

    fun updateDraft(studentId: String, raw: String) {
        val filtered = raw.filter { it.isDigit() }.take(4)
        _uiState.update {
            it.copy(
                drafts = it.drafts + (studentId to filtered),
                autoFilled = false,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun toggleAutoFill() {
        val state = _uiState.value
        if (state.members.isEmpty()) return
        if (state.autoFilled) {
            _uiState.update {
                it.copy(
                    drafts = state.members.associate { m -> m.student.id to "" },
                    autoFilled = false,
                    errorMessage = null,
                    successMessage = null,
                )
            }
        } else {
            val drafts = state.members.mapIndexed { index, member ->
                member.student.id to (index + 1).toString()
            }.toMap()
            _uiState.update {
                it.copy(
                    drafts = drafts,
                    autoFilled = true,
                    errorMessage = null,
                    successMessage = null,
                )
            }
        }
    }

    fun save() {
        val state = _uiState.value
        val parsed = state.members.mapNotNull { member ->
            val text = state.drafts[member.student.id].orEmpty().trim()
            if (text.isEmpty()) return@mapNotNull null
            val roll = text.toIntOrNull()
            if (roll == null || roll !in 1..9999) return@mapNotNull null
            member.student.id to roll
        }

        if (parsed.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Enter at least one roll number.", successMessage = null)
            }
            return
        }

        val duplicates = parsed.groupBy { it.second }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            val n = duplicates.minOrNull() ?: return
            _uiState.update {
                it.copy(
                    errorMessage = "Roll number $n is used more than once.",
                    successMessage = null,
                )
            }
            return
        }

        val assignments = parsed.map { (studentId, roll) ->
            RollNumberAssignment(student_id = studentId, roll_no = roll)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            when (val result = adminRepository.approveRollNumbers(unitId, assignments)) {
                is NetworkResult.Success -> {
                    applyMembers(result.data, keepDrafts = false)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "Roll numbers saved",
                            autoFilled = false,
                        )
                    }
                }
                else -> {
                    val detail = result.userMessage()
                    val mapped = when {
                        detail.contains("Not members of this unit", ignoreCase = true) -> {
                            refresh()
                            "Reload the roster"
                        }
                        detail.isNotBlank() -> detail
                        else -> "Could not save roll numbers."
                    }
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = mapped)
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun applyMembers(members: List<MemberOut>, keepDrafts: Boolean) {
        val drafts = if (keepDrafts) {
            _uiState.value.drafts
        } else {
            members.associate { member ->
                member.student.id to (member.roll_no?.takeIf { it in 1..9999 }?.toString().orEmpty())
            }
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                members = members,
                drafts = drafts,
                errorMessage = null,
            )
        }
    }

    companion object {
        fun provideFactory(
            adminRepository: AdminRepository,
            unitId: String,
            className: String,
            subjectName: String,
        ) = viewModelFactory {
            AdminRollEditViewModel(adminRepository, unitId, className, subjectName)
        }
    }
}
