package com.lushaiedupls.ui.admin.periods

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.PeriodCreate
import com.lushaiedupls.data.remote.dto.PeriodOut
import com.lushaiedupls.data.remote.dto.PeriodUpdate
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminCard
import com.lushaiedupls.ui.admin.AdminDeleteConfirmDialog
import com.lushaiedupls.ui.admin.AdminDeleteRed
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.admin.AdminLeadingIcon
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminNoticeDialog
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.admin.formatHourMinute12h
import com.lushaiedupls.ui.admin.formatPeriodRange12h
import com.lushaiedupls.ui.admin.formatTime12h
import com.lushaiedupls.ui.admin.normalizeTime
import com.lushaiedupls.ui.admin.parseHourMinute
import com.lushaiedupls.ui.admin.sortOrderFromStartTime
import com.lushaiedupls.ui.admin.sortedPeriods
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.FilterRowListLoading
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminPeriodsUiState(
    val items: List<PeriodOut> = emptyList(),
    val institutions: List<InstitutionOut> = emptyList(),
    val selectedInstitutionId: String? = null,
    val formInstitutionId: String? = null,
    val composing: Boolean = false,
    val editingId: String? = null,
    val name: String = "",
    val start: String = "",
    val end: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
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
            val hasContent = _uiState.value.items.isNotEmpty() ||
                _uiState.value.institutions.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    errorMessage = null,
                )
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
            if (selectedInstitutionId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
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
            when (val result = adminRepository.periods(
                includeInactive = false,
                institutionId = selectedInstitutionId,
            )) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        items = result.data.sortedPeriods(),
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
                adminRepository.createPeriod(
                    PeriodCreate(
                        institution_id = state.formInstitutionId.orEmpty(),
                        name = name,
                        start_time = startTime,
                        end_time = endTime,
                        sort_order = sortOrder,
                    ),
                )
            } else {
                adminRepository.updatePeriod(
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
            when (val result = adminRepository.deletePeriod(id)) {
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
            initialInstitutionId = uiState.selectedInstitutionId,
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

@Composable
fun AdminPeriodsScreen(
    uiState: AdminPeriodsUiState,
    onBack: () -> Unit,
    onStartCreate: () -> Unit,
    onStartEdit: (PeriodOut) -> Unit,
    onSelectInstitution: (String) -> Unit,
    onFormInstitution: (String) -> Unit,
    onName: (String) -> Unit,
    onStart: (String) -> Unit,
    onEnd: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit,
    onDismissError: () -> Unit,
    onSetTimetable: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.items.isEmpty() && uiState.institutions.isEmpty() &&
            uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.items.isEmpty() && uiState.institutions.isEmpty() ->
            LoadErrorPanel(
            screenTitle = stringResource(R.string.admin_periods_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRetry,
            isRetrying = uiState.isLoading || uiState.isRefreshing,
            modifier = modifier,
        )
        else -> {
            var managing by rememberSaveable { mutableStateOf(false) }
            var pendingDelete by remember { mutableStateOf<PeriodOut?>(null) }
            LushPullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRetry,
                modifier = modifier.fillMaxSize(),
            ) {
            Column(
                modifier = Modifier
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
                    if (uiState.editingId == null && uiState.institutions.isNotEmpty()) {
                        AdminFilterRow(
                            labels = uiState.institutions.map { it.name },
                            selectedIndex = uiState.institutions.indexOfFirst { it.id == uiState.formInstitutionId }
                                .coerceAtLeast(0),
                            onSelect = { index -> onFormInstitution(uiState.institutions[index].id) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    uiState.errorMessage?.let { message ->
                        Text(text = message, color = BrandOrange, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedAuthField(
                        label = stringResource(R.string.admin_periods_name),
                        value = uiState.name,
                        onValueChange = onName,
                        placeholder = stringResource(R.string.admin_periods_name_hint),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PeriodTimeTapField(
                        label = stringResource(R.string.admin_periods_start),
                        value = uiState.start,
                        placeholder = stringResource(R.string.admin_periods_time_hint),
                        onValueChange = onStart,
                        defaultHour24 = 9,
                        defaultMinute = 0,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PeriodTimeTapField(
                        label = stringResource(R.string.admin_periods_end),
                        value = uiState.end,
                        placeholder = stringResource(R.string.admin_periods_time_hint),
                        onValueChange = onEnd,
                        defaultHour24 = 10,
                        defaultMinute = 0,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = stringResource(R.string.admin_periods_save),
                        onClick = onSave,
                        enabled = uiState.name.isNotBlank() && uiState.start.isNotBlank() &&
                            uiState.end.isNotBlank() && !uiState.isSaving &&
                            (uiState.editingId != null || !uiState.formInstitutionId.isNullOrBlank()),
                        fullyRounded = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    if (uiState.institutions.isNotEmpty()) {
                        AdminFilterRow(
                            labels = uiState.institutions.map { it.name },
                            selectedIndex = uiState.institutions
                                .indexOfFirst { it.id == uiState.selectedInstitutionId }
                                .coerceAtLeast(0),
                            onSelect = { index ->
                                onSelectInstitution(uiState.institutions[index].id)
                            },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    AdminMuted(stringResource(R.string.admin_periods_hint))
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.items.isEmpty()) {
                        if (uiState.isLoading) {
                            FilterRowListLoading()
                        } else {
                            AdminEmptyText(
                                text = stringResource(R.string.admin_periods_empty),
                                icon = Icons.Outlined.Schedule,
                            )
                        }
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
                                    AdminMuted(formatPeriodRange12h(item.start_time, item.end_time))
                                    val institutionName = item.institution_name
                                        ?: uiState.institutions.firstOrNull { it.id == item.institution_id }?.name
                                    if (!institutionName.isNullOrBlank() && uiState.selectedInstitutionId == null) {
                                        AdminMuted(institutionName)
                                    }
                                }
                                if (managing) {
                                    PeriodEditDeleteIcons(
                                        onEdit = { onStartEdit(item) },
                                        onDelete = { pendingDelete = item },
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
            if (!uiState.composing) {
                uiState.errorMessage?.let { message ->
                    AdminNoticeDialog(
                        message = message,
                        onDismiss = onDismissError,
                    )
                }
            }
            pendingDelete?.let { item ->
                AdminDeleteConfirmDialog(
                    title = stringResource(R.string.admin_delete_confirm_title),
                    message = stringResource(
                        R.string.admin_delete_confirm_named,
                        item.name,
                    ) + "\n\n" + stringResource(R.string.admin_delete_confirm_period),
                    onConfirm = {
                        val id = item.id
                        pendingDelete = null
                        onDelete(id)
                    },
                    onDismiss = { pendingDelete = null },
                    isWorking = uiState.isSaving,
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

private val PeriodTimeFieldShape = RoundedCornerShape(12.dp)
private val IosPickerSheetShape = RoundedCornerShape(20.dp)
private val WheelItemHeight = 40.dp
private val WheelVisibleCount = 5
private val HourLabels = (1..12).map { it.toString() }
private val MinuteLabels = (0..59).map { String.format(java.util.Locale.ENGLISH, "%02d", it) }
private val MeridiemLabels = listOf("AM", "PM")

@Composable
private fun PeriodTimeTapField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    defaultHour24: Int,
    defaultMinute: Int,
    modifier: Modifier = Modifier,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val display = value.trim().ifBlank { null }?.let { formatTime12h(it) }.orEmpty()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(PeriodTimeFieldShape)
                .border(1.dp, BorderGray, PeriodTimeFieldShape)
                .background(BgWhite)
                .clickable { showPicker = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = display.ifBlank { placeholder },
                color = if (display.isBlank()) TextSecondary else BrandBlack,
                fontSize = 15.sp,
                fontWeight = if (display.isBlank()) FontWeight.Normal else FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (showPicker) {
        key(value, defaultHour24, defaultMinute) {
            IosAlarmTimePickerSheet(
                title = label,
                initialHour24 = parseHourMinute(value)?.first ?: defaultHour24.coerceIn(0, 23),
                initialMinute = parseHourMinute(value)?.second ?: defaultMinute.coerceIn(0, 59),
                onDismiss = { showPicker = false },
                onConfirm = { hour24, minute ->
                    onValueChange(formatHourMinute12h(hour24, minute))
                    showPicker = false
                },
            )
        }
    }
}

@Composable
private fun IosAlarmTimePickerSheet(
    title: String,
    initialHour24: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour24: Int, minute: Int) -> Unit,
) {
    val initialHour12 = when {
        initialHour24 == 0 -> 12
        initialHour24 > 12 -> initialHour24 - 12
        else -> initialHour24
    }
    var hour12 by remember(initialHour24, initialMinute) {
        mutableIntStateOf(initialHour12.coerceIn(1, 12))
    }
    var minute by remember(initialHour24, initialMinute) {
        mutableIntStateOf(initialMinute.coerceIn(0, 59))
    }
    var isPm by remember(initialHour24, initialMinute) {
        mutableStateOf(initialHour24 >= 12)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(IosPickerSheetShape)
                    .background(Color(0xFFF2F2F7))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.admin_cancel),
                            color = BrandOrange,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.SansSerif,
                        )
                    }
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = BrandBlack,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                    )
                    TextButton(
                        onClick = {
                            val hour24 = when {
                                !isPm && hour12 == 12 -> 0
                                isPm && hour12 != 12 -> hour12 + 12
                                else -> hour12
                            }
                            onConfirm(hour24, minute)
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.ok),
                            color = BrandOrange,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgWhite)
                        .padding(vertical = 8.dp)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to Color.White,
                                        0.22f to Color.White.copy(alpha = 0.88f),
                                        0.42f to Color.Transparent,
                                        0.58f to Color.Transparent,
                                        0.78f to Color.White.copy(alpha = 0.88f),
                                        1f to Color.White,
                                    ),
                                ),
                            )
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(WheelItemHeight)
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandBlack.copy(alpha = 0.06f)),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(WheelItemHeight * WheelVisibleCount),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        IosWheelColumn(
                            labels = HourLabels,
                            selectedIndex = (hour12 - 1).coerceIn(0, 11),
                            onSelectedIndexChange = { hour12 = it + 1 },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = ":",
                            color = BrandBlack,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                        )
                        IosWheelColumn(
                            labels = MinuteLabels,
                            selectedIndex = minute.coerceIn(0, 59),
                            onSelectedIndexChange = { minute = it },
                            modifier = Modifier.weight(1f),
                        )
                        IosWheelColumn(
                            labels = MeridiemLabels,
                            selectedIndex = if (isPm) 1 else 0,
                            onSelectedIndexChange = { isPm = it == 1 },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosWheelColumn(
    labels: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val edgeSlots = WheelVisibleCount / 2

    LaunchedEffect(labels.size) {
        val target = selectedIndex.coerceIn(0, labels.lastIndex)
        listState.scrollToItem(target)
    }

    LaunchedEffect(listState, labels.size) {
        snapshotFlow {
            val layout = listState.layoutInfo
            if (layout.visibleItemsInfo.isEmpty()) return@snapshotFlow selectedIndex
            val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
            layout.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2f) - viewportCenter) }
                ?.index
                ?: listState.firstVisibleItemIndex
        }
            .distinctUntilChanged()
            .map { it.coerceIn(0, labels.lastIndex) }
            .collect { index ->
                if (index != selectedIndex) onSelectedIndexChange(index)
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = WheelItemHeight * edgeSlots),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(WheelItemHeight * WheelVisibleCount),
    ) {
        items(labels.size) { index ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WheelItemHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labels[index],
                    color = if (selected) BrandBlack else TextSecondary.copy(alpha = 0.55f),
                    fontSize = if (selected) 24.sp else 18.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
