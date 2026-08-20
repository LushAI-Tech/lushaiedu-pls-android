package com.lushaiedupls.ui.teacher.secondary

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.TeacherTeachingTimetable
import com.lushaiedupls.data.mock.TeacherTimetableCell
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.AppBackNav
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.teacher.overlays.SetReminderOverlay
import com.lushaiedupls.ui.teacher.overlays.SetSessionOverlay
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

private val TableShape = RoundedCornerShape(14.dp)
private val ChipShape = RoundedCornerShape(12.dp)
private val ReminderShape = RoundedCornerShape(12.dp)
private val HeaderBg = BrandBlack
private val CellMinWidth = 88.dp

@Composable
fun TeacherTimetableRoute(
    teacherRepository: TeacherRepository,
    editable: Boolean,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: TeacherTimetableViewModel = viewModel(
        key = if (editable) "teacher-timetable-edit" else "teacher-timetable-view",
        factory = TeacherTimetableViewModel.provideFactory(teacherRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title = if (editable) {
        stringResource(R.string.teacher_set_timetable_title)
    } else {
        stringResource(R.string.teacher_my_timetable_title)
    }
    when {
        uiState.isLoading && uiState.timetable == null && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.Timetable, title = title, modifier = modifier)
        uiState.errorMessage != null && uiState.timetable == null -> LoadErrorPanel(
            screenTitle = title,
            message = uiState.errorMessage.orEmpty(),
            onRetry = viewModel::refresh,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> TeacherTimetableScreen(
            timetable = uiState.timetable ?: TeacherTeachingTimetable(
                classes = emptyList(),
                days = emptyList(),
                timeSlots = emptyList(),
                cells = emptyMap(),
            ),
            subjects = uiState.subjects,
            editable = editable,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

/**
 * @param editable When true, shows Set Timetable (class chips + tap-to-set).
 * When false, shows My Timetable (Set Reminder, empty slots as —).
 */
@Composable
fun TeacherTimetableScreen(
    timetable: TeacherTeachingTimetable,
    subjects: List<String> = listOf("Mathematics", "Chemistry", "Economics", "Biology"),
    editable: Boolean = false,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedClass by remember(timetable.classes) {
        mutableStateOf(timetable.classes.firstOrNull().orEmpty())
    }
    var showReminder by remember { mutableStateOf(false) }
    var showSetSession by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = if (onBack == null) 12.dp else 0.dp, bottom = 24.dp),
    ) {
        if (onBack != null) {
            AppBackNav(
                onBack = onBack,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            text = stringResource(
                if (editable) {
                    R.string.teacher_set_timetable_title
                } else {
                    R.string.teacher_my_timetable_title
                },
            ),
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.teacher_timetable_subtitle),
            fontSize = 14.sp,
            color = TextSecondary,
            fontFamily = FontFamily.SansSerif,
        )

        if (!editable) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(ReminderShape)
                    .background(BrandOrange)
                    .clickable { showReminder = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = stringResource(R.string.teacher_set_reminder),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }

        if (editable) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.teacher_attendance_class),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgLight)
                    .horizontalScroll(rememberScrollState())
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                timetable.classes.forEach { label ->
                    val selected = label == selectedClass
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(ChipShape)
                            .then(
                                if (selected) {
                                    Modifier
                                        .border(1.dp, BrandBlack, ChipShape)
                                        .background(BgWhite)
                                } else {
                                    Modifier.background(Color.Transparent)
                                },
                            )
                            .clickable { selectedClass = label }
                            .padding(horizontal = 14.dp),
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
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        TeacherTimetableGrid(
            timetable = timetable,
            editable = editable,
            onEmptyTap = { if (editable) showSetSession = true },
        )
    }

    if (showReminder) {
        SetReminderOverlay(onDismiss = { showReminder = false })
    }
    if (showSetSession) {
        SetSessionOverlay(
            subjects = subjects,
            onDismiss = { showSetSession = false },
            onDone = { subject, room ->
                Toast.makeText(
                    context,
                    context.getString(R.string.teacher_session_saved, subject, room.ifBlank { "—" }),
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}

@Composable
private fun TeacherTimetableGrid(
    timetable: TeacherTeachingTimetable,
    editable: Boolean,
    onEmptyTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, TableShape, clip = false)
            .clip(TableShape)
            .border(1.dp, BorderGray.copy(alpha = 0.7f), TableShape)
            .background(BgWhite),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            Column {
                Row(modifier = Modifier.background(HeaderBg)) {
                    HeaderCell(
                        text = stringResource(R.string.teacher_timetable_time_day),
                        modifier = Modifier.width(118.dp),
                    )
                    timetable.days.forEach { day ->
                        HeaderCell(
                            text = day,
                            modifier = Modifier.width(CellMinWidth),
                        )
                    }
                }
                timetable.timeSlots.forEachIndexed { rowIndex, time ->
                    Row {
                        TimeCell(
                            time = time,
                            modifier = Modifier.width(118.dp),
                        )
                        timetable.days.indices.forEach { dayIndex ->
                            val cell = timetable.cells[rowIndex to dayIndex]
                            ScheduleCell(
                                cell = cell,
                                editable = editable,
                                onEmptyTap = onEmptyTap,
                                modifier = Modifier.width(CellMinWidth),
                            )
                        }
                    }
                    if (rowIndex < timetable.timeSlots.lastIndex) {
                        HorizontalDivider(color = BorderGray.copy(alpha = 0.55f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun TimeCell(
    time: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(BgLight)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = time,
            color = BrandBlack,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 14.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.teacher_timetable_subject_label),
            color = BrandBlack,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun ScheduleCell(
    cell: TeacherTimetableCell?,
    editable: Boolean,
    onEmptyTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .then(
                if (editable && cell == null) {
                    Modifier.clickable(onClick = onEmptyTap)
                } else {
                    Modifier
                },
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (cell == null) {
            Text(
                text = stringResource(
                    if (editable) R.string.teacher_tap_to_set else R.string.teacher_timetable_empty,
                ),
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (editable) BgWhite else BgLight)
                    .border(1.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxSize()
                        .background(BrandOrange),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = cell.subject,
                        color = BrandBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                    )
                    Text(
                        text = cell.detail,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 11.sp,
                    )
                }
            }
        }
    }
}
