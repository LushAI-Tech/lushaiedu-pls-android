package com.lushaiedupls.ui.auth.signup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.components.AuthProfilePhotoPicker
import com.lushaiedupls.ui.auth.components.AuthTextLink
import com.lushaiedupls.ui.auth.components.GenderChip
import com.lushaiedupls.ui.auth.components.GoogleButton
import com.lushaiedupls.ui.auth.components.LushAiEduWordmark
import com.lushaiedupls.ui.auth.components.OrContinueWithDivider
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.auth.google.rememberGoogleSignInAction
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

@Composable
fun CreateAccountRoute(
    authRepository: AuthRepository,
    userSessionStore: UserSessionStore,
    studentRepository: StudentRepository,
    onNavigate: (String) -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateAccountViewModel = viewModel(
        factory = CreateAccountViewModel.provideFactory(
            authRepository,
            userSessionStore,
            studentRepository,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.clearError()
    }

    LaunchedEffect(uiState.successRoute) {
        uiState.successRoute?.let { route ->
            viewModel.clearNavigation()
            onNavigate(route)
        }
    }

    val googleSignIn = rememberGoogleSignInAction(
        onIdToken = { token -> viewModel.signInWithGoogle(token, context) },
        onError = viewModel::setError,
    )

    CreateAccountScreen(
        uiState = uiState,
        onFullNameChange = viewModel::onFullNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPhoneChange = viewModel::onPhoneChange,
        onPasswordChange = viewModel::onPasswordChange,
        onAddressChange = viewModel::onAddressChange,
        onGenderSelected = viewModel::onGenderSelected,
        onAvatarSelected = viewModel::onAvatarSelected,
        onRegister = { viewModel.register(context) },
        onSignIn = onSignIn,
        onGoogle = googleSignIn,
        modifier = modifier,
    )
}

@Composable
fun CreateAccountScreen(
    uiState: CreateAccountUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onGenderSelected: (GenderOption) -> Unit,
    onAvatarSelected: (Uri) -> Unit,
    onRegister: () -> Unit,
    onSignIn: () -> Unit,
    onGoogle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onAvatarSelected) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight)
            .systemBarsPadding()
            .imePadding(),
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BgWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LushAiEduWordmark(fontSizeSp = 26)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.create_account_subtitle),
                        color = BrandBlack,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    AuthProfilePhotoPicker(
                        localUri = uiState.avatarUri,
                        enabled = !uiState.isLoading,
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedAuthField(
                        label = stringResource(R.string.full_name),
                        value = uiState.fullName,
                        onValueChange = onFullNameChange,
                        placeholder = stringResource(R.string.full_name_placeholder),
                        leadingIcon = Icons.Outlined.Person,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedAuthField(
                        label = stringResource(R.string.email_address),
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        placeholder = stringResource(R.string.email_placeholder),
                        leadingIcon = Icons.Outlined.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedAuthField(
                        label = stringResource(R.string.phone),
                        value = uiState.phone,
                        onValueChange = onPhoneChange,
                        placeholder = stringResource(R.string.phone_placeholder),
                        leadingIcon = Icons.Outlined.Phone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedAuthField(
                        label = stringResource(R.string.address),
                        value = uiState.address,
                        onValueChange = onAddressChange,
                        placeholder = stringResource(R.string.address_placeholder),
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.gender),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = BrandBlack,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GenderChip(
                            text = stringResource(R.string.gender_male),
                            selected = uiState.gender == GenderOption.Male,
                            onClick = { onGenderSelected(GenderOption.Male) },
                            modifier = Modifier.weight(1f),
                        )
                        GenderChip(
                            text = stringResource(R.string.gender_female),
                            selected = uiState.gender == GenderOption.Female,
                            onClick = { onGenderSelected(GenderOption.Female) },
                            modifier = Modifier.weight(1f),
                        )
                        GenderChip(
                            text = stringResource(R.string.gender_others),
                            selected = uiState.gender == GenderOption.Others,
                            onClick = { onGenderSelected(GenderOption.Others) },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    uiState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(12.dp))
                        if (uiState.accountAlreadyExists || uiState.googleLinkBlocked) {
                            AccountExistsBanner(
                                title = stringResource(
                                    if (uiState.googleLinkBlocked) {
                                        R.string.create_account_google_link_title
                                    } else {
                                        R.string.create_account_exists_title
                                    },
                                ),
                                message = error,
                                onSignIn = onSignIn,
                            )
                        } else {
                            Text(
                                text = error,
                                color = BrandOrange,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryButton(
                        text = if (uiState.isLoading) {
                            stringResource(R.string.loading)
                        } else {
                            stringResource(R.string.create_account)
                        },
                        onClick = onRegister,
                        enabled = !uiState.isLoading,
                        trailingArrow = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    OrContinueWithDivider()
                    Spacer(modifier = Modifier.height(14.dp))
                    GoogleButton(onClick = onGoogle)
                    Spacer(modifier = Modifier.height(8.dp))
                    AuthTextLink(
                        prefix = stringResource(R.string.already_have_account),
                        link = stringResource(R.string.sign_in_link),
                        onClick = onSignIn,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.terms_prefix))
                            append(' ')
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = BrandBlack)) {
                                append(stringResource(R.string.terms_conditions))
                            }
                            append(' ')
                            append(stringResource(R.string.terms_and))
                            append(' ')
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = BrandBlack)) {
                                append(stringResource(R.string.privacy_policy))
                            }
                            append('.')
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountExistsBanner(
    message: String,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.create_account_exists_title),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandOrange.copy(alpha = 0.08f))
            .border(1.dp, BrandOrange.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = BrandOrange,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = BrandBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.create_account_exists_action),
            color = BrandOrange,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .align(Alignment.End)
                .clickable(onClick = onSignIn)
                .padding(vertical = 2.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CreateAccountPreview() {
    LushAIEdu_PLSTheme {
        CreateAccountScreen(
            uiState = CreateAccountUiState(),
            onFullNameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onAddressChange = {},
            onGenderSelected = {},
            onAvatarSelected = {},
            onRegister = {},
            onSignIn = {},
            onGoogle = {},
        )
    }
}
