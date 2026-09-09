package com.lushaiedupls.ui.admin.fees

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.FeeMonth
import com.lushaiedupls.data.remote.dto.FeeLedgerOut
import com.lushaiedupls.data.remote.dto.FeePaymentStatus
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminCard
import com.lushaiedupls.ui.admin.AdminCardShape
import com.lushaiedupls.ui.admin.AdminChipShape
import com.lushaiedupls.ui.admin.AdminDeleteConfirmDialog
import com.lushaiedupls.ui.admin.AdminDeleteRed
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminNoticeDialog
import com.lushaiedupls.ui.admin.AdminPaidGreen
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.admin.formatInrFromPaise
import com.lushaiedupls.ui.admin.formatIsoDate
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.auth.components.SecondaryButton
import com.lushaiedupls.ui.common.AnimatedFilterChipRow
import com.lushaiedupls.ui.common.FilterRowListLoading
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.teacher.components.InstitutionSelectorDropdown
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary
import coil.compose.AsyncImage
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

private val OverlayShape = RoundedCornerShape(20.dp)
private val FeesFieldShape = RoundedCornerShape(14.dp)
private val SegmentShape = RoundedCornerShape(14.dp)

@Composable
fun AdminFeesRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    onCreateClass: (institutionId: String?) -> Unit,
    onCreateSubject: (classId: String?, institutionId: String?) -> Unit,
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
        onCreateClass = { onCreateClass(uiState.selectedInstitutionId) },
        onCreateSubject = {
            onCreateSubject(uiState.selectedClassId, uiState.selectedInstitutionId)
        },
        onSelectTab = viewModel::selectTab,
        onSelectMonth = viewModel::selectMonth,
        onSelectInstitution = viewModel::selectInstitution,
        onSelectClass = viewModel::selectClass,
        onSelectSubject = viewModel::selectSubject,
        onSelectPaymentStatus = viewModel::selectPaymentStatus,
        onAmount = viewModel::onAmountChange,
        onSaveFee = viewModel::saveSubjectFee,
        onDeleteFee = viewModel::deleteSubjectFee,
        onEditFee = viewModel::startEditTemplate,
        onAddFee = viewModel::openAddTemplate,
        onClearTemplateForm = viewModel::clearTemplateForm,
        onGenerate = viewModel::generate,
        onOpenBulkDelete = viewModel::openBulkDelete,
        onCloseBulkDelete = viewModel::closeBulkDelete,
        onToggleIncludePaid = viewModel::setIncludePaidInBulkDelete,
        onConfirmBulkDelete = viewModel::confirmBulkDelete,
        onTogglePaid = viewModel::togglePaid,
        onRetry = viewModel::refresh,
        onDismissError = viewModel::clearError,
        modifier = modifier,
    )
}

