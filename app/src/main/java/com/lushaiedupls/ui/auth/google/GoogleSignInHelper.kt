package com.lushaiedupls.ui.auth.google

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.lushaiedupls.BuildConfig
import com.lushaiedupls.R
import java.security.MessageDigest
import kotlinx.coroutines.launch

object GoogleSignInHelper {
    private const val DEFAULT_WEB_CLIENT_ID =
        "502520884584-8fjhsif2pq1qakn76k4eb1j628p6n4mp.apps.googleusercontent.com"

    /**
     * Web client (oauth type 3) only. Android oauth_client ids in google-services.json are
     * never passed to the SDK — Play Services picks them by package + the APK's SHA-1.
     */
    fun webClientId(context: Context): String {
        val fromJson = runCatching { context.getString(R.string.default_web_client_id) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() && it.contains("apps.googleusercontent.com") }
        if (fromJson != null) return fromJson
        BuildConfig.GOOGLE_WEB_CLIENT_ID.takeIf { it.isNotBlank() }?.let { return it }
        return DEFAULT_WEB_CLIENT_ID
    }

    suspend fun requestIdToken(context: Context): Result<String> {
        val clientId = webClientId(context)
        GoogleOauthLogger.log(
            "Starting Google sign-in webClient=${clientId.substringBefore('.')} " +
                "sha1=${signingSha1(context)}",
        )
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
            .build()
        return try {
            parseIdToken(CredentialManager.create(context).getCredential(context, request).credential)
        } catch (e: GetCredentialCancellationException) {
            GoogleOauthLogger.log("Credential Manager cancelled", e)
            Result.failure(e)
        } catch (e: NoCredentialException) {
            GoogleOauthLogger.log("No matching Google credential (28433); use account picker", e)
            Result.failure(e)
        } catch (e: GetCredentialException) {
            GoogleOauthLogger.log("Sign in with Google failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun needsAccountPicker(error: Throwable?): Boolean {
        val message = error?.message.orEmpty()
        return error is NoCredentialException ||
            message.contains("28433") ||
            message.contains("cannot find a matching credential", ignoreCase = true)
    }

    fun launchAccountPicker(
        activity: Activity,
        onLaunch: (IntentSenderRequest) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val clientId = webClientId(activity)
        GoogleOauthLogger.log("Requesting account picker intent…")
        Identity.getSignInClient(activity)
            .getSignInIntent(
                GetSignInIntentRequest.builder()
                    .setServerClientId(clientId)
                    .build(),
            )
            .addOnSuccessListener { pendingIntent ->
                GoogleOauthLogger.log("Account picker launched")
                onLaunch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            }
            .addOnFailureListener { error ->
                GoogleOauthLogger.log("Google account picker failed", error)
                onError(error)
            }
    }

    fun handleSignInResult(context: Context, data: Intent?, resultCode: Int? = null): Result<String> {
        GoogleOauthLogger.log(
            "Account picker result code=${resultCode ?: "n/a"} dataNull=${data == null}",
        )
        if (data == null) {
            val error = IllegalStateException("Google sign-in was cancelled.")
            GoogleOauthLogger.log("No result data from account picker", error)
            return Result.failure(error)
        }
        return try {
            val credential = Identity.getSignInClient(context).getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken
            if (idToken.isNullOrBlank()) {
                val error = IllegalStateException("Google ID token was empty.")
                GoogleOauthLogger.log("Account selected but ID token empty", error)
                Result.failure(error)
            } else {
                GoogleOauthLogger.log(
                    buildString {
                        appendLine("Google account selected / ID token received")
                        appendLine("  googleUserId: ${credential.id}")
                        appendLine("  displayName: ${credential.displayName}")
                        appendLine("  idTokenLength: ${idToken.length}")
                    }.trimEnd(),
                )
                Result.success(idToken)
            }
        } catch (e: ApiException) {
            GoogleOauthLogger.log("Google sign-in result failed: status=${e.statusCode}", e)
            Result.failure(e)
        } catch (e: Throwable) {
            GoogleOauthLogger.log("Google sign-in result failed", e)
            Result.failure(e)
        }
    }

    private fun parseIdToken(credential: Credential): Result<String> {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = google.idToken
            if (idToken.isBlank()) {
                return Result.failure(IllegalStateException("Google ID token was empty."))
            }
            GoogleOauthLogger.log(
                buildString {
                    appendLine("Google sign-in succeeded")
                    appendLine("  googleUserId: ${google.id}")
                    appendLine("  displayName: ${google.displayName}")
                    appendLine("  idTokenLength: ${idToken.length}")
                }.trimEnd(),
            )
            return Result.success(idToken)
        }
        return Result.failure(IllegalStateException("Unexpected Google credential type."))
    }

    fun signingSha1(context: Context): String {
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES,
            ).signatures
        }
        val first = signatures?.firstOrNull() ?: return "unknown"
        val digest = MessageDigest.getInstance("SHA-1").digest(first.toByteArray())
        return digest.joinToString(":") { "%02X".format(it) }
    }

    fun googleSignInUserMessage(error: Throwable): String {
        val message = error.message.orEmpty()
        val friendly = when {
            error is ApiException && error.statusCode == 12501 ->
                "Google sign-in was cancelled."
            error is GetCredentialCancellationException ||
                message.contains("cancel", ignoreCase = true) ->
                "Google sign-in was cancelled."
            message.contains("28444") ||
                message.contains("Developer console is not set up correctly", ignoreCase = true) ||
                message.contains("reauth failed", ignoreCase = true) ->
                "Google Sign-In could not verify this app build. " +
                    "In Play Console → App signing, copy the App signing key SHA-1 " +
                    "(not the upload key) into Firebase Android fingerprints, " +
                    "re-download google-services.json, and ship a new Play build."
            message.contains("28433") ||
                error is NoCredentialException ||
                message.contains("cannot find a matching credential", ignoreCase = true) ->
                "No Google account was available. Please try again and pick an account."
            message.contains("GOOGLE_WEB_CLIENT_ID", ignoreCase = true) ||
                message.contains("Web Client ID", ignoreCase = true) ->
                message
            else -> message.ifBlank { "Google sign-in failed. Please try again." }
        }
        return GoogleOauthLogger.uiMessage(friendly, error)
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
    onStatus: ((String) -> Unit)? = null,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        GoogleOauthLogger.log("ActivityResult resultCode=${result.resultCode}")
        onStatus?.invoke("Google account picker returned…")
        GoogleSignInHelper.handleSignInResult(context, result.data, result.resultCode)
            .onSuccess { token ->
                onStatus?.invoke("Account selected. Signing in to server…")
                onIdToken(token)
            }
            .onFailure { onError(GoogleSignInHelper.googleSignInUserMessage(it)) }
    }
    return remember(context, onIdToken, onError, onStatus, picker) {
        {
            val activity = context.findActivity()
            if (activity == null) {
                onError("Unable to start Google sign-in.")
            } else {
                scope.launch {
                    GoogleOauthLogger.clear()
                    GoogleOauthLogger.log("Google sign-in tapped")
                    onStatus?.invoke("Starting Google sign-in…")
                    val result = GoogleSignInHelper.requestIdToken(activity)
                    result.fold(
                        onSuccess = { token ->
                            onStatus?.invoke("Google credential received. Signing in to server…")
                            onIdToken(token)
                        },
                        onFailure = { error ->
                            if (GoogleSignInHelper.needsAccountPicker(error)) {
                                GoogleOauthLogger.log("Falling back to account picker")
                                onStatus?.invoke("Opening Google account picker…")
                                GoogleSignInHelper.launchAccountPicker(
                                    activity = activity,
                                    onLaunch = picker::launch,
                                    onError = { onError(GoogleSignInHelper.googleSignInUserMessage(it)) },
                                )
                            } else {
                                onError(GoogleSignInHelper.googleSignInUserMessage(error))
                            }
                        },
                    )
                }
            }
        }
    }
}
