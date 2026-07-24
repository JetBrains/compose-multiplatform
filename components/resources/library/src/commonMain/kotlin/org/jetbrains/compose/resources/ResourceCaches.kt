package org.jetbrains.compose.resources

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AsyncCache<K, V> {
    private val cacheScope = CoroutineScope(SupervisorJob())
    private val mutex = Mutex()
    private val cache = mutableMapOf<K, SharedRequest<V>>()

    private class SharedRequest<V>(val deferred: Deferred<V>) {
        var listenersCount = 0
    }

    init {
        ResourceCaches.registerCache(this)
    }

    private suspend fun getAllActiveJobs(): List<Job> = mutex.withLock {
        cache.values.map { it.deferred }.filter { it.isActive }
    }

    suspend fun getOrLoad(key: K, load: suspend () -> V): V {
        val request = mutex.withLock {
            var cached = cache[key]
            if (cached == null || cached.deferred.isCancelled) {
                cached = SharedRequest(cacheScope.async { load() })
                cache[key] = cached
            }
            cached.listenersCount++
            cached
        }
         return try {
             request.deferred.await()
         } finally {
             mutex.withLock {
                 request.listenersCount--
                 if (request.listenersCount == 0 && request.deferred.isActive) {
                     request.deferred.cancel()
                 }
             }
         }
    }

    suspend fun waitAllJobs() {
        getAllActiveJobs().joinAll()
    }

    suspend fun clear() {
        mutex.withLock {
            cache.forEach { (_, v) -> v.deferred.cancel() }
            cache.clear()
        }
    }
}

object ResourceCaches {
    private val caches = mutableListOf<AsyncCache<*, *>>()

    internal fun registerCache(cache: AsyncCache<*, *>) = caches.add(cache)

    /**
     * Waits for all ongoing resource loading jobs to complete.
     *
     * This method ensures that all asynchronous resource loading operations
     * have finished before proceeding. It is useful for testing and ensuring
     * that resources are fully loaded before further actions are taken.
     */
    internal suspend fun waitAllJobs() {
        caches.toList().forEach { it.waitAllJobs() }
    }

    /**
     * Clears any cached resources maintained internally by the system.
     *
     * It can be useful to release memory or reset cached resources that
     * may be changed or no longer be required.
     */
    internal suspend fun clear() {
        caches.toList().forEach { it.clear() }
    }
}
