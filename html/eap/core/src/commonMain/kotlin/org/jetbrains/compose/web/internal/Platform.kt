/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal

import kotlinx.browser.dom.Element
import kotlinx.browser.dom.DataTransfer
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.KeyboardEvent
import kotlinx.browser.dom.events.MouseEvent

/**
 * Performs an unchecked cast on every platform while keeping the operation
 * available to common code. The implementation uses the platform's native
 * unchecked cast. No runtime type validation is guaranteed.
 */
@PublishedApi
internal expect fun <T> Any?.unsafeCast(): T

/**
 * Stores state without keeping DOM elements alive. Browser implementations use
 * JavaScript WeakMap keys, so arbitrary Kotlin objects are not supported.
 */
internal interface WeakElementMap<K : Element, V : Any> {
    fun delete(key: K)
    fun get(key: K): V?
    fun has(key: K): Boolean
    fun set(key: K, value: V)
}

internal expect fun <K : Element, V : Any> createWeakElementMap(): WeakElementMap<K, V>

/**
 * Schedules [block] in a later browser task, after the current event dispatch
 * finishes. Microtasks can run between native event listeners and are not
 * sufficient for this guarantee. JVM runs it synchronously because its browser
 * declarations are non-functional stubs.
 */
internal expect fun scheduleAfterEvent(block: () -> Unit)

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
