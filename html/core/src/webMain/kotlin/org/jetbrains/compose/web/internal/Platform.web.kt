package org.jetbrains.compose.web.internal

import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.KeyboardEvent
import kotlinx.browser.dom.events.MouseEvent
import kotlin.js.unsafeCast as jsUnsafeCast

@PublishedApi
internal actual fun <T> Any?.unsafeCast(): T = jsUnsafeCast<T>()

private external interface JsWeakMap {
    fun delete(key: Any)
    fun get(key: Any): Any?
    fun has(key: Any): Boolean
    fun set(key: Any, value: Any): JsWeakMap
}

private class JsWeakMapAdapter<K : Any, V : Any> : WeakMap<K, V> {
    private val delegate = js("new WeakMap()").jsUnsafeCast<JsWeakMap>()

    override fun delete(key: K) {
        delegate.delete(key)
    }

    override fun get(key: K): V? = delegate.get(key).jsUnsafeCast<V?>()

    override fun has(key: K): Boolean = delegate.has(key)

    override fun set(key: K, value: V) {
        delegate.set(key, value)
    }
}

internal actual fun <K : Any, V : Any> createWeakMap(): WeakMap<K, V> =
    JsWeakMapAdapter()

internal actual fun MouseEvent.movementXOrZero(): Int =
    (asDynamic().movementX as? Int) ?: 0

internal actual fun MouseEvent.movementYOrZero(): Int =
    (asDynamic().movementY as? Int) ?: 0

internal actual fun KeyboardEvent.localeCompat(): String =
    asDynamic().locale.toString()

internal actual fun Event.animationEventDetails(): AnimationEventDetails {
    val event = asDynamic()
    return AnimationEventDetails(
        animationName = event.animationName.jsUnsafeCast<String>(),
        elapsedTime = event.elapsedTime.jsUnsafeCast<Number>(),
        pseudoElement = event.pseudoElement.jsUnsafeCast<String>(),
    )
}
