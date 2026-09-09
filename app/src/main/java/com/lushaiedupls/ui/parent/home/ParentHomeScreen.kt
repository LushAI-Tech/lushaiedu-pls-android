package com.lushaiedupls.ui.parent.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.OverviewIcon
import com.lushaiedupls.data.remote.dto.FeePaymentStatus
import com.lushaiedupls.data.remote.dto.ParentChildSummary
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.AppTopBar
import com.lushaiedupls.ui.common.CenteredEmptyState
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.MetricCard
import com.lushaiedupls.ui.common.SectionTitle
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.UserAvatar
import com.lushaiedupls.ui.parent.components.ParentLoadErrorPanel
import com.lushaiedupls.ui.parent.components.ParentPendingApprovalPanel
import com.lushaiedupls.ui.parent.formatInrFromPaise
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(18.dp)

@Composable
fun ParentHomeRoute(
    userSessionStore: UserSessionStore,
    parentRepository: ParentRepository,
    studentRepository: StudentRepository? = null,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onScanClick: () -> Unit,
    onFeesClick: () -> Unit = {},
    onStudentReady: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ParentHomeViewModel = viewModel(
        factory = ParentHomeViewModel.provideFactory(userSessionStore, parentRepository, studentRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.selectedStudentId, uiState.isLoading) {
        if (!uiState.isLoading) onStudentReady(uiState.selectedStudentId)
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    ParentHomeScreen(
        uiState = uiState,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick,
        onScanClick = onScanClick,
        onFeesClick = onFeesClick,
        onRefresh = viewModel::refresh,
        onPullRefresh = { viewModel.refresh(asPullRefresh = true) },
        modifier = modifier,
    )
}

@Composable
fun ParentHomeScreen(
    uiState: ParentHomeUiState,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onScanClick: () -> Unit,
    onFeesClick: () -> Unit,
    onRefresh: () -> Unit,
    onPullRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.children.isEmpty() && !uiState.needsApproval &&
            uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.Home, modifier = modifier)
        uiState.needsApproval && uiState.children.isEmpty() -> Column(
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite)
                .padding(top = 16.dp),
        ) {
            AppTopBar(
                displayName = uiState.displayName,
                notificationCount = uiState.notificationCount,
                onNotificationClick = onNotificationsClick,
                onProfileClick = onProfileClick,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            ParentPendingApprovalPanel(
                onScanClick = onScanClick,
                onRefresh = onRefresh,
                isRefreshing = uiState.isLoading,
                modifier = Modifier.weight(1f),
            )
        }
        uiState.errorMessage != null && uiState.children.isEmpty() -> ParentLoadErrorPanel(
            screenTitle = stringResource(R.string.section_overview),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRefresh,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> LushPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onPullRefresh,
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                SectionTitle(text = stringResource(R.string.section_overview))
                Spacer(modifier = Modifier.height(12.dp))
                OverviewGrid(
                    uiState = uiState,
                    onFeesClick = onFeesClick,
                )
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(text = stringResource(R.string.parent_section_children))
                Spacer(modifier = Modifier.height(12.dp))
                if (uiState.children.isEmpty()) {
                    CenteredEmptyState(
                        message = stringResource(R.string.parent_empty_children),
                        icon = Icons.Outlined.Groups,
                        footer = {
                            PrimaryButton(
                                text = stringResource(R.string.parent_scan_qr),
                                onClick = onScanClick,
                                fullyRounded = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                    )
                } else {
                    uiState.children.forEach { child ->
                        ChildCard(child = child)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewGrid(
    uiState: ParentHomeUiState,
    onFeesClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            label = stringResource(R.string.parent_stat_children),
            value = uiState.totalChildren.toString(),
            emphasized = true,
            iconKind = OverviewIcon.Children,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = stringResource(R.string.parent_stat_pending_fees),
            value = uiState.childrenWithPendingFees.toString(),
            emphasized = false,
            iconKind = OverviewIcon.Fees,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onFeesClick),
        )
    }
}

@Composable
private fun ChildCard(
    child: ParentChildSummary,
) {
    val overall = child.overall
    val presentPct = overall.present_pct_all.roundToInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, BorderGray.copy(alpha = 0.7f), CardShape)
            .background(BgWhite)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                url = child.student.avatar_url,
                size = 48.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = child.student.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
            Spacer(modifier = Modifier.height(12.dp))
            MetricCard(
                label = stringResource(R.string.parent_ai_mastery),
                value = "$mastery%",
                emphasized = false,
                iconKind = OverviewIcon.StemMastery,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val feeLine = childFeeLine(child)
        if (feeLine != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = feeLine,
                color = TextSecondary,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Composable
private fun childFeeLine(child: ParentChildSummary): String? {
    if (child.pending_fee_amount_paise > 0) {
        val amount = formatInrFromPaise(child.pending_fee_amount_paise)
        return if (child.pending_fee_months > 0) {
            stringResource(
                R.string.parent_child_fee_pending_months,
                amount,
                child.pending_fee_months,
            )
        } else {
            stringResource(R.string.parent_child_fee_pending, amount)
        }
    }
    return when (child.selected_month_fee_status) {
        FeePaymentStatus.PAID -> stringResource(R.string.parent_child_fee_paid)
        FeePaymentStatus.NOT_PAID -> stringResource(R.string.parent_child_fee_unpaid)
        null -> null
    }
}
