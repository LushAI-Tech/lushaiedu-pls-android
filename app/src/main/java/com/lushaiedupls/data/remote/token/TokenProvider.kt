package com.lushaiedupls.data.remote.token

/**
 * Supplies auth tokens to [com.lushaiedupls.data.remote.interceptors.AuthInterceptor]
 * and [com.lushaiedupls.data.remote.interceptors.TokenRefreshAuthenticator],
 * and persists them after login, refresh, and logout.
 */
interface TokenProvider {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveTokens(accessToken: String, refreshToken: String? = null)
    fun clearTokens()
    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()
}
