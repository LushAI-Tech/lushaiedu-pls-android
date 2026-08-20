package com.lushaiedupls.ui.student.linkparent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.encodeQrBitmap
import com.lushaiedupls.ui.parent.home.label
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(18.dp)

@Composable
fun StudentLinkParentRoute(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentLinkParentViewModel = viewModel(
        factory = StudentLinkParentViewModel.provideFactory(studentRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudentLinkParentScreen(
        uiState = uiState,
        onBack = onBack,
        onRefreshQr = viewModel::issueToken,
        onRevoke = viewModel::revoke,
        modifier = modifier,
    )
}

@Composable
fun StudentLinkParentScreen(
    uiState: StudentLinkParentUiState,
    onBack: () -> Unit,
    onRefreshQr: () -> Unit,
    onRevoke: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val qrBitmap = remember(uiState.token?.token) {
        uiState.token?.token?.let { encodeQrBitmap(it) }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.back),
                    tint = BrandBlack,
                )
            }
            Text(
                text = stringResource(R.string.student_link_parent_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.student_link_parent_body),
            color = TextSecondary,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.student_link_parent_title),
                    modifier = Modifier.size(240.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            val remaining = uiState.remainingSeconds
            Text(
                text = if (remaining > 0) {
                    stringResource(R.string.student_qr_expires, remaining)
                } else {
                    stringResource(R.string.student_qr_expired)
                },
                color = if (remaining > 0) BrandBlack else BrandOrange,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
            )
            uiState.token?.token?.let { token ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = token,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif,
                )
            }
            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message, color = BrandOrange, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            PrimaryButton(
                text = if (uiState.isIssuing) {
                    stringResource(R.string.loading)
                } else {
                    stringResource(R.string.student_refresh_qr)
                },
                onClick = onRefreshQr,
                fullyRounded = true,
                enabled = !uiState.isIssuing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.student_linked_parents),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.parents.isEmpty()) {
            Text(
                text = stringResource(R.string.student_no_linked_parents),
                color = TextSecondary,
                fontSize = 14.sp,
            )
        } else {
            uiState.parents.forEach { link ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = link.parent.name,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBlack,
                        )
                        Text(
                            text = link.relationship.label(),
                            color = TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                    TextButton(onClick = { onRevoke(link.id) }) {
                        Text(text = stringResource(R.string.student_unlink_parent), color = BrandOrange)
                    }
                }
            }
        }
    }
}
