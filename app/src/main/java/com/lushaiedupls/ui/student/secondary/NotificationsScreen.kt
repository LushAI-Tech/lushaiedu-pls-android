package com.lushaiedupls.ui.student.secondary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AppNotification
import com.lushaiedupls.data.mock.NotificationSection
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)
private val SoftOrange = Color(0xFFFFF4ED)

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit = {},
    onOpenNotification: (AppNotification) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var items by remember(notifications) { mutableStateOf(notifications) }
    var selected by remember { mutableStateOf<AppNotification?>(null) }
    val today = items.filter { it.section == NotificationSection.Today }
    val earlier = items.filter { it.section == NotificationSection.Earlier }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgWhite)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = BrandBlack,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.notifications_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = BrandBlack,
                    modifier = Modifier.weight(1f),
                )
            }

            if (items.any { it.unread }) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(
                        onClick = {
                            items = items.map { it.copy(unread = false) }
                            onMarkAllRead()
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.notifications_mark_all_read),
                            color = BrandOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (items.isEmpty()) {
                EmptyNotificationsState()
            } else {
                SectionLabel(stringResource(R.string.notifications_today))
                Spacer(modifier = Modifier.height(8.dp))
                if (today.isEmpty()) {
                    Text(
                        text = stringResource(R.string.notifications_empty_today),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    today.forEach { item ->
                        NotificationCard(
                            item = item,
                            onClick = {
                                items = items.map { n ->
                                    if (n.id == item.id) n.copy(unread = false) else n
                                }
                                onOpenNotification(item)
                                selected = item.copy(unread = false)
                            },
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                SectionLabel(stringResource(R.string.notifications_earlier))
                Spacer(modifier = Modifier.height(8.dp))
                if (earlier.isEmpty()) {
                    Text(
                        text = stringResource(R.string.notifications_empty),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    earlier.forEach { item ->
                        NotificationCard(
                            item = item,
                            onClick = {
                                items = items.map { n ->
                                    if (n.id == item.id) n.copy(unread = false) else n
                                }
                                onOpenNotification(item)
                                selected = item.copy(unread = false)
                            },
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selected != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        ) {
            selected?.let { announcement ->
                AnnouncementDetailScreen(
                    notification = announcement,
                    onBack = { selected = null },
                )
            }
        }
    }
}

@Composable
private fun EmptyNotificationsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SoftOrange),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = BrandOrange,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.notifications_empty),
            color = TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 0.6.sp,
    )
}

@Composable
private fun NotificationCard(
    item: AppNotification,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (item.unread) 2.dp else 0.dp, CardShape, clip = false)
            .clip(CardShape)
            .background(if (item.unread) SoftOrange else BgLight)
            .border(
                width = 1.dp,
                color = if (item.unread) BrandOrange.copy(alpha = 0.25f) else BorderGray.copy(alpha = 0.75f),
                shape = CardShape,
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BrandBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (item.unread) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BrandOrange),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.body,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.timestampLabel,
                    color = TextSecondary.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                )
                Text(
                    text = "·",
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                )
                Text(
                    text = stringResource(R.string.notifications_tap_hint),
                    color = BrandOrange.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier
                .padding(top = 8.dp)
                .size(20.dp),
        )
    }
}

@Composable
private fun AnnouncementDetailScreen(
    notification: AppNotification,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgWhite),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = BrandBlack,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.notifications_detail_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandBlack,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = notification.title,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = BrandBlack,
                lineHeight = 30.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgLight)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetaRow(
                    icon = Icons.Outlined.Schedule,
                    label = stringResource(R.string.notifications_posted),
                    value = notification.timestampLabel,
                )
                notification.authorName?.takeIf { it.isNotBlank() }?.let { author ->
                    MetaRow(
                        icon = Icons.Outlined.PersonOutline,
                        label = stringResource(R.string.notifications_from),
                        value = author,
                    )
                }
                notification.teachingUnitLabel?.takeIf { it.isNotBlank() }?.let { unit ->
                    MetaRow(
                        icon = Icons.Outlined.Class,
                        label = stringResource(R.string.notifications_class),
                        value = unit,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = BorderGray.copy(alpha = 0.7f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = notification.body,
                color = BrandBlack,
                fontSize = 16.sp,
                lineHeight = 26.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Composable
private fun MetaRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = value,
                color = BrandBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
