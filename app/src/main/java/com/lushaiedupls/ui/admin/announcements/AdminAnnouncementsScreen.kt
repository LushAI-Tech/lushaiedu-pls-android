package com.lushaiedupls.ui.admin.announcements

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.AppNotification
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.NotificationAudience
import com.lushaiedupls.data.remote.dto.NotificationCreate
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.admin.AdminDeleteRed
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.admin.AdminManageToggle
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.NotificationDetailScreen
import com.lushaiedupls.ui.common.NotificationEmptyState
import com.lushaiedupls.ui.common.NotificationListCard
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.reloadUiFlags
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary
import com.lushaiedupls.ui.theme.TileSelected
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val ComposeControlShape = RoundedCornerShape(12.dp)

data class ClassUnitGroup(
    val classId: String,
    val className: String,
    val sortOrder: Int,
    val units: List<TeachingUnitOut>,
)

data class InstitutionUnitSection(
    val institutionId: String,
    val institutionName: String,
    val classes: List<ClassUnitGroup>,
)

data class AdminAnnouncementsUiState(
    val items: List<AppNotification> = emptyList(),
    val institutions: List<InstitutionOut> = emptyList(),
    val composing: Boolean = false,
    val title: String = "",
    val body: String = "",
    val audience: NotificationAudience = NotificationAudience.STUDENTS,
    val selectedInstitutionIds: Set<String> = emptySet(),
    val selectedUnitIds: Set<String> = emptySet(),
    val teachingUnits: List<TeachingUnitOut> = emptyList(),
    val classSortById: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val isLoadingUnits: Boolean = false,
    val errorMessage: String? = null,
) {
    val unitAudience: Boolean
        get() = audience == NotificationAudience.TEACHING_UNIT

    val institutionUnits: List<TeachingUnitOut>
        get() = teachingUnits.filter { unit ->
            unit.institution_id != null && unit.institution_id in selectedInstitutionIds
        }

    val selectedUnits: List<TeachingUnitOut>
        get() {
            val allowed = institutionUnits.map { it.id }.toSet()
            return institutionUnits.filter { it.id in selectedUnitIds && it.id in allowed }
        }

    val institutionSections: List<InstitutionUnitSection>
        get() {
            return selectedInstitutionIds.mapNotNull { institutionId ->
                val name = institutions.find { it.id == institutionId }?.name ?: return@mapNotNull null
                val byClass = linkedMapOf<String, ClassUnitGroup>()
                for (unit in institutionUnits.filter { it.institution_id == institutionId }) {
                    val existing = byClass[unit.class_id]
                    if (existing == null) {
                        byClass[unit.class_id] = ClassUnitGroup(
                            classId = unit.class_id,
                            className = unit.class_name,
                            sortOrder = classSortById[unit.class_id] ?: Int.MAX_VALUE,
                            units = listOf(unit),
                        )
                    } else {
                        byClass[unit.class_id] = existing.copy(units = existing.units + unit)
                    }
                }
                val classes = byClass.values
                    .map { group ->
                        group.copy(
                            units = group.units.sortedBy { it.subject_name.lowercase() },
                        )
                    }
                    .sortedWith(compareBy({ it.sortOrder }, { it.className.lowercase() }))
                if (classes.isEmpty()) null
                else InstitutionUnitSection(institutionId, name, classes)
            }
        }

    val canSend: Boolean
        get() = title.isNotBlank() &&
            body.isNotBlank() &&
            selectedInstitutionIds.isNotEmpty() &&
            (!unitAudience || selectedUnits.isNotEmpty()) &&
            !isSaving
}

