package org.jetbrains.compose.web.events

import org.w3c.dom.DragEvent
import org.w3c.dom.TouchEvent
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.FocusEvent
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.pointerevents.PointerEvent

interface GenericWrappedEvent<T : Event> {
    val nativeEvent: T
}

interface WrappedEvent : GenericWrappedEvent<Event>

internal open class WrappedMouseEvent(
    override val nativeEvent: MouseEvent
) : GenericWrappedEvent<MouseEvent> {

    // MouseEvent doesn't support movementX and movementY on IE6-11, and it's OK for now.
    val movementX: Double
        get() = nativeEvent.asDynamic().movementX as Double
    val movementY: Double
        get() = nativeEvent.asDynamic().movementY as Double
}

internal open class WrappedWheelEvent(
    override val nativeEvent: WheelEvent
) : GenericWrappedEvent<WheelEvent>

open class WrappedInputEvent(
    override val nativeEvent: Event
) : GenericWrappedEvent<Event>

open class WrappedKeyboardEvent(
    override val nativeEvent: KeyboardEvent
) : GenericWrappedEvent<KeyboardEvent> {

    fun getNormalizedKey(): String = nativeEvent.key.let {
        normalizedKeys[it] ?: it
    }

    companion object {
        private val normalizedKeys = mapOf(
            "Esc" to "Escape",
            "Spacebar" to " ",
            "Left" to "ArrowLeft",
            "Up" to "ArrowUp",
            "Right" to "ArrowRight",
            "Down" to "ArrowDown",
            "Del" to "Delete",
            "Apps" to "ContextMenu",
            "Menu" to "ContextMenu",
            "Scroll" to "ScrollLock",
            "MozPrintableKey" to "Unidentified",
        )
        // Firefox bug for Windows key https://bugzilla.mozilla.org/show_bug.cgi?id=1232918
    }
}

open class WrappedFocusEvent(
    override val nativeEvent: FocusEvent
) : GenericWrappedEvent<FocusEvent>

open class WrappedTouchEvent(
    override val nativeEvent: TouchEvent
) : GenericWrappedEvent<TouchEvent>

open class WrappedCompositionEvent(
    override val nativeEvent: CompositionEvent
) : GenericWrappedEvent<CompositionEvent>

open class WrappedDragEvent(
    override val nativeEvent: DragEvent
) : GenericWrappedEvent<DragEvent>

open class WrappedPointerEvent(
    override val nativeEvent: PointerEvent
) : GenericWrappedEvent<PointerEvent>

open class WrappedClipboardEvent(
    override val nativeEvent: ClipboardEvent
) : GenericWrappedEvent<ClipboardEvent>

class WrappedTextInputEvent(
    nativeEvent: Event,
    val inputValue: String
) : WrappedInputEvent(nativeEvent)

class WrappedCheckBoxInputEvent(
    override val nativeEvent: Event,
    val checked: Boolean
) : GenericWrappedEvent<Event>

class WrappedRadioInputEvent(
    override val nativeEvent: Event,
    val checked: Boolean
) : GenericWrappedEvent<Event>

class WrappedEventImpl(
    override val nativeEvent: Event
) : WrappedEvent
