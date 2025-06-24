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
        @Suppress("UNCHECKED_CAST")
        registerCache(this as AsyncCache<Any, Any>)
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

    fun clear() {
        cache.clear()
    }
}