class AdminAnnouncementsViewModel(
    private val adminRepository: AdminRepository,
    private val userSessionStore: UserSessionStore? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminAnnouncementsUiState(isLoading = true))
    val uiState: StateFlow<AdminAnnouncementsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.items.isNotEmpty()
            val (loading, refreshing) = reloadUiFlags(_uiState.value.isLoading, hasContent)
            _uiState.update {
                it.copy(
                    isLoading = loading,
                    isRefreshing = refreshing,
                    errorMessage = null,
                )
            }
            val institutions = when (val result = adminRepository.listInstitutions(includeInactive = true)) {
                is NetworkResult.Success -> result.data
                    .filter { it.is_active }
                    .sortedWith(compareBy({ it.sort_order }, { it.name }))
                else -> _uiState.value.institutions
            }
            when (val result = adminRepository.notifications()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        items = StudentUiMappers.notifications(result.data),
                        institutions = institutions,
                        selectedInstitutionIds = it.selectedInstitutionIds
                            .filter { id -> institutions.any { inst -> inst.id == id } }
                            .toSet()
                            .ifEmpty {
                                if (it.composing) defaultInstitutionIds(institutions) else emptySet()
                            },
                    )
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        institutions = institutions,
                        errorMessage = result.userMessage(),
                    )
                }
            }
        }
    }

    fun startCreate() {
        val institutions = _uiState.value.institutions
        _uiState.update {
            it.copy(
                composing = true,
                title = "",
                body = "",
                audience = NotificationAudience.STUDENTS,
                selectedInstitutionIds = defaultInstitutionIds(institutions),
                selectedUnitIds = emptySet(),
                errorMessage = null,
            )
        }
        loadTeachingUnitData()
    }

    fun cancel() = _uiState.update {
        it.copy(
            composing = false,
            title = "",
            body = "",
            selectedUnitIds = emptySet(),
            errorMessage = null,
        )
    }

    fun onTitle(value: String) {
        if (value.length <= 80) _uiState.update { it.copy(title = value) }
    }

    fun onBody(value: String) {
        if (value.length <= 500) _uiState.update { it.copy(body = value) }
    }

    fun onAudience(value: NotificationAudience) {
        _uiState.update { it.copy(audience = value) }
        if (value == NotificationAudience.TEACHING_UNIT) {
            loadTeachingUnitData()
        }
    }

    fun toggleInstitution(id: String) {
        val current = _uiState.value.selectedInstitutionIds
        val next = if (id in current) current - id else current + id
        _uiState.update { state ->
            val keptUnits = state.selectedUnitIds.filter { unitId ->
                val unit = state.teachingUnits.find { it.id == unitId }
                unit?.institution_id != null && unit.institution_id in next
            }.toSet()
            state.copy(selectedInstitutionIds = next, selectedUnitIds = keptUnits)
        }
        if (_uiState.value.unitAudience) {
            loadTeachingUnitData()
        }
    }

    fun toggleUnit(id: String) {
        _uiState.update { state ->
            val next = if (id in state.selectedUnitIds) {
                state.selectedUnitIds - id
            } else {
                state.selectedUnitIds + id
            }
            state.copy(selectedUnitIds = next)
        }
    }

    fun send() {
        val state = _uiState.value
        if (!state.canSend) return
        val title = state.title.trim()
        val body = state.body.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val results = if (!state.unitAudience) {
                state.selectedInstitutionIds.map { institutionId ->
                    adminRepository.createNotification(
                        NotificationCreate(
                            title = title,
                            body = body,
                            audience = state.audience,
                            institution_id = institutionId,
                            teaching_unit_id = null,
                        ),
                    )
                }
            } else {
                state.selectedUnits.map { unit ->
                    adminRepository.createNotification(
                        NotificationCreate(
                            title = title,
                            body = body,
                            audience = NotificationAudience.TEACHING_UNIT,
                            institution_id = unit.institution_id
                                ?: state.selectedInstitutionIds.firstOrNull(),
                            teaching_unit_id = unit.id,
                        ),
                    )
                }
            }
            val failed = results.firstOrNull { it !is NetworkResult.Success }
            if (failed != null) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = failed.userMessage())
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        composing = false,
                        title = "",
                        body = "",
                        selectedUnitIds = emptySet(),
                    )
                }
                refresh()
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            when (val result = adminRepository.deleteNotification(id)) {
                is NetworkResult.Success -> refresh()
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
            }
        }
    }

    private fun defaultInstitutionIds(institutions: List<InstitutionOut>): Set<String> {
        val sessionId = userSessionStore?.getInstitutionId()
            ?.takeIf { id -> institutions.any { it.id == id } }
        return if (sessionId != null) setOf(sessionId) else emptySet()
    }

    private fun loadTeachingUnitData() {
        val institutionIds = _uiState.value.selectedInstitutionIds.toList()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingUnits = true) }
            val units = when (val result = adminRepository.teachingUnits()) {
                is NetworkResult.Success -> result.data
                else -> _uiState.value.teachingUnits
            }
            val sortMap = linkedMapOf<String, Int>()
            if (institutionIds.isNotEmpty()) {
                coroutineScope {
                    institutionIds.map { institutionId ->
                        async {
                            when (
                                val result = adminRepository.listClasses(
                                    includeInactive = false,
                                    institutionId = institutionId,
                                )
                            ) {
                                is NetworkResult.Success -> result.data
                                else -> emptyList()
                            }
                        }
                    }.awaitAll().forEach { classes ->
                        classes.forEach { sortMap[it.id] = it.sort_order }
                    }
                }
            }
            _uiState.update {
                it.copy(
                    teachingUnits = units,
                    classSortById = it.classSortById + sortMap,
                    isLoadingUnits = false,
                    selectedUnitIds = it.selectedUnitIds.filter { unitId ->
                        units.any { unit ->
                            unit.id == unitId &&
                                unit.institution_id != null &&
                                unit.institution_id in it.selectedInstitutionIds
                        }
                    }.toSet(),
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            adminRepository: AdminRepository,
            userSessionStore: UserSessionStore? = null,
        ): ViewModelProvider.Factory =
            viewModelFactory { AdminAnnouncementsViewModel(adminRepository, userSessionStore) }
    }
}

