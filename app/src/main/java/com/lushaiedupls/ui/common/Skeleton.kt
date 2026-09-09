package com.lushaiedupls.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack

private val Bone = Color(0xFFE6E8EC)
private val CardShape = RoundedCornerShape(16.dp)
private val PillShape = RoundedCornerShape(50)

enum class StudentSkeletonKind {
    Home,
    Attendance,
    Timetable,
    AiLearn,
    Calendar,
    Notifications,
    Account,
    Chat,
    TextbookQuestions,
    ExamPreparation,
    Resources,
    List,
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(Bone.copy(alpha = pulse)),
    )
}

@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
) {
    SkeletonBox(modifier = modifier.height(height), shape = PillShape)
}

@Composable
fun StudentPageSkeleton(
    kind: StudentSkeletonKind,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 24.dp),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        when (kind) {
            StudentSkeletonKind.Home -> HomeSkeleton()
            StudentSkeletonKind.Attendance -> AttendanceSkeleton()
            StudentSkeletonKind.Timetable -> TimetableSkeleton()
            StudentSkeletonKind.AiLearn -> AiLearnSkeleton()
            StudentSkeletonKind.Calendar -> CalendarSkeleton()
            StudentSkeletonKind.Notifications -> ListSkeleton(rows = 5)
            StudentSkeletonKind.Account -> AccountSkeleton()
            StudentSkeletonKind.Chat -> ChatSkeleton()
            StudentSkeletonKind.TextbookQuestions -> TextbookQuestionsSkeleton()
            StudentSkeletonKind.ExamPreparation -> ExamPreparationSkeleton()
            StudentSkeletonKind.Resources -> ResourcesSkeleton()
            StudentSkeletonKind.List -> ListSkeleton(rows = 4)
        }
    }
}

@Composable
private fun HomeSkeleton() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SkeletonBox(modifier = Modifier.size(48.dp), shape = CircleShape)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonLine(modifier = Modifier.width(72.dp), height = 10.dp)
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonLine(modifier = Modifier.width(140.dp), height = 16.dp)
        }
        SkeletonBox(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(10.dp))
    }
    Spacer(modifier = Modifier.height(24.dp))
    SkeletonLine(modifier = Modifier.width(110.dp), height = 14.dp)
    Spacer(modifier = Modifier.height(12.dp))
    repeat(2) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(modifier = Modifier.weight(1f).height(88.dp), shape = CardShape)
            SkeletonBox(modifier = Modifier.weight(1f).height(88.dp), shape = CardShape)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
    Spacer(modifier = Modifier.height(8.dp))
    SkeletonLine(modifier = Modifier.width(160.dp), height = 14.dp)
    Spacer(modifier = Modifier.height(12.dp))
    SkeletonBox(modifier = Modifier.fillMaxWidth().height(150.dp), shape = CardShape)
    Spacer(modifier = Modifier.height(24.dp))
    SkeletonLine(modifier = Modifier.width(120.dp), height = 14.dp)
    Spacer(modifier = Modifier.height(12.dp))
    repeat(2) {
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(72.dp), shape = CardShape)
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun AttendanceSkeleton() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            SkeletonBox(modifier = Modifier.weight(1f).height(78.dp), shape = CardShape)
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            SkeletonBox(modifier = Modifier.weight(1f).height(78.dp), shape = CardShape)
        }
    }
    Spacer(modifier = Modifier.height(22.dp))
    SkeletonLine(modifier = Modifier.width(130.dp), height = 14.dp)
    Spacer(modifier = Modifier.height(10.dp))
    SkeletonBox(modifier = Modifier.fillMaxWidth().height(120.dp), shape = CardShape)
    Spacer(modifier = Modifier.height(22.dp))
    SkeletonBox(modifier = Modifier.fillMaxWidth().height(36.dp), shape = PillShape)
    Spacer(modifier = Modifier.height(12.dp))
    SkeletonBox(modifier = Modifier.fillMaxWidth().height(280.dp), shape = CardShape)
}

@Composable
private fun TimetableSkeleton() {
    SkeletonLine(modifier = Modifier.width(80.dp), height = 14.dp)
    Spacer(modifier = Modifier.height(10.dp))
    SkeletonBox(modifier = Modifier.fillMaxWidth().height(44.dp), shape = CardShape)
    Spacer(modifier = Modifier.height(22.dp))
    SkeletonLine(modifier = Modifier.width(150.dp), height = 14.dp)
    Spacer(modifier = Modifier.height(12.dp))
    SkeletonBox(modifier = Modifier.fillMaxWidth().height(320.dp), shape = CardShape)
}

