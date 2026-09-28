package com.lushaiedupls.ui.auth.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.ActiveDeviceSession
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.ui.auth.components.AuthTextLink
import com.lushaiedupls.ui.auth.components.LushAiEduBrandHeader
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LushAuthContentWidth
import com.lushaiedupls.ui.common.verticalScrollWithIme
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme

@Composable
fun SignInRoute(
    authRepository: AuthRepository,
    onNavigate: (String) -> Unit,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = viewModel(
        factory = SignInViewModel.provideFactory(authRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        authRepository.consumePendingSignInNotice()?.let { notice ->
            viewModel.applyPendingNotice(notice)
        }
    }

    LaunchedEffect(uiState.successRoute) {
        uiState.successRoute?.let { route ->
            viewModel.clearNavigation()
            onNavigate(route)
        }
    }

    SignInScreen(
        uiState = uiState,
        onIdentifierChange = viewModel::onIdentifierChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSignIn = viewModel::signIn,
        onSignUp = onSignUp,
        onResolveDeviceConflict = viewModel::resolveDeviceConflict,
        onDismissDeviceConflict = viewModel::dismissDeviceConflict,
        modifier = modifier,
    )
}

@Composable
fun SignInScreen(
    uiState: SignInUiState,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    onResolveDeviceConflict: () -> Unit = {},
    onDismissDeviceConflict: () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight)
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScrollWithIme(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LushAuthContentWidth {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LushAiEduBrandHeader(
                        logoSize = 112.dp,
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = BgWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            OutlinedAuthField(
                                label = stringResource(R.string.email_or_phone_label),
                                value = uiState.identifier,
                                onValueChange = onIdentifierChange,
                                placeholder = stringResource(R.string.email_or_phone_placeholder),
                                leadingIcon = Icons.Outlined.Email,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedAuthField(
                                label = stringResource(R.string.password_label),
                                value = uiState.password,
                                onValueChange = onPasswordChange,
                                placeholder = stringResource(R.string.password_placeholder),
                                leadingIcon = Icons.Outlined.Lock,
                                isPassword = true,
                            )
                            uiState.errorMessage?.let { error ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    color = BrandOrange,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            PrimaryButton(
                                text = if (uiState.isLoading) {
                                    stringResource(R.string.loading)
                                } else {
                                    stringResource(R.string.sign_in)
                                },
                                onClick = onSignIn,
                                enabled = !uiState.isLoading,
                                trailingArrow = true,
                                fullyRounded = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AuthTextLink(
                                prefix = stringResource(R.string.dont_have_account),
                                link = stringResource(R.string.sign_up),
                                onClick = onSignUp,
                                linkColor = BrandOrange,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )
                        }
                    }
                }
            }
        }
    }
    if (uiState.showDeviceConflict) {
        DeviceConflictOverlay(
            message = uiState.deviceConflictMessage.orEmpty(),
            accountLabel = uiState.deviceConflictAccount,
            devices = uiState.deviceConflictDevices,
            isResolving = uiState.isResolvingConflict,
            errorMessage = uiState.conflictResolveError,
            onResolve = onResolveDeviceConflict,
            onDismiss = onDismissDeviceConflict,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignInDeviceConflictPreview() {
    LushAIEdu_PLSTheme {
        SignInScreen(
            uiState = SignInUiState(
                identifier = "student@lushaiedu.example.com",
                showDeviceConflict = true,
                deviceConflictMessage =
                    "No login is allowed while another device is still active. Please sign out on that device first.",
                deviceConflictAccount = "student@lushaiedu.example.com",
                deviceConflictDevices = listOf(
                    ActiveDeviceSession(
                        accountLabel = "student@lushaiedu.example.com",
                        deviceName = "Pixel 8",
                        platform = "Android",
                        lastActive = "15 Sep 2026, 18:20",
                    ),
                ),
            ),
            onIdentifierChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onSignUp = {},
        )
    }
}
