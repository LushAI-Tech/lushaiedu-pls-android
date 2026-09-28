package com.lushaiedupls.ui.auth.google

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.common.api.ApiException
import com.lushaiedupls.BuildConfig
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google OAuth diagnostic logs for login / sign-in.
 *
 * Toggle via `local.properties`: `ENABLE_GOOGLE_OAUTH_LOGS=true` then Sync/Rebuild.
 */
object GoogleOauthLogger {
    const val TAG = "GoogleSignIn"
    private const val MAX_LINES = 120

    val enabled: Boolean
        get() = BuildConfig.ENABLE_GOOGLE_OAUTH_LOGS

    private val lines = CopyOnWriteArrayList<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun clear() {
        lines.clear()
        _revision.value = _revision.value + 1
    }

    fun snapshot(): String = lines.joinToString("\n")

    fun hasLogs(): Boolean = lines.isNotEmpty()

    fun log(message: String, throwable: Throwable? = null) {
        if (!enabled) return
        val stamped = "${timeFormat.format(Date())}  $message"
        append(stamped)
        if (throwable != null) {
            append("  ${throwable.javaClass.simpleName}: ${throwable.message.orEmpty()}")
            if (throwable is ApiException) {
                append("  status=${throwable.statusCode}")
            }
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
        _revision.value = _revision.value + 1
    }

    fun uiMessage(friendly: String, error: Throwable? = null): String {
        log(friendly, error)
        return friendly
    }

    fun uiNetworkMessage(friendly: String, result: NetworkResult<*>): String {
        when (result) {
            is NetworkResult.Error -> log(
                "auth/google failed: HTTP ${result.code} — ${result.message}" +
                    (result.body?.let { " body=$it" }.orEmpty()),
            )
            is NetworkResult.Exception -> log("auth/google failed", result.throwable)
            else -> Unit
        }
        return friendly
    }

    private fun append(line: String) {
        lines.add(line)
        while (lines.size > MAX_LINES) {
            lines.removeAt(0)
        }
    }
}

/**
 * Status + expandable OAuth activity dump under the Google button.
 * Shows for in-progress, success, and error when [statusMessage] is set.
 */
@Composable
fun GoogleOauthErrorPanel(
    statusMessage: String?,
    modifier: Modifier = Modifier,
    isError: Boolean = true,
) {
    val revision by GoogleOauthLogger.revision.collectAsState()
    val summary = statusMessage?.takeIf { it.isNotBlank() }
    val logsEnabled = GoogleOauthLogger.enabled
    val logText = remember(summary, revision, logsEnabled) {
        if (logsEnabled) GoogleOauthLogger.snapshot() else ""
    }
    if (summary == null && logText.isBlank()) return

    var expanded by remember(summary) {
        mutableStateOf(logsEnabled && logText.isNotBlank())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        if (summary != null) {
            Text(
                text = summary,
                color = if (isError) BrandOrange else TextSecondary,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (logsEnabled && logText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (expanded) R.string.oauth_logs_hide else R.string.oauth_logs_show,
                ),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = logText,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgLight)
                        .padding(10.dp),
                )
            }
        }
    }
}