@Composable
private fun AiLearnSkeleton() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            SkeletonBox(modifier = Modifier.weight(1f).height(84.dp), shape = CardShape)
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
    repeat(4) {
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(68.dp), shape = CardShape)
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun CalendarSkeleton() {
    SkeletonLine(modifier = Modifier.width(80.dp), height = 14.dp)
    Spacer(modifier = Modifier.height(10.dp))
    SkeletonBox(modifier = Modifier.fillMaxWidth().height(72.dp), shape = CardShape)
    Spacer(modifier = Modifier.height(20.dp))
    SkeletonBox(modifier = Modifier.fillMaxWidth().height(360.dp), shape = RoundedCornerShape(28.dp))
}

@Composable
private fun AccountSkeleton() {
    SkeletonBox(modifier = Modifier.fillMaxWidth().height(92.dp), shape = CardShape)
    Spacer(modifier = Modifier.height(12.dp))
    SkeletonBox(modifier = Modifier.fillMaxWidth().height(56.dp), shape = CardShape)
    Spacer(modifier = Modifier.height(12.dp))
    SkeletonBox(modifier = Modifier.fillMaxWidth().height(140.dp), shape = CardShape)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatIntroSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 6.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonLine(modifier = Modifier.fillMaxWidth(), height = 14.dp)
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.94f), height = 14.dp)
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.88f), height = 14.dp)
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.62f), height = 14.dp)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBox(modifier = Modifier.width(108.dp).height(34.dp), shape = PillShape)
            SkeletonBox(modifier = Modifier.width(132.dp).height(34.dp), shape = PillShape)
            SkeletonBox(modifier = Modifier.width(96.dp).height(34.dp), shape = PillShape)
        }
        Spacer(modifier = Modifier.height(8.dp))
        AiQuickCheckCardSkeleton()
    }
}

@Composable
fun AiQuickCheckCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(BgWhite)
            .border(1.dp, BorderGray.copy(alpha = 0.5f), CardShape)
            .padding(16.dp),
    ) {
        SkeletonLine(modifier = Modifier.width(92.dp), height = 12.dp)
        Spacer(modifier = Modifier.height(10.dp))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.96f), height = 14.dp)
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.72f), height = 14.dp)
        Spacer(modifier = Modifier.height(14.dp))
        repeat(4) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = PillShape,
            )
            if (it != 3) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AiMenuSectionHeaderSkeleton(
    titleWidth: Dp,
    trailingWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonLine(modifier = Modifier.width(titleWidth), height = 16.dp)
        SkeletonLine(modifier = Modifier.width(trailingWidth), height = 12.dp)
    }
}

@Composable
fun AiMenuQuestionCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(BgWhite)
            .border(1.dp, BorderGray.copy(alpha = 0.65f), CardShape)
            .padding(14.dp),
    ) {
        SkeletonLine(modifier = Modifier.width(28.dp), height = 12.dp)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonLine(modifier = Modifier.fillMaxWidth(), height = 14.dp)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.92f), height = 14.dp)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.68f), height = 14.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonLine(modifier = Modifier.weight(1f), height = 12.dp)
            SkeletonBox(modifier = Modifier.width(72.dp).height(32.dp), shape = PillShape)
        }
    }
}

@Composable
fun AiResourceItemSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgWhite)
            .border(1.dp, BorderGray.copy(alpha = 0.65f), RoundedCornerShape(16.dp)),
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(0.dp),
        )
        Column(modifier = Modifier.padding(14.dp)) {
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.86f), height = 14.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonLine(modifier = Modifier.weight(1f), height = 12.dp)
                Spacer(modifier = Modifier.width(12.dp))
                SkeletonBox(modifier = Modifier.width(62.dp).height(32.dp), shape = PillShape)
            }
        }
    }
}

@Composable
private fun ChatSkeleton() {
    AiChatIntroSkeleton()
}

@Composable
private fun TextbookQuestionsSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AiMenuSectionHeaderSkeleton(titleWidth = 150.dp, trailingWidth = 52.dp)
        repeat(3) {
            AiMenuQuestionCardSkeleton()
        }
    }
}

@Composable
private fun ExamPreparationSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AiMenuSectionHeaderSkeleton(titleWidth = 140.dp, trailingWidth = 72.dp)
        repeat(3) {
            AiMenuQuestionCardSkeleton()
        }
    }
}

@Composable
private fun ResourcesSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AiMenuSectionHeaderSkeleton(titleWidth = 92.dp, trailingWidth = 48.dp)
        repeat(3) {
            AiResourceItemSkeleton()
        }
    }
}

@Composable
private fun ListSkeleton(rows: Int) {
    repeat(rows) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonLine(modifier = Modifier.fillMaxWidth(0.7f), height = 14.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonLine(modifier = Modifier.fillMaxWidth(0.45f), height = 10.dp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}
