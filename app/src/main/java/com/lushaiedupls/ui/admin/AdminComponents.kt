package com.lushaiedupls.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lushaiedupls.R
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.auth.components.SecondaryButton
import com.lushaiedupls.ui.common.AnimatedFilterChipRow
import com.lushaiedupls.ui.common.AppBackNav
import com.lushaiedupls.ui.common.CenteredEmptyState
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

val AdminCardShape = RoundedCornerShape(16.dp)
val AdminChipShape = RoundedCornerShape(50)
val AdminDeleteRed = Color(0xFFF25F5C)
val AdminPaidGreen = Color(0xFF22C55E)

@Composable
fun AdminScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (onBack != null) {
            AppBackNav(
                onBack = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        if (actions != null) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions()
            }
        }
    }
}

@Composable
fun AdminFilterRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedFilterChipRow(
        options = labels,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
fun AdminEditDeleteIcons(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    editDescription: String,
    deleteDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = editDescription,
                tint = BrandBlack,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = deleteDescription,
                tint = AdminDeleteRed,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun AdminManageToggle(
    managing: Boolean,
    onToggle: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier.size(40.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = contentDescription,
            tint = if (managing) BrandOrange else BrandBlack,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun AdminCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AdminCardShape)
            .border(
                1.dp,
                if (highlighted) BrandOrange else BorderGray.copy(alpha = 0.7f),
                AdminCardShape,
            )
            .background(if (highlighted) BrandOrange.copy(alpha = 0.08f) else BgWhite, AdminCardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
fun AdminLeadingIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    background: Color = BgLight,
    tint: Color = BrandBlack,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun AdminSubjectLeadingIcon(
    name: String,
    code: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BrandBlack),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(TeacherUiMappers.subjectIcon(name, code)),
            contentDescription = name,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(Color.White),
        )
    }
}

@Composable
fun AdminEmptyText(
    text: String,
    icon: ImageVector = Icons.Outlined.Inbox,
    modifier: Modifier = Modifier,
    fillMaxSize: Boolean = false,
    compact: Boolean = false,
) {
    CenteredEmptyState(
        message = text,
        icon = icon,
        modifier = modifier,
        fillMaxSize = fillMaxSize,
        compact = compact,
    )
}

@Composable
fun AdminMuted(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 13.sp,
        fontFamily = FontFamily.SansSerif,
    )
}

@Composable
fun AdminActionRow(
    actions: List<Pair<String, () -> Unit>>,
    destructiveIndex: Int? = null,
) {
    if (actions.isEmpty()) return
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.forEachIndexed { index, (label, onClick) ->
            val destructive = index == destructiveIndex
            Text(
                text = label,
                color = if (destructive) AdminDeleteRed else BrandBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .clip(AdminChipShape)
                    .border(
                        1.dp,
                        if (destructive) AdminDeleteRed else BorderGray,
                        AdminChipShape,
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
fun AdminNoticeDialog(
    message: String,
    onDismiss: () -> Unit,
    title: String? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgWhite)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BrandOrange.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = BrandOrange,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        color = BrandBlack,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = message,
                    color = BrandBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(
                    text = stringResource(R.string.ok),
                    onClick = onDismiss,
                    fullyRounded = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun AdminDeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(R.string.admin_delete),
    cancelLabel: String = stringResource(R.string.admin_cancel),
    isWorking: Boolean = false,
) {
    Dialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(enabled = !isWorking, onClick = onDismiss)
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgWhite)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AdminDeleteRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = AdminDeleteRed,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = title,
                    color = BrandBlack,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SecondaryButton(
                        text = cancelLabel,
                        onClick = onDismiss,
                        enabled = !isWorking,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = if (isWorking) stringResource(R.string.loading) else confirmLabel,
                        onClick = onConfirm,
                        enabled = !isWorking,
                        fullyRounded = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
