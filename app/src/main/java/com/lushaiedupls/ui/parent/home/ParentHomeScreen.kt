package com.lushaiedupls.ui.parent.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.OverviewIcon
import com.lushaiedupls.data.remote.dto.ParentChildSummary
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.AppTopBar
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.MetricCard
import com.lushaiedupls.ui.common.SectionTitle
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(18.dp)

@Composable
fun ParentHomeRoute(
    userSessionStore: UserSessionStore,
    parentRepository: ParentRepository,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onScanClick: () -> Unit,
    onChildClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ParentHomeViewModel = viewModel(
        factory = ParentHomeViewModel.provideFactory(userSessionStore, parentRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    ParentHomeScreen(
        uiState = uiState,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick,
        onScanClick = onScanClick,
        onChildClick = { child ->
            viewModel.selectStudent(child.student.id)
            onChildClick(child.student.id, child.student.name)
        },
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun ParentHomeScreen(
    uiState: ParentHomeUiState,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onScanClick: () -> Unit,
    onChildClick: (ParentChildSummary) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.children.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.Home, modifier = modifier)
        uiState.errorMessage != null && uiState.children.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.section_overview),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRefresh,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            AppTopBar(
                displayName = uiState.displayName,
                notificationCount = uiState.notificationCount,
                onNotificationClick = onNotificationsClick,
                onProfileClick = onProfileClick,
            )
            Spacer(modifier = Modifier.height(22.dp))
            SectionTitle(text = stringResource(R.string.parent_section_children))
            Spacer(modifier = Modifier.height(12.dp))
            if (uiState.children.isEmpty()) {
                Text(
                    text = stringResource(R.string.parent_empty_children),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = stringResource(R.string.parent_scan_qr),
                    onClick = onScanClick,
                    fullyRounded = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                uiState.children.forEach { child ->
                    ChildCard(
                        child = child,
                        selected = child.student.id == uiState.selectedStudentId,
                        onClick = { onChildClick(child) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                PrimaryButton(
                    text = stringResource(R.string.parent_scan_another),
                    onClick = onScanClick,
                    fullyRounded = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ChildCard(
    child: ParentChildSummary,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val overall = child.overall
    val presentPct = overall.present_pct_all.roundToInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) BrandOrange else BorderGray.copy(alpha = 0.75f),
                shape = CardShape,
            )
            .background(BgWhite)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(
            text = child.student.name,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = listOfNotNull(
                child.class_name?.takeIf { it.isNotBlank() },
                child.subjects.takeIf { it.isNotEmpty() }?.joinToString(),
            ).joinToString(" · ").ifBlank { child.student.email.orEmpty() },
            color = TextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                label = stringResource(R.string.teacher_stat_present_rate),
                value = "$presentPct%",
                emphasized = true,
                iconKind = OverviewIcon.AverageProgress,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = stringResource(R.string.teacher_stat_total_sessions),
                value = overall.sessions.toString(),
                emphasized = false,
                iconKind = OverviewIcon.Subject,
                modifier = Modifier.weight(1f),
            )
        }
        val mastery = child.ai.stem_mastery_pct?.roundToInt()
        if (child.ai.available && mastery != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.parent_ai_mastery, mastery),
                color = BrandOrange,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}
