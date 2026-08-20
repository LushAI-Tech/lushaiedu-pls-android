package com.lushaiedupls.ui.teacher.menu

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.ui.auth.selectrole.UserRole
import com.lushaiedupls.ui.common.LogoutButton
import com.lushaiedupls.ui.common.MenuListItem
import com.lushaiedupls.ui.common.SlideFromRightOverlay
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary

private val MenuPanelShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)

@Composable
fun TeacherMenuRoute(
    onSwitchRole: (UserRole) -> Unit,
    onAccount: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val themeMessage = stringResource(R.string.theme_stub_message)
    var showRolePicker by remember { mutableStateOf(false) }

    TeacherMenuScreen(
        onSwitchRoles = { showRolePicker = true },
        onAccount = onAccount,
        onTheme = {
            Toast.makeText(context, themeMessage, Toast.LENGTH_SHORT).show()
        },
        onPrivacy = onPrivacy,
        onTerms = onTerms,
        onLogOut = onLogOut,
        modifier = modifier,
    )

    if (showRolePicker) {
        SwitchRolesDialog(
            onDismiss = { showRolePicker = false },
            onRoleSelected = { role ->
                showRolePicker = false
                onSwitchRole(role)
            },
        )
    }
}

@Composable
fun TeacherMenuOverlay(
    onDismiss: () -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    onAccount: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onLogOut: () -> Unit,
) {
    SlideFromRightOverlay(
        onDismiss = onDismiss,
        panelWidthFraction = 0.78f,
        modifier = Modifier
            .clip(MenuPanelShape)
            .background(BgWhite),
    ) {
        TeacherMenuRoute(
            onSwitchRole = onSwitchRole,
            onAccount = onAccount,
            onPrivacy = onPrivacy,
            onTerms = onTerms,
            onLogOut = onLogOut,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun TeacherMenuScreen(
    onSwitchRoles: () -> Unit,
    onAccount: () -> Unit,
    onTheme: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 28.dp),
    ) {
        Text(
            text = stringResource(R.string.menu_title),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(20.dp))
        MenuListItem(
            title = stringResource(R.string.teacher_menu_switch_roles),
            icon = Icons.Outlined.SwapHoriz,
            onClick = onSwitchRoles,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MenuListItem(
            title = stringResource(R.string.menu_account),
            icon = Icons.Outlined.Person,
            onClick = onAccount,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MenuListItem(
            title = stringResource(R.string.menu_theme),
            icon = Icons.Outlined.BrightnessMedium,
            onClick = onTheme,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MenuListItem(
            title = stringResource(R.string.menu_privacy),
            icon = Icons.Outlined.VerifiedUser,
            onClick = onPrivacy,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MenuListItem(
            title = stringResource(R.string.menu_terms),
            icon = Icons.Outlined.Description,
            onClick = onTerms,
        )
        Spacer(modifier = Modifier.height(28.dp))
        LogoutButton(onClick = onLogOut)
    }
}

@Composable
private fun SwitchRolesDialog(
    onDismiss: () -> Unit,
    onRoleSelected: (UserRole) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.teacher_menu_switch_roles),
                fontWeight = FontWeight.Bold,
                color = BrandBlack,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.teacher_switch_roles_hint),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                UserRole.entries.forEach { role ->
                    Text(
                        text = roleLabel(role),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, BorderGray.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .clickable { onRoleSelected(role) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        fontWeight = FontWeight.SemiBold,
                        color = BrandBlack,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.danger_zone_cancel))
            }
        },
    )
}

@Composable
private fun roleLabel(role: UserRole): String = when (role) {
    UserRole.Teacher -> stringResource(R.string.role_teacher)
    UserRole.Student -> stringResource(R.string.role_student)
    UserRole.Admin -> stringResource(R.string.role_admin)
    UserRole.Parents -> stringResource(R.string.role_parents)
}
