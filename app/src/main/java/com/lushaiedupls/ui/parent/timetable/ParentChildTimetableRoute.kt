package com.lushaiedupls.ui.parent.timetable

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
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.student.secondary.TimetableScreen
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.TextSecondary

@Composable
fun ParentChildTimetableRoute(
    parentRepository: ParentRepository,
    studentId: String?,
    onBack: (() -> Unit)? = null,
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
                text = stringResource(R.string.parent_timetable_empty),
                color = TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
            )
        }
        return
    }
    val viewModel: ParentChildTimetableViewModel = viewModel(
        key = studentId,
        factory = ParentChildTimetableViewModel.provideFactory(parentRepository, studentId),
    )
    LaunchedEffect(studentId) { viewModel.refresh() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when {
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
