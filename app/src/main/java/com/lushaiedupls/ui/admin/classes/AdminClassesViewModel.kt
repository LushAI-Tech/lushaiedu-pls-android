package com.lushaiedupls.ui.admin.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.ClassCreate
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.ClassUpdate
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.StemNode
import com.lushaiedupls.data.remote.dto.SubjectCreate
import com.lushaiedupls.data.remote.dto.SubjectOut
import com.lushaiedupls.data.remote.dto.SubjectUpdate
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminClassesUiState(
    val classes: List<ClassOut> = emptyList(),
    val institutions: List<InstitutionOut> = emptyList(),
    val selectedInstitutionId: String? = null,
    val formInstitutionId: String? = null,
    val subjects: List<SubjectOut> = emptyList(),
    val selectedClass: ClassOut? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val isLoadingSubjects: Boolean = false,
    val errorMessage: String? = null,
    val composingClass: Boolean = false,
    val composingSubject: Boolean = false,
    val editingClassId: String? = null,
    val editingSubjectId: String? = null,
    val name: String = "",
    val code: String = "",
    val sortOrder: String = "0",
    val isActive: Boolean = true,
    val bindingSubjectId: String? = null,
    val stemBoards: List<StemNode> = emptyList(),
    val stemGrades: List<StemNode> = emptyList(),
    val stemSubjects: List<StemNode> = emptyList(),
    val selectedBoardId: String? = null,
    val selectedGradeId: String? = null,
    val formStemSubjectId: String? = null,
    val stemLabels: Map<String, String> = emptyMap(),
)

class AdminClassesViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminClassesUiState(isLoading = true))
    val uiState: StateFlow<AdminClassesUiState> = _uiState.asStateFlow()
    private var pendingCreateSubjectClassId: String? = null

    init {
        refresh(forceNetwork = _uiState.value.classes.isNotEmpty())
    }

    fun refresh(forceNetwork: Boolean = true) {
        viewModelScope.launch {
            val hasContent = _uiState.value.classes.isNotEmpty() ||
                _uiState.value.institutions.isNotEmpty()
            if (!hasContent) {
                _uiState.update {
                    it.copy(isLoading = true, isRefreshing = false, errorMessage = null)
                }
            } else {
                _uiState.update {
                    it.copy(isRefreshing = true, isLoading = false, errorMessage = null)
                }
            }
            val institutions = when (val result = adminRepository.listInstitutions(includeInactive = true)) {
                is NetworkResult.Success -> result.data.sortedWith(compareBy({ it.sort_order }, { it.name }))
                else -> _uiState.value.institutions
            }
            val selectedInstitutionId = _uiState.value.selectedInstitutionId
                ?.takeIf { id -> institutions.any { it.id == id } }
                ?: institutions.firstOrNull()?.id
            _uiState.update {
                it.copy(
                    institutions = institutions,
                    selectedInstitutionId = selectedInstitutionId,
                    formInstitutionId = it.formInstitutionId
                        ?.takeIf { id -> institutions.any { inst -> inst.id == id } }
                        ?: selectedInstitutionId,
                )
            }
            if (selectedInstitutionId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        classes = emptyList(),
                        selectedClass = null,
                        subjects = emptyList(),
                        errorMessage = if (institutions.isEmpty()) {
                            "No institutions are available yet."
                        } else {
                            "Select an institution first."
                        },
                    )
                }
                return@launch
            }
            when (val result = adminRepository.listClasses(
                includeInactive = true,
                institutionId = selectedInstitutionId,
                forceRefresh = forceNetwork,
            )) {
                is NetworkResult.Success -> {
                    val selectedId = _uiState.value.selectedClass?.id
                    val classes = result.data.sortedWith(compareBy({ it.sort_order }, { it.name }))
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            classes = classes,
                            selectedClass = classes.firstOrNull { item -> item.id == selectedId },
                        )
                    }
                    if (pendingCreateSubjectClassId != null) {
                        applyPendingCreateSubject()
                    } else {
                        selectedId?.let { loadSubjects(it) }
                    }
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

    fun selectInstitution(institutionId: String) {
        if (institutionId == _uiState.value.selectedInstitutionId) return
        _uiState.update {
            it.copy(
                selectedInstitutionId = institutionId,
                formInstitutionId = institutionId,
                selectedClass = null,
                subjects = emptyList(),
                composingClass = false,
                composingSubject = false,
                editingClassId = null,
                editingSubjectId = null,
                errorMessage = null,
            )
        }
        refresh()
    }

    fun selectClass(item: ClassOut) {
        _uiState.update {
            it.copy(
                selectedClass = item,
                composingClass = false,
                composingSubject = false,
                bindingSubjectId = null,
                errorMessage = null,
            )
        }
        loadSubjects(item.id)
    }

    fun backToClasses() {
        _uiState.update {
            it.copy(
                selectedClass = null,
                subjects = emptyList(),
                composingSubject = false,
                bindingSubjectId = null,
                errorMessage = null,
            )
        }
    }

    fun startCreateClass() {
        _uiState.update {
            it.copy(
                composingClass = true,
                composingSubject = false,
                editingClassId = null,
                name = "",
                sortOrder = nextSortOrder(_uiState.value.classes.maxOfOrNull { it.sort_order }),
                isActive = true,
                formInstitutionId = _uiState.value.selectedInstitutionId
                    ?: _uiState.value.institutions.firstOrNull()?.id,
                errorMessage = null,
            )
        }
    }

    fun openCreateClassForInstitution(institutionId: String?) {
        val targetId = institutionId?.takeIf { it.isNotBlank() }
        if (targetId != null && targetId != _uiState.value.selectedInstitutionId) {
            _uiState.update {
                it.copy(
                    selectedInstitutionId = targetId,
                    formInstitutionId = targetId,
                    selectedClass = null,
                    subjects = emptyList(),
                )
            }
            refresh()
        }
        startCreateClass()
    }

    fun openCreateSubjectForClass(classId: String?, institutionId: String?) {
        pendingCreateSubjectClassId = classId?.takeIf { it.isNotBlank() }
        val targetInst = institutionId?.takeIf { it.isNotBlank() }
        if (targetInst != null && targetInst != _uiState.value.selectedInstitutionId) {
            _uiState.update {
                it.copy(
                    selectedInstitutionId = targetInst,
                    formInstitutionId = targetInst,
                    selectedClass = null,
                    subjects = emptyList(),
                )
            }
            refresh()
            return
        }
        if (_uiState.value.classes.isNotEmpty()) {
            applyPendingCreateSubject()
        } else if (!_uiState.value.isLoading) {
            refresh()
        }
    }

    private fun applyPendingCreateSubject() {
        val requestedId = pendingCreateSubjectClassId
        val item = _uiState.value.classes.firstOrNull { it.id == requestedId }
            ?: _uiState.value.classes.firstOrNull()
            ?: return
        pendingCreateSubjectClassId = null
        if (_uiState.value.selectedClass?.id != item.id) {
            _uiState.update { it.copy(selectedClass = item) }
            loadSubjects(item.id)
        }
        if (!_uiState.value.composingSubject) {
            startCreateSubject()
        }
    }

    fun startEditClass(item: ClassOut) {
        _uiState.update {
            it.copy(
                composingClass = true,
                composingSubject = false,
                editingClassId = item.id,
                name = item.name,
                sortOrder = item.sort_order.toString(),
                isActive = item.is_active,
                formInstitutionId = item.institution_id,
                errorMessage = null,
            )
        }
    }

    fun startCreateSubject() {
        _uiState.update {
            it.copy(
                composingSubject = true,
                composingClass = false,
                editingSubjectId = null,
                name = "",
                code = "",
                sortOrder = nextSortOrder(_uiState.value.subjects.maxOfOrNull { it.sort_order }),
                isActive = true,
                bindingSubjectId = null,
                formStemSubjectId = null,
                selectedBoardId = null,
                selectedGradeId = null,
                stemGrades = emptyList(),
                stemSubjects = emptyList(),
                errorMessage = null,
            )
        }
        ensureStemBoards()
    }

    fun startEditSubject(item: SubjectOut) {
        _uiState.update {
            it.copy(
                composingSubject = true,
                composingClass = false,
                editingSubjectId = item.id,
                name = item.name,
                code = item.code.orEmpty(),
                sortOrder = item.sort_order.toString(),
                isActive = item.is_active,
                bindingSubjectId = null,
                formStemSubjectId = item.stem_subject_id,
                selectedBoardId = null,
                selectedGradeId = null,
                stemGrades = emptyList(),
                stemSubjects = emptyList(),
                errorMessage = null,
            )
        }
        ensureStemBoards()
    }

    fun cancelCompose() {
        _uiState.update {
            it.copy(
                composingClass = false,
                composingSubject = false,
                editingClassId = null,
                editingSubjectId = null,
                bindingSubjectId = null,
                errorMessage = null,
            )
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

    fun onFormInstitution(institutionId: String) =
        _uiState.update { it.copy(formInstitutionId = institutionId, errorMessage = null) }

    fun onCodeChange(value: String) = _uiState.update { it.copy(code = value.take(20)) }

    fun onSortOrderChange(value: String) =
        _uiState.update { it.copy(sortOrder = value.filter { ch -> ch.isDigit() }.take(4)) }

    fun toggleFormActive() = _uiState.update { it.copy(isActive = !it.isActive) }

    fun saveClass() {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isBlank()) return
        val institutionId = if (state.editingClassId == null) {
            state.formInstitutionId
        } else {
            state.formInstitutionId
                ?: state.selectedClass?.institution_id
                ?: state.selectedInstitutionId
        }
        if (institutionId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Select an institution first.") }
            return
        }
        val sortOrder = state.sortOrder.toIntOrNull() ?: 0
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = if (state.editingClassId == null) {
                adminRepository.createClass(
                    ClassCreate(
                        institution_id = institutionId,
                        name = name,
                        sort_order = sortOrder,
                        is_active = true,
                    ),
                )
            } else {
                adminRepository.updateClass(
                    state.editingClassId,
                    institutionId,
                    ClassUpdate(name = name, sort_order = sortOrder, is_active = state.isActive),
                )
            }
            when (result) {
                is NetworkResult.Success -> {
                    val created = state.editingClassId == null
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            composingClass = false,
                            editingClassId = null,
                            selectedClass = if (created) result.data else it.selectedClass,
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

    fun deleteClass(item: ClassOut) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            when (val result = adminRepository.deleteClass(item.id)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(selectedClass = null, subjects = emptyList()) }
                    refresh()
                }
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
            }
        }
    }

    fun saveSubject() {
        val state = _uiState.value
        val classId = state.selectedClass?.id ?: return
        val name = state.name.trim()
        if (name.isBlank()) return
        val institutionId = state.selectedClass?.institution_id
            ?: state.selectedInstitutionId
        if (institutionId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Select an institution first.") }
            return
        }
        val sortOrder = state.sortOrder.toIntOrNull() ?: 0
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = if (state.editingSubjectId == null) {
                adminRepository.createSubject(
                    SubjectCreate(
                        institution_id = institutionId,
                        class_id = classId,
                        name = name,
                        code = state.code.trim().ifBlank { null },
                        sort_order = sortOrder,
                        stem_subject_id = state.formStemSubjectId,
                    ),
                )
            } else {
                adminRepository.updateSubject(
                    state.editingSubjectId,
                    institutionId,
                    SubjectUpdate(
                        name = name,
                        code = state.code.trim().ifBlank { null },
                        sort_order = sortOrder,
                        is_active = state.isActive,
                    ),
                )
            }
            when (result) {
                is NetworkResult.Success -> {
                    val createdId = result.data.id
                    val stemId = state.formStemSubjectId
                    if (state.editingSubjectId != null &&
                        stemId != result.data.stem_subject_id
                    ) {
                        adminRepository.bindStem(createdId, stemId)
                    }
                    _uiState.update {
                        it.copy(isSaving = false, composingSubject = false, editingSubjectId = null)
                    }
                    loadSubjects(classId)
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun deleteSubject(item: SubjectOut) {
        val classId = _uiState.value.selectedClass?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            when (val result = adminRepository.deleteSubject(item.id)) {
                is NetworkResult.Success -> loadSubjects(classId)
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
            }
        }
    }

    fun selectBoard(id: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedBoardId = id,
                    selectedGradeId = null,
                    stemGrades = emptyList(),
                    stemSubjects = emptyList(),
                    errorMessage = null,
                )
            }
            when (val result = adminRepository.stemGrades(id)) {
                is NetworkResult.Success -> {
                    val grades = result.data
                    _uiState.update { it.copy(stemGrades = grades) }
                    // Pre-select the first grade (which then pre-selects the first AI subject).
                    grades.firstOrNull()?.id?.let { selectGrade(it) }
                }
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
            }
        }
    }

    fun selectGrade(id: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(selectedGradeId = id, stemSubjects = emptyList(), errorMessage = null)
            }
            when (val result = adminRepository.stemSubjects(id)) {
                is NetworkResult.Success -> {
                    val subjects = result.data
                    _uiState.update { state ->
                        val retained = state.formStemSubjectId
                            ?.takeIf { sid -> subjects.any { it.id == sid } }
                        state.copy(
                            stemSubjects = subjects,
                            // Pre-select the first AI subject when none is selected for this grade.
                            formStemSubjectId = retained ?: subjects.firstOrNull()?.id,
                        )
                    }
                }
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
            }
        }
    }

    fun bindStem(stemSubjectId: String) {
        val label = currentStemLabel(stemSubjectId)
        if (_uiState.value.composingSubject) {
            _uiState.update {
                it.copy(
                    formStemSubjectId = stemSubjectId,
                    stemLabels = if (label == null) it.stemLabels
                    else it.stemLabels + (stemSubjectId to label),
                )
            }
            return
        }
        setStemBinding(stemSubjectId)
    }

    fun unbindStem() {
        if (_uiState.value.composingSubject && _uiState.value.editingSubjectId == null) {
            _uiState.update { it.copy(formStemSubjectId = null) }
            return
        }
        setStemBinding(null)
    }

    private fun setStemBinding(stemSubjectId: String?) {
        val state = _uiState.value
        val subjectId = state.bindingSubjectId ?: state.editingSubjectId ?: return
        val classId = state.selectedClass?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (val result = adminRepository.bindStem(subjectId, stemSubjectId)) {
                is NetworkResult.Success -> {
                    val label = stemSubjectId?.let { currentStemLabel(it) }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            bindingSubjectId = null,
                            formStemSubjectId = stemSubjectId,
                            subjects = it.subjects.map { item ->
                                if (item.id == subjectId) result.data else item
                            },
                            stemLabels = if (stemSubjectId != null && label != null) {
                                it.stemLabels + (stemSubjectId to label)
                            } else it.stemLabels,
                        )
                    }
                    loadSubjects(classId)
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    private fun loadSubjects(classId: String) {
        viewModelScope.launch {
            val institutionId = _uiState.value.selectedClass?.institution_id
                ?: _uiState.value.selectedInstitutionId
            if (institutionId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isLoadingSubjects = false,
                        subjects = emptyList(),
                        errorMessage = "Select an institution first.",
                    )
                }
                return@launch
            }
            val cached = adminRepository.cachedSubjects(
                classId = classId,
                includeInactive = true,
                institutionId = institutionId,
            )?.sortedWith(compareBy({ it.sort_order }, { it.name }))
            if (!cached.isNullOrEmpty()) {
                _uiState.update { it.copy(subjects = cached, isLoadingSubjects = false) }
            } else {
                _uiState.update { it.copy(isLoadingSubjects = true) }
            }
            when (val result = adminRepository.listSubjects(
                classId = classId,
                institutionId = institutionId,
                includeInactive = true,
                forceRefresh = !cached.isNullOrEmpty(),
            )) {
                is NetworkResult.Success -> {
                    val subjects = result.data.sortedWith(compareBy({ it.sort_order }, { it.name }))
                    _uiState.update {
                        it.copy(isLoadingSubjects = false, subjects = subjects)
                    }
                    val labels = resolveStemLabels(subjects.mapNotNull { it.stem_subject_id }.toSet())
                    _uiState.update { it.copy(stemLabels = labels) }
                }
                else -> _uiState.update {
                    it.copy(isLoadingSubjects = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    private fun ensureStemBoards() {
        viewModelScope.launch {
            if (_uiState.value.stemBoards.isEmpty()) {
                when (val result = adminRepository.stemBoards()) {
                    is NetworkResult.Success -> _uiState.update { it.copy(stemBoards = result.data) }
                    else -> {
                        _uiState.update { it.copy(errorMessage = result.userMessage()) }
                        return@launch
                    }
                }
            }
            val state = _uiState.value
            // Add/Edit subject: pre-select first board → grade → AI subject cascade.
            if (state.composingSubject &&
                state.selectedBoardId == null &&
                state.stemBoards.isNotEmpty()
            ) {
                selectBoard(state.stemBoards.first().id)
            }
        }
    }

    private fun currentStemLabel(stemSubjectId: String): String? {
        val state = _uiState.value
        val subject = state.stemSubjects.firstOrNull { it.id == stemSubjectId } ?: return null
        val board = state.stemBoards.firstOrNull { it.id == state.selectedBoardId }?.name
        val grade = state.stemGrades.firstOrNull { it.id == state.selectedGradeId }?.name
        return listOfNotNull(board, grade, subject.name).joinToString(" · ")
    }

    private suspend fun resolveStemLabels(ids: Set<String>): Map<String, String> {
        val labels = _uiState.value.stemLabels.toMutableMap()
        if (ids.isEmpty() || ids.all { it in labels }) return labels
        val boards = when (val result = adminRepository.stemBoards()) {
            is NetworkResult.Success -> result.data
            else -> return labels
        }
        _uiState.update { it.copy(stemBoards = boards) }
        coroutineScope {
            boards.map { board ->
                async {
                    val grades = (adminRepository.stemGrades(board.id) as? NetworkResult.Success)
                        ?.data
                        .orEmpty()
                    grades.map { grade ->
                        async {
                            val stems = (adminRepository.stemSubjects(grade.id) as? NetworkResult.Success)
                                ?.data
                                .orEmpty()
                            stems.map { stem ->
                                stem.id to listOf(board.name, grade.name, stem.name).joinToString(" · ")
                            }
                        }
                    }.awaitAll().flatten()
                }
            }.awaitAll().flatten().forEach { (id, label) -> labels[id] = label }
        }
        return labels
    }

    private fun nextSortOrder(currentMax: Int?): String =
        ((currentMax ?: -1) + 1).coerceAtLeast(0).toString()

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminClassesViewModel(adminRepository) }
    }
}
