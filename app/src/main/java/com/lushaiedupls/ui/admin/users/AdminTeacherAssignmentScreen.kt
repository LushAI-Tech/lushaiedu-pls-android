package com.lushaiedupls.ui.admin.users

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.TeacherAssignment
import com.lushaiedupls.data.remote.dto.TeacherInstitutionGroup
import com.lushaiedupls.data.remote.dto.UserOut
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.auth.components.SelectionTile
import com.lushaiedupls.ui.common.FilterRowListLoading
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminAssignmentSubjectChoice(
    val classId: String,
    val className: String,
    val subjectId: String,
    val subjectName: String,
) {
    val key: String get() = "$classId:$subjectId"
}

data class AdminTeacherAssignmentUiState(
    val teacherId: String = "",
    val teacherName: String = "",
    val institutions: List<InstitutionOut> = emptyList(),
    val selectedInstitutionId: String? = null,
    val classes: List<ClassOut> = emptyList(),
    val selectedClassIds: Set<String> = emptySet(),
    val subjectChoices: List<AdminAssignmentSubjectChoice> = emptyList(),
    val selectedAssignmentKeys: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isLoadingSubjects: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

class AdminTeacherAssignmentViewModel(
    private val adminRepository: AdminRepository,
    private val teacherId: String,
    teacherName: String,
    private val initialUser: UserOut? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AdminTeacherAssignmentUiState(
            teacherId = teacherId,
            teacherName = teacherName,
            isLoading = true,
        ),
    )
    val uiState: StateFlow<AdminTeacherAssignmentUiState> = _uiState.asStateFlow()

    private var userInstitutions: List<TeacherInstitutionGroup> = emptyList()
    private var loadGeneration = 0

    fun reload(latestUser: UserOut? = initialUser) {
        loadInitial(latestUser)
    }

    private fun resolveUser(network: UserOut?, fallback: UserOut?): UserOut? {
        if (network == null) return fallback
        if (network.teaching_institutions.isNotEmpty() || fallback == null) return network
        if (fallback.teaching_institutions.isEmpty()) return network
        return network.copy(teaching_institutions = fallback.teaching_institutions)
    }

    private fun orderInstitutions(
        catalog: List<InstitutionOut>,
        user: UserOut?,
    ): List<InstitutionOut> {
        if (user == null || user.teaching_institutions.isEmpty()) return catalog
        val byId = catalog.associateBy { it.id }
        val byName = catalog.associateBy { it.name.trim().lowercase() }
        val seen = linkedSetOf<String>()
        val leading = mutableListOf<InstitutionOut>()
        for (group in user.teaching_institutions) {
            val match = byId[group.institution_id]
                ?: group.institution_name.trim().lowercase()
                    .takeIf { it.isNotBlank() }
                    ?.let(byName::get)
                ?: continue
            if (seen.add(match.id)) leading += match
        }
        if (leading.isEmpty()) return catalog
        return leading + catalog.filter { it.id !in seen }
    }

    private fun captureUser(user: UserOut, institutions: List<InstitutionOut>): String? {
        userInstitutions = user.teaching_institutions.filter { group ->
            group.institution_id.isNotBlank()
        }
        val byId = institutions.associateBy { it.id }
        val byName = institutions.associateBy { it.name.trim().lowercase() }
        for (group in userInstitutions) {
            byId[group.institution_id]?.let { return it.id }
            val name = group.institution_name.trim().lowercase()
            if (name.isNotBlank()) {
                byName[name]?.let { return it.id }
            }
        }
        return userInstitutions.firstOrNull()?.institution_id
    }

    private fun hasSavedSelection(): Boolean =
        userInstitutions.any { group ->
            group.assignments.any { it.class_id.isNotBlank() && it.subject_id.isNotBlank() }
        }

    private fun loadInitial(latestUser: UserOut? = initialUser) {
        val generation = ++loadGeneration
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val institutionsDeferred = async {
                    adminRepository.listInstitutions(includeInactive = false)
                }
                val userDeferred = async { adminRepository.getUser(teacherId) }
                val institutionsResult = institutionsDeferred.await()
                val userResult = userDeferred.await()
                if (generation != loadGeneration) return@coroutineScope

                if (institutionsResult !is NetworkResult.Success) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = institutionsResult.userMessage(),
                        )
                    }
                    return@coroutineScope
                }

                val catalog = institutionsResult.data.filter { it.is_active }
                val user = resolveUser(
                    network = (userResult as? NetworkResult.Success)?.data,
                    fallback = latestUser,
                )
                val institutions = orderInstitutions(catalog, user)
                if (user == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            institutions = institutions,
                            errorMessage = userResult.userMessage(),
                        )
                    }
                    return@coroutineScope
                }
                val selectedInstitutionId = captureUser(user, institutions)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        institutions = institutions,
                        selectedInstitutionId = selectedInstitutionId,
                        selectedClassIds = emptySet(),
                        selectedAssignmentKeys = emptySet(),
                        subjectChoices = emptyList(),
                        classes = emptyList(),
                        teacherName = user.name.takeIf { name -> name.isNotBlank() }
                            ?: it.teacherName,
                        errorMessage = null,
                    )
                }
                selectedInstitutionId?.let { loadClasses(it, restoreFromUser = true, generation) }
            }
        }
    }

    fun selectInstitution(index: Int) {
        val selected = _uiState.value.institutions.getOrNull(index) ?: return
        val alreadySelected = selected.id == _uiState.value.selectedInstitutionId
        if (alreadySelected && _uiState.value.selectedClassIds.isNotEmpty()) return
        if (!alreadySelected) {
            _uiState.update {
                it.copy(
                    selectedInstitutionId = selected.id,
                    classes = emptyList(),
                    selectedClassIds = emptySet(),
                    subjectChoices = emptyList(),
                    selectedAssignmentKeys = emptySet(),
                    errorMessage = null,
                )
            }
        }
        loadClasses(selected.id, restoreFromUser = true)
    }

    private fun loadClasses(
        institutionId: String,
        restoreFromUser: Boolean,
        generation: Int = ++loadGeneration,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val result = adminRepository.listClasses(
                    includeInactive = false,
                    institutionId = institutionId,
                )
            ) {
                is NetworkResult.Success -> {
                    if (generation != loadGeneration) return@launch
                    val classes = result.data.filter { c -> c.is_active }
                    val shouldRestore = restoreFromUser && hasSavedSelection() && classes.isNotEmpty()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            classes = classes,
                            selectedClassIds = emptySet(),
                            subjectChoices = emptyList(),
                            selectedAssignmentKeys = emptySet(),
                            isLoadingSubjects = shouldRestore,
                        )
                    }
                    if (shouldRestore) {
                        applySavedUserSelection(classes, generation)
                    }
                }
                else -> {
                    if (generation != loadGeneration) return@launch
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.userMessage())
                    }
                }
            }
        }
    }

    private fun applySavedUserSelection(classes: List<ClassOut>, generation: Int) {
        if (generation != loadGeneration) return
        if (!restoreAssignmentsFromUser(classes, generation)) {
            _uiState.update { it.copy(isLoadingSubjects = false) }
        }
    }

    private fun restoreAssignmentsFromUser(classes: List<ClassOut>, generation: Int): Boolean {
        val institutionId = _uiState.value.selectedInstitutionId
        val classIdsInInstitution = classes.map { it.id }.toSet()
        val scoped = userInstitutions
            .firstOrNull { it.institution_id == institutionId }
            ?.assignments
            .orEmpty()
            .filter { row ->
                row.class_id in classIdsInInstitution && row.subject_id.isNotBlank()
            }
        val classIds = scoped.map { it.class_id }.toSet()
        val keys = scoped.map { "${it.class_id}:${it.subject_id}" }.toSet()
        if (classIds.isEmpty()) return false
        _uiState.update {
            it.copy(
                selectedClassIds = classIds,
                selectedAssignmentKeys = keys,
                isLoadingSubjects = true,
            )
        }
        loadSubjects(classIds, preselectKeys = keys, generation = generation)
        return true
    }

    fun toggleClass(classId: String) {
        _uiState.update { state ->
            val removing = classId in state.selectedClassIds
            val next = if (removing) {
                state.selectedClassIds - classId
            } else {
                state.selectedClassIds + classId
            }
            val keptKeys = if (removing) {
                state.selectedAssignmentKeys.filterNot { it.startsWith("$classId:") }.toSet()
            } else {
                state.selectedAssignmentKeys
            }
            state.copy(
                selectedClassIds = next,
                subjectChoices = emptyList(),
                selectedAssignmentKeys = keptKeys,
                isLoadingSubjects = next.isNotEmpty(),
            )
        }
        val classIds = _uiState.value.selectedClassIds
        if (classIds.isEmpty()) {
            _uiState.update { it.copy(isLoadingSubjects = false) }
            return
        }
        loadSubjects(classIds)
    }

    private fun loadSubjects(
        classIds: Set<String>,
        preselectKeys: Set<String>? = null,
        generation: Int = loadGeneration,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSubjects = true, errorMessage = null) }
            val (choices, error) = loadSubjectChoices(classIds)
            if (generation != loadGeneration) return@launch
            val choiceKeys = choices.map { it.key }.toSet()
            val selectedKeys = preselectKeys?.intersect(choiceKeys)
                ?: _uiState.value.selectedAssignmentKeys.intersect(choiceKeys)
            _uiState.update {
                it.copy(
                    isLoadingSubjects = false,
                    subjectChoices = choices.sortedWith(
                        compareBy({ it.className }, { it.subjectName }),
                    ),
                    selectedAssignmentKeys = selectedKeys,
                    errorMessage = error.takeIf { choices.isEmpty() },
                )
            }
        }
    }

    private suspend fun loadSubjectChoices(
        classIds: Set<String>,
    ): Pair<List<AdminAssignmentSubjectChoice>, String?> {
        val institutionId = _uiState.value.selectedInstitutionId
            ?: return emptyList<AdminAssignmentSubjectChoice>() to null
        val classNames = _uiState.value.classes.associate { it.id to it.name }
        val loaded = coroutineScope {
            classIds.map { classId ->
                async {
                    classId to adminRepository.listSubjects(
                        classId = classId,
                        institutionId = institutionId,
                        includeInactive = false,
                    )
                }
            }.awaitAll()
        }
        val choices = mutableListOf<AdminAssignmentSubjectChoice>()
        var error: String? = null
        loaded.forEach { (classId, result) ->
            when (result) {
                is NetworkResult.Success -> {
                    result.data.filter { it.is_active }.forEach { subject ->
                        choices += AdminAssignmentSubjectChoice(
                            classId = classId,
                            className = classNames[classId].orEmpty(),
                            subjectId = subject.id,
                            subjectName = subject.name,
                        )
                    }
                }
                else -> error = result.userMessage()
            }
        }
        return choices to error
    }

    fun toggleAssignment(key: String) {
        _uiState.update { state ->
            val next = if (key in state.selectedAssignmentKeys) {
                state.selectedAssignmentKeys - key
            } else {
                state.selectedAssignmentKeys + key
            }
            state.copy(selectedAssignmentKeys = next, errorMessage = null)
        }
    }

    fun save() {
        val state = _uiState.value
        val institutionId = state.selectedInstitutionId
        if (institutionId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Select an institution first.") }
            return
        }
        val assignments = state.subjectChoices
            .filter { it.key in state.selectedAssignmentKeys }
            .map { TeacherAssignment(class_id = it.classId, subject_id = it.subjectId) }
        if (assignments.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Select at least one class–subject pair.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (
                val result = adminRepository.assignTeacherInstitution(
                    teacherId = state.teacherId,
                    institutionId = institutionId,
                    assignments = assignments,
                    replaceExisting = true,
                )
            ) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, saved = true) }
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun clearSaved() {
        _uiState.update { it.copy(saved = false) }
    }

    companion object {
        fun provideFactory(
            adminRepository: AdminRepository,
            teacherId: String,
            teacherName: String,
            initialUser: UserOut? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            AdminTeacherAssignmentViewModel(adminRepository, teacherId, teacherName, initialUser)
        }
    }
}

