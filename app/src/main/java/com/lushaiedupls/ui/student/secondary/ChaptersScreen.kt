package com.lushaiedupls.ui.student.secondary

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.ChapterItem
import com.lushaiedupls.data.mock.SubjectChapterStats
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun ChaptersScreen(
    stats: SubjectChapterStats,
    chapters: List<ChapterItem>,
    onBack: () -> Unit,
    onChapterClick: (ChapterItem) -> Unit = {},
    title: String = "",
    modifier: Modifier = Modifier,
) {
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
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = BrandBlack,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = title.ifBlank { stringResource(R.string.chapters_subject_title) },
                modifier = Modifier.align(Alignment.Center),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandBlack,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChapterStatCard(
                value = stats.mastery,
                label = stringResource(R.string.chapters_mastery),
                icon = Icons.Outlined.Timer,
                emphasized = true,
                modifier = Modifier.weight(1f),
            )
            ChapterStatCard(
                value = stats.reading,
                label = stringResource(R.string.chapters_reading),
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                emphasized = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChapterStatCard(
                value = stats.quizCount,
                label = stringResource(R.string.chapters_quiz),
                icon = Icons.Outlined.Quiz,
                emphasized = false,
                modifier = Modifier.weight(1f),
            )
            ChapterStatCard(
                value = stats.quickCheckCount,
                label = stringResource(R.string.chapters_quick_check),
                icon = Icons.Outlined.CalendarMonth,
                emphasized = false,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        if (chapters.isEmpty()) {
            Text(
                text = stringResource(R.string.chapters_empty),
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            chapters.forEach { chapter ->
                ChapterRow(chapter = chapter, onClick = { onChapterClick(chapter) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ChapterStatCard(
    value: String,
    label: String,
    icon: ImageVector,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (emphasized) BrandBlack else BgWhite
    val fg = if (emphasized) Color.White else BrandBlack
    Box(
        modifier = modifier
            .height(112.dp)
            .clip(CardShape)
            .then(
                if (emphasized) {
                    Modifier.background(bg)
                } else {
                    Modifier
                        .border(1.dp, BrandBlack, CardShape)
                        .background(bg)
                },
            ),
    ) {
        if (emphasized) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 18.dp, y = (-20).dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(22.dp),
            )
            Column {
                Text(
                    text = value,
                    color = fg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    color = fg.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: ChapterItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, CardShape)
            .clip(CardShape)
            .background(BgWhite)
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = chapter.title,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(28.dp),
        )
    }
}
