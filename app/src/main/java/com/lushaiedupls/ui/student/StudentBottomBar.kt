package com.lushaiedupls.ui.student

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lushaiedupls.R
import com.lushaiedupls.ui.common.navItemClickable
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack

enum class StudentTab(
    val route: String,
) {
    Home(StudentRoutes.HOME),
    Timetable(StudentRoutes.TIMETABLE),
    Ai(StudentRoutes.AI),
    Attendance(StudentRoutes.ATTENDANCE),
    More(StudentRoutes.MORE),
}

@Composable
fun StudentBottomBar(
    selectedTab: StudentTab,
    onTabSelected: (StudentTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BgWhite)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = BorderGray.copy(alpha = 0.6f), thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomNavIcon(
                icon = Icons.Outlined.Home,
                label = stringResource(R.string.tab_home),
                selected = selectedTab == StudentTab.Home,
                onClick = { onTabSelected(StudentTab.Home) },
            )
            BottomNavIcon(
                icon = Icons.Outlined.Schedule,
                label = stringResource(R.string.tab_timetable),
                selected = selectedTab == StudentTab.Timetable,
                onClick = { onTabSelected(StudentTab.Timetable) },
            )
            AiCenterButton(
                selected = selectedTab == StudentTab.Ai,
                onClick = { onTabSelected(StudentTab.Ai) },
            )
            BottomNavIcon(
                icon = Icons.Outlined.Person,
                label = stringResource(R.string.tab_attendance),
                selected = selectedTab == StudentTab.Attendance,
                onClick = { onTabSelected(StudentTab.Attendance) },
                showCheckBadge = true,
            )
            BottomNavIcon(
                icon = Icons.Outlined.Menu,
                label = stringResource(R.string.tab_more),
                selected = selectedTab == StudentTab.More,
                onClick = { onTabSelected(StudentTab.More) },
            )
        }
    }
}

@Composable
private fun BottomNavIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showCheckBadge: Boolean = false,
) {
    Column(
        modifier = Modifier
            .semantics {
                contentDescription = label
                role = Role.Tab
                this.selected = selected
            }
            .navItemClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandBlack,
                modifier = Modifier.size(26.dp),
            )
            if (showCheckBadge) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = BrandBlack,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(12.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        val indicatorWidth by animateDpAsState(
            targetValue = if (selected) 18.dp else 0.dp,
            animationSpec = tween(220),
            label = "navIndicator",
        )
        Box(
            modifier = Modifier
                .size(width = indicatorWidth, height = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(BrandBlack),
        )
    }
}

@Composable
private fun AiCenterButton(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.16f else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 380f,
        ),
        label = "aiNavScale",
    )

    Image(
        painter = painterResource(R.drawable.ic_nav_ai_center),
        contentDescription = stringResource(R.string.tab_ai),
        modifier = Modifier
            .semantics {
                contentDescription = "AI"
                role = Role.Tab
                this.selected = selected
            }
            .size(width = 52.dp, height = 48.dp)
            .navItemClickable(
                selectedScale = scale,
                onClick = onClick,
            ),
        contentScale = ContentScale.Fit,
    )
}
