package com.lushaiedupls.ui.admin.menu

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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun AdminMenuOverlay(
    onDismiss: () -> Unit,
    onAccount: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onLogOut: () -> Unit,
) {
    // Theme selection — re-enable later.
    // val context = LocalContext.current
    // val themeMessage = stringResource(R.string.theme_stub_message)
    SlideFromRightOverlay(
        onDismiss = onDismiss,
        panelWidthFraction = 0.78f,
        modifier = Modifier
            .clip(MenuPanelShape)
            .background(BgWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgWhite)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.admin_menu_title),
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
            // Theme selection — re-enable later.
            // Spacer(modifier = Modifier.height(12.dp))
            // MenuListItem(
            //     title = stringResource(R.string.menu_theme),
            //     icon = Icons.Outlined.BrightnessMedium,
            //     onClick = {
            //         Toast.makeText(context, themeMessage, Toast.LENGTH_SHORT).show()
            //     },
            // )
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
}
