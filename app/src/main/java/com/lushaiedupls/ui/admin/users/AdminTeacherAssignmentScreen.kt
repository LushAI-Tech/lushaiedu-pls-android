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
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.dto.TeachingUnitStatus
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AdminTeacherAssignmentUiState(
            teacherId = teacherId,
            teacherName = teacherName,
            isLoading = true,
        ),
    )
    val uiState: StateFlow<AdminTeacherAssignmentUiState> = _uiState.asStateFlow()

    /** Existing ACTIVE teaching units for this teacher (class/subject pairs). */
    private var teacherUnits: List<TeachingUnitOut> = emptyList()

    init {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val institutionsDeferred = async {
                    adminRepository.listInstitutions(includeInactive = false)
                }
                val unitsDeferred = async { adminRepository.teachingUnits(forceRefresh = true) }
                val userDeferred = async { adminRepository.getUser(teacherId) }
                val institutionsResult = institutionsDeferred.await()
                val unitsResult = unitsDeferred.await()
                val userResult = userDeferred.await()

                if (institutionsResult !is NetworkResult.Success) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = institutionsResult.userMessage(),
                        )
                    }
                    return@coroutineScope
                }

                val institutions = institutionsResult.data
                    .filter { it.is_active }
                    .sortedWith(compareBy({ it.sort_order }, { it.name }))
                teacherUnits = (unitsResult as? NetworkResult.Success)?.data
                    .orEmpty()
                    .filter { unit ->
                        unit.teacher?.id == teacherId &&
                            unit.status == TeachingUnitStatus.ACTIVE
                    }
                val teacherInstitutionId = (userResult as? NetworkResult.Success)?.data
                    ?.institution_id
                    ?.takeIf { id -> institutions.any { it.id == id } }
                val unitInstitutionId = teacherUnits
                    .mapNotNull { it.institution_id }
                    .firstOrNull { id -> institutions.any { it.id == id } }
                val selectedInstitutionId = teacherInstitutionId
                    ?: unitInstitutionId
                    ?: institutions.firstOrNull()?.id

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        institutions = institutions,
                        selectedInstitutionId = selectedInstitutionId,
                        teacherName = (userResult as? NetworkResult.Success)?.data?.name
                            ?.takeIf { name -> name.isNotBlank() }
                            ?: it.teacherName,
                    )
                }
                selectedInstitutionId?.let { loadClasses(it, applyExisting = true) }
            }
        }
    }

    fun selectInstitution(index: Int) {
        val selected = _uiState.value.institutions.getOrNull(index) ?: return
        if (selected.id == _uiState.value.selectedInstitutionId) return
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
        loadClasses(selected.id, applyExisting = true)
    }

    private fun loadClasses(institutionId: String, applyExisting: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val result = adminRepository.listClasses(
                    includeInactive = false,
                    institutionId = institutionId,
                )
            ) {
                is NetworkResult.Success -> {
                    val classes = result.data.filter { c -> c.is_active }
                    _uiState.update {
                        it.copy(isLoading = false, classes = classes)
                    }
                    if (applyExisting) {
                        applyExistingSelections(institutionId)
                    }
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    private fun applyExistingSelections(institutionId: String) {
        val classIdsInInstitution = _uiState.value.classes.map { it.id }.toSet()
        val existing = teacherUnits.filter { unit ->
            unit.class_id in classIdsInInstitution &&
                (unit.institution_id == null || unit.institution_id == institutionId)
        }
        val classIds = existing.map { it.class_id }.toSet()
        val keys = existing.map { "${it.class_id}:${it.subject_id}" }.toSet()
        if (classIds.isEmpty()) {
            _uiState.update {
                it.copy(
                    selectedClassIds = emptySet(),
                    subjectChoices = emptyList(),
                    selectedAssignmentKeys = emptySet(),
                    isLoadingSubjects = false,
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                selectedClassIds = classIds,
                selectedAssignmentKeys = emptySet(),
                isLoadingSubjects = true,
            )
        }
        loadSubjects(classIds, preselectKeys = keys)
    }

    fun toggleClass(classId: String) {
        _uiState.update { state ->
            val next = if (classId in state.selectedClassIds) {
                state.selectedClassIds - classId
            } else {
                state.selectedClassIds + classId
            }
            state.copy(
                selectedClassIds = next,
                subjectChoices = emptyList(),
                selectedAssignmentKeys = emptySet(),
                isLoadingSubjects = next.isNotEmpty(),
            )
        }
        val classIds = _uiState.value.selectedClassIds
        if (classIds.isEmpty()) {
            _uiState.update { it.copy(isLoadingSubjects = false) }
            return
        }
        loadSubjects(classIds, preselectKeys = null)
    }

    private fun loadSubjects(classIds: Set<String>, preselectKeys: Set<String>?) {
        viewModelScope.launch {
            val institutionId = _uiState.value.selectedInstitutionId ?: return@launch
            val classNames = _uiState.value.classes.associate { it.id to it.name }
            _uiState.update { it.copy(isLoadingSubjects = true, errorMessage = null) }
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
            val choiceKeys = choices.map { it.key }.toSet()
            val selectedKeys = when {
                preselectKeys != null -> preselectKeys.intersect(choiceKeys)
                else -> _uiState.value.selectedAssignmentKeys.intersect(choiceKeys)
            }
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
        ): ViewModelProvider.Factory = viewModelFactory {
            AdminTeacherAssignmentViewModel(adminRepository, teacherId, teacherName)
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
) {
    val viewModel: AdminTeacherAssignmentViewModel = viewModel(
        key = "admin-teacher-assign-$teacherId",
        factory = AdminTeacherAssignmentViewModel.provideFactory(
            adminRepository,
            teacherId,
            teacherName,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                        .indexOfFirst { it.id == uiState.selectedInstitutionId }
                        .coerceAtLeast(0),
                    onSelect = onSelectInstitution,
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
