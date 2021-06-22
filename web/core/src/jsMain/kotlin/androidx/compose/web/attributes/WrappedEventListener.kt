package org.jetbrains.compose.web.attributes

import org.jetbrains.compose.web.events.GenericWrappedEvent
import org.jetbrains.compose.web.events.WrappedCheckBoxInputEvent
import org.jetbrains.compose.web.events.WrappedClipboardEvent
import org.jetbrains.compose.web.events.WrappedDragEvent
import org.jetbrains.compose.web.events.WrappedEventImpl
import org.jetbrains.compose.web.events.WrappedFocusEvent
import org.jetbrains.compose.web.events.WrappedInputEvent
import org.jetbrains.compose.web.events.WrappedKeyboardEvent
import org.jetbrains.compose.web.events.WrappedMouseEvent
import org.jetbrains.compose.web.events.WrappedPointerEvent
import org.jetbrains.compose.web.events.WrappedRadioInputEvent
import org.jetbrains.compose.web.events.WrappedTextInputEvent
import org.jetbrains.compose.web.events.WrappedTouchEvent
import org.jetbrains.compose.web.events.WrappedWheelEvent
import org.w3c.dom.DragEvent
import org.w3c.dom.TouchEvent
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.FocusEvent
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.pointerevents.PointerEvent

open class WrappedEventListener<T : GenericWrappedEvent<*>>(
    val event: String,
    val options: Options,
    val listener: (T) -> Unit
) : org.w3c.dom.events.EventListener {

    @Suppress("UNCHECKED_CAST")
    override fun handleEvent(event: Event) {
        listener(WrappedEventImpl(event) as T)
    }
}

class Options {
    // TODO: add options for addEventListener

    companion object {
        val DEFAULT = Options()
    }
}

internal class MouseEventListener(
    event: String,
    options: Options,
    listener: (WrappedMouseEvent) -> Unit
) : WrappedEventListener<WrappedMouseEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(WrappedMouseEvent(event as MouseEvent))
    }
}

internal class MouseWheelEventListener(
    event: String,
    options: Options,
    listener: (WrappedWheelEvent) -> Unit
) : WrappedEventListener<WrappedWheelEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(WrappedWheelEvent(event as WheelEvent))
    }
}

internal class KeyboardEventListener(
    event: String,
    options: Options,
    listener: (WrappedKeyboardEvent) -> Unit
) : WrappedEventListener<WrappedKeyboardEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(WrappedKeyboardEvent(event as KeyboardEvent))
    }
}

internal class FocusEventListener(
    event: String,
    options: Options,
    listener: (WrappedFocusEvent) -> Unit
) : WrappedEventListener<WrappedFocusEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(WrappedFocusEvent(event as FocusEvent))
    }
}

internal class TouchEventListener(
    event: String,
    options: Options,
    listener: (WrappedTouchEvent) -> Unit
) : WrappedEventListener<WrappedTouchEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(WrappedTouchEvent(event as TouchEvent))
    }
}

internal class DragEventListener(
    event: String,
    options: Options,
    listener: (WrappedDragEvent) -> Unit
) : WrappedEventListener<WrappedDragEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(WrappedDragEvent(event as DragEvent))
    }
}

internal class PointerEventListener(
    event: String,
    options: Options,
    listener: (WrappedPointerEvent) -> Unit
) : WrappedEventListener<WrappedPointerEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(WrappedPointerEvent(event as PointerEvent))
    }
}

internal class ClipboardEventListener(
    event: String,
    options: Options,
    listener: (WrappedClipboardEvent) -> Unit
) : WrappedEventListener<WrappedClipboardEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(WrappedClipboardEvent(event as ClipboardEvent))
    }
}

internal class InputEventListener(
    event: String,
    options: Options,
    listener: (WrappedInputEvent) -> Unit
) : WrappedEventListener<WrappedInputEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(WrappedInputEvent(event as InputEvent))
    }
}

internal class RadioInputEventListener(
    options: Options,
    listener: (WrappedRadioInputEvent) -> Unit
) : WrappedEventListener<WrappedRadioInputEvent>(EventsListenerBuilder.INPUT, options, listener) {
    override fun handleEvent(event: Event) {
        val checked = event.target.asDynamic().checked as Boolean
        listener(WrappedRadioInputEvent(event, checked))
    }
}

internal class CheckBoxInputEventListener(
    options: Options,
    listener: (WrappedCheckBoxInputEvent) -> Unit
) : WrappedEventListener<WrappedCheckBoxInputEvent>(
    EventsListenerBuilder.INPUT, options, listener
) {
    override fun handleEvent(event: Event) {
        val checked = event.target.asDynamic().checked as Boolean
        listener(WrappedCheckBoxInputEvent(event, checked))
    }
}

internal class TextInputEventListener(
    options: Options,
    listener: (WrappedTextInputEvent) -> Unit
) : WrappedEventListener<WrappedTextInputEvent>(EventsListenerBuilder.INPUT, options, listener) {
    override fun handleEvent(event: Event) {
        val text = event.target.asDynamic().value as String
        listener(WrappedTextInputEvent(event as InputEvent, text))
    }
}
