package com.lushaiedupls.data.remote.interceptors

import com.lushaiedupls.data.remote.token.TokenProvider
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenRefreshAuthenticatorTest {

    @Test
    fun refreshesOnceAndRetriesOriginalWhenFirstCallIs401() {
        val overviewHits = AtomicInteger()
        val refreshHits = AtomicInteger()
        val server = startedServer(overviewHits, refreshHits) { hit ->
            if (hit == 1) {
                MockResponse().setResponseCode(401).setBody("""{"detail":"expired"}""")
            } else {
                MockResponse().setBody("""{"ok":true}""").setHeader("Content-Type", "application/json")
            }
        }
        try {
            val tokens = MemoryTokenProvider()
            val logouts = AtomicInteger()
            client(tokens, logouts).newCall(
                Request.Builder().url(server.url("/api/v1/overview")).build(),
            ).execute().use {
                assertEquals(200, it.code)
            }
            assertEquals(1, refreshHits.get())
            assertEquals(0, logouts.get())
            assertEquals("new-access", tokens.getAccessToken())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun retriesRefreshThreeTimesThenLogsOutIfStill401() {
        val refreshHits = AtomicInteger()
        val server = startedServer(AtomicInteger(), refreshHits) {
            MockResponse().setResponseCode(401).setBody("""{"detail":"unauthorized"}""")
        }
        try {
            val tokens = MemoryTokenProvider()
            val logouts = AtomicInteger()
            client(tokens, logouts).newCall(
                Request.Builder().url(server.url("/api/v1/overview")).build(),
            ).execute().use {
                assertEquals(401, it.code)
            }
            assertEquals(3, refreshHits.get())
            assertEquals(1, logouts.get())
            assertNull(tokens.getAccessToken())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun logsOutImmediatelyWhenRefreshReturns401() {
        val refreshHits = AtomicInteger()
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (request.path.orEmpty().endsWith("/auth/refresh")) {
                    refreshHits.incrementAndGet()
                    MockResponse().setResponseCode(401).setBody("""{"detail":"invalid refresh"}""")
                } else {
                    MockResponse().setResponseCode(401).setBody("""{"detail":"unauthorized"}""")
                }
            }
        }
        server.start()
        try {
            val tokens = MemoryTokenProvider()
            val logouts = AtomicInteger()
            client(tokens, logouts).newCall(
                Request.Builder().url(server.url("/api/v1/overview")).build(),
            ).execute().use {
                assertEquals(401, it.code)
            }
            assertEquals(1, refreshHits.get())
            assertEquals(1, logouts.get())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun doesNotRefreshOrLogoutOnPublicAuth401() {
        val refreshHits = AtomicInteger()
        val server = startedServer(AtomicInteger(), refreshHits) {
            MockResponse().setResponseCode(401).setBody("""{"detail":"bad credentials"}""")
        }
        try {
            val tokens = MemoryTokenProvider()
            val logouts = AtomicInteger()
            client(tokens, logouts).newCall(
                Request.Builder()
                    .url(server.url("/api/v1/auth/login"))
                    .post("{}".toRequestBody())
                    .build(),
            ).execute().use {
                assertEquals(401, it.code)
            }
            assertEquals(0, refreshHits.get())
            assertEquals(0, logouts.get())
        } finally {
            server.shutdown()
        }
    }

    private fun startedServer(
        overviewHits: AtomicInteger,
        refreshHits: AtomicInteger,
        overviewResponse: (Int) -> MockResponse,
    ): MockWebServer {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (request.path.orEmpty().endsWith("/auth/refresh")) {
                    refreshHits.incrementAndGet()
                    MockResponse().setBody(TOKEN_JSON).setHeader("Content-Type", "application/json")
                } else {
                    overviewResponse(overviewHits.incrementAndGet())
                }
            }
        }
        server.start()
        return server
    }

    private fun client(tokens: MemoryTokenProvider, logouts: AtomicInteger): OkHttpClient {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokens))
            .authenticator(
                TokenRefreshAuthenticator(
                    tokenProvider = tokens,
                    deviceId = { "device-1" },
                    json = json,
                    refreshClient = OkHttpClient(),
                    onRefreshFailed = {
                        tokens.clearTokens()
                        logouts.incrementAndGet()
                    },
                ),
            )
            .build()
    }

    private class MemoryTokenProvider(
        access: String? = "access-1",
        refresh: String? = "refresh-1",
    ) : TokenProvider {
        @Volatile private var accessToken = access
        @Volatile private var refreshToken = refresh

        override fun getAccessToken(): String? = accessToken
        override fun getRefreshToken(): String? = refreshToken
        override fun saveTokens(accessToken: String, refreshToken: String?) {
            this.accessToken = accessToken
            if (refreshToken != null) this.refreshToken = refreshToken
        }
        override fun clearTokens() {
            accessToken = null
            refreshToken = null
        }
    }

    private companion object {
        const val TOKEN_JSON =
            """{"access_token":"new-access","refresh_token":"new-refresh"}"""
    }
}
