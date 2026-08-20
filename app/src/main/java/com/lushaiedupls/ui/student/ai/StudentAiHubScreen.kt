package com.lushaiedupls.ui.student.ai

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AiHubStat
import com.lushaiedupls.data.mock.AiSubjectItem
import com.lushaiedupls.data.mock.StudentMockRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.InfoMessageCard
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

private val StatShape = RoundedCornerShape(14.dp)
private val SubjectShape = RoundedCornerShape(16.dp)
private val IconShape = RoundedCornerShape(12.dp)
private val ClassChipShape = RoundedCornerShape(50)

@Composable
fun StudentAiHubRoute(
    studentRepository: StudentRepository,
    onSubjectClick: (AiSubjectItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentAiHubViewModel = viewModel(
        factory = StudentAiHubViewModel.provideFactory(studentRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudentAiHubScreen(
        uiState = uiState,
        onSubjectClick = onSubjectClick,
        modifier = modifier,
    )
}

@Composable
fun StudentAiHubScreen(
    uiState: StudentAiHubUiState,
    onSubjectClick: (AiSubjectItem) -> Unit,
    onClassSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite),
    ) {
        Text(
            text = stringResource(R.string.ai_learn_title),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 8.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        if (uiState.classOptions.isNotEmpty()) {
            AiClassSelector(
                classes = uiState.classOptions,
                selectedClass = uiState.selectedClass,
                onClassSelected = onClassSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
            )
        }
        when {
            uiState.isLoading && uiState.subjects.isEmpty() && !uiState.needsApproval -> {
                StudentPageSkeleton(
                    kind = StudentSkeletonKind.AiLearn,
                    modifier = Modifier.weight(1f),
                )
            }
            uiState.subjects.isNotEmpty() -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp, bottom = 24.dp),
                ) {
                    AiStatsRow(stats = uiState.stats)
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.subjects.forEach { subject ->
                            AiSubjectRow(
                                subject = subject,
                                onClick = { onSubjectClick(subject) },
                            )
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    InfoMessageCard(
                        title = if (uiState.needsApproval) {
                            stringResource(R.string.approval_needed_title)
                        } else {
                            stringResource(R.string.ai_learn_empty_title)
                        },
                        body = stringResource(R.string.ai_learn_empty),
                        icon = if (uiState.needsApproval) {
                            Icons.Outlined.VerifiedUser
                        } else {
                            Icons.AutoMirrored.Outlined.MenuBook
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AiClassSelector(
    classes: List<String>,
    selectedClass: String,
    onClassSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        classes.forEach { label ->
            val selected = label == selectedClass
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(ClassChipShape)
                    .background(BgWhite)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) BrandBlack else BorderGray,
                        shape = ClassChipShape,
                    )
                    .clickable { onClassSelected(label) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp,
                    color = if (selected) BrandBlack else TextSecondary,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
    }
}

@Composable
private fun AiStatsRow(stats: List<AiHubStat>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        stats.forEach { stat ->
            AiStatCard(
                stat = stat,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AiStatCard(
    stat: AiHubStat,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(84.dp)
            .clip(StatShape)
            .background(BrandBlack),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 16.dp, y = (-18).dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stat.value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stat.label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Composable
private fun AiSubjectRow(
    subject: AiSubjectItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.75f), SubjectShape)
            .clip(SubjectShape)
            .background(BgWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(IconShape)
                .background(BrandBlack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(subject.iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subject.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subject.abbreviation,
                fontSize = 12.sp,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StudentAiHubPreview() {
    val mock = StudentMockRepository()
    LushAIEdu_PLSTheme {
        StudentAiHubScreen(
            uiState = StudentAiHubUiState(
                stats = mock.aiHubStats(),
                subjects = mock.aiSubjects(),
            ),
            onSubjectClick = {},
        )
    }
}
