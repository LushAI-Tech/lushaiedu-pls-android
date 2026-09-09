package com.lushaiedupls.ui.auth.invitecode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.components.LushAiEduBrandHeader
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.SelectionNavButtons
import com.lushaiedupls.ui.common.verticalScrollWithIme
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

@Composable
fun InviteCodeRoute(
    userSessionStore: UserSessionStore,
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onContinueToClass: () -> Unit,
    onFinished: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteCodeViewModel = viewModel(
        factory = InviteCodeViewModel.provideFactory(userSessionStore, authRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.finishedRoute) {
        val route = uiState.finishedRoute ?: return@LaunchedEffect
        viewModel.clearFinishedRoute()
        onFinished(route)
    }
    InviteCodeScreen(
        uiState = uiState,
        onInviteCodeChange = viewModel::onInviteCodeChange,
        onBack = onBack,
        onContinue = { viewModel.submit(onContinueToClass = onContinueToClass) },
        modifier = modifier,
    )
}

@Composable
fun InviteCodeScreen(
    uiState: InviteCodeUiState,
    onInviteCodeChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScrollWithIme(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LushAiEduBrandHeader(logoSize = 104.dp)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.invite_code_title),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.invite_code_subtitle),
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedAuthField(
                label = stringResource(R.string.invite_code_label),
                value = uiState.inviteCode,
                onValueChange = onInviteCodeChange,
                placeholder = stringResource(R.string.invite_code_hint),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
            )
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error, color = BrandOrange, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SelectionNavButtons(
            onBack = onBack,
            onContinue = onContinue,
            continueEnabled = !uiState.isLoading && uiState.inviteCode.isNotBlank(),
            continueLabel = if (uiState.isLoading) {
                stringResource(R.string.loading)
            } else {
                stringResource(R.string.continue_label)
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InviteCodePreview() {
    LushAIEdu_PLSTheme {
        InviteCodeScreen(
            uiState = InviteCodeUiState(inviteCode = "ABCD-1234"),
            onInviteCodeChange = {},
            onBack = {},
            onContinue = {},
        )
    }
}
