package com.lushaiedupls.ui.student.secondary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AppNotification
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.NotificationDetailScreen
import com.lushaiedupls.ui.common.NotificationEmptyState
import com.lushaiedupls.ui.common.NotificationListCard
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit = {},
    onOpenNotification: (AppNotification) -> Unit = {},
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var items by remember(notifications) { mutableStateOf(notifications) }
    var selected by remember { mutableStateOf<AppNotification?>(null) }

    fun openItem(item: AppNotification) {
        items = items.map { n ->
            if (n.id == item.id) n.copy(unread = false) else n
        }
        onOpenNotification(item)
        selected = item.copy(unread = false)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LushPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
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
                }
                Spacer(modifier = Modifier.height(20.dp))

                if (items.isEmpty()) {
                    NotificationEmptyState(
                        message = stringResource(R.string.notifications_empty),
                    )
                } else {
                    items.forEach { item ->
                        NotificationListCard(
                            item = item,
                            onClick = { openItem(item) },
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
                NotificationDetailScreen(
                    notification = announcement,
                    onBack = { selected = null },
                )
            }
        }
    }
}
