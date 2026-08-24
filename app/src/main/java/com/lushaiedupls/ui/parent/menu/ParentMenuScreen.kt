package com.lushaiedupls.ui.parent.menu

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.lushaiedupls.ui.common.LogoutButton
import com.lushaiedupls.ui.common.MenuListItem
import com.lushaiedupls.ui.common.SlideFromRightOverlay
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack

private val MenuPanelShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)

/**
 * Route-level wrapper that handles Toast for theme stub and delegates to [ParentMenuScreen].
 */
@Composable
fun ParentMenuRoute(
    onAccount: () -> Unit,
    onScanQr: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val themeMessage = stringResource(R.string.theme_stub_message)
    ParentMenuScreen(
        onAccount = onAccount,
        onScanQr = onScanQr,
        onTheme = {
            Toast.makeText(context, themeMessage, Toast.LENGTH_SHORT).show()
        },
        onPrivacy = onPrivacy,
        onTerms = onTerms,
        onLogOut = onLogOut,
        modifier = modifier,
    )
}

/**
 * Slide-from-right overlay panel for parent menu — mirrors [TeacherMenuOverlay] / [StudentMenuOverlay].
 * Shows parent-relevant items only (no "Link a parent" which is a student action).
 */
@Composable
fun ParentMenuOverlay(
    onDismiss: () -> Unit,
    onAccount: () -> Unit,
    onScanQr: () -> Unit,
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
        ParentMenuRoute(
            onAccount = onAccount,
            onScanQr = onScanQr,
            onPrivacy = onPrivacy,
            onTerms = onTerms,
            onLogOut = onLogOut,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun ParentMenuScreen(
    onAccount: () -> Unit,
    onScanQr: () -> Unit,
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
            text = stringResource(R.string.parent_menu_title),
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
            title = stringResource(R.string.menu_account),
            icon = Icons.Outlined.Person,
            onClick = onAccount,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MenuListItem(
            title = stringResource(R.string.parent_menu_scan_qr),
            icon = Icons.Outlined.QrCodeScanner,
            onClick = onScanQr,
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
