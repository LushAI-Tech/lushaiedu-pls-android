package com.lushaiedupls.ui.auth.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.ui.auth.components.GoogleButton
import com.lushaiedupls.ui.auth.components.LushAiEduBrandHeader
import com.lushaiedupls.ui.auth.components.OrContinueWithDivider
import com.lushaiedupls.ui.auth.components.PoweredByFooter
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

@Composable
fun WelcomeRoute(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onGoogle: () -> Unit,
    onParent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WelcomeScreen(
        onCreateAccount = onCreateAccount,
        onSignIn = onSignIn,
        onGoogle = onGoogle,
        onParent = onParent,
        modifier = modifier,
    )
}

@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onGoogle: () -> Unit,
    onParent: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        // Logo → Trouble signing in: centered as one block, nudged slightly down
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LushAiEduBrandHeader(
                subtitle = stringResource(R.string.welcome_tagline),
                logoSize = 120.dp,
            )
            Spacer(modifier = Modifier.height(36.dp))
            PrimaryButton(
                text = stringResource(R.string.create_account),
                onClick = onCreateAccount,
                fullyRounded = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(14.dp))
            PrimaryButton(
                text = stringResource(R.string.sign_in),
                onClick = onSignIn,
                fullyRounded = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = onParent) {
                Text(
                    text = stringResource(R.string.welcome_im_a_parent),
                    color = TextSecondary,
                    fontSize = 14.sp,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            OrContinueWithDivider()
            Spacer(modifier = Modifier.height(16.dp))
            GoogleButton(onClick = onGoogle)
            TextButton(
                onClick = {},
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = stringResource(R.string.trouble_signing_in),
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            }
        }

        PoweredByFooter(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    LushAIEdu_PLSTheme {
        WelcomeScreen(onCreateAccount = {}, onSignIn = {}, onGoogle = {})
    }
}
