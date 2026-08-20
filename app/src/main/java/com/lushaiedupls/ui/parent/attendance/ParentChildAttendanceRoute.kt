package com.lushaiedupls.ui.parent.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.ui.student.attendance.StudentAttendanceScreen
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.TextSecondary

@Composable
fun ParentChildAttendanceRoute(
    parentRepository: ParentRepository,
    studentId: String?,
    modifier: Modifier = Modifier,
) {
    if (studentId.isNullOrBlank()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.parent_attendance_empty),
                color = TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
            )
        }
        return
    }
    val viewModel: ParentChildAttendanceViewModel = viewModel(
        key = studentId,
        factory = ParentChildAttendanceViewModel.provideFactory(parentRepository, studentId),
    )
    LaunchedEffect(studentId) { viewModel.refresh() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudentAttendanceScreen(
        uiState = uiState,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onThisMonth = viewModel::goToThisMonth,
        onSelectDay = viewModel::selectDay,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}
