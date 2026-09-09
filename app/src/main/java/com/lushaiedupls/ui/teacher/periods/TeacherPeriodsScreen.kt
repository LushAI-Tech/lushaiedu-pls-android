package com.lushaiedupls.ui.teacher.periods

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.PeriodCreate
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.PeriodUpdate
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.admin.formatTime12h
import com.lushaiedupls.ui.admin.normalizeTime
import com.lushaiedupls.ui.admin.periods.AdminPeriodsScreen
import com.lushaiedupls.ui.admin.periods.AdminPeriodsUiState
import com.lushaiedupls.ui.admin.sortOrderFromStartTime
import com.lushaiedupls.ui.admin.sortedPeriods
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.teacher.secondary.TeacherTimetableRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherPeriodsViewModel(
    private val teacherRepository: TeacherRepository,
    private val initialInstitutionId: String? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminPeriodsUiState(isLoading = true))
    val uiState: StateFlow<AdminPeriodsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val institutions = when (val result = teacherRepository.institutions()) {
                is NetworkResult.Success -> result.data
                    .filter { it.is_active }
                    .sortedWith(compareBy({ it.sort_order }, { it.name }))
                else -> _uiState.value.institutions
            }
            val selectedInstitutionId = _uiState.value.selectedInstitutionId
                ?.takeIf { id -> institutions.any { it.id == id } }
                ?: initialInstitutionId?.takeIf { id -> institutions.any { it.id == id } }
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
            if (selectedInstitutionId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = emptyList(),
                        errorMessage = if (institutions.isEmpty()) {
                            "No institutions are available yet."
                        } else {
                            "Please select an institution."
                        },
                    )
                }
                return@launch
            }
            when (val result = teacherRepository.periods(
                institutionId = selectedInstitutionId,
                includeInactive = false,
            )) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, items = result.data.sortedPeriods())
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
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
                items = emptyList(),
                composing = false,
                editingId = null,
                errorMessage = null,
            )
        }
        refresh()
    }

    fun startCreate() = _uiState.update {
        it.copy(
            composing = true,
            editingId = null,
            name = "",
            start = "",
            end = "",
            formInstitutionId = it.selectedInstitutionId ?: it.institutions.firstOrNull()?.id,
        )
    }

    fun startEdit(item: PeriodOut) = _uiState.update {
        it.copy(
            composing = true,
            editingId = item.id,
            name = item.name,
            start = formatTime12h(item.start_time),
            end = formatTime12h(item.end_time),
        )
    }

    fun cancel() = _uiState.update { it.copy(composing = false) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun onName(value: String) = _uiState.update { it.copy(name = value) }
    fun onStart(value: String) = _uiState.update { it.copy(start = value) }
    fun onEnd(value: String) = _uiState.update { it.copy(end = value) }
    fun onFormInstitution(institutionId: String) =
        _uiState.update { it.copy(formInstitutionId = institutionId, errorMessage = null) }

    fun save() {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isBlank() || state.start.isBlank() || state.end.isBlank()) return
        if (state.editingId == null && state.formInstitutionId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Select an institution first.") }
            return
        }
        val startTime = normalizeTime(state.start)
        val endTime = normalizeTime(state.end)
        val sortOrder = sortOrderFromStartTime(startTime)
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = if (state.editingId == null) {
                teacherRepository.createPeriod(
                    PeriodCreate(
                        institution_id = state.formInstitutionId.orEmpty(),
                        name = name,
                        start_time = startTime,
                        end_time = endTime,
                        sort_order = sortOrder,
                    ),
                )
            } else {
                teacherRepository.updatePeriod(
                    state.editingId,
                    PeriodUpdate(
                        name = name,
                        start_time = startTime,
                        end_time = endTime,
                        sort_order = sortOrder,
                    ),
                )
            }
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, composing = false) }
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
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (val result = teacherRepository.deletePeriod(id)) {
                is NetworkResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            items = state.items.filterNot { it.id == id },
                            composing = if (state.editingId == id) false else state.composing,
                            editingId = if (state.editingId == id) null else state.editingId,
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

    companion object {
        fun provideFactory(
            teacherRepository: TeacherRepository,
            initialInstitutionId: String? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            TeacherPeriodsViewModel(teacherRepository, initialInstitutionId)
        }
    }
}

@Composable
fun TeacherPeriodsRoute(
    teacherRepository: TeacherRepository,
    onBack: () -> Unit,
    initialInstitutionId: String? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: TeacherPeriodsViewModel = viewModel(
        factory = TeacherPeriodsViewModel.provideFactory(teacherRepository, initialInstitutionId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimetable by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(showTimetable) {
        if (!showTimetable && !uiState.composing) viewModel.refresh()
        onPauseOrDispose { }
    }
    BackHandler(enabled = showTimetable || uiState.composing) {
        when {
            showTimetable -> showTimetable = false
            else -> viewModel.cancel()
        }
    }
    if (showTimetable) {
        TeacherTimetableRoute(
            teacherRepository = teacherRepository,
            editable = true,
            initialInstitutionId = uiState.selectedInstitutionId ?: initialInstitutionId,
            onBack = { showTimetable = false },
            modifier = modifier,
        )
    } else {
        AdminPeriodsScreen(
            uiState = uiState,
            onBack = { if (uiState.composing) viewModel.cancel() else onBack() },
            onStartCreate = viewModel::startCreate,
            onStartEdit = viewModel::startEdit,
            onSelectInstitution = viewModel::selectInstitution,
            onFormInstitution = viewModel::onFormInstitution,
            onName = viewModel::onName,
            onStart = viewModel::onStart,
            onEnd = viewModel::onEnd,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onDismissError = viewModel::clearError,
            onSetTimetable = { showTimetable = true },
            onRetry = viewModel::refresh,
            modifier = modifier,
        )
    }
}
