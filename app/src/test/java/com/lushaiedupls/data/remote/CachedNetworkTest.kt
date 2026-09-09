package com.lushaiedupls.data.remote

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class CachedNetworkTest {

    @Test
    fun ttlCacheExpiresAndRemovePrefix() {
        val cache = TtlCache(defaultTtlMs = 10_000)
        cache.put("week_a", "one")
        cache.put("week_b", "two")
        cache.put("units", "units")
        assertEquals("one", cache.get<String>("week_a"))
        cache.removePrefix("week_")
        assertNull(cache.get<String>("week_a"))
        assertNull(cache.get<String>("week_b"))
        assertEquals("units", cache.get<String>("units"))
    }

    @Test
    fun coalescerSharesInFlightWork() = runBlocking {
        val coalescer = RequestCoalescer()
        val started = AtomicInteger(0)
        val results = (1..8).map {
            async {
                coalescer.run("units") {
                    started.incrementAndGet()
                    delay(30)
                    "shared"
                }
            }
        }.awaitAll()
        assertEquals(1, started.get())
        assertEquals(8, results.size)
        results.forEach { assertSame("shared", it) }
    }

    @Test
    fun cachedCallSkipsNetworkWhenWarm() = runBlocking {
        val cache = TtlCache(60_000)
        val coalescer = RequestCoalescer()
        val fetches = AtomicInteger(0)
        suspend fun load(force: Boolean) = cachedCall(cache, coalescer, "k", force) {
            fetches.incrementAndGet()
            NetworkResult.Success("ok")
        }
        assertEquals("ok", (load(false) as NetworkResult.Success).data)
        assertEquals("ok", (load(false) as NetworkResult.Success).data)
        assertEquals(1, fetches.get())
        load(true)
        assertEquals(2, fetches.get())
    }

    @Test
    fun cachedCallDoesNotRepopulateAfterInvalidate() = runBlocking {
        val cache = TtlCache(60_000)
        val coalescer = RequestCoalescer()
        val started = CompletableDeferred<Unit>()
        val inflight = async {
            cachedCall(cache, coalescer, "k", forceRefresh = false) {
                started.complete(Unit)
                delay(50)
                NetworkResult.Success("stale")
            }
        }
        started.await()
        cache.remove("k")
        inflight.await()
        assertNull(cache.get<String>("k"))
    }
}