@Composable
fun AdminFeesScreen(
    uiState: AdminFeesUiState,
    onBack: () -> Unit,
    onCreateClass: () -> Unit,
    onCreateSubject: () -> Unit,
    onSelectTab: (AdminFeesTab) -> Unit,
    onSelectMonth: (String) -> Unit,
    onSelectInstitution: (String) -> Unit,
    onSelectClass: (String) -> Unit,
    onSelectSubject: (String) -> Unit,
    onSelectPaymentStatus: (FeePaymentStatus?) -> Unit,
    onAmount: (String) -> Unit,
    onSaveFee: () -> Unit,
    onDeleteFee: (String) -> Unit,
    onEditFee: (com.lushaiedupls.data.remote.dto.SubjectMonthlyFeeOut) -> Unit,
    onAddFee: () -> Unit,
    onClearTemplateForm: () -> Unit,
    onGenerate: () -> Unit,
    onOpenBulkDelete: () -> Unit,
    onCloseBulkDelete: () -> Unit,
    onToggleIncludePaid: (Boolean) -> Unit,
    onConfirmBulkDelete: () -> Unit,
    onTogglePaid: (FeeLedgerOut) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.templates.isEmpty() && uiState.ledgers.isEmpty() &&
            uiState.summary == null && uiState.institutions.isEmpty() &&
            uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.summary == null &&
            uiState.ledgers.isEmpty() && uiState.templates.isEmpty() &&
            uiState.institutions.isEmpty() ->
            LoadErrorPanel(
                screenTitle = stringResource(R.string.admin_fees_title),
                message = uiState.errorMessage.orEmpty(),
                onRetry = onRetry,
                isRetrying = uiState.isLoading || uiState.isRefreshing,
                modifier = modifier,
            )
        else -> {
            var showMonthPicker by rememberSaveable { mutableStateOf(false) }
            var pendingDeleteFeeId by remember { mutableStateOf<String?>(null) }
            val showEditOverlay = uiState.editingFeeId != null &&
                uiState.selectedTab == AdminFeesTab.Templates
            val showAddOverlay = uiState.isAddingTemplate &&
                uiState.selectedTab == AdminFeesTab.Templates
            BackHandler(
                enabled = showMonthPicker || showEditOverlay || showAddOverlay ||
                    uiState.showBulkDeleteConfirm || pendingDeleteFeeId != null,
            ) {
                when {
                    pendingDeleteFeeId != null -> pendingDeleteFeeId = null
                    uiState.showBulkDeleteConfirm -> onCloseBulkDelete()
                    showEditOverlay || showAddOverlay -> onClearTemplateForm()
                    else -> showMonthPicker = false
                }
            }
            val needsClass = uiState.needsClassSetup()
            val needsSubject = uiState.needsSubjectSetup()
            LushPullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRetry,
                modifier = modifier.fillMaxSize(),
            ) {
            Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgWhite)
                    .imePadding()
                    .padding(horizontal = 20.dp),
            ) {
                AdminScreenHeader(
                    title = stringResource(R.string.admin_fees_title),
                    onBack = onBack,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FeesTabSegmentedControl(
                    selected = uiState.selectedTab,
                    onSelected = onSelectTab,
                )
                Spacer(modifier = Modifier.height(12.dp))
                FeesStatusMessages(uiState = uiState)
                if (uiState.institutions.isNotEmpty() || uiState.classes.isNotEmpty()) {
                    FeesScopeFilterPanel(
                        uiState = uiState,
                        onSelectInstitution = onSelectInstitution,
                        onSelectClass = onSelectClass,
                        onSelectSubject = onSelectSubject,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (needsClass || needsSubject) {
                    FeesSetupRequiredPanel(
                        needsClass = needsClass,
                        onCreateClass = onCreateClass,
                        onCreateSubject = onCreateSubject,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                    ) {
                        FeeMonthSelector(
                            month = uiState.month,
                            onClick = { showMonthPicker = true },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        when (uiState.selectedTab) {
                            AdminFeesTab.Templates -> FeeTemplatesTabContent(
                                uiState = uiState,
                                onAddFee = onAddFee,
                                onDeleteFee = { pendingDeleteFeeId = it },
                                onEditFee = onEditFee,
                                onGenerate = onGenerate,
                            )
                            AdminFeesTab.Ledgers -> FeeLedgersTabContent(
                                uiState = uiState,
                                onSelectPaymentStatus = onSelectPaymentStatus,
                                onTogglePaid = onTogglePaid,
                                onBulkDelete = onOpenBulkDelete,
                            )
                        }
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
            if (showEditOverlay) {
                val editingRow = uiState.templates.firstOrNull { it.fee.id == uiState.editingFeeId }
                if (editingRow != null) {
                    FeeTemplateFormOverlay(
                        title = stringResource(R.string.admin_fees_update_template),
                        saveLabel = stringResource(R.string.admin_fees_update_template_action),
                        subjectName = editingRow.fee.subject_name,
                        className = editingRow.fee.class_name,
                        month = editingRow.fee.month,
                        overlayKey = editingRow.fee.id,
                        uiState = uiState,
                        onDismiss = onClearTemplateForm,
                        onAmount = onAmount,
                        onSave = onSaveFee,
                    )
                }
            }
            if (showAddOverlay) {
                val selectedClass = uiState.classes.firstOrNull { it.id == uiState.selectedClassId }
                val selectedSubject = uiState.subjects.firstOrNull { it.id == uiState.selectedSubjectId }
                if (selectedClass != null && selectedSubject != null) {
                    FeeTemplateFormOverlay(
                        title = stringResource(R.string.admin_fees_add_template),
                        saveLabel = stringResource(R.string.admin_fees_save),
                        subjectName = selectedSubject.name,
                        className = selectedClass.name,
                        month = uiState.month,
                        overlayKey = "add",
                        uiState = uiState,
                        onDismiss = onClearTemplateForm,
                        onAmount = onAmount,
                        onSave = onSaveFee,
                    )
                }
            }
            if (uiState.showBulkDeleteConfirm) {
                FeeLedgerBulkDeleteOverlay(
                    uiState = uiState,
                    onDismiss = onCloseBulkDelete,
                    onToggleIncludePaid = onToggleIncludePaid,
                    onConfirm = onConfirmBulkDelete,
                )
            }
            uiState.errorMessage?.takeIf {
                uiState.summary != null ||
                    uiState.templates.isNotEmpty() ||
                    uiState.ledgers.isNotEmpty() ||
                    uiState.institutions.isNotEmpty() ||
                    showEditOverlay ||
                    showAddOverlay ||
                    uiState.showBulkDeleteConfirm
            }?.let { message ->
                AdminNoticeDialog(
                    message = message,
                    onDismiss = onDismissError,
                )
            }
            pendingDeleteFeeId?.let { feeId ->
                val label = uiState.templates.firstOrNull { it.fee.id == feeId }
                    ?.let { "${it.fee.subject_name} · ${it.fee.month}" }
                    ?: feeId
                AdminDeleteConfirmDialog(
                    title = stringResource(R.string.admin_delete_confirm_title),
                    message = stringResource(
                        R.string.admin_delete_confirm_named,
                        label,
                    ) + "\n\n" + stringResource(R.string.admin_delete_confirm_fee),
                    onConfirm = {
                        pendingDeleteFeeId = null
                        onDeleteFee(feeId)
                    },
                    onDismiss = { pendingDeleteFeeId = null },
                    isWorking = uiState.isSaving,
                )
            }
            }
            }
        }
    }
}

@Composable
private fun FeesTabSegmentedControl(
    selected: AdminFeesTab,
    onSelected: (AdminFeesTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SegmentShape)
            .background(BgLight)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FeesSegmentTab(
            label = stringResource(R.string.admin_fees_tab_templates),
            selected = selected == AdminFeesTab.Templates,
            onClick = { onSelected(AdminFeesTab.Templates) },
            modifier = Modifier.weight(1f),
        )
        FeesSegmentTab(
            label = stringResource(R.string.admin_fees_tab_ledgers),
            selected = selected == AdminFeesTab.Ledgers,
            onClick = { onSelected(AdminFeesTab.Ledgers) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FeesSegmentTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) {
                    Modifier
                        .border(1.dp, BrandBlack, RoundedCornerShape(12.dp))
                        .background(BgWhite)
                } else {
                    Modifier.background(Color.Transparent)
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FeesStatusMessages(uiState: AdminFeesUiState) {
    uiState.successMessage?.let { message ->
        Text(
            text = message,
            color = AdminPaidGreen,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    uiState.infoMessage?.let { message ->
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FeeTemplatesTabContent(
    uiState: AdminFeesUiState,
    onAddFee: () -> Unit,
    onDeleteFee: (String) -> Unit,
    onEditFee: (com.lushaiedupls.data.remote.dto.SubjectMonthlyFeeOut) -> Unit,
    onGenerate: () -> Unit,
) {
    if (uiState.isLoading && uiState.templates.isEmpty()) {
        FilterRowListLoading()
    } else if (uiState.templates.isEmpty()) {
        AdminEmptyText(
            text = stringResource(R.string.admin_fees_templates_empty),
            icon = Icons.Outlined.Payments,
            compact = true,
        )
    } else {
        uiState.templates.forEach { row ->
            FeeTemplateCard(
                row = row,
                onEdit = { onEditFee(row.fee) },
                onDelete = { onDeleteFee(row.fee.id) },
                enabled = !uiState.isSaving,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    val canAddFees = uiState.templates.isEmpty() &&
        uiState.selectedClassId != null &&
        uiState.selectedSubjectId != null
    if (canAddFees) {
        PrimaryButton(
            text = stringResource(R.string.admin_fees_add_action),
            onClick = onAddFee,
            enabled = !uiState.isSaving,
            fullyRounded = true,
            modifier = Modifier.fillMaxWidth(),
        )
    } else if (uiState.templates.isNotEmpty()) {
        PrimaryButton(
            text = stringResource(R.string.admin_fees_generate),
            onClick = onGenerate,
            enabled = FeeMonth.isYearMonth(uiState.month) && !uiState.isSaving,
            fullyRounded = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FeeTemplateCard(
    row: AdminFeeTemplateRow,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
) {
    val fee = row.fee
    val locked = row.lockStatus == FeeTemplateLockStatus.Locked
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AdminCardShape)
            .border(1.dp, BorderGray.copy(alpha = 0.7f), AdminCardShape)
            .background(BgWhite, AdminCardShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                Text(
                    text = fee.subject_name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(2.dp))
                AdminMuted(fee.class_name)
                AdminMuted(formatMonthLabel(fee.month))
                AdminMuted(formatInrFromPaise(fee.amount_paise))
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Top,
            ) {
                if (!locked) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onEdit,
                            enabled = enabled,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.admin_fees_edit_template),
                                tint = BrandBlack,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            enabled = enabled,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.admin_fees_delete),
                                tint = AdminDeleteRed,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                FeeTemplateStatusChip(locked = locked)
            }
        }
    }
}

@Composable
private fun FeeTemplateStatusChip(locked: Boolean) {
    Row(
        modifier = Modifier
            .clip(AdminChipShape)
            .background(if (locked) BorderGray.copy(alpha = 0.35f) else AdminPaidGreen.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (locked) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = stringResource(
                if (locked) {
                    R.string.admin_fees_status_locked
                } else {
                    R.string.admin_fees_status_editable
                },
            ),
            color = if (locked) TextSecondary else AdminPaidGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun FeeLedgersTabContent(
    uiState: AdminFeesUiState,
    onSelectPaymentStatus: (FeePaymentStatus?) -> Unit,
    onTogglePaid: (FeeLedgerOut) -> Unit,
    onBulkDelete: () -> Unit,
) {
    val allStatusLabel = stringResource(R.string.admin_fees_all_statuses)
    val statusOptions = listOf(
        allStatusLabel,
        stringResource(R.string.admin_fees_paid),
        stringResource(R.string.admin_fees_unpaid),
    )
    val statusIndex = when (uiState.selectedPaymentStatus) {
        null -> 0
        FeePaymentStatus.PAID -> 1
        FeePaymentStatus.NOT_PAID -> 2
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedFilterChipRow(
            options = statusOptions,
            selectedIndex = statusIndex,
            onSelect = { index ->
                onSelectPaymentStatus(
                    when (index) {
                        1 -> FeePaymentStatus.PAID
                        2 -> FeePaymentStatus.NOT_PAID
                        else -> null
                    },
                )
            },
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onBulkDelete,
            enabled = FeeMonth.isYearMonth(uiState.month) && !uiState.isSaving,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = AdminDeleteRed,
                contentColor = Color.White,
                disabledContainerColor = AdminDeleteRed.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.8f),
            ),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.admin_fees_bulk_delete),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    if (uiState.isLoading && uiState.ledgers.isEmpty()) {
        FilterRowListLoading()
    } else if (uiState.ledgers.isEmpty()) {
        AdminEmptyText(
            text = stringResource(R.string.admin_fees_empty),
            icon = Icons.Outlined.Payments,
            compact = true,
        )
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

private fun parseYearMonth(value: String): YearMonth =
    runCatching { YearMonth.parse(value) }.getOrNull() ?: YearMonth.now()

private fun formatMonthLabel(value: String): String {
    val month = parseYearMonth(value)
    return "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}"
}

private fun AdminFeesUiState.needsClassSetup(): Boolean =
    !isLoading &&
        classes.isEmpty() &&
        !selectedInstitutionId.isNullOrBlank() &&
        errorMessage == null

private fun AdminFeesUiState.needsSubjectSetup(): Boolean =
    selectedTab == AdminFeesTab.Templates &&
        !isLoading &&
        selectedClassId != null &&
        classes.isNotEmpty() &&
        subjects.isEmpty() &&
        errorMessage == null

@Composable
private fun FeesSetupRequiredPanel(
    needsClass: Boolean,
    onCreateClass: () -> Unit,
    onCreateSubject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = stringResource(
        if (needsClass) {
            R.string.admin_fees_class_required
        } else {
            R.string.admin_fees_subject_required
        },
    )
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandOrange.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = message,
                    tint = BrandOrange,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = BrandBlack,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = stringResource(
                    if (needsClass) {
                        R.string.admin_fees_create_class
                    } else {
                        R.string.admin_fees_create_subject
                    },
                ),
                onClick = if (needsClass) onCreateClass else onCreateSubject,
                fullyRounded = true,
                height = 40.dp,
                modifier = Modifier.widthIn(min = 160.dp),
            )
        }
    }
}

@Composable
private fun FeesScopeFilterPanel(
    uiState: AdminFeesUiState,
    onSelectInstitution: (String) -> Unit,
    onSelectClass: (String) -> Unit,
    onSelectSubject: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.institutions.isNotEmpty()) {
            InstitutionSelectorDropdown(
                label = stringResource(R.string.timetable_institution),
                institutions = uiState.institutions.map { it.name },
                selectedIndex = uiState.institutions
                    .indexOfFirst { it.id == uiState.selectedInstitutionId }
                    .coerceAtLeast(0),
                onSelect = { onSelectInstitution(uiState.institutions[it].id) },
                icon = Icons.Outlined.Apartment,
            )
        }
        if (uiState.classes.isNotEmpty()) {
            AnimatedFilterChipRow(
                label = stringResource(R.string.admin_fees_class),
                options = uiState.classes.map { it.name },
                selectedIndex = uiState.classes
                    .indexOfFirst { it.id == uiState.selectedClassId }
                    .coerceAtLeast(0),
                onSelect = { index -> onSelectClass(uiState.classes[index].id) },
            )
        }
        if (uiState.subjects.isNotEmpty()) {
            AnimatedFilterChipRow(
                label = stringResource(R.string.admin_fees_subject),
                options = uiState.subjects.map { it.name },
                selectedIndex = uiState.subjects
                    .indexOfFirst { it.id == uiState.selectedSubjectId }
                    .coerceAtLeast(0),
                onSelect = { index -> onSelectSubject(uiState.subjects[index].id) },
            )
        }
    }
}

@Composable
private fun FeeMonthSelector(
    month: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FeesFieldShape)
            .border(1.dp, BorderGray, FeesFieldShape)
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

private const val EditOverlayExitMs = 220

@Composable
private fun FeeTemplateFormOverlay(
    title: String,
    saveLabel: String,
    subjectName: String,
    className: String,
    month: String,
    overlayKey: String,
    uiState: AdminFeesUiState,
    onDismiss: () -> Unit,
    onAmount: (String) -> Unit,
    onSave: () -> Unit,
) {
    var visible by remember(overlayKey) { mutableStateOf(false) }
    var hasEntered by remember(overlayKey) { mutableStateOf(false) }

    fun requestDismiss() {
        visible = false
    }

    LaunchedEffect(overlayKey) {
        visible = true
        hasEntered = true
    }

    LaunchedEffect(uiState.successMessage, uiState.isSaving) {
        if (!uiState.isSaving &&
            uiState.successMessage == FeeMonth.TEMPLATE_SAVED_MESSAGE
        ) {
            requestDismiss()
        }
    }

    LaunchedEffect(visible, hasEntered) {
        if (hasEntered && !visible) {
            delay(EditOverlayExitMs.toLong())
            onDismiss()
        }
    }

    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val keyboardOpen = imeBottom > 0

    Dialog(
        onDismissRequest = ::requestDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(onClick = ::requestDismiss),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = if (keyboardOpen) Alignment.BottomCenter else Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(
                        initialScale = 0.88f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    ) + fadeIn(animationSpec = tween(180)),
                    exit = scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(200),
                    ) + fadeOut(animationSpec = tween(200)),
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
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = BrandBlack,
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = ::requestDismiss,
                                enabled = !uiState.isSaving,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.admin_fees_cancel),
                                    tint = BrandBlack,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = subjectName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BrandBlack,
                            fontFamily = FontFamily.SansSerif,
                        )
                        AdminMuted(className)
                        AdminMuted(formatMonthLabel(month))
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedAuthField(
                            label = stringResource(R.string.admin_fees_set_amount),
                            value = uiState.amountRupees,
                            onValueChange = onAmount,
                            placeholder = stringResource(R.string.admin_fees_amount_hint),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        uiState.errorMessage?.let { message ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = message,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            PrimaryButton(
                                text = stringResource(R.string.admin_fees_cancel),
                                onClick = ::requestDismiss,
                                enabled = !uiState.isSaving,
                                fullyRounded = true,
                                height = 44.dp,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                            )
                            PrimaryButton(
                                text = saveLabel,
                                onClick = onSave,
                                enabled = uiState.amountRupees.isNotBlank() && !uiState.isSaving,
                                fullyRounded = true,
                                height = 44.dp,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeeLedgerBulkDeleteOverlay(
    uiState: AdminFeesUiState,
    onDismiss: () -> Unit,
    onToggleIncludePaid: (Boolean) -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!uiState.isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(enabled = !uiState.isSaving, onClick = onDismiss)
                .padding(horizontal = 24.dp),
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
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Text(
                    text = stringResource(R.string.admin_fees_bulk_delete_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.admin_fees_bulk_delete_include_paid),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = BrandBlack,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = uiState.includePaidInBulkDelete,
                        onCheckedChange = onToggleIncludePaid,
                        enabled = !uiState.isSaving,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = AdminDeleteRed,
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = BorderGray.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White,
                        ),
                    )
                }
                uiState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SecondaryButton(
                        text = stringResource(R.string.admin_fees_cancel),
                        onClick = onDismiss,
                        enabled = !uiState.isSaving,
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = stringResource(R.string.admin_fees_bulk_delete_confirm),
                        onClick = onConfirm,
                        enabled = !uiState.isSaving,
                        fullyRounded = true,
                        height = 44.dp,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
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
    val paidAt = formatIsoDate(item.paid_at).takeIf { it.isNotBlank() }
    AdminCard {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                    item.class_name?.takeIf { it.isNotBlank() }?.let { AdminMuted(it) }
                    if (row.subjectLabel.isNotBlank()) {
                        AdminMuted(row.subjectLabel)
                    }
                    AdminMuted(formatMonthLabel(item.month))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatInrFromPaise(item.amount_paise),
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.admin_fees_amount_snapshot_cd),
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    paidAt?.let {
                        AdminMuted(stringResource(R.string.admin_fees_paid_at, it))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(
                        if (paid) {
                            R.string.admin_fees_mark_unpaid
                        } else {
                            R.string.admin_fees_mark_paid
                        },
                    ),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .clip(AdminChipShape)
                        .background(if (paid) AdminPaidGreen else BrandBlack)
                        .clickable(onClick = onTogglePaid)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