@Composable
fun AdminAnnouncementsRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    userSessionStore: UserSessionStore? = null,
) {
    val viewModel: AdminAnnouncementsViewModel = viewModel(
        factory = AdminAnnouncementsViewModel.provideFactory(adminRepository, userSessionStore),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        if (!uiState.composing) viewModel.refresh()
        onPauseOrDispose { }
    }
    AdminAnnouncementsScreen(
        uiState = uiState,
        onBack = { if (uiState.composing) viewModel.cancel() else onBack() },
        onStartCreate = viewModel::startCreate,
        onToggleInstitution = viewModel::toggleInstitution,
        onTitle = viewModel::onTitle,
        onBody = viewModel::onBody,
        onAudience = viewModel::onAudience,
        onToggleUnit = viewModel::toggleUnit,
        onSend = viewModel::send,
        onDelete = viewModel::delete,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminAnnouncementsScreen(
    uiState: AdminAnnouncementsUiState,
    onBack: () -> Unit,
    onStartCreate: () -> Unit,
    onToggleInstitution: (String) -> Unit,
    onTitle: (String) -> Unit,
    onBody: (String) -> Unit,
    onAudience: (NotificationAudience) -> Unit,
    onToggleUnit: (String) -> Unit,
    onSend: () -> Unit,
    onDelete: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audiences = listOf(
        NotificationAudience.STUDENTS,
        NotificationAudience.TEACHERS,
        NotificationAudience.PARENTS,
        NotificationAudience.TEACHING_UNIT,
    )
    val audienceLabels = listOf(
        stringResource(R.string.admin_audience_students),
        stringResource(R.string.admin_audience_teachers),
        stringResource(R.string.admin_audience_parents),
        stringResource(R.string.admin_audience_teaching_unit),
    )
    when {
        uiState.isLoading && uiState.items.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.Notifications, modifier = modifier)
        uiState.errorMessage != null && uiState.items.isEmpty() && !uiState.composing -> LoadErrorPanel(
            screenTitle = stringResource(R.string.admin_announce_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRetry,
            isRetrying = uiState.isLoading || uiState.isRefreshing,
            modifier = modifier,
        )
        else -> {
            var managing by rememberSaveable { mutableStateOf(false) }
            var detailItem by remember { mutableStateOf<AppNotification?>(null) }
            BackHandler(enabled = detailItem != null) {
                detailItem = null
            }
            BackHandler(enabled = uiState.composing && detailItem == null) {
                onBack()
            }
            val detail = detailItem
            if (detail != null && !uiState.composing) {
                NotificationDetailScreen(
                    notification = detail,
                    onBack = { detailItem = null },
                    modifier = modifier
                        .fillMaxSize()
                        .background(BgWhite),
                )
            } else {
            LushPullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRetry,
                modifier = modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgWhite)
                        .imePadding(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = if (uiState.composing) 24.dp else 88.dp),
                    ) {
                        AnnouncementTopBar(
                            title = if (uiState.composing) {
                                stringResource(R.string.admin_announce_new)
                            } else {
                                stringResource(R.string.admin_announce_title)
                            },
                            onBack = onBack,
                            showManage = !uiState.composing && uiState.items.isNotEmpty(),
                            managing = managing,
                            onToggleManage = { managing = !managing },
                        )
                        if (uiState.composing) {
                            Spacer(modifier = Modifier.height(16.dp))
                            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                                Text(
                                    text = message,
                                    color = BrandOrange,
                                    fontSize = 13.sp,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            OutlinedAuthField(
                                label = stringResource(R.string.admin_announce_subject),
                                value = uiState.title,
                                onValueChange = onTitle,
                                placeholder = stringResource(R.string.admin_announce_subject),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedAuthField(
                                label = stringResource(R.string.admin_announce_body),
                                value = uiState.body,
                                onValueChange = onBody,
                                placeholder = stringResource(R.string.admin_announce_body),
                                singleLine = false,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.admin_announce_institution),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = BrandBlack,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                uiState.institutions.forEach { institution ->
                                    InstitutionCheckTile(
                                        name = institution.name,
                                        selected = institution.id in uiState.selectedInstitutionIds,
                                        onToggle = { onToggleInstitution(institution.id) },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            AdminFilterRow(
                                labels = audienceLabels,
                                selectedIndex = audiences.indexOf(uiState.audience).coerceAtLeast(0),
                                onSelect = { onAudience(audiences[it]) },
                            )
                            if (uiState.unitAudience) {
                                Spacer(modifier = Modifier.height(14.dp))
                                TeachingUnitPicker(
                                    sections = uiState.institutionSections,
                                    selectedUnitIds = uiState.selectedUnitIds,
                                    showInstitutionHeaders = uiState.selectedInstitutionIds.size > 1,
                                    selectedCount = uiState.selectedUnits.size,
                                    onToggleUnit = onToggleUnit,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            PrimaryButton(
                                text = stringResource(R.string.admin_announce_send),
                                onClick = onSend,
                                enabled = uiState.canSend,
                                fullyRounded = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Spacer(modifier = Modifier.height(20.dp))
                            if (uiState.items.isEmpty()) {
                                NotificationEmptyState(
                                    message = stringResource(R.string.admin_announce_empty),
                                )
                            } else {
                                uiState.items.forEach { item ->
                                    NotificationListCard(
                                        item = item,
                                        onClick = { detailItem = item },
                                        showUnreadDot = false,
                                        chipLabel = item.audienceChipLabel,
                                        trailingContent = if (managing) {
                                            {
                                                IconButton(
                                                    onClick = { onDelete(item.id) },
                                                    modifier = Modifier.size(40.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.DeleteOutline,
                                                        contentDescription = stringResource(
                                                            R.string.admin_delete_announcement,
                                                        ),
                                                        tint = AdminDeleteRed,
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                }
                                            }
                                        } else {
                                            null
                                        },
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
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
                                contentDescription = stringResource(R.string.admin_announce_new),
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun InstitutionCheckTile(
    name: String,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(ComposeControlShape)
            .border(
                1.dp,
                if (selected) TileSelected else BorderGray.copy(alpha = 0.7f),
                ComposeControlShape,
            )
            .background(if (selected) BgLight else BgWhite)
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = BrandBlack,
                uncheckedColor = BorderGray,
                checkmarkColor = Color.White,
            ),
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            fontSize = 13.sp,
            color = BrandBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeachingUnitPicker(
    sections: List<InstitutionUnitSection>,
    selectedUnitIds: Set<String>,
    showInstitutionHeaders: Boolean,
    selectedCount: Int,
    onToggleUnit: (String) -> Unit,
) {
    if (sections.isEmpty()) {
        Text(
            text = stringResource(R.string.admin_announce_no_classes),
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderGray.copy(alpha = 0.7f), ComposeControlShape)
                .padding(horizontal = 14.dp, vertical = 20.dp),
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.admin_announce_subjects),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = BrandBlack,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (selectedCount == 0) {
                stringResource(R.string.admin_announce_choose_units)
            } else {
                stringResource(R.string.admin_announce_units_selected, selectedCount)
            },
            color = TextSecondary,
            fontSize = 12.sp,
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    sections.forEach { section ->
        if (showInstitutionHeaders) {
            Text(
                text = section.institutionName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        section.classes.forEach { group ->
            Text(
                text = group.className,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                group.units.forEach { unit ->
                    InstitutionCheckTile(
                        name = unit.subject_name,
                        selected = unit.id in selectedUnitIds,
                        onToggle = { onToggleUnit(unit.id) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AnnouncementTopBar(
    title: String,
    onBack: () -> Unit,
    managing: Boolean,
    showManage: Boolean,
    onToggleManage: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_back),
                tint = BrandBlack,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = BrandBlack,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showManage) {
            AdminManageToggle(
                managing = managing,
                onToggle = onToggleManage,
                contentDescription = stringResource(R.string.admin_manage_announcements),
            )
        }
    }
}
