@file:Suppress("UNUSED_PARAMETER")
package androidx.compose.ui.input.pointer


// TODO: Instead of multiple syncronized for different packages
// maybe have a common in the common compose.
inline fun <R> synchronized(lock: Any, block: () -> R): R = block()

