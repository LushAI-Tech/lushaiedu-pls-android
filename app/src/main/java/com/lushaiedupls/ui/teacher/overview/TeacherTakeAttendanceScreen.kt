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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.TeacherAttendanceMark
import com.lushaiedupls.data.mock.TeacherMockRepository
import com.lushaiedupls.data.mock.TeacherStudent
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.AppBackNav
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)
private val ButtonShape = RoundedCornerShape(12.dp)
private val SaveShape = RoundedCornerShape(28.dp)
private val PresentGreen = Color(0xFF36D399)
private val AbsentCoral = Color(0xFFF87272)
private val ToggleIdle = Color(0xFFE5E7EB)

@Composable
fun TeacherTakeAttendanceRoute(
    unitId: String,
    dateLabel: String,
    teacherRepository: TeacherRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherTakeAttendanceViewModel = viewModel(
        factory = TeacherTakeAttendanceViewModel.provideFactory(
            teacherRepository = teacherRepository,
            unitId = unitId,
            dateLabel = dateLabel,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.teacher_attendance_saved)

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    when {
        uiState.isLoading && uiState.session == null && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.session == null -> LoadErrorPanel(
            screenTitle = stringResource(R.string.teacher_overview_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = viewModel::refresh,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> TeacherTakeAttendanceScreen(
            uiState = uiState,
            onBack = onBack,
            onMark = viewModel::setMark,
            onSave = viewModel::saveAttendance,
            modifier = modifier,
        )
    }
}

@Composable
fun TeacherTakeAttendanceScreen(
    uiState: TeacherTakeAttendanceUiState,
    onBack: () -> Unit,
    onMark: (String, TeacherAttendanceMark) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = uiState.session ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .padding(horizontal = 20.dp),
    ) {
        AppBackNav(
            onBack = onBack,
            modifier = Modifier.padding(top = 4.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.teacher_take_attendance_title,
                        session.dateLabel,
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.teacher_take_attendance_meta,
                        session.subjectLabel,
                        session.timeLabel,
                    ),
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.SansSerif,
                )
            }
            Text(
                text = stringResource(
                    R.string.teacher_attendance_selected,
                    uiState.selectedCount,
                    uiState.totalCount,
                ),
                fontSize = 13.sp,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            session.students.forEach { row ->
                val mark = uiState.marks[row.student.id] ?: TeacherAttendanceMark.None
                AttendanceStudentCard(
                    student = row.student,
                    mark = mark,
                    onPresent = {
                        onMark(row.student.id, TeacherAttendanceMark.Present)
                    },
                    onAbsent = {
                        onMark(row.student.id, TeacherAttendanceMark.Absent)
                    },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
                .height(54.dp)
                .clip(SaveShape)
                .background(BrandBlack)
                .clickable(onClick = onSave),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.teacher_save_attendance),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Composable
private fun AttendanceStudentCard(
    student: TeacherStudent,
    mark: TeacherAttendanceMark,
    onPresent: () -> Unit,
    onAbsent: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
            .background(BgWhite, CardShape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AttendanceToggle(
                label = stringResource(R.string.teacher_mark_present),
                selected = mark == TeacherAttendanceMark.Present,
                selectedColor = PresentGreen,
                onClick = onPresent,
                modifier = Modifier.weight(1f),
            )
            AttendanceToggle(
                label = stringResource(R.string.teacher_mark_absent),
                selected = mark == TeacherAttendanceMark.Absent,
                selectedColor = AbsentCoral,
                onClick = onAbsent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AttendanceToggle(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(ButtonShape)
            .background(if (selected) selectedColor else ToggleIdle)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else BrandBlack,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun TeacherTakeAttendancePreview() {
    val session = TeacherMockRepository().attendanceSession()
    LushAIEdu_PLSTheme {
        TeacherTakeAttendanceScreen(
            uiState = TeacherTakeAttendanceUiState(
                session = session,
                marks = session.students.associate { it.student.id to it.mark },
            ),
            onBack = {},
            onMark = { _, _ -> },
            onSave = {},
        )
    }
}
