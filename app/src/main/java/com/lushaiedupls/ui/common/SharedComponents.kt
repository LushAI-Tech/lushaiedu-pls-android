package com.lushaiedupls.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Box {
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = stringResource(R.string.cd_notifications),
                    tint = BrandBlack,
                    modifier = Modifier.size(26.dp),
                )
            }
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE11D48)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = notificationCount.coerceAtMost(9).toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
) {
    val bg = if (emphasized) BrandBlack else BgLight
    val fg = if (emphasized) Color.White else BrandBlack
    val glow = if (emphasized) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
    val icon = iconFor(iconKind)

    Box(
        modifier = modifier
            .clip(MetricShape)
            .background(bg)
            .height(88.dp),
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

private fun iconFor(kind: OverviewIcon): ImageVector = when (kind) {
    OverviewIcon.Subject -> Icons.AutoMirrored.Outlined.MenuBook
    OverviewIcon.StemMastery -> Icons.Outlined.Speed
    OverviewIcon.ReadingProgress -> Icons.AutoMirrored.Outlined.TrendingUp
    OverviewIcon.AverageProgress -> Icons.Outlined.CalendarMonth
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
