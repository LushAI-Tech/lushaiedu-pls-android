package com.lushaiedupls.data.repository

import com.lushaiedupls.data.remote.token.TokenProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SessionRepository(
    private val tokenProvider: TokenProvider,
) {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    fun isLoggedIn(): Boolean = tokenProvider.isLoggedIn()

    fun saveSession(accessToken: String, refreshToken: String? = null) {
        tokenProvider.saveTokens(accessToken, refreshToken)
    }

    fun logout() {
        tokenProvider.clearTokens()
    }

    fun onUnauthorized() {
        tokenProvider.clearTokens()
        _sessionExpired.tryEmit(Unit)
    }
}
