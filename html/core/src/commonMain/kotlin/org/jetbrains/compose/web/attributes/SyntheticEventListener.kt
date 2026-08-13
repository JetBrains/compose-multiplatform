package org.jetbrains.compose.web.attributes

import androidx.compose.web.events.SyntheticDragEvent
import androidx.compose.web.events.SyntheticEvent
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import kotlinx.browser.dom.DragEvent
import kotlinx.browser.dom.TouchEvent
import kotlinx.browser.dom.clipboard.ClipboardEvent
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventListener
import kotlinx.browser.dom.events.EventTarget
import kotlinx.browser.dom.events.FocusEvent
import kotlinx.browser.dom.events.KeyboardEvent
import kotlinx.browser.dom.events.MouseEvent
import kotlinx.browser.dom.events.WheelEvent
import org.jetbrains.compose.web.events.SyntheticAnimationEvent
import org.jetbrains.compose.web.events.SyntheticClipboardEvent
import org.jetbrains.compose.web.events.SyntheticFocusEvent
import org.jetbrains.compose.web.events.SyntheticKeyboardEvent
import org.jetbrains.compose.web.events.SyntheticTouchEvent
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.NamedEventListener
import org.jetbrains.compose.web.internal.unsafeCast

@OptIn(ComposeWebInternalApi::class)
open class SyntheticEventListener<T : SyntheticEvent<*>> internal constructor(
    val event: String,
    val listener: (T) -> Unit
) : EventListener, NamedEventListener {

    override val name: String = event

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
        listener(SyntheticAnimationEvent(event))
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
