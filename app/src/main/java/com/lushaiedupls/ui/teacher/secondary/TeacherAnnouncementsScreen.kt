package com.lushaiedupls.ui.teacher.secondary

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AppNotification
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.admin.AdminEditDeleteIcons
import com.lushaiedupls.ui.admin.AdminManageToggle
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.NotificationDetailScreen
import com.lushaiedupls.ui.common.NotificationEmptyState
import com.lushaiedupls.ui.common.NotificationListCard
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange

@Composable
fun TeacherAnnouncementsRoute(
    teacherRepository: TeacherRepository,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherAnnouncementsViewModel = viewModel(
        factory = TeacherAnnouncementsViewModel.provideFactory(teacherRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TeacherAnnouncementsScreen(
        uiState = uiState,
        onBack = {
            if (uiState.composing) viewModel.cancelCompose() else onBack()
        },
        onCreate = onCreate,
        onStartEdit = viewModel::startEdit,
        onTitle = viewModel::onTitle,
        onBody = viewModel::onBody,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun TeacherAnnouncementsScreen(
    uiState: TeacherAnnouncementsUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onStartEdit: (AppNotification) -> Unit,
    onTitle: (String) -> Unit,
    onBody: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var managing by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<AppNotification?>(null) }

    BackHandler(enabled = uiState.composing || selected != null) {
        when {
            uiState.composing -> onBack()
            else -> selected = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .imePadding(),
    ) {
        if (uiState.composing) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                AnnouncementsHeader(
                    title = stringResource(R.string.admin_edit_announcement),
                    onBack = onBack,
                    managing = managing,
                    showManage = false,
                    onToggleManage = { managing = !managing },
                )
                uiState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = message, color = BrandOrange, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedAuthField(
                    label = stringResource(R.string.teacher_announcement_subject),
                    value = uiState.title,
                    onValueChange = onTitle,
                    placeholder = stringResource(R.string.teacher_announcement_subject_hint),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedAuthField(
                    label = stringResource(R.string.teacher_announcement_body),
                    value = uiState.body,
                    onValueChange = onBody,
                    placeholder = stringResource(R.string.teacher_announcement_body_hint),
                    singleLine = false,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = stringResource(R.string.admin_save),
                    onClick = onSave,
                    enabled = uiState.title.isNotBlank() &&
                        uiState.body.isNotBlank() &&
                        !uiState.isSaving,
                    fullyRounded = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            LushPullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 96.dp),
                ) {
                    AnnouncementsHeader(
                        title = stringResource(R.string.teacher_announcements_title),
                        onBack = onBack,
                        managing = managing,
                        showManage = true,
                        onToggleManage = { managing = !managing },
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    if (uiState.announcements.isEmpty()) {
                        NotificationEmptyState(
                            message = stringResource(R.string.teacher_announcements_empty_title),
                        )
                    } else {
                        uiState.announcements.forEach { item ->
                            NotificationListCard(
                                item = item,
                                onClick = { selected = item },
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
                    .size(56.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(BrandBlack)
                    .clickable(onClick = onCreate),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.cd_new_announcement),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = selected != null && !uiState.composing,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        ) {
            selected?.let { announcement ->
                NotificationDetailScreen(
                    notification = announcement,
                    onBack = { selected = null },
                )
            }
        }
    }
}

@Composable
private fun AnnouncementsHeader(
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
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = BrandBlack,
            modifier = Modifier.weight(1f),
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
