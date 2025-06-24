package org.jetbrains.compose.resources

private val caches = mutableListOf<AsyncCache<*, *>>()

internal fun registerCache(cache: AsyncCache<*, *>) {
    caches.add(cache)
}

/**
 * Clears any cached resources maintained internally by the system.
 *
 * It can be useful to release memory or reset cached resources that
 * may be changed or no longer be required.
 *
 * Note that frequent or unnecessary calls to this function may impact
 * performance by removing resources that might otherwise benefit from being cached.
 */
fun dropResourceCaches() {
   caches.forEach { it.clear() }
}