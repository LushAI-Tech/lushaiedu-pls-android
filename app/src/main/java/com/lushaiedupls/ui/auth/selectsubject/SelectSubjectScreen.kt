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
import androidx.compose.runtime.remember
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
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.components.LushAiEduBrandHeader
import com.lushaiedupls.ui.auth.components.OnboardingStepHeader
import com.lushaiedupls.ui.auth.components.SelectionNavButtons
import com.lushaiedupls.ui.auth.components.SelectionTile
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme

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
    val subjectsByClass = remember(uiState.subjects) {
        uiState.subjects
            .groupBy { it.classId }
            .entries
            .map { (classId, subjects) ->
                ClassSubjectGroup(
                    classId = classId,
                    className = subjects.firstOrNull()?.className
                        ?.takeIf { it.isNotBlank() }
                        ?: classId,
                    subjects = subjects,
                )
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LushAiEduBrandHeader(logoSize = 104.dp)
        Spacer(modifier = Modifier.height(28.dp))
        OnboardingStepHeader(
            title = stringResource(R.string.select_subjects),
            step = 3,
            totalSteps = 3,
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
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    subjectsByClass.forEach { group ->
                        ClassSubjectSection(
                            group = group,
                            selectedSubjectIds = uiState.selectedSubjectIds,
                            onSubjectToggled = onSubjectToggled,
                            showClassTag = subjectsByClass.size > 1 ||
                                group.className.isNotBlank(),
                        )
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

private data class ClassSubjectGroup(
    val classId: String,
    val className: String,
    val subjects: List<SubjectChoice>,
)

@Composable
private fun ClassSubjectSection(
    group: ClassSubjectGroup,
    selectedSubjectIds: Set<String>,
    onSubjectToggled: (String) -> Unit,
    showClassTag: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showClassTag) {
            ClassCategoryTag(label = group.className)
        }
        group.subjects.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                row.forEach { subject ->
                    SelectionTile(
                        label = subject.name,
                        selected = subject.id in selectedSubjectIds,
                        onClick = { onSubjectToggled(subject.id) },
                        iconRes = TeacherUiMappers.subjectIcon(subject.name),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ClassCategoryTag(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier,
        color = BrandBlack,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        fontFamily = FontFamily.SansSerif,
    )
}

@Preview(showBackground = true)
@Composable
private fun SelectSubjectPreview() {
    LushAIEdu_PLSTheme {
        SelectSubjectScreen(
            uiState = SelectSubjectUiState(
                subjects = listOf(
                    SubjectChoice("1", "Chemistry", classId = "c1", className = "Class XI"),
                    SubjectChoice("2", "Physics", classId = "c1", className = "Class XI"),
                    SubjectChoice("3", "Mathematics", classId = "c2", className = "Class XII"),
                    SubjectChoice("4", "Biology", classId = "c2", className = "Class XII"),
                ),
                selectedSubjectIds = setOf("1"),
            ),
            onSubjectToggled = {},
            onBack = {},
            onDone = {},
        )
    }
}
