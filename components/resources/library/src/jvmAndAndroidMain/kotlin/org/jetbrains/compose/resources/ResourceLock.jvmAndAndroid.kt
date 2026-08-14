package org.jetbrains.compose.resources

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal actual class ResourceLock actual constructor() {
    private val lock = ReentrantLock()

    actual fun <T> withLock(block: () -> T): T = lock.withLock(block)
}
