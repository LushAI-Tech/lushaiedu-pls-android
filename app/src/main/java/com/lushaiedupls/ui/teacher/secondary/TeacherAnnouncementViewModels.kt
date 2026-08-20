package com.lushaiedupls.ui.teacher.secondary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.NotificationAudience
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherAnnouncementsViewModel(
    private val teacherRepository: TeacherRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherAnnouncementsUiState(isLoading = true))
    val uiState: StateFlow<TeacherAnnouncementsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = teacherRepository.notifications()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        announcements = TeacherUiMappers.announcements(result.data),
                    )
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            teacherRepository: TeacherRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherAnnouncementsViewModel(teacherRepository)
        }
    }
}

class TeacherNewAnnouncementViewModel(
    private val teacherRepository: TeacherRepository,
) : ViewModel() {

    private var classIds: Set<String> = emptySet()

    private val _uiState = MutableStateFlow(
        TeacherNewAnnouncementUiState(isLoading = true),
    )
    val uiState: StateFlow<TeacherNewAnnouncementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = teacherRepository.teachingUnits()) {
                is NetworkResult.Success -> {
                    val audiences = TeacherUiMappers.announcementAudiences(result.data)
                    classIds = audiences.filterNot { it.isSelectAll }.map { it.id }.toSet()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            audiences = audiences,
                            selectedAudienceIds = classIds.take(1).toSet(),
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun toggleAudience(id: String) {
        _uiState.update { current ->
            val audience = current.audiences.find { it.id == id } ?: return@update current
            val next = current.selectedAudienceIds.toMutableSet()
            if (audience.isSelectAll) {
                if (next.containsAll(classIds) && next.contains(id)) {
                    next.clear()
                } else {
                    next.clear()
                    next.add(id)
                    next.addAll(classIds)
                }
            } else {
                if (!next.add(id)) next.remove(id)
                if (classIds.isNotEmpty() && next.containsAll(classIds)) {
                    next.add("all")
                } else {
                    next.remove("all")
                }
            }
            current.copy(selectedAudienceIds = next)
        }
    }

    fun setPriority(priority: AnnouncementPriority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun onSubjectChange(value: String) {
        if (value.length <= 180) {
            _uiState.update { it.copy(subject = value) }
        }
    }

    fun onBodyChange(value: String) {
        if (value.length <= 2000) {
            _uiState.update { it.copy(body = value) }
        }
    }

    fun send() {
        val current = _uiState.value
        if (!current.canSend) return
        val title = if (current.priority == AnnouncementPriority.Urgent) {
            "[Urgent] ${current.subject.trim()}"
        } else {
            current.subject.trim()
        }
        val body = current.body.trim()
        val unitIds = current.selectedAudienceIds.filter { it != "all" }
        val selectAll = "all" in current.selectedAudienceIds ||
            (classIds.isNotEmpty() && current.selectedAudienceIds.containsAll(classIds))
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            val results = if (selectAll || unitIds.isEmpty()) {
                listOf(
                    teacherRepository.createNotification(
                        title = title,
                        body = body,
                        audience = NotificationAudience.STUDENTS,
                    ),
                )
            } else {
                unitIds.map { unitId ->
                    teacherRepository.createNotification(
                        title = title,
                        body = body,
                        audience = NotificationAudience.TEACHING_UNIT,
                        teachingUnitId = unitId,
                    )
                }
            }
            val failed = results.firstOrNull { it !is NetworkResult.Success }
            if (failed != null) {
                _uiState.update {
                    it.copy(isSending = false, errorMessage = failed.userMessage())
                }
            } else {
                _uiState.update { it.copy(isSending = false, sent = true) }
            }
        }
    }

    companion object {
        fun provideFactory(
            teacherRepository: TeacherRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherNewAnnouncementViewModel(teacherRepository)
        }
    }
}
