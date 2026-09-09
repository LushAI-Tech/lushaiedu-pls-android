package com.lushaiedupls.ui.admin.announcements

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.lushaiedupls.data.remote.dto.NotificationAudience
import com.lushaiedupls.data.remote.dto.NotificationCreate
import com.lushaiedupls.data.remote.dto.NotificationUpdate
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminEditDeleteIcons
import com.lushaiedupls.ui.admin.AdminManageToggle
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.NotificationDetailScreen
import com.lushaiedupls.ui.common.NotificationEmptyState
import com.lushaiedupls.ui.common.NotificationListCard
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminAnnouncementsUiState(
    val items: List<AppNotification> = emptyList(),
    val composing: Boolean = false,
    val editingId: String? = null,
    val title: String = "",
    val body: String = "",
    val audience: NotificationAudience = NotificationAudience.ALL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class AdminAnnouncementsViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminAnnouncementsUiState(isLoading = true))
    val uiState: StateFlow<AdminAnnouncementsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.items.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    errorMessage = null,
                )
            }
            when (val result = adminRepository.notifications()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        items = StudentUiMappers.notifications(result.data),
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

    fun startCreate() = _uiState.update {
        it.copy(
            composing = true,
            editingId = null,
            title = "",
            body = "",
            audience = NotificationAudience.ALL,
        )
    }

    fun startEdit(item: AppNotification) = _uiState.update {
        it.copy(
            composing = true,
            editingId = item.id,
            title = item.title,
            body = item.body,
        )
    }

    fun cancel() = _uiState.update { it.copy(composing = false) }
    fun onTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun onBody(value: String) = _uiState.update { it.copy(body = value) }
    fun onAudience(value: NotificationAudience) = _uiState.update { it.copy(audience = value) }

    fun send() {
        val state = _uiState.value
        if (state.title.isBlank() || state.body.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = if (state.editingId == null) {
                adminRepository.createNotification(
                    NotificationCreate(
                        title = state.title.trim(),
                        body = state.body.trim(),
                        audience = state.audience,
                    ),
                )
            } else {
                adminRepository.updateNotification(
                    state.editingId,
                    NotificationUpdate(
                        title = state.title.trim(),
                        body = state.body.trim(),
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
            when (val result = adminRepository.deleteNotification(id)) {
                is NetworkResult.Success -> refresh()
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
            }
        }
    }

    companion object {
        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminAnnouncementsViewModel(adminRepository) }
    }
}

@Composable
fun AdminAnnouncementsRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminAnnouncementsViewModel = viewModel(
        factory = AdminAnnouncementsViewModel.provideFactory(adminRepository),
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
        onStartEdit = viewModel::startEdit,
        onTitle = viewModel::onTitle,
        onBody = viewModel::onBody,
        onAudience = viewModel::onAudience,
        onSend = viewModel::send,
        onDelete = viewModel::delete,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun AdminAnnouncementsScreen(
    uiState: AdminAnnouncementsUiState,
    onBack: () -> Unit,
    onStartCreate: () -> Unit,
    onStartEdit: (AppNotification) -> Unit,
    onTitle: (String) -> Unit,
    onBody: (String) -> Unit,
    onAudience: (NotificationAudience) -> Unit,
    onSend: () -> Unit,
    onDelete: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audiences = listOf(
        NotificationAudience.ALL,
        NotificationAudience.STUDENTS,
        NotificationAudience.TEACHERS,
        NotificationAudience.PARENTS,
    )
    when {
        uiState.isLoading && uiState.items.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.Notifications, modifier = modifier)
        uiState.errorMessage != null && uiState.items.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.admin_announce_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRetry,
            isRetrying = uiState.isLoading || uiState.isRefreshing,
            modifier = modifier,
        )
        else -> {
            var managing by rememberSaveable { mutableStateOf(false) }
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            val selectedItem = uiState.items.firstOrNull { it.id == selected }
            BackHandler(enabled = uiState.composing || selected != null) {
                when {
                    uiState.composing -> onBack()
                    else -> selected = null
                }
            }
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
                        title = when {
                            uiState.composing && uiState.editingId == null ->
                                stringResource(R.string.admin_announce_new)
                            uiState.composing -> stringResource(R.string.admin_edit_announcement)
                            else -> stringResource(R.string.admin_announce_title)
                        },
                        onBack = onBack,
                        showManage = !uiState.composing,
                        managing = managing,
                        onToggleManage = { managing = !managing },
                    )
                    if (uiState.composing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (uiState.editingId == null) {
                            AdminFilterRow(
                                labels = listOf(
                                    stringResource(R.string.admin_audience_all),
                                    stringResource(R.string.admin_audience_students),
                                    stringResource(R.string.admin_audience_teachers),
                                    stringResource(R.string.admin_audience_parents),
                                ),
                                selectedIndex = audiences.indexOf(uiState.audience).coerceAtLeast(0),
                                onSelect = { onAudience(audiences[it]) },
                            )
                            Spacer(modifier = Modifier.height(12.dp))
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
                        PrimaryButton(
                            text = stringResource(
                                if (uiState.editingId == null) {
                                    R.string.admin_announce_send
                                } else {
                                    R.string.admin_save
                                },
                            ),
                            onClick = onSend,
                            enabled = uiState.title.isNotBlank() &&
                                uiState.body.isNotBlank() &&
                                !uiState.isSaving,
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
                                    onClick = { selected = item.id },
                                    showUnreadDot = false,
                                    trailingContent = if (managing) {
                                        {
                                            AdminEditDeleteIcons(
                                                onEdit = {
                                            selected = null
                                            onStartEdit(item)
                                        },
                                                onDelete = { onDelete(item.id) },
                                                editDescription = stringResource(
                                                    R.string.admin_edit_announcement,
                                                ),
                                                deleteDescription = stringResource(
                                                    R.string.admin_delete_announcement,
                                                ),
                                            )
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
                AnimatedVisibility(
                    visible = selectedItem != null && !uiState.composing,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                ) {
                    selectedItem?.let { announcement ->
                        NotificationDetailScreen(
                            notification = announcement,
                            onBack = { selected = null },
                        )
                    }
                }
            }
            }
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
