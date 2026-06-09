package org.jetbrains.compose.resources

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AsyncCache<K, V> {
    private val cacheJob = SupervisorJob()
    private val cacheScope = CoroutineScope(cacheJob)
    private val mutex = Mutex()
    private val cache = mutableMapOf<K, Deferred<V>>()

    init {
        ResourceCaches.registerCache(this)
    }

    private val currentJobs get() = cacheJob.children.toList()

    suspend fun getOrLoad(key: K, load: suspend () -> V): V {
        val deferred = mutex.withLock {
            var cached = cache[key]
            if (cached == null || cached.isCancelled) {
                cached = cacheScope.async { load() }
                cache[key] = cached
            }
            cached
        }
        return deferred.await()
    }

    suspend fun waitAllJobs() {
        currentJobs.joinAll()
    }

    suspend fun clear() {
        mutex.withLock {
            cache.clear()
            currentJobs.forEach { it.cancelAndJoin() }
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
