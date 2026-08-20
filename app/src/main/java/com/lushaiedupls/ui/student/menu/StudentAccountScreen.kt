package com.lushaiedupls.ui.student.menu

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.RegisteredDevice
import com.lushaiedupls.data.mock.StudentMockRepository
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LogoutButton
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)
private val IconShape = RoundedCornerShape(10.dp)
private val PillShape = RoundedCornerShape(50)
private val DangerRed = Color(0xFFF25F5C)

@Composable
fun StudentAccountRoute(
    userSessionStore: UserSessionStore,
    studentRepository: StudentRepository,
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onLogOut: () -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentAccountViewModel = viewModel(
        factory = StudentAccountViewModel.provideFactory(
            userSessionStore,
            studentRepository,
            authRepository,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val profileUpdated = stringResource(R.string.account_profile_updated)
    val passwordChanged = stringResource(R.string.account_password_changed)
    val passwordSet = stringResource(R.string.account_password_set)

    LaunchedEffect(uiState.notice) {
        val notice = uiState.notice ?: return@LaunchedEffect
        val message = when (notice) {
            AccountNotice.ProfileUpdated -> profileUpdated
            AccountNotice.PasswordChanged -> passwordChanged
            AccountNotice.PasswordSet -> passwordSet
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.clearNotice()
    }

    LaunchedEffect(uiState.signOutAllSucceeded) {
        if (!uiState.signOutAllSucceeded) return@LaunchedEffect
        viewModel.clearSignOutAllSucceeded()
        onLogOut()
    }

    if (uiState.isLoading && uiState.email.isBlank() && uiState.devices.isEmpty()) {
        StudentPageSkeleton(
            kind = StudentSkeletonKind.Account,
            title = stringResource(R.string.account_title),
            modifier = modifier,
        )
        return
    }
    StudentAccountScreen(
        displayName = uiState.displayName,
        email = uiState.email,
        avatarUrl = uiState.avatarUrl,
        hasPassword = uiState.hasPassword,
        devices = uiState.devices,
        onBack = onBack,
        onEditProfile = viewModel::openEditProfile,
        onPassword = viewModel::openPassword,
        onSignOutAll = viewModel::openSignOutAllConfirm,
        onLogOut = onLogOut,
        onDeleteAccountConfirmed = onDeleteAccountConfirmed,
        modifier = modifier,
    )
    if (uiState.showEditProfile) {
        val ctx = LocalContext.current
        EditProfileOverlay(
            name = uiState.editName,
            phone = uiState.editPhone,
            address = uiState.editAddress,
            currentAvatarUrl = uiState.avatarUrl,
            pendingAvatarUri = uiState.editAvatarUri,
            isSaving = uiState.isSaving,
            errorMessage = uiState.formError,
            onNameChange = viewModel::onEditNameChange,
            onPhoneChange = viewModel::onEditPhoneChange,
            onAddressChange = viewModel::onEditAddressChange,
            onAvatarSelected = viewModel::onEditAvatarSelected,
            onSave = { viewModel.saveProfile(ctx) },
            onDismiss = viewModel::dismissEditProfile,
        )
    }
    if (uiState.showPassword) {
        PasswordOverlay(
            hasPassword = uiState.hasPassword,
            currentPassword = uiState.currentPassword,
            newPassword = uiState.newPassword,
            confirmPassword = uiState.confirmPassword,
            isSaving = uiState.isSaving,
            errorMessage = uiState.formError,
            onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
            onNewPasswordChange = viewModel::onNewPasswordChange,
            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
            onSave = viewModel::savePassword,
            onDismiss = viewModel::dismissPassword,
        )
    }
    if (uiState.showSignOutAllConfirm) {
        SignOutAllOverlay(
            isSigningOut = uiState.isSigningOutAll,
            errorMessage = uiState.signOutAllError,
            onConfirm = viewModel::confirmSignOutAll,
            onDismiss = viewModel::dismissSignOutAllConfirm,
        )
    }
}

@Composable
fun StudentAccountScreen(
    displayName: String,
    email: String,
    devices: List<RegisteredDevice>,
    onBack: () -> Unit,
    onLogOut: () -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    hasPassword: Boolean = true,
    onEditProfile: () -> Unit = {},
    onPassword: () -> Unit = {},
    onSignOutAll: () -> Unit = {},
) {
    var showDeleteOverlay by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgLight)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgWhite)
                    .padding(horizontal = 4.dp)
                    .height(56.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = BrandBlack,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.account_title),
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                ProfileCard(
                    displayName = displayName,
                    email = email,
                    avatarUrl = avatarUrl,
                    onEditProfile = onEditProfile,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SimpleActionCard(
                    title = stringResource(
                        if (hasPassword) {
                            R.string.account_change_password
                        } else {
                            R.string.account_set_password
                        },
                    ),
                    icon = Icons.Outlined.Lock,
                    onClick = onPassword,
                )
                Spacer(modifier = Modifier.height(12.dp))
                DevicesCard(
                    devices = devices,
                    onSignOutAll = onSignOutAll,
                )
                Spacer(modifier = Modifier.height(20.dp))
                LogoutButton(onClick = onLogOut)
                Spacer(modifier = Modifier.height(12.dp))
                LogoutButton(
                    onClick = { showDeleteOverlay = true },
                    text = stringResource(R.string.account_delete),
                )
            }
        }

        if (showDeleteOverlay) {
            DeleteAccountOverlay(
                onDismiss = { showDeleteOverlay = false },
                onConfirmDelete = {
                    showDeleteOverlay = false
                    onDeleteAccountConfirmed()
                },
            )
        }
    }
}

