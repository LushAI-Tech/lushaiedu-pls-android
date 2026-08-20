package com.lushaiedupls.ui.teacher.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.mock.TeacherAttendanceMark
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherTakeAttendanceViewModel(
    private val teacherRepository: TeacherRepository,
    private val unitId: String,
    private val dateLabel: String,
    private val initialPeriodId: String?,
    private val initialIsExtraClass: Boolean,
    private val initialExtraLabel: String?,
) : ViewModel() {

    private var periodId: String? = initialPeriodId
    private var isExtraClass: Boolean = initialIsExtraClass
    private var extraLabel: String? = initialExtraLabel

    private val _uiState = MutableStateFlow(TeacherTakeAttendanceUiState(isLoading = true))
    val uiState: StateFlow<TeacherTakeAttendanceUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setMark(studentId: String, mark: TeacherAttendanceMark) {
        _uiState.update { current ->
            val currentMark = current.marks[studentId] ?: TeacherAttendanceMark.None
            val next = if (currentMark == mark) TeacherAttendanceMark.None else mark
            current.copy(
                marks = current.marks + (studentId to next),
                saved = false,
            )
        }
    }

    fun saveAttendance() {
        val entries = _uiState.value.marks.mapNotNull { (studentId, mark) ->
            val status = TeacherUiMappers.attendanceMarkToStatus(mark) ?: return@mapNotNull null
            studentId to status
        }
        if (entries.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Mark at least one student before saving.") }
            return
        }
        if (!isExtraClass && periodId.isNullOrBlank()) {
            _uiState.update {
                it.copy(errorMessage = "A period is required unless this is an extra class.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (
                val result = teacherRepository.saveRoll(
                    unitId = unitId,
                    date = dateLabel,
                    periodId = if (isExtraClass) null else periodId,
                    isExtraClass = isExtraClass,
                    extraLabel = if (isExtraClass) extraLabel else null,
                    entries = entries,
                )
            ) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isSaving = false, saved = true)
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val rosterResult = teacherRepository.unitRoster(
                unitId = unitId,
                date = dateLabel,
                periodId = if (isExtraClass) null else periodId,
                isExtraClass = isExtraClass,
                extraLabel = if (isExtraClass) extraLabel else null,
            )
            when (rosterResult) {
                is NetworkResult.Success -> {
                    val session = TeacherUiMappers.attendanceSession(rosterResult.data)
                    if (!isExtraClass) {
                        periodId = rosterResult.data.period_id ?: periodId
                    }
                    isExtraClass = rosterResult.data.is_extra_class
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            session = session,
                            marks = session.students.associate { row -> row.student.id to row.mark },
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = rosterResult.userMessage())
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            teacherRepository: TeacherRepository,
            unitId: String,
            dateLabel: String,
            periodId: String?,
            isExtraClass: Boolean,
            extraLabel: String?,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherTakeAttendanceViewModel(
                teacherRepository = teacherRepository,
                unitId = unitId,
                dateLabel = dateLabel,
                initialPeriodId = periodId,
                initialIsExtraClass = isExtraClass,
                initialExtraLabel = extraLabel,
            )
        }
    }
}
