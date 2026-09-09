package com.lushaiedupls.data.remote.device

fun interface FcmTokenSource {
    suspend fun currentToken(): String?
}
