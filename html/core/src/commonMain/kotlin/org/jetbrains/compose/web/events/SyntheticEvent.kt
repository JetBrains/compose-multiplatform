/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.events

import kotlinx.browser.toDouble
import kotlinx.browser.toList
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget
import org.jetbrains.compose.web.internal.unsafeCast

open class SyntheticEvent<Element : EventTarget> internal constructor(
    val nativeEvent: Event
) {
    val target: Element = nativeEvent.target.unsafeCast<Element>()
    val bubbles: Boolean = nativeEvent.bubbles
    val cancelable: Boolean = nativeEvent.cancelable
    val composed: Boolean = nativeEvent.composed
    val currentTarget: EventTarget? = nativeEvent.currentTarget
    val eventPhase: Short = nativeEvent.eventPhase
    val defaultPrevented: Boolean = nativeEvent.defaultPrevented
    val timestamp: Number = nativeEvent.timeStamp.toDouble()
    val type: String = nativeEvent.type
    val isTrusted: Boolean = nativeEvent.isTrusted

    fun preventDefault(): Unit = nativeEvent.preventDefault()
    fun stopPropagation(): Unit = nativeEvent.stopPropagation()
    fun stopImmediatePropagation(): Unit = nativeEvent.stopImmediatePropagation()
    fun composedPath(): Array<EventTarget> = nativeEvent.composedPath().toList().toTypedArray()
}