@Composable
fun AdminTeacherAssignmentRoute(
    adminRepository: AdminRepository,
    teacherId: String,
    teacherName: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    initialUser: UserOut? = null,
) {
    val viewModel: AdminTeacherAssignmentViewModel = viewModel(
        key = "admin-teacher-assign-$teacherId-v2",
        factory = AdminTeacherAssignmentViewModel.provideFactory(
            adminRepository,
            teacherId,
            teacherName,
            initialUser,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(teacherId) {
        viewModel.reload(initialUser)
    }
    BackHandler(onBack = onBack)
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.clearSaved()
            onSaved()
        }
    }
    AdminTeacherAssignmentScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectInstitution = viewModel::selectInstitution,
        onToggleClass = viewModel::toggleClass,
        onToggleAssignment = viewModel::toggleAssignment,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

@Composable
fun AdminTeacherAssignmentScreen(
    uiState: AdminTeacherAssignmentUiState,
    onBack: () -> Unit,
    onSelectInstitution: (Int) -> Unit,
    onToggleClass: (String) -> Unit,
    onToggleAssignment: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        AdminScreenHeader(
            title = stringResource(R.string.admin_teacher_assign_title),
            onBack = onBack,
        )
        Text(
            text = uiState.teacherName,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = BrandOrange,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading && uiState.institutions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BrandBlack)
            }
        } else {
            if (uiState.institutions.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.timetable_institution),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AdminFilterRow(
                    labels = uiState.institutions.map { it.name },
                    selectedIndex = uiState.institutions
                        .indexOfFirst { it.id == uiState.selectedInstitutionId },
                    onSelect = onSelectInstitution,
                    allowUnselected = true,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.classes.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.select_classes),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(8.dp))
                uiState.classes.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        row.forEach { classOut ->
                            SelectionTile(
                                label = classOut.name,
                                selected = classOut.id in uiState.selectedClassIds,
                                onClick = { onToggleClass(classOut.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            } else if (uiState.isLoading) {
                FilterRowListLoading()
            }

            if (uiState.isLoadingSubjects) {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(color = BrandBlack)
            } else if (uiState.subjectChoices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.select_subjects_per_class),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(4.dp))
                AdminMuted(stringResource(R.string.select_subjects_per_class_hint))
                Spacer(modifier = Modifier.height(8.dp))
                uiState.subjectChoices.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        row.forEach { choice ->
                            SelectionTile(
                                label = "${choice.className} · ${choice.subjectName}",
                                selected = choice.key in uiState.selectedAssignmentKeys,
                                onClick = { onToggleAssignment(choice.key) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = BrandOrange, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(
            text = stringResource(
                if (uiState.isSaving) R.string.loading else R.string.admin_teacher_assign_save,
            ),
            onClick = onSave,
            enabled = !uiState.isSaving && !uiState.isLoading,
            fullyRounded = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
