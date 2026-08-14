package org.jetbrains.compose.resources

import kotlin.concurrent.AtomicInt

internal actual class ResourceLock actual constructor() {
    private val isLocked = AtomicInt(UNLOCKED)

    actual fun <T> withLock(block: () -> T): T {
        while (!isLocked.compareAndSet(UNLOCKED, LOCKED)) {
            //waiting for the lock to be released
        }
        try {
            return block()
        } finally {
            isLocked.value = UNLOCKED
        }
    }

    private companion object {
        const val UNLOCKED = 0
        const val LOCKED = 1
    }
}
