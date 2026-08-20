package com.lushaiedupls.ui.teacher.groups

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import com.lushaiedupls.data.mock.TeacherGroup
import com.lushaiedupls.data.mock.TeacherMockRepository
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.auth.components.LushAiEduWordmark
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun TeacherMyGroupsRoute(
    teacherRepository: TeacherRepository,
    onGroupClick: (TeacherGroup) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TeacherMyGroupsViewModel = viewModel(
        factory = TeacherMyGroupsViewModel.provideFactory(teacherRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        uiState.isLoading && uiState.groups.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.groups.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.teacher_my_classes_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = viewModel::refresh,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> TeacherMyGroupsScreen(
            uiState = uiState,
            onGroupClick = onGroupClick,
            modifier = modifier,
        )
    }
}

@Composable
fun TeacherMyGroupsScreen(
    uiState: TeacherMyGroupsUiState,
    onGroupClick: (TeacherGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.teacher_my_classes_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.6.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.teacher_my_classes_subtitle),
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.SansSerif,
                )
            }
            LushAiEduWordmark(fontSizeSp = 16)
        }

        Spacer(modifier = Modifier.height(22.dp))

        if (uiState.groups.isEmpty()) {
            Text(
                text = stringResource(R.string.teacher_my_classes_empty),
                color = TextSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.groups, key = { it.id }) { group ->
                    GroupCard(
                        group = group,
                        onClick = { onGroupClick(group) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: TeacherGroup,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, BorderGray.copy(alpha = 0.8f), CardShape)
            .background(BgWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandBlack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(group.subjectIconRes),
                contentDescription = group.subjectName.ifBlank { "Subject" },
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(Color.White),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.teacher_group_meta,
                    group.code,
                    group.status,
                ),
                fontSize = 13.sp,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TeacherMyGroupsPreview() {
    LushAIEdu_PLSTheme {
        TeacherMyGroupsScreen(
            uiState = TeacherMyGroupsUiState(
                groups = TeacherMockRepository().groups(),
            ),
            onGroupClick = {},
        )
    }
}
