package org.jetbrains.compose.resources

/**
 * A non-suspending lock for the short critical sections of the resource caches.
 *
 * A `kotlinx.coroutines.sync.Mutex` cannot be used there: it hands the ownership over to a waiting
 * coroutine, and that coroutine is resumed through its own dispatcher. So a lock taken by
 * a coroutine of the UI dispatcher is not released while the UI thread reads a resource
 * synchronously (see `runResourceBlocking`) - the application freezes forever.
 *
 * A blocking lock has no such handoff: it is always released by the thread which holds it.
 * Nothing may suspend inside [withLock].
 */
internal expect class ResourceLock() {
    fun <T> withLock(block: () -> T): T
}
