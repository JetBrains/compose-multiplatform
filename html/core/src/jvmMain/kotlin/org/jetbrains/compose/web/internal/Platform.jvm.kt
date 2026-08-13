package org.jetbrains.compose.web.internal

import java.util.WeakHashMap
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.KeyboardEvent
import kotlinx.browser.dom.events.MouseEvent

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal actual fun <T> Any?.unsafeCast(): T = this as T

private class JvmWeakMap<K : Any, V : Any> : WeakMap<K, V> {
    private val delegate = WeakHashMap<K, V>()

    override fun delete(key: K) {
        delegate.remove(key)
    }

    override fun get(key: K): V? = delegate[key]

    override fun has(key: K): Boolean = delegate.containsKey(key)

    override fun set(key: K, value: V) {
        delegate[key] = value
    }
}

internal actual fun <K : Any, V : Any> createWeakMap(): WeakMap<K, V> =
    JvmWeakMap()

internal actual fun MouseEvent.movementXOrZero(): Int = 0

internal actual fun MouseEvent.movementYOrZero(): Int = 0

internal actual fun KeyboardEvent.localeCompat(): String = ""

internal actual fun Event.animationEventDetails(): AnimationEventDetails =
    AnimationEventDetails(
        animationName = "",
        elapsedTime = 0.0,
        pseudoElement = "",
    )
