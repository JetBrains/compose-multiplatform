package org.jetbrains.compose.web.attributes

import org.jetbrains.compose.web.events.SyntheticInputEvent
import androidx.compose.web.events.SyntheticDragEvent
import androidx.compose.web.events.SyntheticEvent
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.CHANGE
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.INPUT
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.SELECT
import org.jetbrains.compose.web.events.*
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.NamedEventListener
import org.w3c.dom.DragEvent
import org.w3c.dom.TouchEvent
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.*

@OptIn(ComposeWebInternalApi::class)
open class SyntheticEventListener<T : SyntheticEvent<*>> internal constructor(
    val event: String,
    val listener: (T) -> Unit
) : EventListener, NamedEventListener {

    override val name: String = event

    @Suppress("UNCHECKED_CAST")
    override fun handleEvent(event: Event) {
        listener(SyntheticEvent<EventTarget>(event).unsafeCast<T>())
    }
}

internal class AnimationEventListener(
    event: String,
    listener: (SyntheticAnimationEvent) -> Unit
) : SyntheticEventListener<SyntheticAnimationEvent>(
    event, listener
) {
    override fun handleEvent(event: Event) {
        listener(SyntheticAnimationEvent(event, event.unsafeCast<AnimationEventDetails>()))
    }
}

internal class MouseEventListener(
    event: String,
    listener: (SyntheticMouseEvent) -> Unit
) : SyntheticEventListener<SyntheticMouseEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticMouseEvent(event.unsafeCast<MouseEvent>()))
    }
}

internal class MouseWheelEventListener(
    event: String,
    listener: (SyntheticWheelEvent) -> Unit
) : SyntheticEventListener<SyntheticWheelEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticWheelEvent(event.unsafeCast<WheelEvent>()))
    }
}

internal class KeyboardEventListener(
    event: String,
    listener: (SyntheticKeyboardEvent) -> Unit
) : SyntheticEventListener<SyntheticKeyboardEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticKeyboardEvent(event.unsafeCast<KeyboardEvent>()))
    }
}

internal class FocusEventListener(
    event: String,
    listener: (SyntheticFocusEvent) -> Unit
) : SyntheticEventListener<SyntheticFocusEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticFocusEvent(event.unsafeCast<FocusEvent>()))
    }
}

internal class TouchEventListener(
    event: String,
    listener: (SyntheticTouchEvent) -> Unit
) : SyntheticEventListener<SyntheticTouchEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticTouchEvent(event.unsafeCast<TouchEvent>()))
    }
}

internal class DragEventListener(
    event: String,
    listener: (SyntheticDragEvent) -> Unit
) : SyntheticEventListener<SyntheticDragEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticDragEvent(event.unsafeCast<DragEvent>()))
    }
}

internal class ClipboardEventListener(
    event: String,
    listener: (SyntheticClipboardEvent) -> Unit
) : SyntheticEventListener<SyntheticClipboardEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticClipboardEvent(event.unsafeCast<ClipboardEvent>()))
    }
}

internal class InputEventListener<InputValueType, Target: EventTarget>(
    eventName: String = INPUT,
    val inputType: InputType<InputValueType>,
    listener: (SyntheticInputEvent<InputValueType, Target>) -> Unit
) : SyntheticEventListener<SyntheticInputEvent<InputValueType, Target>>(
    eventName, listener
) {
    override fun handleEvent(event: Event) {
        val value = inputType.inputValue(event)
        listener(SyntheticInputEvent(value, event))
    }
}

internal class ChangeEventListener<InputValueType, Target: EventTarget>(
    val inputType: InputType<InputValueType>,
    listener: (SyntheticChangeEvent<InputValueType, Target>) -> Unit
) : SyntheticEventListener<SyntheticChangeEvent<InputValueType, Target>>(
    CHANGE, listener
) {
    override fun handleEvent(event: Event) {
        val value = inputType.inputValue(event)
        listener(SyntheticChangeEvent(value, event))
    }
}

internal class SelectEventListener<Target: EventTarget>(
    listener: (SyntheticSelectEvent<Target>) -> Unit
) : SyntheticEventListener<SyntheticSelectEvent<Target>>(
    SELECT, listener
) {
    override fun handleEvent(event: Event) {
        listener(SyntheticSelectEvent(event, event.target.unsafeCast<SelectionInfoDetails>()))
    }
}

