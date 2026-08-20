package com.lushaiedupls.ui.teacher.secondary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.TeacherAnnouncement
import com.lushaiedupls.ui.common.AppBackNav
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun TeacherAnnouncementsScreen(
    announcements: List<TeacherAnnouncement>,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 96.dp),
        ) {
            AppBackNav(
                onBack = onBack,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.teacher_announcements_title),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.teacher_announcements_subtitle),
                fontSize = 14.sp,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (announcements.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, CardShape, clip = false)
                        .clip(CardShape)
                        .background(BgLight)
                        .padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.teacher_announcements_empty_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrandBlack,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.SansSerif,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.teacher_announcements_empty_body),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            } else {
                announcements.forEach { item ->
                    AnnouncementCard(item = item)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .size(56.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(BrandBlack)
                .clickable(onClick = onCreate),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.cd_new_announcement),
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun AnnouncementCard(item: TeacherAnnouncement) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
            .background(BgWhite, CardShape)
            .padding(16.dp),
    ) {
        Text(
            text = item.subject,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.body,
            fontSize = 13.sp,
            color = TextSecondary,
            fontFamily = FontFamily.SansSerif,
            maxLines = 3,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${item.audienceLabel} · ${item.priority} · ${item.sentAtLabel}",
            fontSize = 12.sp,
            color = TextSecondary,
            fontFamily = FontFamily.SansSerif,
        )
    }
}
