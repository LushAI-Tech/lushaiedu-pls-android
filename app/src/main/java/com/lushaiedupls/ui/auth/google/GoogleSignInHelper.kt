package com.lushaiedupls.ui.auth.google

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.common.api.ApiException
import com.lushaiedupls.BuildConfig

object GoogleSignInHelper {
    private const val TAG = "GoogleSignIn"
    private const val DEFAULT_WEB_CLIENT_ID =
        "502520884584-8fjhsif2pq1qakn76k4eb1j628p6n4mp.apps.googleusercontent.com"

    fun webClientId(): String {
        BuildConfig.GOOGLE_WEB_CLIENT_ID.takeIf { it.isNotBlank() }?.let { return it }
        return DEFAULT_WEB_CLIENT_ID
    }

    fun launchSignIn(
        activity: Activity,
        onLaunch: (IntentSenderRequest) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val clientId = webClientId()
        Log.d(TAG, "Starting Google sign-in (clientId=${clientId.take(24)}...)")
        if (clientId.isBlank()) {
            onError(IllegalStateException("Google Web Client ID is not configured."))
            return
        }
        Identity.getSignInClient(activity)
            .getSignInIntent(
                GetSignInIntentRequest.builder()
                    .setServerClientId(clientId)
                    .build(),
            )
            .addOnSuccessListener { pendingIntent ->
                onLaunch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Google sign-in launch failed", error)
                onError(error)
            }
    }

    fun handleSignInResult(context: Context, data: Intent?): Result<String> {
        if (data == null) {
            return Result.failure(IllegalStateException("Google sign-in was cancelled."))
        }
        return try {
            val credential = Identity.getSignInClient(context).getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken
            if (idToken.isNullOrBlank()) {
                Result.failure(IllegalStateException("Google ID token was empty."))
            } else {
                Log.d(
                    TAG,
                    buildString {
                        appendLine("Google sign-in succeeded")
                        appendLine("  googleUserId: ${credential.id}")
                        appendLine("  displayName: ${credential.displayName}")
                        appendLine("  idTokenLength: ${idToken.length}")
                        if (BuildConfig.DEBUG) {
                            appendLine("  idToken: $idToken")
                        }
                    }.trimEnd(),
                )
                Result.success(idToken)
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in result failed: status=${e.statusCode}", e)
            Result.failure(e)
        } catch (e: Throwable) {
            Log.e(TAG, "Google sign-in result failed", e)
            Result.failure(e)
        }
    }

    fun googleSignInUserMessage(error: Throwable): String {
        val message = error.message.orEmpty()
        if (error is ApiException && error.statusCode == 12501) {
            return "Google sign-in was cancelled."
        }
        return when {
            message.contains("28444") ||
                message.contains("Developer console is not set up correctly", ignoreCase = true) ||
                (error is ApiException && error.statusCode == 10) ->
                "Google Sign-In could not start. In Firebase project lushaiedupls-694e1, confirm debug SHA-1 is added, Google sign-in is enabled under Authentication, then rebuild the app."
            message.contains("cancel", ignoreCase = true) ->
                "Google sign-in was cancelled."
            message.contains("GOOGLE_WEB_CLIENT_ID", ignoreCase = true) ||
                message.contains("Web Client ID", ignoreCase = true) ->
                message
            else -> message.ifBlank { "Google sign-in failed. Please try again." }
        }
    }
}

fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun rememberGoogleSignInAction(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        GoogleSignInHelper.handleSignInResult(context, result.data)
            .onSuccess(onIdToken)
            .onFailure { onError(GoogleSignInHelper.googleSignInUserMessage(it)) }
    }
    return remember(context, launcher) {
        {
            val activity = context.findActivity()
            if (activity == null) {
                onError("Unable to start Google sign-in.")
            } else {
                GoogleSignInHelper.launchSignIn(
                    activity = activity,
                    onLaunch = launcher::launch,
                    onError = { onError(GoogleSignInHelper.googleSignInUserMessage(it)) },
                )
            }
        }
    }
}
