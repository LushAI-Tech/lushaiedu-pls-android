package com.lushaiedupls.ui.auth.setupprofile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.components.AuthProfilePhotoPicker
import com.lushaiedupls.ui.auth.components.GenderChip
import com.lushaiedupls.ui.auth.components.LushAiEduWordmark
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.auth.signup.GenderOption
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

@Composable
fun SetupProfileRoute(
    authRepository: AuthRepository,
    studentRepository: StudentRepository,
    userSessionStore: UserSessionStore,
    onContinue: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupProfileViewModel = viewModel(
        factory = SetupProfileViewModel.provideFactory(
            authRepository,
            studentRepository,
            userSessionStore,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = !uiState.isLoading, onBack = onBack)

    LaunchedEffect(uiState.successRoute) {
        uiState.successRoute?.let { route ->
            viewModel.clearNavigation()
            onContinue(route)
        }
    }

    SetupProfileScreen(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChange,
        onPhoneChange = viewModel::onPhoneChange,
        onAddressChange = viewModel::onAddressChange,
        onGenderSelected = viewModel::onGenderSelected,
        onAvatarSelected = viewModel::onAvatarSelected,
        onContinue = { viewModel.submit(context) },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun SetupProfileScreen(
    uiState: SetupProfileUiState,
    onUsernameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onGenderSelected: (GenderOption) -> Unit,
    onAvatarSelected: (Uri) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
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
                        text = stringResource(R.string.setup_profile_title),
                        color = BrandBlack,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.setup_profile_subtitle),
                        color = TextSecondary,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    AuthProfilePhotoPicker(
                        localUri = uiState.avatarUri,
                        remoteUrl = uiState.avatarUrl,
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
                        label = stringResource(R.string.username),
                        value = uiState.username,
                        onValueChange = onUsernameChange,
                        placeholder = stringResource(R.string.username_placeholder),
                        leadingIcon = Icons.Outlined.Person,
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
                        Text(
                            text = error,
                            color = BrandOrange,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryButton(
                        text = if (uiState.isLoading) {
                            stringResource(R.string.loading)
                        } else {
                            stringResource(R.string.continue_label)
                        },
                        onClick = onContinue,
                        enabled = !uiState.isLoading,
                        trailingArrow = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = onBack,
                        enabled = !uiState.isLoading,
                    ) {
                        Text(
                            text = stringResource(R.string.back),
                            color = TextSecondary,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}
