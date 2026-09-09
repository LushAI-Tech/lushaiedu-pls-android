package com.lushaiedupls.ui.admin

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.ui.common.navItemClickable
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange

enum class AdminTab(val route: String) {
    Home(AdminRoutes.HOME),
    Users(AdminRoutes.USERS),
    Classes(AdminRoutes.CLASSES),
    More(AdminRoutes.MORE),
}

@Composable
fun AdminBottomBar(
    selectedTab: AdminTab,
    onTabSelected: (AdminTab) -> Unit,
    modifier: Modifier = Modifier,
    pendingUserCount: Int = 0,
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
                selected = selectedTab == AdminTab.Home,
                onClick = { onTabSelected(AdminTab.Home) },
            )
            BottomNavIcon(
                icon = Icons.Outlined.People,
                label = stringResource(R.string.admin_tab_users),
                selected = selectedTab == AdminTab.Users,
                onClick = { onTabSelected(AdminTab.Users) },
                badgeCount = pendingUserCount,
            )
            BottomNavIcon(
                icon = Icons.Outlined.School,
                label = stringResource(R.string.admin_tab_classes),
                selected = selectedTab == AdminTab.Classes,
                onClick = { onTabSelected(AdminTab.Classes) },
            )
            BottomNavIcon(
                icon = Icons.Outlined.Menu,
                label = stringResource(R.string.tab_more),
                selected = selectedTab == AdminTab.More,
                onClick = { onTabSelected(AdminTab.More) },
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
    badgeCount: Int = 0,
) {
    Column(
        modifier = Modifier
            .semantics {
                contentDescription = if (badgeCount > 0) {
                    "$label, $badgeCount pending"
                } else {
                    label
                }
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
            if (badgeCount > 0) {
                val diameter = 18.dp
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 7.dp, y = (-5).dp)
                        .size(diameter)
                        .clip(CircleShape)
                        .background(BrandOrange),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = if (badgeCount > 9) 8.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = if (badgeCount > 9) 8.sp else 10.sp,
                        modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both,
                            ),
                        ),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        val indicatorWidth by animateDpAsState(
            targetValue = if (selected) 18.dp else 0.dp,
            animationSpec = tween(220),
            label = "adminNavIndicator",
        )
        Box(
            modifier = Modifier
                .size(width = indicatorWidth, height = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(BrandBlack),
        )
    }
}
