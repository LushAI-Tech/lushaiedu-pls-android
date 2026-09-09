package com.lushaiedupls.ui.common

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AttendanceRecord
import com.lushaiedupls.data.mock.AttendanceStatus
import com.lushaiedupls.data.mock.OverviewIcon
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)
private val ChipShape = RoundedCornerShape(50)
private val MetricShape = RoundedCornerShape(18.dp)
private val LabelGray = Color(0xFF9CA3AF)
private val ValueGray = Color(0xFF6B7280)
private val PresentGreen = Color(0xFF22C55E)
private val LeaveAmber = Color(0xFFD97706)
val AttendanceRingPresent = BrandOrange
val AttendanceRingAbsent = Color(0xFFFFC9A8)
val AttendanceRingLeave = Color(0xFFFFE8DC)

@Composable
fun AppBackNav(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .offset(x = (-6).dp)
            .clickable(onClick = onBack)
            .padding(vertical = 8.dp, horizontal = 2.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.cd_back),
            tint = TextSecondary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(R.string.back),
            color = TextSecondary,
            fontSize = 15.sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
fun UserAvatar(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val imageModifier = modifier
        .size(size)
        .clip(CircleShape)
    val imageUrl = url?.takeIf { it.isNotBlank() }
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = imageModifier,
            error = painterResource(R.drawable.ic_avatar_placeholder),
        )
    } else {
        Image(
            painter = painterResource(R.drawable.ic_avatar_placeholder),
            contentDescription = contentDescription,
            modifier = imageModifier,
        )
    }
}

@Composable
fun AppTopBar(
    displayName: String,
    notificationCount: Int = 0,
    onNotificationClick: () -> Unit = {},
    onProfileClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_avatar_placeholder),
            contentDescription = stringResource(R.string.cd_avatar),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .then(
                    if (onProfileClick != null) {
                        Modifier.clickable(onClick = onProfileClick)
                    } else {
                        Modifier
                    },
                ),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.greeting_hi),
                color = BrandOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 22.sp,
            )
            Text(
                text = displayName,
                color = BrandBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 24.sp,
            )
        }
        IconButton(
            onClick = onNotificationClick,
            modifier = Modifier.size(44.dp),
        ) {
            BadgedBox(
                badge = {
                    if (notificationCount > 0) {
                        Badge(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White,
                        ) {
                            Text(
                                text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = stringResource(R.string.cd_notifications),
                    tint = BrandBlack,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = BrandBlack,
        fontFamily = FontFamily.SansSerif,
    )
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    emphasized: Boolean,
    iconKind: OverviewIcon,
    modifier: Modifier = Modifier,
    cardHeight: Dp = 88.dp,
) {
    val bg = if (emphasized) BrandBlack else BgLight
    val fg = if (emphasized) Color.White else BrandBlack
    val glow = if (emphasized) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
    val icon = iconFor(iconKind)

    Box(
        modifier = modifier
            .clip(MetricShape)
            .background(bg)
            .height(cardHeight),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 18.dp, y = (-18).dp)
                .size(64.dp)
                .clip(CircleShape)
                .background(glow),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    color = fg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 28.sp,
                )
                Text(
                    text = label,
                    color = fg.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
    }
}

@Composable
fun AttendanceDonut(
    present: Int,
    absent: Int,
    leave: Int,
    percent: Int,
    modifier: Modifier = Modifier,
) {
    val total = (present + absent + leave).coerceAtLeast(1).toFloat()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.13f
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val presentSweep = 360f * (present / total)
            val absentSweep = 360f * (absent / total)
            val leaveSweep = 360f - presentSweep - absentSweep
            var start = -90f
            drawArc(AttendanceRingPresent, start, presentSweep, false, topLeft, arcSize, style = stroke)
            start += presentSweep
            drawArc(AttendanceRingAbsent, start, absentSweep, false, topLeft, arcSize, style = stroke)
            start += absentSweep
            drawArc(AttendanceRingLeave, start, leaveSweep, false, topLeft, arcSize, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            Text(
                text = stringResource(R.string.section_attendance),
                fontSize = 13.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

private fun iconFor(kind: OverviewIcon): ImageVector = when (kind) {
    OverviewIcon.Subject -> Icons.AutoMirrored.Outlined.MenuBook
    OverviewIcon.StemMastery -> Icons.Outlined.Speed
    OverviewIcon.ReadingProgress -> Icons.AutoMirrored.Outlined.TrendingUp
    OverviewIcon.AverageProgress -> Icons.Outlined.CalendarMonth
    OverviewIcon.Children -> Icons.Outlined.Groups
    OverviewIcon.Fees -> Icons.Outlined.Payments
    OverviewIcon.Feedback -> Icons.Outlined.Forum
    OverviewIcon.Staff -> Icons.Outlined.Person
    OverviewIcon.Classes -> Icons.Outlined.Class
    OverviewIcon.Attendance -> Icons.AutoMirrored.Outlined.FactCheck
}

@Composable
fun StatusChip(
    status: AttendanceStatus,
    modifier: Modifier = Modifier,
) {
    val (label, color) = when (status) {
        AttendanceStatus.Present -> stringResource(R.string.status_present) to PresentGreen
        AttendanceStatus.Absent -> stringResource(R.string.status_absent) to BrandOrange
        AttendanceStatus.Leave -> stringResource(R.string.status_leave) to LeaveAmber
    }
    Box(
        modifier = modifier
            .border(1.dp, color, ChipShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
fun AttendanceRecordCard(
    record: AttendanceRecord,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.7f), CardShape)
            .background(BgWhite, CardShape)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.col_date),
                    color = LabelGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.date,
                    color = ValueGray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.col_status),
                    color = LabelGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusChip(status = record.status)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = BorderGray.copy(alpha = 0.55f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            LabeledValue(
                label = stringResource(R.string.col_class),
                value = record.className,
                modifier = Modifier.weight(1f),
            )
            LabeledValue(
                label = stringResource(R.string.col_subject),
                value = record.subject,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LabeledValue(
            label = stringResource(R.string.col_time),
            value = record.time,
        )
        val note = record.note
        if (!note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                    contentDescription = null,
                    tint = BrandBlack,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.attendance_reason_with_note, note),
                    color = BrandBlack,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = LabelGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = ValueGray,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
fun MenuListItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, BorderGray, CardShape)
            .background(BgWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandBlack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = BrandBlack,
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
        )
    }
}

@Composable
fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.log_out),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF25F5C))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

@Composable
fun PlaceholderTab(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BrandBlack)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle ?: stringResource(R.string.tab_coming_soon),
                color = TextSecondary,
                fontSize = 14.sp,
            )
        }
    }
}
