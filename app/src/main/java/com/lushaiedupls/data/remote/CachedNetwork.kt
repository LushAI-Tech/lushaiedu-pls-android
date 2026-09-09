package com.lushaiedupls.data.remote

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Coalesces concurrent callers of the same key into one in-flight request. */
class RequestCoalescer {
    private val inFlight = ConcurrentHashMap<String, Deferred<Any?>>()
    private val mutex = Mutex()

    suspend fun <T> run(key: String, block: suspend () -> T): T {
        val pair: Pair<Deferred<Any?>, Boolean> = mutex.withLock {
            val existing = inFlight[key]
            if (existing != null) {
                Pair(existing, false)
            } else {
                val deferred = CompletableDeferred<Any?>()
                inFlight[key] = deferred
                Pair(deferred, true)
            }
        }
        val deferred = pair.first
        if (!pair.second) {
            @Suppress("UNCHECKED_CAST")
            return deferred.await() as T
        }
        val leader = deferred as CompletableDeferred<Any?>
        try {
            val result = block()
            leader.complete(result)
            return result
        } catch (e: Throwable) {
            leader.completeExceptionally(e)
            throw e
        } finally {
            mutex.withLock { inFlight.remove(key) }
        }
    }
}

/** Process-local TTL cache. Values may be lists or DTOs. */
class TtlCache(private val defaultTtlMs: Long) {
    private data class Entry(val value: Any?, val expiresAt: Long)

    private val map = ConcurrentHashMap<String, Entry>()
    private val generation = AtomicLong(0)

    fun generation(): Long = generation.get()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = map[key] ?: return null
        if (entry.expiresAt <= System.currentTimeMillis()) {
            map.remove(key, entry)
            return null
        }
        return entry.value as T
    }

    fun put(key: String, value: Any?, ttlMs: Long = defaultTtlMs, expectedGeneration: Long? = null) {
        if (expectedGeneration != null && expectedGeneration != generation.get()) return
        map[key] = Entry(value, System.currentTimeMillis() + ttlMs)
    }

    fun remove(key: String) {
        map.remove(key)
        generation.incrementAndGet()
    }

    fun removePrefix(prefix: String) {
        map.keys.filter { it.startsWith(prefix) }.forEach { map.remove(it) }
        generation.incrementAndGet()
    }

    fun clear() {
        map.clear()
        generation.incrementAndGet()
    }
}

suspend fun <T> cachedCall(
    cache: TtlCache,
    coalescer: RequestCoalescer,
    key: String,
    forceRefresh: Boolean,
    ttlMs: Long? = null,
    fetch: suspend () -> NetworkResult<T>,
): NetworkResult<T> {
    val generation = cache.generation()
    if (!forceRefresh) {
        cache.get<T>(key)?.let { return NetworkResult.Success(it) }
    }
    return coalescer.run("$key@$generation${if (forceRefresh) "!force" else ""}") {
        if (!forceRefresh) {
            cache.get<T>(key)?.let { return@run NetworkResult.Success(it) }
        }
        val result = fetch()
        if (result is NetworkResult.Success) {
            if (ttlMs != null) {
                cache.put(key, result.data, ttlMs, expectedGeneration = generation)
            } else {
                cache.put(key, result.data, expectedGeneration = generation)
            }
        }
        result
    }
}
