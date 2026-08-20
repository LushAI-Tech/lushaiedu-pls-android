package com.lushaiedupls.ui.teacher.secondary

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.TeacherAnnouncement
import com.lushaiedupls.data.mock.TeacherAnnouncementAudience
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.ui.common.AppBackNav
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)
private val FieldShape = RoundedCornerShape(12.dp)
private val PriorityShape = RoundedCornerShape(50)
private val SendShape = RoundedCornerShape(28.dp)
private val SelectedAudienceBg = Color(0xFFFFE8D6)

@Composable
fun TeacherNewAnnouncementRoute(
    teacherRepository: TeacherRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherNewAnnouncementViewModel = viewModel(
        factory = TeacherNewAnnouncementViewModel.provideFactory(teacherRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sentMessage = stringResource(R.string.teacher_announcement_sent)

    LaunchedEffect(uiState.sent) {
        if (uiState.sent) {
            Toast.makeText(context, sentMessage, Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    TeacherNewAnnouncementScreen(
        uiState = uiState,
        onBack = onBack,
        onToggleAudience = viewModel::toggleAudience,
        onPriority = viewModel::setPriority,
        onSubjectChange = viewModel::onSubjectChange,
        onBodyChange = viewModel::onBodyChange,
        onSend = { viewModel.send() },
        modifier = modifier,
    )
}

@Composable
fun TeacherAnnouncementsRoute(
    teacherRepository: TeacherRepository,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherAnnouncementsViewModel = viewModel(
        factory = TeacherAnnouncementsViewModel.provideFactory(teacherRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TeacherAnnouncementsScreen(
        announcements = uiState.announcements,
        onBack = onBack,
        onCreate = onCreate,
        modifier = modifier,
    )
}

@Composable
fun TeacherNewAnnouncementScreen(
    uiState: TeacherNewAnnouncementUiState,
    onBack: () -> Unit,
    onToggleAudience: (String) -> Unit,
    onPriority: (AnnouncementPriority) -> Unit,
    onSubjectChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        Text(
            text = stringResource(R.string.teacher_send_announcement_title),
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.teacher_announcement_audience),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
                    .clip(CardShape)
                    .background(BgWhite),
            ) {
                uiState.audiences.forEachIndexed { index, audience ->
                    AudienceRow(
                        audience = audience,
                        selected = audience.id in uiState.selectedAudienceIds,
                        onClick = { onToggleAudience(audience.id) },
                    )
                    if (index < uiState.audiences.lastIndex) {
                        HorizontalDivider(color = BorderGray.copy(alpha = 0.55f), thickness = 0.5.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.teacher_announcement_priority),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PriorityChip(
                    label = stringResource(R.string.teacher_priority_normal),
                    selected = uiState.priority == AnnouncementPriority.Normal,
                    onClick = { onPriority(AnnouncementPriority.Normal) },
                )
                PriorityChip(
                    label = stringResource(R.string.teacher_priority_urgent),
                    selected = uiState.priority == AnnouncementPriority.Urgent,
                    onClick = { onPriority(AnnouncementPriority.Urgent) },
                )
            }

            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.teacher_announcement_message),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
                    .background(BgWhite, CardShape)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.teacher_announcement_subject),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = BrandBlack,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AnnouncementField(
                    value = uiState.subject,
                    onValueChange = onSubjectChange,
                    placeholder = stringResource(R.string.teacher_announcement_subject_hint),
                    singleLine = true,
                    minHeight = 48.dp,
                )
                Text(
                    text = "${uiState.subjectCount}/180",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    textAlign = TextAlign.End,
                    fontSize = 12.sp,
                    color = TextSecondary,
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.teacher_announcement_body),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = BrandBlack,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AnnouncementField(
                    value = uiState.body,
                    onValueChange = onBodyChange,
                    placeholder = stringResource(R.string.teacher_announcement_body_hint),
                    singleLine = false,
                    minHeight = 140.dp,
                )
                Text(
                    text = "${uiState.bodyCount}/2000",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    textAlign = TextAlign.End,
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
                .height(54.dp)
                .clip(SendShape)
                .background(if (uiState.canSend) BrandBlack else BrandBlack.copy(alpha = 0.35f))
                .clickable(enabled = uiState.canSend, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.teacher_send_announcement),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Composable
private fun AudienceRow(
    audience: TeacherAnnouncementAudience,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) SelectedAudienceBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(
                    width = 1.5.dp,
                    color = if (selected) BrandOrange else BorderGray,
                    shape = RoundedCornerShape(6.dp),
                )
                .background(if (selected) BrandOrange else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audience.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = BrandBlack,
            )
            Text(
                text = audience.subtitle,
                fontSize = 12.sp,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun PriorityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(PriorityShape)
            .border(
                width = 1.dp,
                color = if (selected) BrandBlack else BorderGray,
                shape = PriorityShape,
            )
            .background(BgWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 14.sp,
            color = if (selected) BrandBlack else TextSecondary,
        )
    }
}

@Composable
private fun AnnouncementField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(minHeight)
            .clip(FieldShape)
            .background(BgLight)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = TextSecondary,
                fontSize = 14.sp,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            cursorBrush = SolidColor(BrandBlack),
            textStyle = TextStyle(
                color = BrandBlack,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
