package com.lushaiedupls.ui.teacher.secondary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.mock.AppNotification
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
            val hasContent = _uiState.value.announcements.isNotEmpty()
            _uiState.update {
                if (hasContent) {
                    it.copy(isRefreshing = true, isLoading = false, errorMessage = null)
                } else {
                    it.copy(isLoading = true, isRefreshing = false, errorMessage = null)
                }
            }
            when (val result = teacherRepository.notifications()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        announcements = StudentUiMappers.notifications(result.data),
                    )
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = result.userMessage(),
                    )
                }
            }
        }
    }

    fun startEdit(item: AppNotification) {
        _uiState.update {
            it.copy(
                composing = true,
                editingId = item.id,
                title = item.title,
                body = item.body,
                errorMessage = null,
            )
        }
    }

    fun cancelCompose() {
        _uiState.update {
            it.copy(
                composing = false,
                editingId = null,
                title = "",
                body = "",
                errorMessage = null,
            )
        }
    }

    fun onTitle(value: String) {
        if (value.length <= 180) {
            _uiState.update { it.copy(title = value, errorMessage = null) }
        }
    }

    fun onBody(value: String) {
        if (value.length <= 2000) {
            _uiState.update { it.copy(body = value, errorMessage = null) }
        }
    }

    fun save() {
        val state = _uiState.value
        val id = state.editingId ?: return
        if (state.title.isBlank() || state.body.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (
                val result = teacherRepository.updateNotification(
                    notificationId = id,
                    title = state.title.trim(),
                    body = state.body.trim(),
                )
            ) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            composing = false,
                            editingId = null,
                            title = "",
                            body = "",
                        )
                    }
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            when (val result = teacherRepository.deleteNotification(id)) {
                is NetworkResult.Success -> refresh()
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
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
