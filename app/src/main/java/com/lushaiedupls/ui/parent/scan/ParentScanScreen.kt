package com.lushaiedupls.ui.parent.scan

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.ParentRepository

@Composable
fun rememberParentQrScanLauncher(
    parentRepository: ParentRepository,
    onLinked: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val viewModel: ParentScanViewModel = viewModel(
        factory = ParentScanViewModel.provideFactory(parentRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    return {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            scanLauncher.launch(parentScanOptions(context.getString(R.string.parent_scan_prompt)))
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

private fun parentScanOptions(prompt: String): ScanOptions =
    ScanOptions()
        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        .setPrompt(prompt)
        .setBeepEnabled(false)
        .setOrientationLocked(true)
