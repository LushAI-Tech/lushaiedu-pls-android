package com.lushaiedupls.ui.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.AdminUserCreateRequest
import com.lushaiedupls.data.remote.dto.AdminUserEditRequest
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.Gender
import com.lushaiedupls.data.remote.dto.UserOut
import com.lushaiedupls.data.remote.dto.UserRole
import com.lushaiedupls.data.remote.dto.UserStatus
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.common.viewModelFactory
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdminUserFilter {
    Pending,
    Students,
    Teachers,
    Parents,
    Admins,
    All,
}

data class AdminUsersUiState(
    val users: List<UserOut> = emptyList(),
    val classes: List<ClassOut> = emptyList(),
    val filter: AdminUserFilter = AdminUserFilter.Pending,
    val query: String = "",
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val composing: Boolean = false,
    val editingId: String? = null,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val dob: String = "",
    val role: UserRole = UserRole.STUDENT,
    val gender: Gender? = null,
    val classId: String? = null,
    val createdPassword: String? = null,
    val createdName: String? = null,
    val enrollmentByUserId: Map<String, String> = emptyMap(),
)

class AdminUsersViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUsersUiState(isLoading = true))
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var listJob: Job? = null

    init {
        refresh()
    }

    fun setFilter(filter: AdminUserFilter) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                filter = filter,
                page = 1,
                users = emptyList(),
                hasMore = true,
                errorMessage = null,
            )
        }
        refresh()
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        searchJob?.cancel()
        listJob?.cancel()
        _uiState.update { it.copy(isLoadingMore = false) }
        searchJob = viewModelScope.launch {
            delay(3_000)
            refresh()
        }
    }

    fun refresh() {
        load(reset = true)
    }

    fun loadMore() {
        if (searchJob?.isActive == true) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        val current = _uiState.value
        if (current.composing || current.createdPassword != null) return
        if (!reset) {
            if (current.isLoading || current.isLoadingMore || !current.hasMore || current.users.isEmpty()) {
                return
            }
        }
        listJob?.cancel()
        val nextPage = if (reset) 1 else current.page + 1
        _uiState.update {
            it.copy(
                isLoading = reset,
                isLoadingMore = !reset,
                errorMessage = null,
                hasMore = if (reset) true else it.hasMore,
            )
        }
        listJob = viewModelScope.launch {
            val state = _uiState.value
            val role = when (state.filter) {
                AdminUserFilter.Students -> UserRole.STUDENT
                AdminUserFilter.Teachers -> UserRole.TEACHER
                AdminUserFilter.Parents -> UserRole.PARENT
                AdminUserFilter.Admins -> UserRole.ADMIN
                else -> null
            }
            val status = when (state.filter) {
                AdminUserFilter.Pending -> UserStatus.PENDING_APPROVAL
                else -> null
            }
            val usersResult = adminRepository.listUsers(
                role = role,
                status = status,
                query = state.query,
                page = nextPage,
                limit = PageSize,
            )
            val classesResult = if (reset) {
                adminRepository.listClasses(includeInactive = false)
            } else {
                null
            }
            when (usersResult) {
                is NetworkResult.Success -> {
                    val page = usersResult.data
                    val classes = (classesResult as? NetworkResult.Success)?.data
                    _uiState.update {
                        val items = if (reset) {
                            page.items
                        } else {
                            (it.users + page.items).distinctBy { user -> user.id }
                        }
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            users = items,
                            page = page.page,
                            totalPages = page.total_pages.coerceAtLeast(1),
                            total = page.total,
                            hasMore = page.page < page.total_pages && page.items.isNotEmpty(),
                            classes = classes ?: it.classes,
                            enrollmentByUserId = if (reset) emptyMap() else it.enrollmentByUserId,
                        )
                    }
                    val loaded = _uiState.value
                    val pending = loaded.users.filter { it.status == UserStatus.PENDING_APPROVAL }
                    if (pending.isNotEmpty()) {
                        val enrollment = resolveEnrollments(
                            pending = pending,
                            classes = loaded.classes,
                            existing = loaded.enrollmentByUserId,
                        )
                        _uiState.update { it.copy(enrollmentByUserId = enrollment) }
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = usersResult.userMessage(),
                    )
                }
            }
        }
    }

    fun startCreate() {
        _uiState.update {
            it.copy(
                composing = true,
                editingId = null,
                createdPassword = null,
                createdName = null,
                name = "",
                phone = "",
                email = "",
                address = "",
                dob = "",
                role = UserRole.STUDENT,
                gender = null,
                classId = null,
                errorMessage = null,
            )
        }
    }

    fun startEdit(user: UserOut) {
        _uiState.update {
            it.copy(
                composing = true,
                editingId = user.id,
                createdPassword = null,
                createdName = null,
                name = user.name,
                phone = user.phone.orEmpty(),
                email = user.email.orEmpty(),
                address = user.address.orEmpty(),
                dob = "",
                role = user.role,
                gender = user.gender,
                classId = user.class_id,
                errorMessage = null,
            )
        }
    }

    fun cancelCompose() {
        _uiState.update {
            it.copy(
                composing = false,
                editingId = null,
                createdPassword = null,
                createdName = null,
                errorMessage = null,
            )
        }
        refresh()
    }

    fun onName(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun onPhone(value: String) = _uiState.update { it.copy(phone = value, errorMessage = null) }
    fun onEmail(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun onAddress(value: String) = _uiState.update { it.copy(address = value, errorMessage = null) }
    fun onDob(value: String) = _uiState.update { it.copy(dob = value, errorMessage = null) }
    fun onRole(value: UserRole) = _uiState.update {
        it.copy(
            role = value,
            classId = if (value == UserRole.STUDENT) it.classId else null,
            errorMessage = null,
        )
    }
    fun onGender(value: Gender?) = _uiState.update { it.copy(gender = value, errorMessage = null) }
    fun onClassId(value: String?) = _uiState.update { it.copy(classId = value, errorMessage = null) }

    fun saveForm() {
        val state = _uiState.value
        val name = state.name.trim()
        val phone = state.phone.trim()
        val email = state.email.trim().ifBlank { null }
        val address = state.address.trim().ifBlank { null }
        if (name.isBlank() || phone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name and phone are required.") }
            return
        }
        if (phone.length !in 7..32) {
            _uiState.update { it.copy(errorMessage = "Phone must be 7 to 32 characters.") }
            return
        }
        val editingId = state.editingId
        if (editingId == null) {
            val dob = state.dob.trim()
            if (!isIsoDate(dob)) {
                _uiState.update { it.copy(errorMessage = "Date of birth must be YYYY-MM-DD.") }
                return
            }
            if (state.role == UserRole.ADMIN) {
                _uiState.update { it.copy(errorMessage = "Create a teacher or admin with an invite code.") }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isWorking = true, errorMessage = null) }
                when (
                    val result = adminRepository.createUser(
                        AdminUserCreateRequest(
                            name = name,
                            phone = phone,
                            role = state.role,
                            dob = dob,
                            email = email,
                            gender = state.gender,
                            address = address,
                            class_id = state.classId.takeIf { state.role == UserRole.STUDENT },
                        ),
                    )
                ) {
                    is NetworkResult.Success -> _uiState.update {
                        it.copy(
                            isWorking = false,
                            composing = false,
                            createdPassword = result.data.temporary_password,
                            createdName = result.data.user.name,
                        )
                    }
                    else -> _uiState.update {
                        it.copy(isWorking = false, errorMessage = result.userMessage())
                    }
                }
            }
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isWorking = true, errorMessage = null) }
                when (
                    val result = adminRepository.editUser(
                        editingId,
                        AdminUserEditRequest(
                            name = name,
                            phone = phone,
                            email = email,
                            gender = state.gender,
                            address = address,
                            role = state.role.takeUnless { it == UserRole.ADMIN },
                            class_id = state.classId.takeIf { state.role == UserRole.STUDENT },
                        ),
                    )
                ) {
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(isWorking = false, composing = false, editingId = null)
                        }
                        refresh()
                    }
                    else -> _uiState.update {
                        it.copy(isWorking = false, errorMessage = result.userMessage())
                    }
                }
            }
        }
    }

    fun approve(userId: String) = mutate { adminRepository.approveUser(userId) }

    fun reject(userId: String) = mutate { adminRepository.rejectUser(userId) }

    fun suspendUser(userId: String) = mutate {
        adminRepository.updateUserStatus(userId, UserStatus.SUSPENDED)
    }

    fun reactivate(userId: String) = mutate {
        adminRepository.updateUserStatus(userId, UserStatus.ACTIVE)
    }

    private fun mutate(block: suspend () -> NetworkResult<UserOut>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, errorMessage = null) }
            when (val result = block()) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isWorking = false) }
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isWorking = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    private suspend fun resolveEnrollments(
        pending: List<UserOut>,
        classes: List<ClassOut>,
        existing: Map<String, String>,
    ): Map<String, String> {
        val missing = pending.filter { it.id !in existing }
        if (missing.isEmpty()) return existing
        val classNameById = classes.associate { it.id to it.name }
        val classByUser = missing.associate { user ->
            user.id to classNameById[user.class_id]
        }.toMutableMap()
        val subjectsByUser = missing.associate { it.id to mutableListOf<String>() }.toMutableMap()
        when (val unitsResult = adminRepository.teachingUnits()) {
            is NetworkResult.Success -> {
                val pendingIds = missing.map { it.id }.toSet()
                for (unit in unitsResult.data) {
                    val teacherId = unit.teacher?.id
                    if (teacherId != null && teacherId in pendingIds) {
                        subjectsByUser.getOrPut(teacherId) { mutableListOf() }.add(unit.subject_name)
                        if (classByUser[teacherId].isNullOrBlank()) {
                            classByUser[teacherId] = unit.class_name
                        }
                    }
                }
                val studentClassIds = missing
                    .filter { it.role == UserRole.STUDENT }
                    .mapNotNull { it.class_id }
                    .toSet()
                val relevantUnits = unitsResult.data.filter { it.class_id in studentClassIds }
                if (relevantUnits.isNotEmpty()) {
                    coroutineScope {
                        relevantUnits.map { unit ->
                            async {
                                unit to ((adminRepository.members(unit.id) as? NetworkResult.Success)?.data.orEmpty())
                            }
                        }.awaitAll()
                    }.forEach { (unit, members) ->
                        members.forEach { member ->
                            if (member.student.id in pendingIds) {
                                subjectsByUser.getOrPut(member.student.id) { mutableListOf() }
                                    .add(unit.subject_name)
                            }
                        }
                    }
                }
            }
            else -> Unit
        }
        val withoutSubjects = missing.filter { user ->
            subjectsByUser[user.id].isNullOrEmpty() && !user.class_id.isNullOrBlank()
        }
        if (withoutSubjects.isNotEmpty()) {
            val subjectsByClass = coroutineScope {
                withoutSubjects.mapNotNull { it.class_id }.distinct().map { classId ->
                    async {
                        classId to (
                            (adminRepository.listSubjects(classId, includeInactive = false) as? NetworkResult.Success)
                                ?.data
                                ?.map { it.name }
                                .orEmpty()
                            )
                    }
                }.awaitAll().toMap()
            }
            withoutSubjects.forEach { user ->
                val names = user.class_id?.let { subjectsByClass[it] }.orEmpty()
                subjectsByUser[user.id] = names.toMutableList()
            }
        }
        val formatted = missing.mapNotNull { user ->
            val line = formatEnrollment(
                className = classByUser[user.id],
                subjects = subjectsByUser[user.id].orEmpty().distinct(),
            ) ?: return@mapNotNull null
            user.id to line
        }.toMap()
        return existing + formatted
    }

    private fun formatEnrollment(className: String?, subjects: List<String>): String? {
        val subjectPart = subjects.joinToString(", ").takeIf { it.isNotBlank() }
        return listOfNotNull(className?.takeIf { it.isNotBlank() }, subjectPart)
            .joinToString(" · ")
            .takeIf { it.isNotBlank() }
    }

    private fun isIsoDate(value: String): Boolean {
        if (!value.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) return false
        return try {
            LocalDate.parse(value)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }

    companion object {
        private const val PageSize = 50

        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminUsersViewModel(adminRepository) }
    }
}
