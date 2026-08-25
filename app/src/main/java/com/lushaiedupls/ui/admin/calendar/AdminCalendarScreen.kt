package com.lushaiedupls.ui.admin.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
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
import com.lushaiedupls.data.remote.dto.CalendarEventCreate
import com.lushaiedupls.data.remote.dto.CalendarEventOut
import com.lushaiedupls.data.remote.dto.CalendarEventType
import com.lushaiedupls.data.remote.dto.CalendarEventUpdate
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminCard
import com.lushaiedupls.ui.admin.AdminDeleteRed
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.admin.AdminLeadingIcon
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.admin.formatIsoDate
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminCalendarUiState(
    val items: List<CalendarEventOut> = emptyList(),
    val composing: Boolean = false,
    val editingId: String? = null,
    val title: String = "",
    val start: String = LocalDate.now().toString(),
    val end: String = LocalDate.now().toString(),
    val type: CalendarEventType = CalendarEventType.EVENT,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class AdminCalendarViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminCalendarUiState(isLoading = true))
    val uiState: StateFlow<AdminCalendarUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val from = LocalDate.now().minusMonths(1).toString()
            val to = LocalDate.now().plusMonths(6).toString()
            when (val result = adminRepository.calendarEvents(from, to)) {
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
        it.copy(
            composing = true,
            editingId = null,
            title = "",
            start = LocalDate.now().toString(),
            end = LocalDate.now().toString(),
            type = CalendarEventType.EVENT,
        )
    }

    fun startEdit(item: CalendarEventOut) = _uiState.update {
        it.copy(
            composing = true,
            editingId = item.id,
            title = item.title,
            start = formatIsoDate(item.start_date).ifBlank { item.start_date.take(10) },
            end = formatIsoDate(item.end_date).ifBlank { item.end_date.take(10) },
            type = item.event_type,
        )
    }

    fun cancel() = _uiState.update { it.copy(composing = false) }
    fun onTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun onStart(value: String) = _uiState.update { it.copy(start = value) }
    fun onEnd(value: String) = _uiState.update { it.copy(end = value) }
    fun onType(type: CalendarEventType) = _uiState.update { it.copy(type = type) }

    fun save() {
        val state = _uiState.value
        val title = state.title.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (
                val result = if (state.editingId == null) {
                    adminRepository.createCalendarEvent(
                        CalendarEventCreate(
                            title = title,
                            event_type = state.type,
                            start_date = state.start.trim(),
                            end_date = state.end.trim(),
                        ),
                    )
                } else {
                    adminRepository.updateCalendarEvent(
                        state.editingId,
                        CalendarEventUpdate(
                            title = title,
                            event_type = state.type,
                            start_date = state.start.trim(),
                            end_date = state.end.trim(),
                        ),
                    )
                }
            ) {
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
            when (val result = adminRepository.deleteCalendarEvent(id)) {
                is NetworkResult.Success -> refresh()
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
            }
        }
    }

    companion object {
        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminCalendarViewModel(adminRepository) }
    }
}

@Composable
fun AdminCalendarRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminCalendarViewModel = viewModel(
        factory = AdminCalendarViewModel.provideFactory(adminRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        if (!uiState.composing) viewModel.refresh()
        onPauseOrDispose { }
    }
    BackHandler(enabled = uiState.composing) { viewModel.cancel() }
    AdminCalendarScreen(
        uiState = uiState,
        onBack = { if (uiState.composing) viewModel.cancel() else onBack() },
        onStartCreate = viewModel::startCreate,
        onStartEdit = viewModel::startEdit,
        onTitle = viewModel::onTitle,
        onStart = viewModel::onStart,
        onEnd = viewModel::onEnd,
        onType = viewModel::onType,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun AdminCalendarScreen(
    uiState: AdminCalendarUiState,
    onBack: () -> Unit,
    onStartCreate: () -> Unit,
    onStartEdit: (CalendarEventOut) -> Unit,
    onTitle: (String) -> Unit,
    onStart: (String) -> Unit,
    onEnd: (String) -> Unit,
    onType: (CalendarEventType) -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.items.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.Calendar, modifier = modifier)
        uiState.errorMessage != null && uiState.items.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.admin_calendar_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRetry,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> {
            var managing by rememberSaveable { mutableStateOf(false) }
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(BgWhite)
                    .imePadding(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = if (uiState.composing) 24.dp else 88.dp),
                ) {
                    AdminScreenHeader(
                        title = stringResource(R.string.admin_calendar_title),
                        onBack = onBack,
                        actions = if (!uiState.composing) {
                            {
                                IconButton(
                                    onClick = { managing = !managing },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = stringResource(R.string.admin_manage_calendar),
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
                        val types = CalendarEventType.entries
                        AdminFilterRow(
                            labels = types.map { it.typeLabel() },
                            selectedIndex = types.indexOf(uiState.type),
                            onSelect = { onType(types[it]) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedAuthField(
                            label = stringResource(R.string.admin_calendar_title_field),
                            value = uiState.title,
                            onValueChange = onTitle,
                            placeholder = stringResource(R.string.admin_calendar_title_hint),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedAuthField(
                            label = stringResource(R.string.admin_calendar_start),
                            value = uiState.start,
                            onValueChange = onStart,
                            placeholder = stringResource(R.string.admin_calendar_date_hint),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedAuthField(
                            label = stringResource(R.string.admin_calendar_end),
                            value = uiState.end,
                            onValueChange = onEnd,
                            placeholder = stringResource(R.string.admin_calendar_date_hint),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PrimaryButton(
                            text = stringResource(R.string.admin_calendar_save),
                            onClick = onSave,
                            enabled = uiState.title.isNotBlank() && !uiState.isSaving,
                            fullyRounded = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        if (uiState.items.isEmpty()) {
                            AdminEmptyText(stringResource(R.string.admin_calendar_empty))
                        }
                        uiState.items.forEach { item ->
                            AdminCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AdminLeadingIcon(icon = item.event_type.leadingIcon())
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = BrandBlack,
                                            fontFamily = FontFamily.SansSerif,
                                        )
                                        AdminMuted(
                                            "${item.event_type.typeLabel()} · ${formatIsoDate(item.start_date)} – ${formatIsoDate(item.end_date)}",
                                        )
                                    }
                                    if (managing) {
                                        EventEditDeleteIcons(
                                            onEdit = { onStartEdit(item) },
                                            onDelete = { onDelete(item.id) },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                if (!uiState.composing) {
                    FloatingActionButton(
                        onClick = onStartCreate,
                        shape = CircleShape,
                        containerColor = BrandBlack,
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.admin_calendar_add),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventEditDeleteIcons(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    IconButton(
        onClick = onEdit,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(R.string.admin_edit_event),
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
            contentDescription = stringResource(R.string.admin_calendar_delete),
            tint = AdminDeleteRed,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CalendarEventType.typeLabel(): String = when (this) {
    CalendarEventType.HOLIDAY -> stringResource(R.string.admin_calendar_holiday)
    CalendarEventType.EXAM -> stringResource(R.string.admin_calendar_exam)
    CalendarEventType.EVENT -> stringResource(R.string.admin_calendar_event)
}

private fun CalendarEventType.leadingIcon() = when (this) {
    CalendarEventType.HOLIDAY -> Icons.Outlined.Celebration
    CalendarEventType.EXAM -> Icons.Outlined.Quiz
    CalendarEventType.EVENT -> Icons.Outlined.Event
}