@Composable
private fun ProfileCard(
    displayName: String,
    email: String,
    onEditProfile: () -> Unit,
    avatarUrl: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(BgWhite)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(
            url = avatarUrl,
            uri = null,
            size = 52,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = email,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(PillShape)
                .background(BrandBlack)
                .clickable(onClick = onEditProfile)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.account_edit_profile),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SimpleActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(BgWhite)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(IconShape)
                .background(BrandBlack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = BrandBlack,
        )
    }
}

@Composable
private fun DevicesCard(
    devices: List<RegisteredDevice>,
    onSignOutAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(BgWhite)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(IconShape)
                    .background(BrandBlack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Devices,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.account_registered_devices),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = BrandBlack,
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DeviceHeader(stringResource(R.string.account_platform), Modifier.weight(1f))
            DeviceHeader(stringResource(R.string.account_last_active), Modifier.weight(1.4f))
            DeviceHeader(stringResource(R.string.account_sessions), Modifier.weight(0.8f), TextAlign.End)
        }
        Spacer(modifier = Modifier.height(8.dp))
        devices.forEachIndexed { index, device ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = device.platform, modifier = Modifier.weight(1f), fontSize = 13.sp, color = BrandBlack)
                Text(text = device.lastActive, modifier = Modifier.weight(1.4f), fontSize = 12.sp, color = TextSecondary)
                Text(
                    text = device.sessions,
                    modifier = Modifier.weight(0.8f),
                    fontSize = 13.sp,
                    color = BrandBlack,
                    textAlign = TextAlign.End,
                )
            }
            if (index != devices.lastIndex) {
                HorizontalDivider(color = BorderGray.copy(alpha = 0.5f))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(PillShape)
                    .background(BrandBlack)
                    .clickable(onClick = onSignOutAll)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.account_sign_out_all),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DeviceHeader(
    text: String,
    modifier: Modifier,
    align: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = modifier,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = align,
    )
}

@Composable
private fun AvatarImage(
    url: String?,
    uri: Uri?,
    size: Int,
    modifier: Modifier = Modifier,
) {
    val mod = modifier
        .size(size.dp)
        .clip(CircleShape)
    val imageModel: Any? = uri ?: url?.takeIf { it.isNotBlank() }
    if (imageModel != null) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            modifier = mod,
            error = painterResource(R.drawable.ic_avatar_placeholder),
        )
    } else {
        Image(
            painter = painterResource(R.drawable.ic_avatar_placeholder),
            contentDescription = null,
            modifier = mod,
        )
    }
}

