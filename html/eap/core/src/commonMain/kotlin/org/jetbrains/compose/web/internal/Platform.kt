/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal

import kotlinx.browser.JsAny
import kotlinx.browser.dom.DataTransfer
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.KeyboardEvent
import kotlinx.browser.dom.events.MouseEvent

/**
 * Performs the cast used by each platform while keeping the call available to
 * common code. Kotlin/JS delegates to its unchecked JavaScript cast. Wasm and
 * JVM use a regular Kotlin cast because this contract also accepts Kotlin values;
 * the Wasm JavaScript cast is limited to JavaScript values.
 */
@PublishedApi
internal expect fun <T> Any?.unsafeCast(): T

/**
 * Small common contract for state that must not keep DOM elements alive.
 *
 */
internal interface WeakMap<K : JsAny, V : Any> {
    fun delete(key: K)
    fun get(key: K): V?
    fun has(key: K): Boolean
    fun set(key: K, value: V)
}

internal expect fun <K : JsAny, V : Any> createWeakMap(): WeakMap<K, V>

/**
 * Schedules [block] on the browser microtask queue, after the current event
 * dispatch and before the browser can paint. JVM runs it synchronously because
 * its browser declarations are non-functional stubs.
 */
internal expect fun scheduleMicrotask(block: () -> Unit)

/**
 * Reads browser-only mouse movement fields while preserving the existing
 * zero fallback for browsers where the fields are absent.
 */
internal expect fun MouseEvent.movementXOrZero(): Int

internal expect fun MouseEvent.movementYOrZero(): Int

/**
 * Preserves Compose HTML's legacy KeyboardEvent.locale API even though the
 * property is absent from the portable kotlinx-browser declarations.
 */
internal expect fun KeyboardEvent.localeCompat(): String

/**
 * Preserves InputEvent properties that are absent from the portable
 * kotlinx-browser 0.5.0 declarations. The receiver stays Event because some
 * input callbacks are represented by a plain browser Event.
 */
internal expect fun Event.inputTypeCompat(): String?

internal expect fun Event.inputDataTransferCompat(): DataTransfer?

internal expect fun Event.inputDataCompat(): String?

internal expect fun Event.inputIsComposingCompat(): Boolean

internal expect fun Event.targetValueCompat(): String?

internal expect fun Event.targetCheckedCompat(): Boolean

internal expect fun Event.targetValueAsNumberCompat(): Number?

internal expect fun Event.animationNameCompat(): String

internal expect fun Event.animationElapsedTimeCompat(): Number

internal expect fun Event.animationPseudoElementCompat(): String

internal expect fun Event.selectionStartCompat(): Int

internal expect fun Event.selectionEndCompat(): Int
