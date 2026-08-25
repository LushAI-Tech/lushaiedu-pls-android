package com.lushaiedupls.ui.admin.fees

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.dto.FeeLedgerOut
import com.lushaiedupls.data.remote.dto.FeePaymentStatus
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminCard
import com.lushaiedupls.ui.admin.AdminChipShape
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminPaidGreen
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.admin.formatInrFromPaise
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary
import coil.compose.AsyncImage
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val OverlayShape = RoundedCornerShape(20.dp)

@Composable
fun AdminFeesRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminFeesViewModel = viewModel(
        factory = AdminFeesViewModel.provideFactory(adminRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    AdminFeesScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectMonth = viewModel::selectMonth,
        onSelectClass = viewModel::selectClass,
        onAmount = viewModel::onAmountChange,
        onSaveFee = viewModel::saveClassFee,
        onGenerate = viewModel::generate,
        onTogglePaid = viewModel::togglePaid,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun AdminFeesScreen(
    uiState: AdminFeesUiState,
    onBack: () -> Unit,
    onSelectMonth: (String) -> Unit,
    onSelectClass: (String) -> Unit,
    onAmount: (String) -> Unit,
    onSaveFee: () -> Unit,
    onGenerate: () -> Unit,
    onTogglePaid: (FeeLedgerOut) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.ledgers.isEmpty() && uiState.summary == null &&
            uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.summary == null && uiState.ledgers.isEmpty() ->
            LoadErrorPanel(
                screenTitle = stringResource(R.string.admin_fees_title),
                message = uiState.errorMessage.orEmpty(),
                onRetry = onRetry,
                isRetrying = uiState.isLoading,
                modifier = modifier,
            )
        else -> {
            var showSettings by rememberSaveable { mutableStateOf(false) }
            var showMonthPicker by rememberSaveable { mutableStateOf(false) }
            BackHandler(enabled = showSettings || showMonthPicker) {
                if (showMonthPicker) showMonthPicker = false else showSettings = false
            }
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(BgWhite)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                AdminScreenHeader(title = stringResource(R.string.admin_fees_title), onBack = onBack)
                FeeMonthSelector(
                    month = uiState.month,
                    onClick = { showMonthPicker = true },
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (uiState.classes.isNotEmpty()) {
                    val selectedIndex = uiState.classes.indexOfFirst { it.id == uiState.selectedClassId }
                        .coerceAtLeast(0)
                    AdminFilterRow(
                        labels = uiState.classes.map { it.name },
                        selectedIndex = selectedIndex,
                        onSelect = { onSelectClass(uiState.classes[it].id) },
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = stringResource(R.string.admin_fees_manage),
                    onClick = { showSettings = true },
                    fullyRounded = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.ledgers.isEmpty()) {
                    AdminEmptyText(stringResource(R.string.admin_fees_empty))
                } else {
                    uiState.ledgers.forEach { row ->
                        FeeStudentCard(
                            row = row,
                            onTogglePaid = { onTogglePaid(row.ledger) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            if (showMonthPicker) {
                FeeMonthPickerOverlay(
                    selectedMonth = parseYearMonth(uiState.month),
                    onDismiss = { showMonthPicker = false },
                    onSelect = { month ->
                        showMonthPicker = false
                        onSelectMonth(month.toString())
                    },
                )
            }
            if (showSettings) {
                FeeSettingsOverlay(
                    uiState = uiState,
                    onAmount = onAmount,
                    onSaveFee = onSaveFee,
                    onGenerate = onGenerate,
                    onDismiss = { showSettings = false },
                )
            }
        }
    }
}

private fun parseYearMonth(value: String): YearMonth =
    runCatching { YearMonth.parse(value) }.getOrNull() ?: YearMonth.now()

private fun formatMonthLabel(value: String): String {
    val month = parseYearMonth(value)
    return "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}"
}

@Composable
private fun FeeMonthSelector(
    month: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatMonthLabel(month),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = stringResource(R.string.admin_fees_select_month),
            tint = BrandBlack,
        )
    }
}

@Composable
private fun FeeMonthPickerOverlay(
    selectedMonth: YearMonth,
    onDismiss: () -> Unit,
    onSelect: (YearMonth) -> Unit,
) {
    var visibleYear by remember(selectedMonth) { mutableIntStateOf(selectedMonth.year) }
    val now = remember { YearMonth.now() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OverlayShape)
                    .background(BgWhite)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 18.dp, vertical = 20.dp),
            ) {
                Text(
                    text = stringResource(R.string.admin_fees_select_month),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgLight)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { visibleYear -= 1 }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.cd_prev_year),
                            tint = BrandBlack,
                        )
                    }
                    Text(
                        text = visibleYear.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = BrandBlack,
                        fontFamily = FontFamily.SansSerif,
                    )
                    IconButton(onClick = { visibleYear += 1 }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.cd_next_year),
                            tint = BrandBlack,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Month.entries.chunked(3).forEach { rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowMonths.forEach { month ->
                            val candidate = YearMonth.of(visibleYear, month)
                            val selected = candidate == selectedMonth
                            val isCurrent = candidate == now
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) BrandBlack else BgLight)
                                    .border(
                                        width = if (!selected && isCurrent) 1.dp else 0.dp,
                                        color = if (!selected && isCurrent) BorderGray else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable { onSelect(candidate) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (selected) Color.White else BrandBlack,
                                    fontFamily = FontFamily.SansSerif,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FeeStudentCard(
    row: AdminFeeLedgerRow,
    onTogglePaid: () -> Unit,
) {
    val item = row.ledger
    val paid = item.payment_status == FeePaymentStatus.PAID
    val avatarUrl = item.student.avatar_url?.takeIf { it.isNotBlank() }
    val rollLabel = stringResource(R.string.admin_fees_roll, row.rollNumber)
    AdminCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandBlack)
                    .semantics { contentDescription = rollLabel },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = row.rollNumber.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = stringResource(R.string.cd_avatar),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    error = painterResource(R.drawable.ic_avatar_placeholder),
                    placeholder = painterResource(R.drawable.ic_avatar_placeholder),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_avatar_placeholder),
                    contentDescription = stringResource(R.string.cd_avatar),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.student.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                AdminMuted(formatInrFromPaise(item.amount_paise))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(
                    if (paid) R.string.admin_fees_paid else R.string.admin_fees_unpaid,
                ),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .clip(AdminChipShape)
                    .background(if (paid) AdminPaidGreen else BrandBlack)
                    .clickable(onClick = onTogglePaid)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun FeeSettingsOverlay(
    uiState: AdminFeesUiState,
    onAmount: (String) -> Unit,
    onSaveFee: () -> Unit,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss)
                .imePadding()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OverlayShape)
                    .background(BgWhite)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.admin_fees_manage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(16.dp))
                uiState.summary?.let { summary ->
                    AdminCard {
                        Text(
                            text = "${stringResource(R.string.admin_fees_total)} ${formatInrFromPaise(summary.total_amount_paise)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BrandBlack,
                            fontFamily = FontFamily.SansSerif,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AdminMuted(
                            "${stringResource(R.string.admin_fees_paid)} ${formatInrFromPaise(summary.paid_amount_paise)} · " +
                                "${stringResource(R.string.admin_fees_pending)} ${formatInrFromPaise(summary.pending_amount_paise)}",
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AdminMuted("${summary.paid_students} / ${summary.total_students}")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (uiState.selectedClassId != null) {
                    OutlinedAuthField(
                        label = stringResource(R.string.admin_fees_set_amount),
                        value = uiState.amountRupees,
                        onValueChange = onAmount,
                        placeholder = stringResource(R.string.admin_fees_amount_hint),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PrimaryButton(
                        text = stringResource(R.string.admin_fees_save),
                        onClick = onSaveFee,
                        enabled = uiState.amountRupees.isNotBlank() && !uiState.isSaving,
                        fullyRounded = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                PrimaryButton(
                    text = stringResource(R.string.admin_fees_generate),
                    onClick = onGenerate,
                    enabled = !uiState.isSaving,
                    fullyRounded = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                uiState.infoMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            }
        }
    }
}
