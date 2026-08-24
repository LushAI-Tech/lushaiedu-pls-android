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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
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
private val HeaderBg = Color(0xFF4B5563)
private val DayColWidth = 96.dp
private val PeriodColWidth = 120.dp
private val RowHeight = 72.dp
private val HeaderHeight = 48.dp

@Composable
fun TeacherTimetableRoute(
    teacherRepository: TeacherRepository,
    editable: Boolean,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: TeacherTimetableViewModel = viewModel(
        key = if (editable) "teacher-timetable-edit" else "teacher-timetable-view",
        factory = TeacherTimetableViewModel.provideFactory(teacherRepository, editable),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
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
            onSelectClass = viewModel::selectClass,
            onSaveSlot = viewModel::saveSlot,
            onClearSlot = viewModel::clearSlot,
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
    onSelectClass: (Int) -> Unit = {},
    onSaveSlot: (timeIndex: Int, dayIndex: Int, subject: String, room: String, onDone: (Boolean) -> Unit) -> Unit = { _, _, _, _, cb -> cb(true) },
    onClearSlot: (timeIndex: Int, dayIndex: Int, onDone: (Boolean) -> Unit) -> Unit = { _, _, cb -> cb(true) },
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedClassIndex by remember(timetable.classes) {
        mutableStateOf(0)
    }
    var showReminder by remember { mutableStateOf(false) }
    var targetCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

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

        Spacer(modifier = Modifier.height(18.dp))

        if (!editable) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ReminderShape)
                    .background(BgLight)
                    .clickable { showReminder = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.teacher_reminder_card_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandBlack,
                            fontFamily = FontFamily.SansSerif,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.teacher_timetable_subtitle),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandBlack)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.teacher_set_reminder),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (editable) {
            Text(
                text = stringResource(R.string.timetable_class),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ChipShape)
                    .background(BgLight)
                    .horizontalScroll(rememberScrollState())
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                timetable.classes.forEachIndexed { index, label ->
                    val selected = index == selectedClassIndex
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(ChipShape)
                            .then(
                                if (selected) {
                                    Modifier
                                        .border(1.dp, BorderGray, ChipShape)
                                        .background(BgWhite)
                                } else {
                                    Modifier.background(Color.Transparent)
                                },
                            )
                            .clickable {
                                selectedClassIndex = index
                                onSelectClass(index)
                            }
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
            onCellTap = { timeIndex, dayIndex ->
                if (editable) {
                    targetCell = timeIndex to dayIndex
                }
            },
        )
    }

    if (showReminder) {
        SetReminderOverlay(onDismiss = { showReminder = false })
    }
    targetCell?.let { (timeIndex, dayIndex) ->
        val existingCell = timetable.cells[timeIndex to dayIndex]
        SetSessionOverlay(
            subjects = subjects,
            initialSubject = existingCell?.subject,
            initialRoom = existingCell?.detail?.takeIf { it.isNotBlank() },
            onDismiss = { targetCell = null },
            onDone = { subject, room ->
                onSaveSlot(timeIndex, dayIndex, subject, room) { ok ->
                    if (ok) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.teacher_session_saved, subject, room.ifBlank { "—" }),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
                targetCell = null
            },
            onClear = {
                onClearSlot(timeIndex, dayIndex) { ok ->
                    if (ok) {
                        Toast.makeText(context, "Slot cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                targetCell = null
            },
        )
    }
}

@Composable
private fun TeacherTimetableGrid(
    timetable: TeacherTeachingTimetable,
    editable: Boolean,
    onCellTap: (timeIndex: Int, dayIndex: Int) -> Unit,
) {
    val tableBorderColor = BorderGray
    val cellDividerColor = BorderGray
    val headerLine = Color.White.copy(alpha = 0.2f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, TableShape, clip = false)
            .clip(TableShape)
            .border(1.dp, tableBorderColor, TableShape)
            .background(BgWhite),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .background(HeaderBg)
                        .height(HeaderHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderCell(
                        text = stringResource(R.string.teacher_timetable_day_time),
                        modifier = Modifier.width(DayColWidth),
                        dividerColor = headerLine,
                        bottomDividerColor = tableBorderColor,
                        showEndDivider = true,
                    )
                    timetable.timeSlots.forEachIndexed { index, time ->
                        HeaderCell(
                            text = time,
                            modifier = Modifier.width(PeriodColWidth),
                            dividerColor = headerLine,
                            bottomDividerColor = tableBorderColor,
                            showEndDivider = index != timetable.timeSlots.lastIndex,
                            maxLines = 2,
                        )
                    }
                }

                timetable.days.forEachIndexed { dayIndex, day ->
                    val isLastRow = dayIndex == timetable.days.lastIndex
                    Row(
                        modifier = Modifier.height(RowHeight),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DayCell(
                            day = day,
                            modifier = Modifier.width(DayColWidth),
                            dividerColor = cellDividerColor,
                            showEndDivider = true,
                            showBottomDivider = !isLastRow,
                        )
                        timetable.timeSlots.indices.forEach { timeIndex ->
                            val cell = timetable.cells[timeIndex to dayIndex]
                            ScheduleCell(
                                cell = cell,
                                editable = editable,
                                onClick = { onCellTap(timeIndex, dayIndex) },
                                modifier = Modifier.width(PeriodColWidth),
                                dividerColor = cellDividerColor,
                                showEndDivider = timeIndex != timetable.timeSlots.lastIndex,
                                showBottomDivider = !isLastRow,
                            )
                        }
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
    dividerColor: Color = Color.White.copy(alpha = 0.2f),
    bottomDividerColor: Color = BorderGray,
    showEndDivider: Boolean = true,
    maxLines: Int = 1,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .drawBehind {
                val stroke = 1.dp.toPx()
                if (showEndDivider) {
                    drawLine(
                        color = dividerColor,
                        start = Offset(size.width - stroke / 2f, 0f),
                        end = Offset(size.width - stroke / 2f, size.height),
                        strokeWidth = stroke,
                    )
                }
                drawLine(
                    color = bottomDividerColor,
                    start = Offset(0f, size.height - stroke / 2f),
                    end = Offset(size.width, size.height - stroke / 2f),
                    strokeWidth = stroke,
                )
            }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun DayCell(
    day: String,
    modifier: Modifier = Modifier,
    dividerColor: Color = BorderGray,
    showEndDivider: Boolean = true,
    showBottomDivider: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFFF3F4F6))
            .drawBehind {
                val stroke = 1.dp.toPx()
                if (showEndDivider) {
                    drawLine(
                        color = dividerColor,
                        start = Offset(size.width - stroke / 2f, 0f),
                        end = Offset(size.width - stroke / 2f, size.height),
                        strokeWidth = stroke,
                    )
                }
                if (showBottomDivider) {
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, size.height - stroke / 2f),
                        end = Offset(size.width, size.height - stroke / 2f),
                        strokeWidth = stroke,
                    )
                }
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = day,
            color = BrandBlack,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ScheduleCell(
    cell: TeacherTimetableCell?,
    editable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dividerColor: Color = BorderGray,
    showEndDivider: Boolean = true,
    showBottomDivider: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (cell != null) Color(0xFFF4F5F7) else BgWhite)
            .drawBehind {
                val stroke = 1.dp.toPx()
                if (showEndDivider) {
                    drawLine(
                        color = dividerColor,
                        start = Offset(size.width - stroke / 2f, 0f),
                        end = Offset(size.width - stroke / 2f, size.height),
                        strokeWidth = stroke,
                    )
                }
                if (showBottomDivider) {
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, size.height - stroke / 2f),
                        end = Offset(size.width, size.height - stroke / 2f),
                        strokeWidth = stroke,
                    )
                }
            }
            .then(
                if (editable) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = if (cell == null) Alignment.Center else Alignment.CenterStart,
    ) {
        if (cell == null) {
            Text(
                text = stringResource(
                    if (editable) R.string.teacher_tap_to_set else R.string.teacher_timetable_empty,
                ),
                color = Color(0xFF71717A),
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .fillMaxHeight()
                        .background(BrandOrange),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = cell.subject,
                        color = BrandBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = cell.detail,
                        color = Color(0xFF71717A),
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
