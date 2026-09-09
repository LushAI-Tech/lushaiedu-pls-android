package com.lushaiedupls.ui.auth.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lushaiedupls.R
import com.lushaiedupls.ui.auth.components.AuthTextLink
import com.lushaiedupls.ui.auth.components.GoogleButton
import com.lushaiedupls.ui.auth.components.LushAiEduBrandHeader
import com.lushaiedupls.ui.auth.components.OrContinueWithDivider
import com.lushaiedupls.ui.auth.components.PoweredByFooter
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LushAiEduBrandHeader(
                subtitle = stringResource(R.string.welcome_tagline),
                logoSize = 120.dp,
                showAtomOrbit = true,
            )
            Spacer(modifier = Modifier.height(36.dp))
            PrimaryButton(
                text = stringResource(R.string.sign_in),
                onClick = onSignIn,
                fullyRounded = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(20.dp))
            OrContinueWithDivider()
            Spacer(modifier = Modifier.height(16.dp))
            GoogleButton(onClick = onGoogle)
            Spacer(modifier = Modifier.height(18.dp))
            AuthTextLink(
                prefix = stringResource(R.string.dont_have_account),
                link = stringResource(R.string.sign_up),
                onClick = onCreateAccount,
                linkColor = BrandOrange,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        PoweredByFooter(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            centered = true,
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
