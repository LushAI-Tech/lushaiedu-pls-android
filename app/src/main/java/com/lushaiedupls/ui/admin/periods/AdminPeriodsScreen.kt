package com.lushaiedupls.ui.admin.periods

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.PeriodCreate
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.PeriodUpdate
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminCard
import com.lushaiedupls.ui.admin.AdminDeleteRed
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminLeadingIcon
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.admin.normalizeTime
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminPeriodsUiState(
    val items: List<PeriodOut> = emptyList(),
    val composing: Boolean = false,
    val editingId: String? = null,
    val name: String = "",
    val start: String = "",
    val end: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class AdminPeriodsViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminPeriodsUiState(isLoading = true))
    val uiState: StateFlow<AdminPeriodsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = adminRepository.periods(includeInactive = true)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, items = result.data)
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun startCreate() = _uiState.update {
        it.copy(composing = true, editingId = null, name = "", start = "", end = "")
    }

    fun startEdit(item: PeriodOut) = _uiState.update {
        it.copy(
            composing = true,
            editingId = item.id,
            name = item.name,
            start = item.start_time.take(5),
            end = item.end_time.take(5),
        )
    }

    fun cancel() = _uiState.update { it.copy(composing = false) }
    fun onName(value: String) = _uiState.update { it.copy(name = value) }
    fun onStart(value: String) = _uiState.update { it.copy(start = value) }
    fun onEnd(value: String) = _uiState.update { it.copy(end = value) }

    fun save() {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isBlank() || state.start.isBlank() || state.end.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = if (state.editingId == null) {
                adminRepository.createPeriod(
                    PeriodCreate(
                        name = name,
                        start_time = normalizeTime(state.start),
                        end_time = normalizeTime(state.end),
                    ),
                )
            } else {
                adminRepository.updatePeriod(
                    state.editingId,
                    PeriodUpdate(
                        name = name,
                        start_time = normalizeTime(state.start),
                        end_time = normalizeTime(state.end),
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
            when (val result = adminRepository.deletePeriod(id)) {
                is NetworkResult.Success -> refresh()
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
            }
        }
    }

    companion object {
        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminPeriodsViewModel(adminRepository) }
    }
}

@Composable
fun AdminPeriodsRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminPeriodsViewModel = viewModel(
        factory = AdminPeriodsViewModel.provideFactory(adminRepository),
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
        AdminTimetableRoute(
            adminRepository = adminRepository,
            onBack = { showTimetable = false },
            modifier = modifier,
        )
    } else {
        AdminPeriodsScreen(
            uiState = uiState,
            onBack = { if (uiState.composing) viewModel.cancel() else onBack() },
            onStartCreate = viewModel::startCreate,
            onStartEdit = viewModel::startEdit,
            onName = viewModel::onName,
            onStart = viewModel::onStart,
            onEnd = viewModel::onEnd,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onSetTimetable = { showTimetable = true },
            onRetry = viewModel::refresh,
            modifier = modifier,
        )
    }
}

@Composable
fun AdminPeriodsScreen(
    uiState: AdminPeriodsUiState,
    onBack: () -> Unit,
    onStartCreate: () -> Unit,
    onStartEdit: (PeriodOut) -> Unit,
    onName: (String) -> Unit,
    onStart: (String) -> Unit,
    onEnd: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit,
    onSetTimetable: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.items.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.items.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.admin_periods_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRetry,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> {
            var managing by rememberSaveable { mutableStateOf(false) }
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
                    title = stringResource(R.string.admin_periods_title),
                    onBack = onBack,
                    actions = if (!uiState.composing) {
                        {
                            IconButton(
                                onClick = { managing = !managing },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.admin_manage_periods),
                                    tint = if (managing) BrandOrange else BrandBlack,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    } else {
                        null
                    },
                )
                if (uiState.composing) {
                    OutlinedAuthField(
                        label = stringResource(R.string.admin_periods_name),
                        value = uiState.name,
                        onValueChange = onName,
                        placeholder = stringResource(R.string.admin_periods_name_hint),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedAuthField(
                        label = stringResource(R.string.admin_periods_start),
                        value = uiState.start,
                        onValueChange = onStart,
                        placeholder = stringResource(R.string.admin_periods_time_hint),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedAuthField(
                        label = stringResource(R.string.admin_periods_end),
                        value = uiState.end,
                        onValueChange = onEnd,
                        placeholder = stringResource(R.string.admin_periods_time_hint),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = stringResource(R.string.admin_periods_save),
                        onClick = onSave,
                        enabled = uiState.name.isNotBlank() && uiState.start.isNotBlank() &&
                            uiState.end.isNotBlank() && !uiState.isSaving,
                        fullyRounded = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    AdminMuted(stringResource(R.string.admin_periods_hint))
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.items.isEmpty()) {
                        AdminEmptyText(stringResource(R.string.admin_periods_empty))
                    }
                    uiState.items.forEach { item ->
                        AdminCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AdminLeadingIcon(icon = Icons.Outlined.Schedule)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = BrandBlack,
                                        fontFamily = FontFamily.SansSerif,
                                    )
                                    AdminMuted("${item.start_time.take(5)} – ${item.end_time.take(5)}")
                                }
                                if (managing) {
                                    PeriodEditDeleteIcons(
                                        onEdit = { onStartEdit(item) },
                                        onDelete = { onDelete(item.id) },
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    PrimaryButton(
                        text = stringResource(
                            if (managing) R.string.admin_set_periods else R.string.admin_set_timetable,
                        ),
                        onClick = if (managing) onStartCreate else onSetTimetable,
                        enabled = managing || uiState.items.any { it.is_active },
                        trailingArrow = true,
                        fullyRounded = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodEditDeleteIcons(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    IconButton(
        onClick = onEdit,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(R.string.admin_edit_period),
            tint = BrandBlack,
            modifier = Modifier.size(20.dp),
        )
    }
    IconButton(
        onClick = onDelete,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = stringResource(R.string.admin_periods_delete),
            tint = AdminDeleteRed,
            modifier = Modifier.size(22.dp),
        )
    }
}
