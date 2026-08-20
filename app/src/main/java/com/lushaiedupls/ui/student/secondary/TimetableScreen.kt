package com.lushaiedupls.ui.student.secondary

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.WeeklyTimetable
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.ApprovalNeededPanel
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary

private val TableShape = RoundedCornerShape(14.dp)
private val ChipBarShape = RoundedCornerShape(14.dp)
private val ChipShape = RoundedCornerShape(50)
private val HeaderBg = Color(0xFF4B5563)
private val DayColWidth = 92.dp
private val PeriodColWidth = 118.dp
private val RowHeight = 48.dp
private val HeaderHeight = 52.dp

@Composable
fun TimetableRoute(
    studentRepository: StudentRepository,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: TimetableViewModel = viewModel(
        factory = TimetableViewModel.provideFactory(studentRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        uiState.needsApproval -> ApprovalNeededPanel(
            screenTitle = stringResource(R.string.timetable_title),
            featureLabel = stringResource(R.string.approval_needed_feature_timetable),
            modifier = modifier,
        )
        uiState.timetable == null && uiState.isLoading -> StudentPageSkeleton(
            kind = StudentSkeletonKind.Timetable,
            title = stringResource(R.string.timetable_title),
            modifier = modifier,
        )
        uiState.timetable == null -> LoadErrorPanel(
            screenTitle = stringResource(R.string.timetable_title),
            message = uiState.errorMessage.orEmpty()
                .ifBlank { stringResource(R.string.load_error_title) },
            onRetry = viewModel::refresh,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> {
            val timetable = uiState.timetable ?: return
            TimetableScreen(
                timetable = timetable,
                onBack = onBack,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun TimetableScreen(
    timetable: WeeklyTimetable,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val subjects = timetable.subjects
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull().orEmpty()) }
    LaunchedEffect(subjects) {
        if (selectedSubject.isBlank() || selectedSubject !in subjects) {
            selectedSubject = subjects.firstOrNull().orEmpty()
        }
    }
    val rows = timetable.cellsBySubject[selectedSubject].orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = BrandBlack,
                    )
                }
            }
            Text(
                text = stringResource(R.string.timetable_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
        }

        Text(
            text = stringResource(R.string.timetable_class),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(10.dp))
        SubjectChipBar(
            subjects = subjects,
            selectedSubject = selectedSubject,
            onSubjectSelected = { selectedSubject = it },
        )

        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.timetable_weekly),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (timetable.days.isEmpty() || timetable.timeSlots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(1.dp, BorderGray, TableShape)
                    .clip(TableShape)
                    .background(BgWhite),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No timetable available",
                    color = TextSecondary,
                    fontSize = 14.sp,
                )
            }
        } else {
            WeeklyTimetableTable(
                days = timetable.days,
                timeSlots = timetable.timeSlots,
                rows = rows,
            )
        }
    }
}

@Composable
private fun SubjectChipBar(
    subjects: List<String>,
    selectedSubject: String,
    onSubjectSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ChipBarShape)
            .background(BgLight)
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        subjects.forEach { subject ->
            val selected = subject == selectedSubject
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .widthIn(min = 72.dp)
                    .then(
                        if (selected) {
                            Modifier
                                .shadow(2.dp, ChipShape, clip = false)
                                .clip(ChipShape)
                                .background(BgWhite)
                        } else {
                            Modifier.clip(ChipShape)
                        },
                    )
                    .clickable { onSubjectSelected(subject) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = subject,
                    color = if (selected) BrandBlack else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WeeklyTimetableTable(
    days: List<String>,
    timeSlots: List<String>,
    rows: List<List<String>>,
) {
    val hScroll = rememberScrollState()
    val gridLine = BorderGray
    val headerLine = Color.White.copy(alpha = 0.35f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, TableShape, clip = false)
            .clip(TableShape)
            .border(1.dp, gridLine, TableShape)
            .background(BgWhite),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(hScroll),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .background(HeaderBg)
                        .height(HeaderHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GridHeaderCell(
                        text = stringResource(R.string.timetable_day_time),
                        modifier = Modifier.width(DayColWidth),
                        dividerColor = headerLine,
                        showEndDivider = true,
                    )
                    timeSlots.forEachIndexed { index, slot ->
                        GridHeaderCell(
                            text = slot,
                            modifier = Modifier.width(PeriodColWidth),
                            dividerColor = headerLine,
                            showEndDivider = index != timeSlots.lastIndex,
                            maxLines = 2,
                        )
                    }
                }
                HorizontalDivider(color = gridLine, thickness = 1.dp)

                days.forEachIndexed { dayIndex, day ->
                    val cells = rows.getOrNull(dayIndex).orEmpty()
                    Row(
                        modifier = Modifier.height(RowHeight),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GridBodyCell(
                            text = day,
                            modifier = Modifier.width(DayColWidth),
                            background = BgLight,
                            dividerColor = gridLine,
                            showEndDivider = true,
                            contentAlignment = Alignment.CenterStart,
                            horizontalPadding = 10.dp,
                        )
                        timeSlots.indices.forEach { col ->
                            GridBodyCell(
                                text = cells.getOrNull(col) ?: "Off",
                                modifier = Modifier.width(PeriodColWidth),
                                background = BgWhite,
                                dividerColor = gridLine,
                                showEndDivider = col != timeSlots.lastIndex,
                                contentAlignment = Alignment.Center,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    if (dayIndex != days.lastIndex) {
                        HorizontalDivider(color = gridLine, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GridHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    dividerColor: Color,
    showEndDivider: Boolean,
    maxLines: Int = 1,
) {
    Box(
        modifier = modifier
            .height(HeaderHeight)
            .drawBehind {
                if (showEndDivider) {
                    val stroke = 1.dp.toPx()
                    drawLine(
                        color = dividerColor,
                        start = Offset(size.width - stroke / 2f, 0f),
                        end = Offset(size.width - stroke / 2f, size.height),
                        strokeWidth = stroke,
                    )
                }
            }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = if (maxLines > 1) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 13.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GridBodyCell(
    text: String,
    modifier: Modifier = Modifier,
    background: Color,
    dividerColor: Color,
    showEndDivider: Boolean,
    contentAlignment: Alignment,
    horizontalPadding: Dp = 4.dp,
    textAlign: TextAlign = TextAlign.Start,
) {
    Box(
        modifier = modifier
            .height(RowHeight)
            .background(background)
            .drawBehind {
                if (showEndDivider) {
                    val stroke = 1.dp.toPx()
                    drawLine(
                        color = dividerColor,
                        start = Offset(size.width - stroke / 2f, 0f),
                        end = Offset(size.width - stroke / 2f, size.height),
                        strokeWidth = stroke,
                    )
                }
            }
            .padding(horizontal = horizontalPadding),
        contentAlignment = contentAlignment,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
            textAlign = textAlign,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = if (textAlign == TextAlign.Center) Modifier.fillMaxWidth() else Modifier,
        )
    }
}
