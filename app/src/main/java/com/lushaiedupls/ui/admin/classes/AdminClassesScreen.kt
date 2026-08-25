package com.lushaiedupls.ui.admin.classes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.SubjectOut
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminCard
import com.lushaiedupls.ui.admin.AdminDeleteRed
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.admin.AdminLeadingIcon
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.admin.AdminSubjectLeadingIcon
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

@Composable
fun AdminClassesRoute(
    adminRepository: AdminRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminClassesViewModel = viewModel(
        factory = AdminClassesViewModel.provideFactory(adminRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        if (!uiState.composingClass && !uiState.composingSubject) {
            viewModel.refresh()
        }
        onPauseOrDispose { }
    }
    val overlayOpen = uiState.composingClass ||
        uiState.composingSubject ||
        uiState.selectedClass != null
    if (overlayOpen) {
        BackHandler {
            when {
                uiState.composingClass || uiState.composingSubject -> viewModel.cancelCompose()
                else -> viewModel.backToClasses()
            }
        }
    }
    AdminClassesScreen(
        uiState = uiState,
        onSelectClass = viewModel::selectClass,
        onBackToClasses = viewModel::backToClasses,
        onStartCreateClass = viewModel::startCreateClass,
        onStartEditClass = viewModel::startEditClass,
        onStartCreateSubject = viewModel::startCreateSubject,
        onStartEditSubject = viewModel::startEditSubject,
        onCancel = viewModel::cancelCompose,
        onName = viewModel::onNameChange,
        onSortOrder = viewModel::onSortOrderChange,
        onSaveClass = viewModel::saveClass,
        onToggleActive = viewModel::toggleFormActive,
        onDeleteClass = viewModel::deleteClass,
        onSaveSubject = viewModel::saveSubject,
        onDeleteSubject = viewModel::deleteSubject,
        onSelectBoard = viewModel::selectBoard,
        onSelectGrade = viewModel::selectGrade,
        onBindStem = viewModel::bindStem,
        onUnbindStem = viewModel::unbindStem,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun AdminClassesScreen(
    uiState: AdminClassesUiState,
    onSelectClass: (ClassOut) -> Unit,
    onBackToClasses: () -> Unit,
    onStartCreateClass: () -> Unit,
    onStartEditClass: (ClassOut) -> Unit,
    onStartCreateSubject: () -> Unit,
    onStartEditSubject: (SubjectOut) -> Unit,
    onCancel: () -> Unit,
    onName: (String) -> Unit,
    onSortOrder: (String) -> Unit,
    onSaveClass: () -> Unit,
    onToggleActive: () -> Unit,
    onDeleteClass: (ClassOut) -> Unit,
    onSaveSubject: () -> Unit,
    onDeleteSubject: (SubjectOut) -> Unit,
    onSelectBoard: (String) -> Unit,
    onSelectGrade: (String) -> Unit,
    onBindStem: (String) -> Unit,
    onUnbindStem: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.classes.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.classes.isEmpty() && uiState.selectedClass == null ->
            LoadErrorPanel(
                screenTitle = stringResource(R.string.admin_classes_title),
                message = uiState.errorMessage.orEmpty(),
                onRetry = onRetry,
                isRetrying = uiState.isLoading,
                modifier = modifier,
            )
        else -> {
            val selectedClass = uiState.selectedClass
            val composing = uiState.composingClass || uiState.composingSubject
            var managingClasses by rememberSaveable { mutableStateOf(false) }
            var managingSubjects by rememberSaveable { mutableStateOf(false) }
            val managing = if (selectedClass != null) managingSubjects else managingClasses
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(BgWhite)
                    .imePadding(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = if (composing) 24.dp else 88.dp),
                ) {
                    val title = when {
                        uiState.composingClass && uiState.editingClassId == null ->
                            stringResource(R.string.admin_add_class)
                        uiState.composingClass -> stringResource(R.string.admin_edit_class)
                        uiState.composingSubject && uiState.editingSubjectId == null ->
                            stringResource(R.string.admin_add_subject)
                        uiState.composingSubject -> stringResource(R.string.admin_edit_subject)
                        selectedClass != null -> selectedClass.name
                        else -> stringResource(R.string.admin_classes_title)
                    }
                    AdminScreenHeader(
                        title = title,
                        onBack = when {
                            composing -> onCancel
                            selectedClass != null -> onBackToClasses
                            else -> null
                        },
                        actions = if (!composing) {
                            {
                                IconButton(
                                    onClick = {
                                        if (selectedClass != null) {
                                            managingSubjects = !managingSubjects
                                        } else {
                                            managingClasses = !managingClasses
                                        }
                                    },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = stringResource(
                                            if (selectedClass != null) {
                                                R.string.admin_manage_subjects
                                            } else {
                                                R.string.admin_manage_classes
                                            },
                                        ),
                                        tint = if (managing) BrandOrange else BrandBlack,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                    uiState.errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    when {
                        uiState.composingClass -> ClassForm(
                            uiState = uiState,
                            onName = onName,
                            onSortOrder = onSortOrder,
                            onToggleActive = onToggleActive,
                            onSaveClass = onSaveClass,
                        )
                        uiState.composingSubject -> SubjectForm(
                            uiState = uiState,
                            onName = onName,
                            onSortOrder = onSortOrder,
                            onToggleActive = onToggleActive,
                            onSaveSubject = onSaveSubject,
                            onSelectBoard = onSelectBoard,
                            onSelectGrade = onSelectGrade,
                            onBindStem = onBindStem,
                            onUnbindStem = onUnbindStem,
                        )
                        selectedClass != null -> SubjectList(
                            uiState = uiState,
                            managing = managing,
                            onStartEditSubject = onStartEditSubject,
                            onDeleteSubject = onDeleteSubject,
                        )
                        else -> ClassList(
                            uiState = uiState,
                            managing = managing,
                            onSelectClass = onSelectClass,
                            onStartEditClass = onStartEditClass,
                            onDeleteClass = onDeleteClass,
                        )
                    }
                }
                if (!composing) {
                    FloatingActionButton(
                        onClick = if (selectedClass != null) onStartCreateSubject else onStartCreateClass,
                        shape = CircleShape,
                        containerColor = BrandBlack,
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(
                                if (selectedClass != null) {
                                    R.string.admin_add_subject
                                } else {
                                    R.string.admin_add_class
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassList(
    uiState: AdminClassesUiState,
    managing: Boolean,
    onSelectClass: (ClassOut) -> Unit,
    onStartEditClass: (ClassOut) -> Unit,
    onDeleteClass: (ClassOut) -> Unit,
) {
    if (uiState.classes.isEmpty()) {
        AdminEmptyText(stringResource(R.string.admin_classes_empty))
    } else {
        uiState.classes.forEach { item ->
            AdminCard(onClick = { onSelectClass(item) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AdminLeadingIcon(
                        icon = Icons.Outlined.School,
                        background = BrandBlack,
                        tint = Color.White,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrandBlack,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (managing) {
                        EditDeleteIcons(
                            editDescription = stringResource(R.string.admin_edit_class),
                            deleteDescription = stringResource(R.string.admin_delete_class),
                            onEdit = { onStartEditClass(item) },
                            onDelete = { onDeleteClass(item) },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SubjectList(
    uiState: AdminClassesUiState,
    managing: Boolean,
    onStartEditSubject: (SubjectOut) -> Unit,
    onDeleteSubject: (SubjectOut) -> Unit,
) {
    Text(
        text = stringResource(R.string.admin_subjects_title),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = BrandBlack,
        fontFamily = FontFamily.SansSerif,
    )
    Spacer(modifier = Modifier.height(8.dp))
    if (uiState.subjects.isEmpty() && !uiState.isLoadingSubjects) {
        AdminEmptyText(stringResource(R.string.admin_subjects_empty))
    }
    uiState.subjects.forEach { item ->
        AdminCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AdminSubjectLeadingIcon(name = item.name, code = item.code)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BrandBlack,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (managing) {
                            EditDeleteIcons(
                                editDescription = stringResource(R.string.admin_edit_subject),
                                deleteDescription = stringResource(R.string.admin_delete_subject),
                                onEdit = { onStartEditSubject(item) },
                                onDelete = { onDeleteSubject(item) },
                            )
                        }
                    }
                    val boundLabel = item.stem_subject_id?.let { uiState.stemLabels[it] }
                    val isBound = boundLabel != null || item.ai_enabled || item.stem_subject_id != null
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            boundLabel != null -> stringResource(R.string.admin_ai_bound, boundLabel)
                            isBound -> stringResource(R.string.admin_ai_on)
                            else -> stringResource(R.string.admin_ai_off)
                        },
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun StemBindingPanel(
    uiState: AdminClassesUiState,
    selectedStemId: String?,
    onSelectBoard: (String) -> Unit,
    onSelectGrade: (String) -> Unit,
    onBindStem: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.admin_stem_board),
        color = TextSecondary,
        fontSize = 13.sp,
        fontFamily = FontFamily.SansSerif,
    )
    Spacer(modifier = Modifier.height(6.dp))
    if (uiState.stemBoards.isEmpty()) {
        AdminMuted(stringResource(R.string.admin_stem_empty))
    } else {
        AdminFilterRow(
            labels = uiState.stemBoards.map { it.name },
            selectedIndex = uiState.stemBoards.indexOfFirst { it.id == uiState.selectedBoardId },
            onSelect = { onSelectBoard(uiState.stemBoards[it].id) },
        )
    }
    if (uiState.stemGrades.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.admin_stem_grade),
            color = TextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(6.dp))
        AdminFilterRow(
            labels = uiState.stemGrades.map { it.name },
            selectedIndex = uiState.stemGrades.indexOfFirst { it.id == uiState.selectedGradeId },
            onSelect = { onSelectGrade(uiState.stemGrades[it].id) },
        )
    }
    if (uiState.stemSubjects.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.admin_stem_subject),
            color = TextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(6.dp))
        AdminFilterRow(
            labels = uiState.stemSubjects.map { it.name },
            selectedIndex = uiState.stemSubjects.indexOfFirst { it.id == selectedStemId },
            onSelect = { onBindStem(uiState.stemSubjects[it].id) },
        )
    }
}

@Composable
private fun ClassForm(
    uiState: AdminClassesUiState,
    onName: (String) -> Unit,
    onSortOrder: (String) -> Unit,
    onToggleActive: () -> Unit,
    onSaveClass: () -> Unit,
) {
    OutlinedAuthField(
        label = stringResource(R.string.admin_class_name),
        value = uiState.name,
        onValueChange = onName,
        placeholder = stringResource(R.string.admin_class_name_hint),
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedAuthField(
        label = stringResource(R.string.admin_sort_order),
        value = uiState.sortOrder,
        onValueChange = onSortOrder,
        placeholder = stringResource(R.string.admin_sort_order_hint),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    if (uiState.editingClassId != null) {
        Spacer(modifier = Modifier.height(16.dp))
        ActiveToggleRow(isActive = uiState.isActive, onToggle = onToggleActive)
    }
    Spacer(modifier = Modifier.height(16.dp))
    PrimaryButton(
        text = stringResource(R.string.admin_save_class),
        onClick = onSaveClass,
        enabled = uiState.name.isNotBlank() && !uiState.isSaving,
        fullyRounded = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SubjectForm(
    uiState: AdminClassesUiState,
    onName: (String) -> Unit,
    onSortOrder: (String) -> Unit,
    onToggleActive: () -> Unit,
    onSaveSubject: () -> Unit,
    onSelectBoard: (String) -> Unit,
    onSelectGrade: (String) -> Unit,
    onBindStem: (String) -> Unit,
    onUnbindStem: () -> Unit,
) {
    OutlinedAuthField(
        label = stringResource(R.string.admin_subject_name),
        value = uiState.name,
        onValueChange = onName,
        placeholder = stringResource(R.string.admin_subject_name_hint),
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedAuthField(
        label = stringResource(R.string.admin_sort_order),
        value = uiState.sortOrder,
        onValueChange = onSortOrder,
        placeholder = stringResource(R.string.admin_sort_order_hint),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    if (uiState.editingSubjectId != null) {
        Spacer(modifier = Modifier.height(16.dp))
        ActiveToggleRow(isActive = uiState.isActive, onToggle = onToggleActive)
    }
    Spacer(modifier = Modifier.height(16.dp))
    val alreadyBound = uiState.editingSubjectId != null &&
        uiState.subjects.any {
            it.id == uiState.editingSubjectId && (it.stem_subject_id != null || it.ai_enabled)
        }
    val boundLabel = uiState.formStemSubjectId?.let { uiState.stemLabels[it] }
        ?: uiState.editingSubjectId?.let { id ->
            uiState.subjects.firstOrNull { it.id == id }?.stem_subject_id
                ?.let { uiState.stemLabels[it] }
        }
    Text(
        text = stringResource(R.string.admin_stem_optional),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = BrandOrange,
        fontFamily = FontFamily.SansSerif,
    )
    if (boundLabel != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.admin_ai_bound, boundLabel),
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
        )
    }
    if (!alreadyBound) {
        Spacer(modifier = Modifier.height(8.dp))
        StemBindingPanel(
            uiState = uiState,
            selectedStemId = uiState.formStemSubjectId,
            onSelectBoard = onSelectBoard,
            onSelectGrade = onSelectGrade,
            onBindStem = onBindStem,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    if (alreadyBound) {
        PrimaryButton(
            text = stringResource(R.string.admin_unbind_subject),
            onClick = onUnbindStem,
            enabled = !uiState.isSaving,
            fullyRounded = true,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        PrimaryButton(
            text = stringResource(R.string.admin_save_subject),
            onClick = onSaveSubject,
            enabled = uiState.name.isNotBlank() && !uiState.isSaving,
            fullyRounded = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ActiveToggleRow(
    isActive: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                if (isActive) R.string.admin_active else R.string.admin_inactive,
            ),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = isActive,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = BrandOrange,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = BorderGray.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.White,
            ),
        )
    }
}

@Composable
private fun EditDeleteIcons(
    editDescription: String,
    deleteDescription: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    IconButton(
        onClick = onEdit,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = editDescription,
            tint = BrandBlack,
            modifier = Modifier.size(20.dp),
        )
    }
    IconButton(
        onClick = onDelete,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = deleteDescription,
            tint = AdminDeleteRed,
            modifier = Modifier.size(22.dp),
        )
    }
}
