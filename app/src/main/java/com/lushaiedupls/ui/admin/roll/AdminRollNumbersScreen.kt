package com.lushaiedupls.ui.admin.roll

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.teacher.components.InstitutionSelectorDropdown
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun AdminRollNumbersRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    onOpenUnit: (TeachingUnitOut) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminRollNumbersViewModel = viewModel(
        factory = AdminRollNumbersViewModel.provideFactory(adminRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        uiState.isLoading && uiState.institutions.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.institutions.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.admin_roll_numbers_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = viewModel::refresh,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> AdminRollNumbersScreen(
            uiState = uiState,
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onSelectInstitution = viewModel::selectInstitution,
            onSelectClass = viewModel::selectClass,
            onOpenUnit = onOpenUnit,
            modifier = modifier,
        )
    }
}

@Composable
fun AdminRollNumbersScreen(
    uiState: AdminRollNumbersUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectInstitution: (String) -> Unit,
    onSelectClass: (String) -> Unit,
    onOpenUnit: (TeachingUnitOut) -> Unit,
    modifier: Modifier = Modifier,
) {
    LushPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            AdminScreenHeader(
                title = stringResource(R.string.admin_roll_numbers_title),
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(12.dp))

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
                Spacer(modifier = Modifier.height(12.dp))
            }

            when {
                uiState.classes.isEmpty() -> {
                    AdminEmptyText(stringResource(R.string.admin_roll_no_classes))
                }
                else -> {
                    AdminFilterRow(
                        labels = uiState.classes.map { it.name },
                        selectedIndex = uiState.classes
                            .indexOfFirst { it.id == uiState.selectedClassId }
                            .coerceAtLeast(0),
                        onSelect = { onSelectClass(uiState.classes[it].id) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.units.isEmpty()) {
                        AdminEmptyText(stringResource(R.string.admin_roll_no_subjects))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.units, key = { it.id }) { unit ->
                                SubjectUnitCard(unit = unit, onClick = { onOpenUnit(unit) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectUnitCard(
    unit: TeachingUnitOut,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, BorderGray.copy(alpha = 0.8f), CardShape)
            .background(BgWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = BrandBlack,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = unit.subject_name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(4.dp))
            AdminMuted(
                text = stringResource(
                    R.string.admin_roll_student_count,
                    unit.student_count,
                ),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}
