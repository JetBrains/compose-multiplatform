package org.jetbrains.compose.resources

/**
 * The web targets are single threaded, so there is nothing to lock.
 */
internal actual class ResourceLock actual constructor() {
    actual fun <T> withLock(block: () -> T): T = block()
}
