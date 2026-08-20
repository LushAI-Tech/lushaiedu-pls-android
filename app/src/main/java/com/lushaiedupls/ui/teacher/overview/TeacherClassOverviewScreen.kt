package com.lushaiedupls.ui.teacher.overview

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

private val CardShape = RoundedCornerShape(18.dp)
private val StudentCardShape = RoundedCornerShape(16.dp)
private val SegmentShape = RoundedCornerShape(14.dp)
private val ChooseShape = RoundedCornerShape(10.dp)
private val ExtraCardBg = Color(0xFFE8E8EA)

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
                    onAddStudent = { showAddStudent = true },
                )
            }
            TeacherClassSection.Parents -> {
                ParentsInClassSection(
                    students = uiState.students,
                    onChoose = { inviteForStudent = it },
                    onOpenStudent = { inviteForStudent = it },
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
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.cd_edit_students),
                tint = BrandBlack,
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
                StudentRosterCard(student = student)
            }
        }
    }
}

@Composable
private fun ParentsInClassSection(
    students: List<TeacherStudent>,
    onChoose: (TeacherStudent) -> Unit,
    onOpenStudent: (TeacherStudent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        students.forEach { student ->
            val selected = student.hasParentsSelected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray.copy(alpha = 0.75f), StudentCardShape)
                    .background(BgWhite, StudentCardShape)
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
                    )
                    Text(
                        text = student.email,
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(ChooseShape)
                        .then(
                            if (selected) {
                                Modifier.background(BrandOrange)
                            } else {
                                Modifier
                                    .border(1.dp, BorderGray, ChooseShape)
                                    .background(BgWhite)
                            },
                        )
                        .clickable {
                            if (!selected) onChoose(student)
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (selected) {
                            stringResource(R.string.teacher_parent_selected)
                        } else {
                            stringResource(R.string.teacher_parent_choose)
                        },
                        color = if (selected) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onOpenStudent(student) }) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = stringResource(R.string.cd_invite_parent),
                        tint = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentRosterCard(student: TeacherStudent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.75f), StudentCardShape)
            .background(BgWhite, StudentCardShape)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Spacer(modifier = Modifier.width(12.dp))
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = student.email,
                fontSize = 12.sp,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif,
            )
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
