package com.lushaiedupls.ui.auth.selectsubject

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.components.LushAiEduBrandHeader
import com.lushaiedupls.ui.auth.components.SelectionNavButtons
import com.lushaiedupls.ui.auth.components.SelectionTile
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

@Composable
fun SelectSubjectRoute(
    userSessionStore: UserSessionStore,
    studentRepository: StudentRepository,
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelectSubjectViewModel = viewModel(
        factory = SelectSubjectViewModel.provideFactory(
            userSessionStore,
            studentRepository,
            authRepository,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.done) {
        if (uiState.done) {
            viewModel.clearDone()
            onDone()
        }
    }
    SelectSubjectScreen(
        uiState = uiState,
        onSubjectToggled = viewModel::onSubjectToggled,
        onBack = onBack,
        onDone = viewModel::submitProfile,
        modifier = modifier,
    )
}

@Composable
fun SelectSubjectScreen(
    uiState: SelectSubjectUiState,
    onSubjectToggled: (String) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LushAiEduBrandHeader(logoSize = 96.dp)
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.select_subjects),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.select_subjects_hint),
            color = TextSecondary,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BrandBlack)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    uiState.subjects.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            row.forEach { subject ->
                                SelectionTile(
                                    label = if (subject.className.isBlank()) {
                                        subject.name
                                    } else {
                                        "${subject.name}\n${subject.className}"
                                    },
                                    selected = subject.id in uiState.selectedSubjectIds,
                                    onClick = { onSubjectToggled(subject.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, color = BrandOrange, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        SelectionNavButtons(
            onBack = onBack,
            onContinue = onDone,
            continueEnabled = !uiState.isLoading &&
                !uiState.isSubmitting &&
                uiState.selectedSubjectIds.isNotEmpty(),
            continueLabel = if (uiState.isSubmitting) {
                stringResource(R.string.loading)
            } else {
                stringResource(R.string.continue_label)
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectSubjectPreview() {
    LushAIEdu_PLSTheme {
        SelectSubjectScreen(
            uiState = SelectSubjectUiState(
                subjects = listOf(
                    SubjectChoice("1", "Chemistry", classId = "c1", className = "Class IX"),
                    SubjectChoice("2", "Physics", classId = "c1", className = "Class IX"),
                ),
                selectedSubjectIds = setOf("1"),
            ),
            onSubjectToggled = {},
            onBack = {},
            onDone = {},
        )
    }
}
