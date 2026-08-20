package com.lushaiedupls.ui.auth.selectsubject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.CompleteOnboardingRequest
import com.lushaiedupls.data.remote.dto.Gender
import com.lushaiedupls.data.remote.dto.TeacherAssignment
import com.lushaiedupls.data.remote.dto.UserRole as ApiUserRole
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.selectrole.UserRole
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SelectSubjectViewModel(
    private val userSessionStore: UserSessionStore,
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val classIds = userSessionStore.getClassIds()
    private val appRole = userSessionStore.getRole()

    private val _uiState = MutableStateFlow(SelectSubjectUiState(isLoading = true))
    val uiState: StateFlow<SelectSubjectUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    fun loadSubjects() {
        if (classIds.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "No class selected.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val classNames = when (val classes = studentRepository.classes()) {
                is NetworkResult.Success -> classes.data.associate { it.id to it.name }
                else -> emptyMap()
            }
            val loaded = coroutineScope {
                classIds.map { classId ->
                    async {
                        classId to studentRepository.subjects(classId)
                    }
                }.awaitAll()
            }
            val subjects = mutableListOf<SubjectChoice>()
            var error: String? = null
            loaded.forEach { (classId, result) ->
                when (result) {
                    is NetworkResult.Success -> {
                        result.data
                            .filter { it.is_active }
                            .sortedBy { it.sort_order }
                            .forEach { subject ->
                                subjects += SubjectChoice(
                                    id = subject.id,
                                    name = subject.name,
                                    classId = classId,
                                    className = classNames[classId].orEmpty(),
                                )
                            }
                    }
                    else -> error = result.userMessage()
                }
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    subjects = subjects,
                    selectedSubjectIds = userSessionStore.getSubjectIds()
                        .filter { id -> subjects.any { s -> s.id == id } }
                        .toSet(),
                    errorMessage = error.takeIf { subjects.isEmpty() },
                )
            }
        }
    }

    fun onSubjectToggled(subjectId: String) {
        _uiState.update { state ->
            val next = if (subjectId in state.selectedSubjectIds) {
                state.selectedSubjectIds - subjectId
            } else {
                state.selectedSubjectIds + subjectId
            }
            state.copy(selectedSubjectIds = next, errorMessage = null)
        }
    }

    fun submitProfile() {
        val selected = _uiState.value.subjects.filter { it.id in _uiState.value.selectedSubjectIds }
        if (selected.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one subject.") }
            return
        }
        val apiRole = when (appRole) {
            UserRole.Student -> ApiUserRole.STUDENT
            UserRole.Teacher -> ApiUserRole.TEACHER
            UserRole.Admin -> ApiUserRole.ADMIN
            UserRole.Parents -> ApiUserRole.PARENT
            null -> {
                _uiState.update { it.copy(errorMessage = "Please select a role first.") }
                return
            }
        }
        val name = userSessionStore.getDisplayName().trim().ifBlank { "User" }
        val gender = when (userSessionStore.getPendingGender()) {
            "MALE" -> Gender.MALE
            "FEMALE" -> Gender.FEMALE
            "OTHER" -> Gender.OTHER
            else -> null
        }
        val invite = userSessionStore.getPendingInviteCode()
        if (apiRole == ApiUserRole.TEACHER && invite.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Invite code is required for teachers.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val request = when (apiRole) {
                ApiUserRole.STUDENT -> CompleteOnboardingRequest(
                    role = apiRole,
                    name = name,
                    phone = userSessionStore.getPendingPhone(),
                    gender = gender,
                    address = userSessionStore.getPendingAddress(),
                    class_id = classIds.first(),
                    subject_ids = selected.map { it.id },
                )
                ApiUserRole.TEACHER -> CompleteOnboardingRequest(
                    role = apiRole,
                    name = name,
                    invite_code = invite,
                    phone = userSessionStore.getPendingPhone(),
                    gender = gender,
                    address = userSessionStore.getPendingAddress(),
                    assignments = selected.map {
                        TeacherAssignment(class_id = it.classId, subject_id = it.id)
                    },
                )
                else -> CompleteOnboardingRequest(
                    role = apiRole,
                    name = name,
                    invite_code = invite,
                    phone = userSessionStore.getPendingPhone(),
                    gender = gender,
                    address = userSessionStore.getPendingAddress(),
                )
            }
            when (val result = authRepository.completeOnboarding(request)) {
                is NetworkResult.Success -> {
                    userSessionStore.setSubjectIds(selected.map { it.id })
                    _uiState.update { it.copy(isSubmitting = false, done = true) }
                }
                else -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun clearDone() {
        _uiState.update { it.copy(done = false) }
    }

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            studentRepository: StudentRepository,
            authRepository: AuthRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            SelectSubjectViewModel(userSessionStore, studentRepository, authRepository)
        }
    }
}
