package com.lushaiedupls.data.remote

object ApiConfig {
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L
    const val LONG_READ_TIMEOUT_SECONDS = 120L
    const val LONG_WRITE_TIMEOUT_SECONDS = 60L

    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_ACCEPT = "Accept"
    const val HEADER_ACCEPT_LANGUAGE = "Accept-Language"
    const val HEADER_APP_VERSION = "X-App-Version"
    const val HEADER_PLATFORM = "X-Platform"

    const val CONTENT_TYPE_JSON = "application/json"
    const val PLATFORM_ANDROID = "android"

    const val AUTH_REFRESH_PATH = "/api/v1/auth/refresh"
}
