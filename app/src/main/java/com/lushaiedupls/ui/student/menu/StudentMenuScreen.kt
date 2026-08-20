package com.lushaiedupls.ui.student.menu

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
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.ui.common.LogoutButton
import com.lushaiedupls.ui.common.MenuListItem
import com.lushaiedupls.ui.common.SlideFromRightOverlay
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme

private val MenuPanelShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)

@Composable
fun StudentMenuRoute(
    onAccount: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onLogOut: () -> Unit,
    onLinkParent: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val themeMessage = stringResource(R.string.theme_stub_message)
    StudentMenuScreen(
        onAccount = onAccount,
        onLinkParent = onLinkParent,
        onTheme = {
            Toast.makeText(context, themeMessage, Toast.LENGTH_SHORT).show()
        },
        onPrivacy = onPrivacy,
        onTerms = onTerms,
        onLogOut = onLogOut,
        modifier = modifier,
    )
}

@Composable
fun StudentMenuOverlay(
    onDismiss: () -> Unit,
    onAccount: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onLogOut: () -> Unit,
    onLinkParent: (() -> Unit)? = null,
) {
    SlideFromRightOverlay(
        onDismiss = onDismiss,
        panelWidthFraction = 0.78f,
        modifier = Modifier
            .clip(MenuPanelShape)
            .background(BgWhite),
    ) {
        StudentMenuRoute(
            onAccount = onAccount,
            onLinkParent = onLinkParent,
            onPrivacy = onPrivacy,
            onTerms = onTerms,
            onLogOut = onLogOut,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun StudentMenuScreen(
    onAccount: () -> Unit,
    onTheme: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onLogOut: () -> Unit,
    onLinkParent: (() -> Unit)? = null,
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
            title = stringResource(R.string.menu_account),
            icon = Icons.Outlined.Person,
            onClick = onAccount,
        )
        if (onLinkParent != null) {
            Spacer(modifier = Modifier.height(12.dp))
            MenuListItem(
                title = stringResource(R.string.student_link_parent_title),
                icon = Icons.Outlined.FamilyRestroom,
                onClick = onLinkParent,
            )
        }
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

@Preview(showBackground = true)
@Composable
private fun StudentMenuPreview() {
    LushAIEdu_PLSTheme {
        StudentMenuScreen(
            onAccount = {},
            onTheme = {},
            onPrivacy = {},
            onTerms = {},
            onLogOut = {},
        )
    }
}
