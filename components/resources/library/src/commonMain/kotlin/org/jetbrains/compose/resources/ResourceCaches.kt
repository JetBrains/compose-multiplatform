package org.jetbrains.compose.resources

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class AsyncCache<K, V> {
    private val mutex = Mutex()
    private val cache = mutableMapOf<K, Entry<V>>()

    // The loading runs in a cache-owned scope so that cancelling one caller
    // does not directly cancel the shared Deferred stored in the cache.
    private val scope = CoroutineScope(SupervisorJob())

    private class Entry<V>(val deferred: Deferred<V>) {
        var subscribers = 0
    }

    init {
        ResourceCaches.registerCache(this)
    }

    suspend fun getOrLoad(key: K, load: suspend () -> V): V {
        val entry = mutex.withLock {
            val existing = cache[key]
            val entry = if (existing == null || existing.deferred.isCancelled) {
                //LAZY - to free the mutex lock as fast as possible
                Entry(scope.async(start = CoroutineStart.LAZY) { load() }).also { cache[key] = it }
            } else {
                existing
            }
            entry.subscribers++
            entry
        }
        try {
            return entry.deferred.await()
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    entry.subscribers--
                    // Cancel the shared load only when nobody else is waiting for it.
                    // A successfully completed deferred is kept in the cache for reuse.
                    if (entry.subscribers == 0 && !entry.deferred.isCompleted && cache[key] === entry) {
                        cache.remove(key)
                        entry.deferred.cancel()
                    }
                }
            }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            cache.values.forEach { it.deferred.cancel() }
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
