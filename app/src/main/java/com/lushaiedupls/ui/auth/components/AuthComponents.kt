package com.lushaiedupls.ui.auth.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
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
            fontSize = 16.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
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
    singleLine: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
            modifier = Modifier.fillMaxWidth(),
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
            singleLine = singleLine,
            shape = FieldShape,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
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
