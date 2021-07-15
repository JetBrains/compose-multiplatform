package org.jetbrains.compose.web.attributes

import androidx.compose.web.attributes.SyntheticInputEvent
import androidx.compose.web.events.SyntheticDragEvent
import androidx.compose.web.events.SyntheticEvent
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import org.jetbrains.compose.web.attributes.EventsListenerBuilder.Companion.CHANGE
import org.jetbrains.compose.web.attributes.EventsListenerBuilder.Companion.INPUT
import org.jetbrains.compose.web.attributes.EventsListenerBuilder.Companion.SELECT
import org.jetbrains.compose.web.events.*
import org.w3c.dom.DragEvent
import org.w3c.dom.TouchEvent
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.*

open class SyntheticEventListener<T : SyntheticEvent<*>>(
    val event: String,
    val options: Options,
    val listener: (T) -> Unit
) : EventListener {

    @Suppress("UNCHECKED_CAST")
    override fun handleEvent(event: Event) {
        listener(SyntheticEvent<EventTarget>(event) as T)
    }
}

class Options {
    // TODO: add options for addEventListener

    companion object {
        val DEFAULT = Options()
    }
}

internal class AnimationEventListener(
    event: String,
    options: Options,
    listener: (SyntheticAnimationEvent) -> Unit
) : SyntheticEventListener<SyntheticAnimationEvent>(
    event, options, listener
) {
    override fun handleEvent(event: Event) {
        listener(SyntheticAnimationEvent(event))
    }
}

internal class MouseEventListener(
    event: String,
    options: Options,
    listener: (SyntheticMouseEvent) -> Unit
) : SyntheticEventListener<SyntheticMouseEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticMouseEvent(event.unsafeCast<MouseEvent>()))
    }
}

internal class MouseWheelEventListener(
    event: String,
    options: Options,
    listener: (SyntheticWheelEvent) -> Unit
) : SyntheticEventListener<SyntheticWheelEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticWheelEvent(event as WheelEvent))
    }
}

internal class KeyboardEventListener(
    event: String,
    options: Options,
    listener: (SyntheticKeyboardEvent) -> Unit
) : SyntheticEventListener<SyntheticKeyboardEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticKeyboardEvent(event as KeyboardEvent))
    }
}

internal class FocusEventListener(
    event: String,
    options: Options,
    listener: (SyntheticFocusEvent) -> Unit
) : SyntheticEventListener<SyntheticFocusEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticFocusEvent(event as FocusEvent))
    }
}

internal class TouchEventListener(
    event: String,
    options: Options,
    listener: (SyntheticTouchEvent) -> Unit
) : SyntheticEventListener<SyntheticTouchEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticTouchEvent(event as TouchEvent))
    }
}

internal class DragEventListener(
    event: String,
    options: Options,
    listener: (SyntheticDragEvent) -> Unit
) : SyntheticEventListener<SyntheticDragEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticDragEvent(event as DragEvent))
    }
}

internal class ClipboardEventListener(
    event: String,
    options: Options,
    listener: (SyntheticClipboardEvent) -> Unit
) : SyntheticEventListener<SyntheticClipboardEvent>(event, options, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticClipboardEvent(event as ClipboardEvent))
    }
}

internal class InputEventListener<InputValueType, Target: EventTarget>(
    eventName: String = INPUT,
    options: Options,
    val inputType: InputType<InputValueType>,
    listener: (SyntheticInputEvent<InputValueType, Target>) -> Unit
) : SyntheticEventListener<SyntheticInputEvent<InputValueType, Target>>(
    eventName, options, listener
) {
    override fun handleEvent(event: Event) {
        val value = inputType.inputValue(event)
        listener(SyntheticInputEvent(value, event))
    }
}

internal class ChangeEventListener<InputValueType, Target: EventTarget>(
    options: Options,
    val inputType: InputType<InputValueType>,
    listener: (SyntheticChangeEvent<InputValueType, Target>) -> Unit
) : SyntheticEventListener<SyntheticChangeEvent<InputValueType, Target>>(
    CHANGE, options, listener
) {
    override fun handleEvent(event: Event) {
        val value = inputType.inputValue(event)
        listener(SyntheticChangeEvent(value, event))
    }
}

internal class SelectEventListener<Target: EventTarget>(
    options: Options,
    listener: (SyntheticSelectEvent<Target>) -> Unit
) : SyntheticEventListener<SyntheticSelectEvent<Target>>(
    SELECT, options, listener
) {
    override fun handleEvent(event: Event) {
        listener(SyntheticSelectEvent(event))
    }
}

