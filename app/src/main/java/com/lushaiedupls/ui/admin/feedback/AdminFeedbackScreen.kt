package com.lushaiedupls.ui.admin.feedback

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.dto.FeedbackStatus
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminCard
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.admin.formatIsoDate
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary

@Composable
fun AdminFeedbackRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminFeedbackViewModel = viewModel(
        factory = AdminFeedbackViewModel.provideFactory(adminRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        if (uiState.selected == null) viewModel.refresh()
        onPauseOrDispose { }
    }
    if (uiState.selected != null) BackHandler { viewModel.closeDetail() }
    AdminFeedbackScreen(
        uiState = uiState,
        onBack = { if (uiState.selected != null) viewModel.closeDetail() else onBack() },
        onFilter = viewModel::setFilter,
        onSelect = viewModel::select,
        onNotes = viewModel::onNotes,
        onMarkSeen = viewModel::markSeen,
        onSaveNotes = viewModel::saveNotes,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun AdminFeedbackScreen(
    uiState: AdminFeedbackUiState,
    onBack: () -> Unit,
    onFilter: (FeedbackStatus?) -> Unit,
    onSelect: (com.lushaiedupls.data.remote.dto.ParentFeedbackOut) -> Unit,
    onNotes: (String) -> Unit,
    onMarkSeen: () -> Unit,
    onSaveNotes: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.items.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.items.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.admin_feedback_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRetry,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            AdminScreenHeader(title = stringResource(R.string.admin_feedback_title), onBack = onBack)
            if (uiState.selected == null) {
                val filters = listOf<FeedbackStatus?>(FeedbackStatus.UNSEEN, FeedbackStatus.SEEN, null)
                AdminFilterRow(
                    labels = listOf(
                        stringResource(R.string.parent_feedback_status_unseen),
                        stringResource(R.string.parent_feedback_status_seen),
                        stringResource(R.string.admin_filter_all),
                    ),
                    selectedIndex = filters.indexOf(uiState.filter),
                    onSelect = { onFilter(filters[it]) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.items.isEmpty()) {
                    AdminEmptyText(stringResource(R.string.admin_feedback_empty))
                } else {
                    uiState.items.forEach { item ->
                        AdminCard(onClick = { onSelect(item) }) {
                            Text(
                                text = item.subject,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BrandBlack,
                                fontFamily = FontFamily.SansSerif,
                            )
                            AdminMuted(
                                listOfNotNull(
                                    item.parent.name,
                                    item.student?.name,
                                    formatIsoDate(item.created_at),
                                ).joinToString(" · "),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.message,
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 3,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            } else {
                val item = uiState.selected
                Text(
                    text = item.subject,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(6.dp))
                AdminMuted(
                    listOfNotNull(item.parent.name, item.student?.name, formatIsoDate(item.created_at))
                        .joinToString(" · "),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.message,
                    color = BrandBlack,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedAuthField(
                    label = stringResource(R.string.parent_feedback_admin_notes),
                    value = uiState.notes,
                    onValueChange = onNotes,
                    placeholder = stringResource(R.string.admin_feedback_notes_hint),
                    singleLine = false,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (item.status == FeedbackStatus.UNSEEN) {
                    PrimaryButton(
                        text = stringResource(R.string.admin_feedback_mark_seen),
                        onClick = onMarkSeen,
                        enabled = !uiState.isSaving,
                        fullyRounded = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                PrimaryButton(
                    text = stringResource(R.string.admin_feedback_save_notes),
                    onClick = onSaveNotes,
                    enabled = !uiState.isSaving,
                    fullyRounded = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
