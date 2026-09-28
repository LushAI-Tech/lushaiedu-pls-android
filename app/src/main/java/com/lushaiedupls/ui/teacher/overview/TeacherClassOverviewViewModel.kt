package com.lushaiedupls.ui.teacher.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.MemberOut
import com.lushaiedupls.data.remote.dto.RollNumberAssignment
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.reloadUiFlags
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherClassOverviewViewModel(
    private val teacherRepository: TeacherRepository,
    private val groupId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherClassOverviewUiState(isLoading = true))
    val uiState: StateFlow<TeacherClassOverviewUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onSectionSelected(section: TeacherClassSection) {
        _uiState.update { it.copy(section = section) }
    }

    fun updateStudentRoll(studentId: String, rollStr: String) {
        val digitsOnly = rollStr.filter { it.isDigit() }.take(4)
        _uiState.update { state ->
            state.copy(
                rollDrafts = state.rollDrafts + (studentId to digitsOnly),
                autoFilled = false,
                errorMessage = null,
            )
        }
    }

    fun toggleAutoFill() {
        val state = _uiState.value
        if (state.students.isEmpty()) return
        if (state.autoFilled) {
            _uiState.update {
                it.copy(
                    rollDrafts = state.students.associate { s -> s.id to "" },
                    autoFilled = false,
                    errorMessage = null,
                )
            }
        } else {
            val drafts = state.students.mapIndexed { index, student ->
                student.id to (index + 1).toString()
            }.toMap()
            _uiState.update {
                it.copy(
                    rollDrafts = drafts,
                    autoFilled = true,
                    errorMessage = null,
                )
            }
        }
    }

    fun saveRollNumbers() {
        val state = _uiState.value
        val parsed = state.students.mapNotNull { student ->
            val text = state.rollDrafts[student.id].orEmpty().trim()
            if (text.isEmpty()) return@mapNotNull null
            val roll = text.toIntOrNull()
            if (roll == null || roll !in 1..9999) return@mapNotNull null
            student.id to roll
        }

        if (parsed.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Enter at least one roll number.", actionMessage = null)
            }
            return
        }

        val duplicates = parsed.groupBy { it.second }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            val n = duplicates.minOrNull() ?: return
            _uiState.update {
                it.copy(
                    errorMessage = "Roll number $n is used more than once.",
                    actionMessage = null,
                )
            }
            return
        }

        val assignments = parsed.map { (studentId, roll) ->
            RollNumberAssignment(student_id = studentId, roll_no = roll)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isApprovingRolls = true, errorMessage = null) }
            when (val result = teacherRepository.approveRollNumbers(groupId, assignments)) {
                is NetworkResult.Success -> {
                    applyMembers(result.data, keepDrafts = false)
                    _uiState.update {
                        it.copy(
                            isApprovingRolls = false,
                            autoFilled = false,
                            actionMessage = "Roll numbers saved",
                        )
                    }
                    refreshSilently()
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
                        it.copy(isApprovingRolls = false, errorMessage = mapped)
                    }
                }
            }
        }
    }

    fun markParentsSelected(studentId: String) {
        _uiState.update { current ->
            current.copy(
                students = current.students.map { student ->
                    if (student.id == studentId) {
                        student.copy(hasParentsSelected = true)
                    } else {
                        student
                    }
                },
            )
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.overview != null
            val (loading, refreshing) = reloadUiFlags(_uiState.value.isLoading, hasContent)
            _uiState.update {
                it.copy(
                    isRefreshing = refreshing,
                    isLoading = loading,
                    errorMessage = null,
                )
            }
            refreshSilently()
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }

    private fun applyMembers(members: List<MemberOut>, keepDrafts: Boolean) {
        val parentIds = _uiState.value.students
            .filter { it.hasParentsSelected }
            .map { it.id }
            .toSet()
        val students = TeacherUiMappers.students(members, parentIds)
        val drafts = if (keepDrafts) {
            _uiState.value.rollDrafts
        } else {
            members.associate { member ->
                member.student.id to (member.roll_no?.takeIf { it in 1..9999 }?.toString().orEmpty())
            }
        }
        _uiState.update {
            it.copy(students = students, rollDrafts = drafts)
        }
    }

    private suspend fun refreshSilently() {
        coroutineScope {
            val unitDeferred = async { teacherRepository.teachingUnit(groupId) }
            val membersDeferred = async { teacherRepository.members(groupId) }
            val summaryDeferred = async { teacherRepository.unitSummary(groupId) }
            val parentsDeferred = async { teacherRepository.parents(groupId) }
            val unitResult = unitDeferred.await()
            val membersResult = membersDeferred.await()
            val summaryResult = summaryDeferred.await()
            val parentsResult = parentsDeferred.await()
            when (unitResult) {
                is NetworkResult.Success -> {
                    val members = (membersResult as? NetworkResult.Success)?.data.orEmpty()
                    val summary = (summaryResult as? NetworkResult.Success)?.data
                    val parentIds = (parentsResult as? NetworkResult.Success)?.data?.let {
                        TeacherUiMappers.parentIds(it)
                    }.orEmpty()
                    val students = TeacherUiMappers.students(members, parentIds)
                    val drafts = members.associate { member ->
                        member.student.id to
                            (member.roll_no?.takeIf { it in 1..9999 }?.toString().orEmpty())
                    }
                    _uiState.update {
                        it.copy(
                            overview = TeacherUiMappers.classOverview(
                                unit = unitResult.data,
                                summary = summary,
                                memberCount = members.size,
                            ),
                            students = students,
                            rollDrafts = drafts,
                            autoFilled = false,
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(errorMessage = unitResult.userMessage())
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            teacherRepository: TeacherRepository,
            groupId: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherClassOverviewViewModel(teacherRepository, groupId)
        }
    }
}
