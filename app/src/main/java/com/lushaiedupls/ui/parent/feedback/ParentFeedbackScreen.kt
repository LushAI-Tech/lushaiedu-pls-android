package com.lushaiedupls.ui.parent.feedback

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.dto.FeedbackStatus
import com.lushaiedupls.data.remote.dto.LinkedStudentOut
import com.lushaiedupls.data.remote.dto.ParentFeedbackOut
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.AnimatedFilterChipRow
import com.lushaiedupls.ui.common.AppBackNav
import com.lushaiedupls.ui.common.CenteredEmptyState
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.parent.components.ParentLoadErrorPanel
import com.lushaiedupls.ui.parent.components.ParentPendingApprovalPanel
import com.lushaiedupls.ui.parent.formatIsoDate
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)
private val DeleteRed = Color(0xFFF25F5C)

@Composable
fun ParentFeedbackRoute(
    parentRepository: ParentRepository,
    studentId: String?,
    onScanClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ParentFeedbackViewModel = viewModel(
        factory = ParentFeedbackViewModel.provideFactory(parentRepository, studentId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        if (!uiState.composing) viewModel.refresh()
        onPauseOrDispose { }
    }
    BackHandler(enabled = uiState.composing) { viewModel.cancelComposer() }
    ParentFeedbackScreen(
        uiState = uiState,
        onBack = {
            if (uiState.composing) viewModel.cancelComposer() else onBack()
        },
        onRetry = viewModel::refresh,
        onPullRefresh = { viewModel.refresh(asPullRefresh = true) },
        onScanClick = onScanClick,
        onNew = viewModel::startCreate,
        onOpen = viewModel::startEdit,
        onSubjectChange = viewModel::setSubject,
        onMessageChange = viewModel::setMessage,
        onStudentChange = viewModel::setStudentId,
        onSave = viewModel::save,
        onDelete = viewModel::deleteCurrent,
        modifier = modifier,
    )
}

@Composable
fun ParentFeedbackScreen(
    uiState: ParentFeedbackUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPullRefresh: () -> Unit,
    onScanClick: () -> Unit,
    onNew: () -> Unit,
    onOpen: (ParentFeedbackOut) -> Unit,
    onSubjectChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onStudentChange: (String?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.items.isEmpty() && uiState.errorMessage == null &&
            !uiState.needsApproval && !uiState.composing ->
            StudentPageSkeleton(
                kind = StudentSkeletonKind.List,
                title = stringResource(R.string.parent_feedback_title),
                modifier = modifier,
            )
        uiState.needsApproval && uiState.items.isEmpty() && !uiState.composing ->
            ParentPendingApprovalPanel(
                onScanClick = onScanClick,
                onRefresh = onRetry,
                isRefreshing = uiState.isLoading,
                modifier = modifier,
            )
        uiState.errorMessage != null && uiState.items.isEmpty() && !uiState.composing ->
            ParentLoadErrorPanel(
                screenTitle = stringResource(R.string.parent_feedback_title),
                message = uiState.errorMessage.orEmpty(),
                onRetry = onRetry,
                isRetrying = uiState.isLoading,
                modifier = modifier,
            )
        uiState.composing -> FeedbackComposer(
            uiState = uiState,
            onBack = onBack,
            onSubjectChange = onSubjectChange,
            onMessageChange = onMessageChange,
            onStudentChange = onStudentChange,
            onSave = onSave,
            onDelete = onDelete,
            modifier = modifier,
        )
        else -> FeedbackList(
            uiState = uiState,
            onBack = onBack,
            onNew = onNew,
            onOpen = onOpen,
            onPullRefresh = onPullRefresh,
            modifier = modifier,
        )
    }
}

@Composable
private fun FeedbackList(
    uiState: ParentFeedbackUiState,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onOpen: (ParentFeedbackOut) -> Unit,
    onPullRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LushPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onPullRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 96.dp),
            ) {
                AppBackNav(onBack = onBack)
                Text(
                    text = stringResource(R.string.parent_feedback_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.items.isEmpty()) {
                    CenteredEmptyState(
                        message = stringResource(R.string.parent_feedback_empty),
                        icon = Icons.Filled.Forum,
                    )
                } else {
                    uiState.items.forEach { item ->
                        FeedbackCard(item = item, onClick = { onOpen(item) })
                        Spacer(modifier = Modifier.height(12.dp))
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
                    .clickable(onClick = onNew),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.parent_feedback_new),
                    tint = BgWhite,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun FeedbackComposer(
    uiState: ParentFeedbackUiState,
    onBack: () -> Unit,
    onSubjectChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onStudentChange: (String?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        AppBackNav(onBack = onBack)
        Text(
            text = if (uiState.editingId == null) {
                stringResource(R.string.parent_feedback_new)
            } else {
                stringResource(R.string.parent_feedback_title)
            },
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        uiState.status?.let { status ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feedbackStatusLabel(status),
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (uiState.children.isNotEmpty()) {
            Text(
                text = stringResource(R.string.parent_feedback_child),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChildChips(
                children = uiState.children,
                selectedStudentId = uiState.studentId,
                includeNone = true,
                onSelect = onStudentChange,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        OutlinedAuthField(
            label = stringResource(R.string.parent_feedback_subject),
            value = uiState.subject,
            onValueChange = onSubjectChange,
            placeholder = stringResource(R.string.parent_feedback_subject_hint),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedAuthField(
            label = stringResource(R.string.parent_feedback_message),
            value = uiState.message,
            onValueChange = onMessageChange,
            placeholder = stringResource(R.string.parent_feedback_message_hint),
            singleLine = false,
        )
        val notes = uiState.adminNotes?.trim().orEmpty()
        if (notes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.parent_feedback_admin_notes),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = notes,
                color = TextSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
        if (!uiState.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.errorMessage.orEmpty(),
                color = DeleteRed,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(R.string.parent_feedback_save),
            onClick = onSave,
            enabled = !uiState.isSaving,
            fullyRounded = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (uiState.editingId != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.parent_feedback_delete),
                color = DeleteRed,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !uiState.isSaving, onClick = onDelete)
                    .padding(vertical = 12.dp),
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    item: ParentFeedbackOut,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, BorderGray.copy(alpha = 0.7f), CardShape)
            .background(BgWhite)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Forum,
            contentDescription = null,
            tint = BrandBlack,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = item.subject,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = feedbackStatusLabel(item.status),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = listOfNotNull(
                    item.student?.name?.takeIf { it.isNotBlank() },
                    formatIsoDate(item.created_at).takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                color = TextSecondary,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
            )
            if (item.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.message,
                    color = BrandBlack,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ChildChips(
    children: List<LinkedStudentOut>,
    selectedStudentId: String?,
    includeNone: Boolean,
    onSelect: (String?) -> Unit,
) {
    val noneLabel = stringResource(R.string.parent_feedback_child_none)
    val options = buildList {
        if (includeNone) add(noneLabel)
        addAll(children.map { it.student.name })
    }
    val selectedIndex = when {
        includeNone && selectedStudentId == null -> 0
        else -> {
            val childIndex = children.indexOfFirst { it.student.id == selectedStudentId }
            if (childIndex < 0) 0 else childIndex + if (includeNone) 1 else 0
        }
    }
    AnimatedFilterChipRow(
        options = options,
        selectedIndex = selectedIndex,
        onSelect = { index ->
            if (includeNone && index == 0) {
                onSelect(null)
            } else {
                val childIndex = index - if (includeNone) 1 else 0
                onSelect(children[childIndex].student.id)
            }
        },
    )
}

@Composable
private fun feedbackStatusLabel(status: FeedbackStatus): String = when (status) {
    FeedbackStatus.UNSEEN -> stringResource(R.string.parent_feedback_status_unseen)
    FeedbackStatus.SEEN -> stringResource(R.string.parent_feedback_status_seen)
}
