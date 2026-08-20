package com.lushaiedupls.ui.teacher.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.RollNumberAssignment
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    private var originalStudents: List<com.lushaiedupls.data.mock.TeacherStudent> = emptyList()

    init {
        refresh()
    }

    fun onSectionSelected(section: TeacherClassSection) {
        _uiState.update { it.copy(section = section) }
    }

    fun toggleEditMode() {
        _uiState.update { state ->
            val nextEditing = !state.isEditing
            if (nextEditing) {
                originalStudents = state.students
                state.copy(
                    isEditing = true,
                    rollDrafts = state.students.associate { it.id to it.rollNumber.toString() },
                    pendingDeleteStudentIds = emptySet(),
                )
            } else {
                state.copy(
                    isEditing = false,
                    students = if (originalStudents.isNotEmpty()) originalStudents else state.students,
                    rollDrafts = emptyMap(),
                    pendingDeleteStudentIds = emptySet(),
                )
            }
        }
    }

    fun updateStudentRoll(studentId: String, rollStr: String) {
        val digitsOnly = rollStr.filter { it.isDigit() }.take(4)
        _uiState.update { state ->
            val updatedDrafts = state.rollDrafts.toMutableMap().apply {
                put(studentId, digitsOnly)
            }
            val parsedRoll = digitsOnly.toIntOrNull()
            val updatedStudents = if (parsedRoll != null && parsedRoll > 0) {
                state.students.map { student ->
                    if (student.id == studentId) student.copy(rollNumber = parsedRoll) else student
                }
            } else {
                state.students
            }
            state.copy(rollDrafts = updatedDrafts, students = updatedStudents)
        }
    }

    fun toggleMarkStudentDelete(studentId: String) {
        _uiState.update { state ->
            val current = state.pendingDeleteStudentIds
            val updated = if (studentId in current) {
                current - studentId
            } else {
                current + studentId
            }
            state.copy(pendingDeleteStudentIds = updated)
        }
    }

    fun autoAssignSequentialRolls() {
        _uiState.update { state ->
            var roll = 1
            val updatedStudents = state.students.map { student ->
                if (student.id in state.pendingDeleteStudentIds) {
                    student
                } else {
                    student.copy(rollNumber = roll++)
                }
            }
            val drafts = updatedStudents.associate { it.id to it.rollNumber.toString() }
            state.copy(students = updatedStudents, rollDrafts = drafts)
        }
    }

    fun approveRollNumbers() {
        val state = _uiState.value
        val pendingDeletes = state.pendingDeleteStudentIds
        val remainingStudents = state.students.filter { it.id !in pendingDeletes }
        val assignments = remainingStudents.mapIndexed { index, student ->
            val draftVal = state.rollDrafts[student.id]?.toIntOrNull()
            val roll = draftVal?.takeIf { it > 0 } ?: student.rollNumber.takeIf { it > 0 } ?: (index + 1)
            RollNumberAssignment(student_id = student.id, roll_no = roll)
        }
        val studentIdsToApprove = remainingStudents.map { it.id }

        viewModelScope.launch {
            _uiState.update { it.copy(isApprovingRolls = true, errorMessage = null) }

            // 1. Delete all marked students from the group in parallel
            if (pendingDeletes.isNotEmpty()) {
                coroutineScope {
                    pendingDeletes.map { studentId ->
                        async { teacherRepository.removeMember(groupId, studentId) }
                    }.awaitAll()
                }
            }

            // 2. Set roll numbers
            val setResult = teacherRepository.setRollNumbers(groupId, assignments)
            if (setResult !is NetworkResult.Success) {
                _uiState.update {
                    it.copy(
                        isApprovingRolls = false,
                        errorMessage = setResult.userMessage(),
                    )
                }
                return@launch
            }

            // 3. Approve roll numbers
            when (val approveResult = teacherRepository.approveRollNumbers(groupId, studentIdsToApprove)) {
                is NetworkResult.Success -> {
                    val updatedStudents = TeacherUiMappers.students(approveResult.data)
                    originalStudents = updatedStudents
                    _uiState.update {
                        it.copy(
                            isApprovingRolls = false,
                            isEditing = false,
                            students = updatedStudents,
                            rollDrafts = emptyMap(),
                            pendingDeleteStudentIds = emptySet(),
                            actionMessage = "Roll numbers approved successfully",
                        )
                    }
                    refreshSilently()
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            isApprovingRolls = false,
                            errorMessage = approveResult.userMessage(),
                        )
                    }
                }
            }
        }
    }

    fun removeStudent(studentId: String) {
        toggleMarkStudentDelete(studentId)
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            refreshSilently()
            _uiState.update { it.copy(isLoading = false) }
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
                    _uiState.update {
                        it.copy(
                            overview = TeacherUiMappers.classOverview(
                                unit = unitResult.data,
                                summary = summary,
                                memberCount = members.size,
                            ),
                            students = TeacherUiMappers.students(members, parentIds),
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
