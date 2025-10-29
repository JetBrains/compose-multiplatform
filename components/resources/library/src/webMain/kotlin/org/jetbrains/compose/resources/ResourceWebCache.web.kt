package org.jetbrains.compose.resources

import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.w3c.fetch.Response
import org.w3c.workers.Cache
import org.w3c.workers.CacheQueryOptions
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.asJsException
import kotlin.js.js

/**
 * We use [Cache] APIs to cache the successful strings.cvr and other responses.
 * We can't rely on the default browser cache because it makes http requests to check if the cached value is not expired,
 * which may take long if the connection is slow.
 *
 * Cache limits:
 * https://developer.mozilla.org/en-US/docs/Web/API/Storage_API/Storage_quotas_and_eviction_criteria#other_web_technologies
 *
 * The Cache API is available only in secure contexts (HTTPS or localhost).
 * In non-secure contexts it will behave as if there are no cached values.
 */
@OptIn(ExperimentalWasmJsInterop::class)
internal object ResourceWebCache {
    // This cache will be shared between all Compose instances (independent ComposeViewport) in the same session
    private const val CACHE_NAME = "compose_web_resources_cache"

    // A collection of mutexes to prevent the concurrent requests for the same resource but allow such requests for
    // distinct resources
    private val mutexes = mutableMapOf<String, Mutex>()

    // A mutex to avoid multiple cache reset
    private val resetMutex = Mutex()

    private val supportsCacheApi: Boolean by lazy { supportsCacheApi() }

    suspend fun load(path: String, onNoCacheHit: suspend (path: String) -> Response): Response {
        if (!supportsCacheApi) return onNoCacheHit(path)

        if (isNewSession()) {
            // There can be many load requests, and there must be 1 reset max. Therefore, using `resetMutex`.
            resetMutex.withLock {
                // Checking isNewSession() again in case it was just changed by another load request.
                // I avoid wrapping withLock in if (isNewSession()) check to avoid unnecessary locking on every load request
                if (isNewSession()) {
                    sessionStarted = true
                    resetCache()
                }
            }
        }

        val mutex = mutexes.getOrPut(path) { Mutex() }

        return mutex.withLock {
            val cache = window.caches.open(CACHE_NAME).await()
            val response = (cache.match(path, CacheQueryOptions()) as Promise<Response?>).await()

            response?.clone() ?: onNoCacheHit(path).also {
                if (it.ok) {
                    cache.put(path, it.clone()).await()
                }
            }
        }.also {
            mutexes.remove(path)
        }
    }

    suspend fun resetCache() {
        window.caches.delete(CACHE_NAME).await()
    }

    // In this case it's not true session as browsers mean it.
    // Here a new session is created on every page refresh.
    private var sessionStarted = false

    private fun isNewSession(): Boolean {
        return !sessionStarted
    }
}


// https://developer.mozilla.org/en-US/docs/Web/API/Window/caches
// Supported only in secure contexts (HTTPS or localhost)
private fun supportsCacheApi(): Boolean = js("Boolean(window.caches)")

// Promise.await is not yet available in webMain: https://github.com/Kotlin/kotlinx.coroutines/issues/4544
// TODO(o.karpovich): get rid of this function, when kotlinx-coroutines provide Promise.await in webMain out of a box
@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun <R : JsAny?> Promise<R>.await(): R = suspendCancellableCoroutine { continuation ->
    this.then(
        onFulfilled = { continuation.resumeWith(Result.success(it)); null },
        onRejected = { continuation.resumeWithException(it.asJsException()); null }
    )
}
