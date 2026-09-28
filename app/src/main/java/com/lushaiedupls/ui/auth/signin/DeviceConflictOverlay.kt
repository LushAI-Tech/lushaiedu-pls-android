package com.lushaiedupls.ui.auth.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.ActiveDeviceSession
import com.lushaiedupls.ui.common.OverlayScrimDialog
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)
private val PillShape = RoundedCornerShape(50)

@Composable
fun DeviceConflictOverlay(
    message: String,
    accountLabel: String?,
    devices: List<ActiveDeviceSession>,
    isResolving: Boolean,
    errorMessage: String?,
    onResolve: () -> Unit,
    onDismiss: () -> Unit,
) {
    val rows = devices.ifEmpty {
        listOf(ActiveDeviceSession(accountLabel = accountLabel))
    }
    OverlayScrimDialog(
        onDismiss = onDismiss,
        dismissEnabled = !isResolving,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .border(1.dp, BorderGray, CardShape)
                .background(BgWhite)
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.sign_in_device_conflict_title),
                color = BrandBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                color = BrandBlack,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                fontFamily = FontFamily.SansSerif,
            )
            if (!accountLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.sign_in_device_conflict_account),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = BrandBlack,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    title = accountLabel,
                    subtitle = null,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.sign_in_device_conflict_devices),
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgLight)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                rows.forEachIndexed { index, device ->
                    val title = device.deviceName?.takeIf { it.isNotBlank() }
                        ?: device.platform?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.sign_in_device_conflict_unknown_device)
                    val subtitle = buildList {
                        device.accountLabel?.takeIf {
                            it.isNotBlank() && !it.equals(accountLabel, ignoreCase = true)
                        }?.let(::add)
                        device.platform?.takeIf {
                            it.isNotBlank() && !it.equals(title, ignoreCase = true)
                        }?.let(::add)
                        device.lastActive?.takeIf { it.isNotBlank() }?.let(::add)
                    }.joinToString(" · ").ifBlank { null }
                    InfoRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Devices,
                                contentDescription = null,
                                tint = BrandBlack,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        title = title,
                        subtitle = subtitle,
                    )
                    if (index != rows.lastIndex) {
                        HorizontalDivider(color = BorderGray.copy(alpha = 0.6f))
                    }
                }
            }
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
                    .clickable(enabled = !isResolving, onClick = onResolve),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isResolving) {
                        stringResource(R.string.loading)
                    } else {
                        stringResource(R.string.sign_in_device_conflict_resolve)
                    },
                    color = androidx.compose.ui.graphics.Color.White,
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
                    .clickable(enabled = !isResolving, onClick = onDismiss),
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

@Composable
private fun InfoRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = BrandBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
