package com.lushaiedupls.ui.parent.fees

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.lushaiedupls.data.remote.dto.FeeLedgerOut
import com.lushaiedupls.data.remote.dto.FeePaymentStatus
import com.lushaiedupls.data.remote.dto.LinkedStudentOut
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.ui.common.AppBackNav
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.parent.formatInrFromPaise
import com.lushaiedupls.ui.parent.formatIsoDate
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)
private val TabShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
private val PaidGreen = Color(0xFF22C55E)
private val UnpaidRed = Color(0xFFF25F5C)

@Composable
fun ParentFeesRoute(
    parentRepository: ParentRepository,
    studentId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ParentFeesViewModel = viewModel(
        key = studentId.orEmpty(),
        factory = ParentFeesViewModel.provideFactory(parentRepository, studentId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    ParentFeesScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectStudent = viewModel::selectStudent,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun ParentFeesScreen(
    uiState: ParentFeesUiState,
    onBack: () -> Unit,
    onSelectStudent: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.rows.isEmpty() && uiState.errorMessage == null &&
            uiState.children.isEmpty() -> StudentPageSkeleton(
            kind = StudentSkeletonKind.Home,
            title = stringResource(R.string.parent_fees_title),
            modifier = modifier,
        )
        uiState.errorMessage != null && uiState.rows.isEmpty() && uiState.children.isEmpty() ->
            LoadErrorPanel(
                screenTitle = stringResource(R.string.parent_fees_title),
                message = uiState.errorMessage.orEmpty(),
                onRetry = onRetry,
                isRetrying = uiState.isLoading,
                modifier = modifier,
            )
        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            AppBackNav(onBack = onBack)
            Text(
                text = stringResource(R.string.parent_fees_title),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.children.isEmpty()) {
                Text(
                    text = stringResource(R.string.parent_fees_empty_children),
                    color = TextSecondary,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .border(1.dp, BorderGray, CardShape)
                        .background(BgWhite),
                ) {
                    ChildSelectTabs(
                        children = uiState.children,
                        selectedStudentId = uiState.selectedStudentId,
                        onSelect = onSelectStudent,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BgWhite)
                            .padding(16.dp),
                    ) {
                        when {
                            uiState.rows.isEmpty() && uiState.isLoading -> { /* keep tabs while history loads */ }
                            uiState.errorMessage != null && uiState.rows.isEmpty() -> Text(
                                text = uiState.errorMessage.orEmpty(),
                                color = TextSecondary,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.SansSerif,
                            )
                            uiState.rows.isEmpty() -> Text(
                                text = stringResource(R.string.parent_fees_empty),
                                color = TextSecondary,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.SansSerif,
                            )
                            else -> uiState.rows.forEachIndexed { index, row ->
                                if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                                FeeRowCard(row = row)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildSelectTabs(
    children: List<LinkedStudentOut>,
    selectedStudentId: String?,
    onSelect: (String) -> Unit,
) {
    val useWeights = children.size <= 3
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandBlack)
            .then(
                if (useWeights) Modifier else Modifier.horizontalScroll(rememberScrollState()),
            ),
    ) {
        children.forEachIndexed { index, child ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(BgWhite.copy(alpha = 0.16f)),
                )
            }
            val selected = child.student.id == selectedStudentId
            Box(
                modifier = Modifier
                    .then(if (useWeights) Modifier.weight(1f) else Modifier.widthIn(min = 104.dp))
                    .height(44.dp)
                    .clip(TabShape)
                    .background(if (selected) BgWhite else BrandBlack)
                    .clickable { onSelect(child.student.id) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = child.student.name,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (selected) BrandBlack else BgWhite,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FeeRowCard(row: FeeLedgerOut) {
    val isPaid = row.payment_status == FeePaymentStatus.PAID
    val statusColor = if (isPaid) PaidGreen else UnpaidRed
    val statusLabel = when (row.payment_status) {
        FeePaymentStatus.PAID -> {
            val paidOn = formatIsoDate(row.paid_at).takeIf { it.isNotBlank() }
            if (paidOn != null) {
                stringResource(R.string.parent_fee_paid_on, paidOn)
            } else {
                stringResource(R.string.parent_fee_paid)
            }
        }
        FeePaymentStatus.NOT_PAID -> stringResource(R.string.parent_fee_not_paid)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, BorderGray.copy(alpha = 0.7f), CardShape)
            .background(BgWhite)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Payments,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = row.month,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Text(
                    text = formatInrFromPaise(row.amount_paise),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = statusColor,
                    fontFamily = FontFamily.SansSerif,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            val className = row.class_name?.takeIf { it.isNotBlank() }
            Text(
                text = listOfNotNull(className, statusLabel).joinToString(" · "),
                color = statusColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}
