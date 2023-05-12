package androidx.compose.web.events

import org.jetbrains.compose.web.JsWasmNumber
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

open class SyntheticEvent<Element : EventTarget> internal constructor(
    val nativeEvent: Event
) {
    val target: Element = nativeEvent.target as Element
    val bubbles: Boolean = nativeEvent.bubbles
    val cancelable: Boolean = nativeEvent.cancelable
    val composed: Boolean = nativeEvent.composed
    val currentTarget: EventTarget? = nativeEvent.currentTarget
    val eventPhase: Short = nativeEvent.eventPhase
    val defaultPrevented: Boolean = nativeEvent.defaultPrevented
    val timestamp: JsWasmNumber = nativeEvent.timeStamp
    val type: String = nativeEvent.type
    val isTrusted: Boolean = nativeEvent.isTrusted

    fun preventDefault(): Unit = nativeEvent.preventDefault()
    fun stopPropagation(): Unit = nativeEvent.stopPropagation()
    fun stopImmediatePropagation(): Unit = nativeEvent.stopImmediatePropagation()
    fun composedPath(): JsArray<EventTarget> = nativeEvent.composedPath()
}
