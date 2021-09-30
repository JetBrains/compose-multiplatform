package androidx.compose.ui.platform

// TODO: Instead of multiple syncronized for different packages
// maybe have a common in the common compose.
actual inline fun <R> synchronized(lock: Any, block: () -> R): R = block()