@Composable
private fun AccountFormDialog(
    onDismiss: () -> Unit,
    dismissEnabled: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (dismissEnabled) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(enabled = dismissEnabled, onClick = onDismiss)
                .imePadding()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .clip(CardShape)
                    .background(BgWhite)
                    .clickable(enabled = false) {}
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun EditProfileOverlay(
    name: String,
    phone: String,
    address: String,
    currentAvatarUrl: String?,
    pendingAvatarUri: Uri?,
    isSaving: Boolean,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onAvatarSelected: (Uri) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onAvatarSelected) }

    AccountFormDialog(onDismiss = onDismiss, dismissEnabled = !isSaving) {
        Text(
            text = stringResource(R.string.account_edit_profile),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Avatar picker circle
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clickable(enabled = !isSaving) {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
            ) {
                AvatarImage(
                    url = currentAvatarUrl,
                    uri = pendingAvatarUri,
                    size = 84,
                )
                // Camera badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(BrandBlack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedAuthField(
            label = stringResource(R.string.full_name),
            value = name,
            onValueChange = onNameChange,
            placeholder = stringResource(R.string.full_name_placeholder),
            leadingIcon = Icons.Outlined.Person,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedAuthField(
            label = stringResource(R.string.phone),
            value = phone,
            onValueChange = onPhoneChange,
            placeholder = stringResource(R.string.phone_placeholder),
            leadingIcon = Icons.Outlined.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedAuthField(
            label = stringResource(R.string.address),
            value = address,
            onValueChange = onAddressChange,
            placeholder = stringResource(R.string.address_placeholder),
        )
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = error, color = BrandOrange, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(
            text = if (isSaving) {
                stringResource(R.string.loading)
            } else {
                stringResource(R.string.account_save)
            },
            onClick = onSave,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(PillShape)
                .clickable(enabled = !isSaving, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.danger_zone_cancel),
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun PasswordOverlay(
    hasPassword: Boolean,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    isSaving: Boolean,
    errorMessage: String?,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AccountFormDialog(onDismiss = onDismiss, dismissEnabled = !isSaving) {
        Text(
            text = stringResource(
                if (hasPassword) {
                    R.string.account_change_password
                } else {
                    R.string.account_set_password
                },
            ),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (hasPassword) {
            OutlinedAuthField(
                label = stringResource(R.string.account_current_password),
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                placeholder = stringResource(R.string.account_current_password_placeholder),
                leadingIcon = Icons.Outlined.Lock,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        OutlinedAuthField(
            label = stringResource(R.string.account_new_password),
            value = newPassword,
            onValueChange = onNewPasswordChange,
            placeholder = stringResource(R.string.account_new_password_placeholder),
            leadingIcon = Icons.Outlined.Lock,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedAuthField(
            label = stringResource(R.string.account_confirm_password),
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = stringResource(R.string.account_confirm_password_placeholder),
            leadingIcon = Icons.Outlined.Lock,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
        )
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = error, color = BrandOrange, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(
            text = if (isSaving) {
                stringResource(R.string.loading)
            } else {
                stringResource(R.string.account_save)
            },
            onClick = onSave,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(PillShape)
                .clickable(enabled = !isSaving, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.danger_zone_cancel),
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
fun SignOutAllOverlay(
    isSigningOut: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!isSigningOut) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(enabled = !isSigningOut, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(CardShape)
                    .border(1.dp, BorderGray, CardShape)
                    .background(BgWhite)
                    .padding(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.account_sign_out_all_title),
                    color = BrandBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.account_sign_out_all_body),
                    color = BrandBlack,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    fontFamily = FontFamily.SansSerif,
                )
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = error, color = BrandOrange, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(PillShape)
                        .background(BrandBlack)
                        .clickable(enabled = !isSigningOut, onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isSigningOut) {
                            stringResource(R.string.loading)
                        } else {
                            stringResource(R.string.account_sign_out_all_confirm)
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(PillShape)
                        .clickable(enabled = !isSigningOut, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.danger_zone_cancel),
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteAccountOverlay(
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(CardShape)
                    .border(1.5.dp, DangerRed, CardShape)
                    .background(BgWhite)
                    .padding(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.danger_zone_title),
                    color = DangerRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.danger_zone_body),
                    color = BrandBlack,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(PillShape)
                        .background(DangerRed)
                        .clickable(onClick = onConfirmDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.account_delete),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(PillShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.danger_zone_cancel),
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun LegalDocumentScreen(
    title: String,
    body: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = BrandBlack,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BrandBlack,
            )
        }
        Text(
            text = body,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            color = BrandBlack,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun StudentAccountPreview() {
    val mock = StudentMockRepository()
    LushAIEdu_PLSTheme {
        StudentAccountScreen(
            displayName = "V Lalfakea",
            email = mock.accountEmail(),
            devices = mock.registeredDevices(),
            onBack = {},
            onLogOut = {},
            onDeleteAccountConfirmed = {},
        )
    }
}
