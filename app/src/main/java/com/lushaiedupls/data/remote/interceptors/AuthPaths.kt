package com.lushaiedupls.data.remote.interceptors

object AuthPaths {
    fun isRefresh(encodedPath: String): Boolean =
        encodedPath.endsWith("/auth/refresh")

    fun isPublicAuth(encodedPath: String): Boolean =
        encodedPath.endsWith("/auth/login") ||
            encodedPath.endsWith("/auth/register") ||
            encodedPath.endsWith("/auth/google") ||
            encodedPath.endsWith("/auth/device-conflict/resolve") ||
            isRefresh(encodedPath)
}
