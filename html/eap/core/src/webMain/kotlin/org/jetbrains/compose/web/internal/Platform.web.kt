/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal

import kotlinx.browser.dom.DataTransfer
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

internal actual fun Event.inputTypeCompat(): String? =
    asDynamic().inputType?.jsUnsafeCast<String>()

internal actual fun Event.inputDataTransferCompat(): DataTransfer? =
    asDynamic().dataTransfer?.jsUnsafeCast<DataTransfer>()

internal actual fun Event.targetValueCompat(): String? =
    target.asDynamic()?.value?.jsUnsafeCast<String>()

internal actual fun Event.targetCheckedCompat(): Boolean =
    target.asDynamic()?.checked?.jsUnsafeCast<Boolean>() ?: false

internal actual fun Event.targetValueAsNumberCompat(): Number? =
    target.asDynamic()?.valueAsNumber?.jsUnsafeCast<Number>()

internal actual fun Event.animationNameCompat(): String =
    asDynamic().animationName.jsUnsafeCast<String>()

internal actual fun Event.animationElapsedTimeCompat(): Number =
    asDynamic().elapsedTime.jsUnsafeCast<Number>()

internal actual fun Event.animationPseudoElementCompat(): String =
    asDynamic().pseudoElement.jsUnsafeCast<String>()

internal actual fun Event.selectionStartCompat(): Int =
    target.asDynamic().selectionStart.jsUnsafeCast<Int>()

internal actual fun Event.selectionEndCompat(): Int =
    target.asDynamic().selectionEnd.jsUnsafeCast<Int>()
