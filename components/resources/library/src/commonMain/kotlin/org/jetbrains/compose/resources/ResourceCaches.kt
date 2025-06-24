package org.jetbrains.compose.resources

private val caches = mutableListOf<AsyncCache<Any, Any>>()

internal fun registerCache(cache: AsyncCache<Any, Any>) {
    caches.add(cache)
}

/**
 * Clears any cached resources maintained internally by the system.
 *
 * This method is intended for use from Compose Hot Reload,
 * to reload resources that might be changed.
 * It can be also useful to release memory or reset cached resources that
 * may no longer be required.
 *
 * Note that frequent or unnecessary calls to this function may impact
 * performance by removing resources that might otherwise benefit from being cached.
 */
fun dropResourceCaches() {
   caches.forEach { it.clear() }
}