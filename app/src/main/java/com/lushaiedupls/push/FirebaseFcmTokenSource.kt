package com.lushaiedupls.push

import com.google.firebase.messaging.FirebaseMessaging
import com.lushaiedupls.data.remote.device.FcmTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirebaseFcmTokenSource : FcmTokenSource {
    override suspend fun currentToken(): String? = suspendCancellableCoroutine { cont ->
        val task = runCatching { FirebaseMessaging.getInstance().token }.getOrElse {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        task.addOnCompleteListener { completed ->
            if (!cont.isActive) return@addOnCompleteListener
            val token = if (completed.isSuccessful) {
                completed.result?.trim()?.takeIf { it.isNotEmpty() }
            } else {
                null
            }
            cont.resume(token)
        }
    }
}
