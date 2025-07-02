package org.jetbrains.compose.resources

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AsyncCache<K, V> {
    private val mutex = Mutex()
    private val cache = mutableMapOf<K, Deferred<V>>()

    init {
        ResourceCaches.registerCache(this)
    }

    suspend fun getOrLoad(key: K, load: suspend () -> V): V = coroutineScope {
        val deferred = mutex.withLock {
            var cached = cache[key]
            if (cached == null || cached.isCancelled) {
                //LAZY - to free the mutex lock as fast as possible
                cached = async(start = CoroutineStart.LAZY) { load() }
                cache[key] = cached
            }
            cached
        }
        deferred.await()
    }

    suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
}

object ResourceCaches {
    private val caches = mutableListOf<AsyncCache<*, *>>()

    internal fun registerCache(cache: AsyncCache<*, *>) = caches.add(cache)

    /**
     * Clears any cached resources maintained internally by the system.
     *
     * It can be useful to release memory or reset cached resources that
     * may be changed or no longer be required.
     */
    internal suspend fun clear() {
        caches.forEach { it.clear() }
    }
}
