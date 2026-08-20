package com.lushaiedupls.ui.parent.scan

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.dto.ParentRelationship
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.parent.home.label
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

private val ChipShape = RoundedCornerShape(50)

@Composable
fun ParentScanRoute(
    parentRepository: ParentRepository,
    onLinked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ParentScanViewModel = viewModel(
        factory = ParentScanViewModel.provideFactory(parentRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) viewModel.onScanned(contents)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scanLauncher.launch(parentScanOptions(context.getString(R.string.parent_scan_prompt)))
        } else {
            Toast.makeText(context, context.getString(R.string.parent_camera_denied), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        val message = uiState.successMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.consumeSuccess()
        onLinked()
    }

    ParentScanScreen(
        uiState = uiState,
        onTokenChange = viewModel::onTokenChange,
        onRelationshipSelected = viewModel::onRelationshipSelected,
        onScanClick = {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                scanLauncher.launch(parentScanOptions(context.getString(R.string.parent_scan_prompt)))
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onLinkClick = viewModel::redeem,
        modifier = modifier,
    )
}

@Composable
fun ParentScanScreen(
    uiState: ParentScanUiState,
    onTokenChange: (String) -> Unit,
    onRelationshipSelected: (ParentRelationship) -> Unit,
    onScanClick: () -> Unit,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 28.dp),
    ) {
        Text(
            text = stringResource(R.string.parent_scan_title),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.parent_scan_body),
            color = TextSecondary,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(R.string.parent_scan_qr),
            onClick = onScanClick,
            fullyRounded = true,
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.parent_relationship),
            fontWeight = FontWeight.SemiBold,
            color = BrandBlack,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ParentRelationship.entries.forEach { relation ->
                val selected = uiState.relationship == relation
                Text(
                    text = relation.label(),
                    modifier = Modifier
                        .clip(ChipShape)
                        .background(if (selected) BrandBlack else BgWhite)
                        .border(1.dp, if (selected) BrandBlack else BorderGray, ChipShape)
                        .clickable { onRelationshipSelected(relation) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (selected) androidx.compose.ui.graphics.Color.White else BrandBlack,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        OutlinedAuthField(
            value = uiState.token,
            onValueChange = onTokenChange,
            label = stringResource(R.string.parent_token_label),
            placeholder = stringResource(R.string.parent_token_placeholder),
        )
        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, color = BrandOrange, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(
            text = if (uiState.isSubmitting) {
                stringResource(R.string.loading)
            } else {
                stringResource(R.string.parent_link_student)
            },
            onClick = onLinkClick,
            fullyRounded = true,
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun parentScanOptions(prompt: String): ScanOptions =
    ScanOptions()
        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        .setPrompt(prompt)
        .setBeepEnabled(false)
        .setOrientationLocked(true)
