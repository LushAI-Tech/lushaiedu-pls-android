package com.lushaiedupls.ui.teacher.overview

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.TeacherAttendanceBlock
import com.lushaiedupls.data.mock.TeacherClassStudentScore
import com.lushaiedupls.data.mock.TeacherMockRepository
import com.lushaiedupls.data.mock.TeacherStudent
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.AppBackNav
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.teacher.overlays.AddStudentOverlay
import com.lushaiedupls.ui.teacher.overlays.InviteParentOverlay
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextDecoration

private val CardShape = RoundedCornerShape(18.dp)
private val StudentCardShape = RoundedCornerShape(16.dp)
private val SegmentShape = RoundedCornerShape(14.dp)
private val ChooseShape = RoundedCornerShape(10.dp)
private val PillShape = RoundedCornerShape(50)
private val ExtraCardBg = Color(0xFFE8E8EA)
private val DangerRed = Color(0xFFF25F5C)

@Composable
fun TeacherClassOverviewRoute(
    groupId: String,
    teacherRepository: TeacherRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherClassOverviewViewModel = viewModel(
        factory = TeacherClassOverviewViewModel.provideFactory(teacherRepository, groupId),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    when {
        uiState.isLoading && uiState.overview == null && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.overview == null -> LoadErrorPanel(
            screenTitle = stringResource(R.string.teacher_overview_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = viewModel::refresh,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> TeacherClassOverviewScreen(
            uiState = uiState,
            onBack = onBack,
            onSectionSelected = viewModel::onSectionSelected,
            onToggleEdit = viewModel::toggleEditMode,
            onUpdateStudentRoll = viewModel::updateStudentRoll,
            onAutoAssignRolls = viewModel::autoAssignSequentialRolls,
            onApproveRollNumbers = viewModel::approveRollNumbers,
            onToggleMarkStudentDelete = viewModel::toggleMarkStudentDelete,
            onParentsLinked = viewModel::markParentsSelected,
            modifier = modifier,
        )
    }
}

@Composable
fun TeacherClassOverviewScreen(
    uiState: TeacherClassOverviewUiState,
    onBack: () -> Unit,
    onSectionSelected: (TeacherClassSection) -> Unit,
    onToggleEdit: () -> Unit = {},
    onUpdateStudentRoll: (String, String) -> Unit = { _, _ -> },
    onAutoAssignRolls: () -> Unit = {},
    onApproveRollNumbers: () -> Unit = {},
    onToggleMarkStudentDelete: (String) -> Unit = {},
    onParentsLinked: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val overview = uiState.overview ?: return
    var showAddStudent by remember { mutableStateOf(false) }
    var inviteForStudent by remember { mutableStateOf<TeacherStudent?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        AppBackNav(
            onBack = onBack,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = overview.title,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.teacher_class_overview_meta,
                overview.orgLabel,
                overview.studentCount,
            ),
            fontSize = 14.sp,
            color = TextSecondary,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(16.dp))
        ClassSegmentedControl(
            selected = uiState.section,
            onSelected = onSectionSelected,
        )
        Spacer(modifier = Modifier.height(18.dp))

        when (uiState.section) {
            TeacherClassSection.Overview -> {
                AverageAttendanceCard(
                    percent = overview.averageAttendancePercent,
                    periodLabel = overview.periodLabel,
                    sessionsLabel = overview.regularAttendance.totalSessions,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.teacher_regular_classes),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ClassAttendanceStatsRow(
                    block = overview.regularAttendance,
                    emphasized = true,
                    secondLabel = stringResource(R.string.teacher_stat_total_sessions),
                    thirdLabel = stringResource(R.string.teacher_stat_present_absent_leave),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.teacher_extra_classes),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ClassAttendanceStatsRow(
                    block = overview.extraAttendance,
                    emphasized = false,
                    secondLabel = stringResource(R.string.teacher_stat_present_absent_leave),
                    thirdLabel = stringResource(R.string.teacher_stat_total_extra_sessions),
                )
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = stringResource(R.string.teacher_student_attendance),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(12.dp))
                StudentScoresCard(scores = overview.studentScores)
            }
            TeacherClassSection.Students -> {
                StudentsInClassSection(
                    students = uiState.students,
                    isEditing = uiState.isEditing,
                    isApprovingRolls = uiState.isApprovingRolls,
                    rollDrafts = uiState.rollDrafts,
                    pendingDeleteStudentIds = uiState.pendingDeleteStudentIds,
                    onToggleEdit = onToggleEdit,
                    onUpdateStudentRoll = onUpdateStudentRoll,
                    onAutoAssignRolls = onAutoAssignRolls,
                    onApproveRollNumbers = onApproveRollNumbers,
                    onToggleMarkStudentDelete = onToggleMarkStudentDelete,
                    onAddStudent = { showAddStudent = true },
                )
            }
            TeacherClassSection.Parents -> {
                ParentsInClassSection(
                    students = uiState.students,
                    onChoose = { inviteForStudent = it },
                )
            }
        }
    }

    if (showAddStudent) {
        AddStudentOverlay(onDismiss = { showAddStudent = false })
    }
    inviteForStudent?.let { student ->
        InviteParentOverlay(
            studentName = student.name,
            onDismiss = { inviteForStudent = null },
            onLink = { onParentsLinked(student.id) },
        )
    }
}

@Composable
private fun StudentsInClassSection(
    students: List<TeacherStudent>,
    isEditing: Boolean,
    isApprovingRolls: Boolean,
    rollDrafts: Map<String, String>,
    pendingDeleteStudentIds: Set<String>,
    onToggleEdit: () -> Unit,
    onUpdateStudentRoll: (String, String) -> Unit,
    onAutoAssignRolls: () -> Unit,
    onApproveRollNumbers: () -> Unit,
    onToggleMarkStudentDelete: (String) -> Unit,
    onAddStudent: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.teacher_students_in_class),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onToggleEdit) {
            Icon(
                imageVector = if (isEditing) Icons.Outlined.Close else Icons.Outlined.Edit,
                contentDescription = stringResource(
                    if (isEditing) R.string.cd_cancel_edit_students else R.string.cd_edit_students,
                ),
                tint = if (isEditing) BrandOrange else BrandBlack,
            )
        }
        IconButton(onClick = onAddStudent) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.cd_add_student),
                tint = BrandBlack,
            )
        }
    }
    if (isEditing && students.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.teacher_auto_assign_rolls),
                color = BrandOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAutoAssignRolls)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    if (students.isEmpty()) {
        Text(
            text = stringResource(R.string.teacher_my_classes_empty),
            fontSize = 14.sp,
            color = TextSecondary,
            fontFamily = FontFamily.SansSerif,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            students.forEach { student ->
                val draft = rollDrafts[student.id] ?: student.rollNumber.toString()
                val isMarkedForDelete = student.id in pendingDeleteStudentIds
                StudentRosterCard(
                    student = student,
                    isEditing = isEditing,
                    isMarkedForDelete = isMarkedForDelete,
                    rollDraft = draft,
                    onUpdateRoll = { onUpdateStudentRoll(student.id, it) },
                    onToggleMarkDelete = { onToggleMarkStudentDelete(student.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // "Approve roll numbers" button (matching design: black pill button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(PillShape)
                .background(
                    if (isApprovingRolls) BrandBlack.copy(alpha = 0.5f) else BrandBlack,
                )
                .clickable(
                    enabled = !isApprovingRolls,
                    onClick = onApproveRollNumbers,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isApprovingRolls) {
                    stringResource(R.string.teacher_approving)
                } else {
                    stringResource(R.string.teacher_approve_roll_numbers)
                },
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Composable
private fun ParentsInClassSection(
    students: List<TeacherStudent>,
    onChoose: (TeacherStudent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        students.forEach { student ->
            val selected = student.hasParentsSelected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray.copy(alpha = 0.75f), StudentCardShape)
                    .background(BgWhite, StudentCardShape)
                    .clickable { onChoose(student) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_avatar_placeholder),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = BrandBlack,
                        fontFamily = FontFamily.SansSerif,
                    )
                    Text(
                        text = student.email,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(ChooseShape)
                        .then(
                            if (selected) {
                                Modifier
                                    .border(1.dp, BrandOrange, ChooseShape)
                                    .background(BrandOrange.copy(alpha = 0.10f))
                            } else {
                                Modifier
                                    .border(1.dp, BorderGray, ChooseShape)
                                    .background(BgWhite)
                            },
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (selected) {
                            stringResource(R.string.teacher_parent_selected).uppercase()
                        } else {
                            stringResource(R.string.teacher_parent_choose).uppercase()
                        },
                        color = if (selected) BrandOrange else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentRosterCard(
    student: TeacherStudent,
    isEditing: Boolean = false,
    isMarkedForDelete: Boolean = false,
    rollDraft: String = student.rollNumber.toString(),
    onUpdateRoll: (String) -> Unit = {},
    onToggleMarkDelete: () -> Unit = {},
) {
    val cardBorder = if (isMarkedForDelete) {
        BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f))
    } else {
        BorderStroke(1.dp, BorderGray.copy(alpha = 0.75f))
    }
    val cardBg = if (isMarkedForDelete) Color(0xFFFFF4F4) else BgWhite

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(cardBorder.width, cardBorder.brush, StudentCardShape)
            .background(cardBg, StudentCardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isEditing) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isMarkedForDelete) BorderGray else BrandBlack)
                    .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isMarkedForDelete) {
                    Text(
                        text = student.rollNumber.toString(),
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif,
                    )
                } else {
                    BasicTextField(
                        value = rollDraft,
                        onValueChange = onUpdateRoll,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(Color.White),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.SansSerif,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandBlack),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = student.rollNumber.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Image(
            painter = painterResource(R.drawable.ic_avatar_placeholder),
            contentDescription = null,
            alpha = if (isMarkedForDelete) 0.45f else 1f,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (isMarkedForDelete) TextSecondary else BrandBlack,
                textDecoration = if (isMarkedForDelete) TextDecoration.LineThrough else TextDecoration.None,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isMarkedForDelete) {
                    stringResource(R.string.teacher_marked_delete_label)
                } else {
                    student.email
                },
                fontSize = 12.sp,
                color = if (isMarkedForDelete) DangerRed else TextSecondary,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isEditing) {
            if (isMarkedForDelete) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, DangerRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .background(BgWhite)
                        .clickable(onClick = onToggleMarkDelete)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.teacher_undo_delete),
                        color = DangerRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            } else {
                IconButton(onClick = onToggleMarkDelete) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_mark_delete),
                        tint = DangerRed,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassSegmentedControl(
    selected: TeacherClassSection,
    onSelected: (TeacherClassSection) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SegmentShape)
            .background(BgLight)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ClassSegmentTab(
            label = stringResource(R.string.teacher_tab_overview),
            selected = selected == TeacherClassSection.Overview,
            onClick = { onSelected(TeacherClassSection.Overview) },
            modifier = Modifier.weight(1f),
        )
        ClassSegmentTab(
            label = stringResource(R.string.teacher_students_tab),
            selected = selected == TeacherClassSection.Students,
            onClick = { onSelected(TeacherClassSection.Students) },
            modifier = Modifier.weight(1f),
        )
        ClassSegmentTab(
            label = stringResource(R.string.teacher_parents_tab),
            selected = selected == TeacherClassSection.Parents,
            onClick = { onSelected(TeacherClassSection.Parents) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ClassSegmentTab(
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
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
            color = if (selected) BrandBlack else TextSecondary,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun AverageAttendanceCard(
    percent: Int,
    periodLabel: String,
    sessionsLabel: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(BrandBlack)
            .padding(20.dp),
    ) {
        Text(
            text = stringResource(R.string.teacher_average_attendance),
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "$percent%",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(
                R.string.teacher_class_overview_period,
                periodLabel,
                sessionsLabel,
            ),
            color = BrandOrange,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun ClassAttendanceStatsRow(
    block: TeacherAttendanceBlock,
    emphasized: Boolean,
    secondLabel: String,
    thirdLabel: String,
) {
    val cards = listOf(
        stringResource(R.string.teacher_stat_present_rate) to block.presentRate,
        secondLabel to if (emphasized) block.totalSessions else block.presentAbsentLeave,
        thirdLabel to if (emphasized) block.presentAbsentLeave else block.totalSessions,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cards.forEach { (label, value) ->
            val bg = if (emphasized) BrandBlack else ExtraCardBg
            val labelColor = if (emphasized) Color.White.copy(alpha = 0.72f) else TextSecondary
            val valueColor = if (emphasized) Color.White else BrandBlack
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .padding(horizontal = 10.dp, vertical = 12.dp)
                    .height(72.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 11.sp,
                )
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = valueColor,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StudentScoresCard(scores: List<TeacherClassStudentScore>) {
    if (scores.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
                .background(BgLight, CardShape)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.teacher_no_student_attendance),
                fontSize = 14.sp,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif,
            )
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
            .background(BgWhite, CardShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        scores.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!item.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_avatar_placeholder),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = BrandBlack,
                        fontFamily = FontFamily.SansSerif,
                    )
                    if (item.detail.isNotBlank()) {
                        Text(
                            text = item.detail,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.SansSerif,
                        )
                    }
                }
                Text(
                    text = "${item.percent}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BrandOrange,
                    fontFamily = FontFamily.SansSerif,
                )
            }
            if (index < scores.lastIndex) {
                HorizontalDivider(color = BorderGray.copy(alpha = 0.55f), thickness = 0.5.dp)
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun TeacherClassOverviewPreview() {
    LushAIEdu_PLSTheme {
        TeacherClassOverviewScreen(
            uiState = TeacherClassOverviewUiState(
                overview = TeacherMockRepository().classOverview("g1"),
                students = TeacherMockRepository().studentsInClass("g1"),
            ),
            onBack = {},
            onSectionSelected = {},
        )
    }
}
