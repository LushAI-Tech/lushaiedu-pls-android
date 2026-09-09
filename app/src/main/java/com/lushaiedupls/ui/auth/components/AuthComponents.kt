package com.lushaiedupls.ui.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary
import com.lushaiedupls.ui.theme.TileGray
import com.lushaiedupls.ui.theme.TileSelected

private val PillShape = RoundedCornerShape(28.dp)
private val ActionShape = RoundedCornerShape(12.dp)
private val FieldShape = RoundedCornerShape(12.dp)
private val TileShape = RoundedCornerShape(18.dp)

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingArrow: Boolean = false,
    fullyRounded: Boolean = false,
    height: Dp = 52.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = if (fullyRounded) PillShape else ActionShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandBlack,
            contentColor = Color.White,
            disabledContainerColor = BrandBlack.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(
            text = if (trailingArrow) "$text  →" else text,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = ActionShape,
        border = BorderStroke(1.dp, BrandBlack),
        colors = ButtonDefaults.buttonColors(
            containerColor = com.lushaiedupls.ui.theme.BackButtonBg,
            contentColor = BrandBlack,
            disabledContainerColor = com.lushaiedupls.ui.theme.BackButtonBg.copy(alpha = 0.5f),
            disabledContentColor = BrandBlack.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        )
    }
}

@Composable
fun OnboardingStepHeader(
    title: String,
    step: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    hint: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_step, step, totalSteps),
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
            textAlign = TextAlign.Center,
        )
        subtitle?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = TextSecondary,
                fontSize = 14.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            )
        }
        hint?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = TextSecondary,
                fontSize = 13.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun SelectionNavButtons(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    continueLabel: String = stringResource(R.string.continue_label),
    backLabel: String = stringResource(R.string.back),
    continueTrailingArrow: Boolean = true,
    continueEnabled: Boolean = true,
    backEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryButton(
            text = backLabel,
            onClick = onBack,
            enabled = backEnabled,
            height = 48.dp,
            modifier = Modifier
                .weight(0.9f)
                .fillMaxWidth(),
        )
        PrimaryButton(
            text = continueLabel,
            onClick = onContinue,
            enabled = continueEnabled,
            trailingArrow = continueTrailingArrow,
            height = 48.dp,
            modifier = Modifier
                .weight(1.2f)
                .fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutlinedAuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var passwordVisible by remember { mutableStateOf(false) }
    val resolvedKeyboardOptions = if (isPassword) {
        keyboardOptions.copy(keyboardType = KeyboardType.Password)
    } else {
        keyboardOptions
    }
    val resolvedTransformation = when {
        isPassword && !passwordVisible -> PasswordVisualTransformation()
        isPassword -> VisualTransformation.None
        else -> visualTransformation
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester),
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = BrandBlack,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusEvent { event ->
                    if (event.isFocused) {
                        scope.launch {
                            withFrameNanos { }
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                },
            placeholder = {
                Text(text = placeholder, color = TextSecondary, fontSize = 14.sp)
            },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = TextSecondary,
                    )
                }
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = stringResource(
                                if (passwordVisible) {
                                    R.string.cd_hide_password
                                } else {
                                    R.string.cd_show_password
                                },
                            ),
                            tint = TextSecondary,
                        )
                    }
                }
            } else {
                null
            },
            enabled = enabled,
            singleLine = singleLine,
            shape = FieldShape,
            keyboardOptions = resolvedKeyboardOptions,
            visualTransformation = resolvedTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandBlack,
                unfocusedBorderColor = BorderGray,
                focusedContainerColor = BgWhite,
                unfocusedContainerColor = BgWhite,
                cursorColor = BrandBlack,
            ),
        )
    }
}

@Composable
fun OrContinueWithDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(BorderGray),
        )
        Text(
            text = stringResource(R.string.or_continue_with),
            modifier = Modifier.padding(horizontal = 12.dp),
            color = BrandBlack,
            fontSize = 13.sp,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(BorderGray),
        )
    }
}

@Composable
fun AuthProfilePhotoPicker(
    localUri: Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    remoteUrl: String? = null,
    enabled: Boolean = true,
    size: Dp = 88.dp,
) {
    val context = LocalContext.current
    val displayModel: Any? = localUri
        ?: AuthRepository.normalizeAvatarUrl(remoteUrl)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            key(displayModel) {
                if (displayModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(displayModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.cd_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape),
                        placeholder = painterResource(R.drawable.ic_avatar_placeholder),
                        error = painterResource(R.drawable.ic_avatar_placeholder),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_avatar_placeholder),
                        contentDescription = stringResource(R.string.cd_avatar),
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape),
                    )
                }
            }
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
                    contentDescription = stringResource(R.string.setup_profile_picture),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_profile_picture),
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun GoogleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderGray),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlack),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.google),
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
        )
    }
}

@Composable
fun GenderChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(FieldShape)
            .border(
                width = 1.dp,
                color = if (selected) BrandBlack else BorderGray,
                shape = FieldShape,
            )
            .background(if (selected) BrandBlack.copy(alpha = 0.06f) else BgWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            color = BrandBlack,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun SelectionTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconRes: Int? = null,
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(6.dp, TileShape, clip = false)
            .clip(TileShape)
            .background(if (selected) TileSelected else TileGray)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            iconRes != null -> {
                androidx.compose.foundation.Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    colorFilter = ColorFilter.tint(Color.White),
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            icon != null -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
        Text(
            text = label.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
        )
    }
}

@Composable
fun AuthTextLink(
    prefix: String,
    link: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    linkColor: Color = BrandBlack,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = "$prefix ",
            color = TextSecondary,
            fontSize = 14.sp,
        )
        Text(
            text = link,
            color = linkColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}
