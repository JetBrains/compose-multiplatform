@file:Suppress("UNUSED_PARAMETER")
package androidx.compose.ui.autofill

inline fun <R> synchronized(lock: Any, block: () -> R): R = block()

