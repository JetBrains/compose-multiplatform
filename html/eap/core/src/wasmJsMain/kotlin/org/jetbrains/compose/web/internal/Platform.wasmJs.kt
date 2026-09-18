/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web.internal

import kotlinx.browser.JsAny
import kotlinx.browser.JsNumber
import kotlinx.browser.dom.DataTransfer
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.KeyboardEvent
import kotlinx.browser.dom.events.MouseEvent
import kotlinx.browser.toDouble
import kotlin.js.JsReference
import kotlin.js.get
import kotlin.js.js
import kotlin.js.toJsReference
import kotlin.js.unsafeCast as jsUnsafeCast

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal actual fun <T> Any?.unsafeCast(): T = this as T

private external interface JsWeakMap<V : JsAny> : JsAny {
    fun delete(key: JsAny): Boolean
    fun get(key: JsAny): V?
    fun has(key: JsAny): Boolean
    fun set(key: JsAny, value: V): JsWeakMap<V>
}

private fun <V : JsAny> createJsWeakMap(): JsWeakMap<V> =
    js("new WeakMap()")

private class WasmWeakElementMap<K : Element, V : Any> : WeakElementMap<K, V> {
    private val delegate = createJsWeakMap<JsReference<V>>()

    override fun delete(key: K) {
        delegate.delete(key)
    }

    override fun get(key: K): V? = delegate.get(key)?.get()

    override fun has(key: K): Boolean = delegate.has(key)

    override fun set(key: K, value: V) {
        delegate.set(key, value.toJsReference())
    }
}

internal actual fun <K : Element, V : Any> createWeakElementMap(): WeakElementMap<K, V> =
    WasmWeakElementMap()

private external interface MouseEventFields : JsAny {
    val movementX: JsNumber?
    val movementY: JsNumber?
}

private external interface KeyboardEventFields : JsAny {
    val locale: String
}

private external interface InputEventFields : JsAny {
    val inputType: String?
    val dataTransfer: DataTransfer?
    val data: String?
    val isComposing: Boolean?
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
    jsUnsafeCast<MouseEventFields>().movementX.toExactIntOrZero()

internal actual fun MouseEvent.movementYOrZero(): Int =
    jsUnsafeCast<MouseEventFields>().movementY.toExactIntOrZero()

private fun JsNumber?.toExactIntOrZero(): Int {
    val value = this?.toDouble() ?: return 0
    if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) return 0
    val intValue = value.toInt()
    return if (intValue.toDouble() == value) intValue else 0
}

internal actual fun KeyboardEvent.localeCompat(): String =
    jsUnsafeCast<KeyboardEventFields>().locale

internal actual fun Event.inputTypeCompat(): String? =
    jsUnsafeCast<InputEventFields>().inputType

internal actual fun Event.inputDataTransferCompat(): DataTransfer? =
    jsUnsafeCast<InputEventFields>().dataTransfer

internal actual fun Event.inputDataCompat(): String? =
    jsUnsafeCast<InputEventFields>().data

internal actual fun Event.inputIsComposingCompat(): Boolean =
    jsUnsafeCast<InputEventFields>().isComposing ?: false

@JsFun("node => typeof node.nonce === 'string' ? node.nonce : null")
private external fun readNonceProperty(node: Element): String?

internal actual fun Element.noncePropertyOrNull(): String? =
    readNonceProperty(this)

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
