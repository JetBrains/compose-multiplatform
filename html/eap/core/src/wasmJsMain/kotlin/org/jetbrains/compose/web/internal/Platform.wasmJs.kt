/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web.internal

import kotlinx.browser.JsAny
import kotlinx.browser.dom.DataTransfer
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.KeyboardEvent
import kotlinx.browser.dom.events.MouseEvent
import kotlin.js.JsReference
import kotlin.js.get
import kotlin.js.js
import kotlin.js.toJsReference
import kotlin.js.unsafeCast as jsUnsafeCast

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal actual fun <T> Any?.unsafeCast(): T = this as T

private external interface JsWeakMap<K : JsAny, V : JsAny> : JsAny {
    fun delete(key: K): Boolean
    fun get(key: K): V?
    fun has(key: K): Boolean
    fun set(key: K, value: V): JsWeakMap<K, V>
}

private fun <K : JsAny, V : JsAny> createJsWeakMap(): JsWeakMap<K, V> =
    js("new WeakMap()")

private class WasmWeakMap<K : JsAny, V : Any> : WeakMap<K, V> {
    private val delegate = createJsWeakMap<K, JsReference<V>>()

    override fun delete(key: K) {
        delegate.delete(key)
    }

    override fun get(key: K): V? = delegate.get(key)?.get()

    override fun has(key: K): Boolean = delegate.has(key)

    override fun set(key: K, value: V) {
        delegate.set(key, value.toJsReference())
    }
}

internal actual fun <K : JsAny, V : Any> createWeakMap(): WeakMap<K, V> =
    WasmWeakMap()

private external interface MouseEventFields : JsAny {
    val movementX: Int?
    val movementY: Int?
}

private external interface KeyboardEventFields : JsAny {
    val locale: String
}

private external interface InputEventFields : JsAny {
    val inputType: String?
    val dataTransfer: DataTransfer?
}

private external interface FormControlFields : JsAny {
    val value: String?
    val checked: Boolean?
    val valueAsNumber: Double?
    val selectionStart: Int
    val selectionEnd: Int
}

private external interface AnimationEventFields : JsAny {
    val animationName: String
    val elapsedTime: Double
    val pseudoElement: String
}

internal actual fun MouseEvent.movementXOrZero(): Int =
    jsUnsafeCast<MouseEventFields>().movementX ?: 0

internal actual fun MouseEvent.movementYOrZero(): Int =
    jsUnsafeCast<MouseEventFields>().movementY ?: 0

internal actual fun KeyboardEvent.localeCompat(): String =
    jsUnsafeCast<KeyboardEventFields>().locale

internal actual fun Event.inputTypeCompat(): String? =
    jsUnsafeCast<InputEventFields>().inputType

internal actual fun Event.inputDataTransferCompat(): DataTransfer? =
    jsUnsafeCast<InputEventFields>().dataTransfer

internal actual fun Event.targetValueCompat(): String? =
    target?.jsUnsafeCast<FormControlFields>()?.value

internal actual fun Event.targetCheckedCompat(): Boolean =
    target?.jsUnsafeCast<FormControlFields>()?.checked ?: false

internal actual fun Event.targetValueAsNumberCompat(): Number? =
    target?.jsUnsafeCast<FormControlFields>()?.valueAsNumber

internal actual fun Event.animationNameCompat(): String =
    jsUnsafeCast<AnimationEventFields>().animationName

internal actual fun Event.animationElapsedTimeCompat(): Number =
    jsUnsafeCast<AnimationEventFields>().elapsedTime

internal actual fun Event.animationPseudoElementCompat(): String =
    jsUnsafeCast<AnimationEventFields>().pseudoElement

internal actual fun Event.selectionStartCompat(): Int =
    target!!.jsUnsafeCast<FormControlFields>().selectionStart

internal actual fun Event.selectionEndCompat(): Int =
    target!!.jsUnsafeCast<FormControlFields>().selectionEnd
