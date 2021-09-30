@file:Suppress("UNUSED_PARAMETER")
package androidx.compose.animation.core

inline fun <R> synchronized(lock: Any, block: () -> R): R = block()

