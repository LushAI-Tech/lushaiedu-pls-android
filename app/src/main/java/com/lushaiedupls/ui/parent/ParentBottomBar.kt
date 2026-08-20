package com.lushaiedupls.ui.parent

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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

enum class ParentTab(val route: String) {
    Home(ParentRoutes.HOME),
    Scan(ParentRoutes.SCAN),
    Attendance(ParentRoutes.ATTENDANCE),
    More(ParentRoutes.HOME),
}

@Composable
fun ParentBottomBar(
    selectedTab: ParentTab,
    onTabSelected: (ParentTab) -> Unit,
    onMoreClick: () -> Unit,
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
                selected = selectedTab == ParentTab.Home,
                onClick = { onTabSelected(ParentTab.Home) },
            )
            BottomNavIcon(
                icon = Icons.Outlined.QrCodeScanner,
                label = stringResource(R.string.parent_tab_scan),
                selected = selectedTab == ParentTab.Scan,
                onClick = { onTabSelected(ParentTab.Scan) },
            )
            BottomNavIcon(
                icon = Icons.Outlined.Person,
                label = stringResource(R.string.tab_attendance),
                selected = selectedTab == ParentTab.Attendance,
                onClick = { onTabSelected(ParentTab.Attendance) },
                showCheckBadge = true,
            )
            BottomNavIcon(
                icon = Icons.Outlined.Menu,
                label = stringResource(R.string.tab_more),
                selected = selectedTab == ParentTab.More,
                onClick = onMoreClick,
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
                        .size(12.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        val indicatorWidth by animateDpAsState(
            targetValue = if (selected) 18.dp else 0.dp,
            animationSpec = tween(220),
            label = "parentNavIndicator",
        )
        Box(
            modifier = Modifier
                .size(width = indicatorWidth, height = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(BrandBlack),
        )
    }
}
