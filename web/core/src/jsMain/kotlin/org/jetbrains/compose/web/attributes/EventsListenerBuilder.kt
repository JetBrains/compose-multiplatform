package org.jetbrains.compose.web.attributes

import androidx.compose.web.events.SyntheticDragEvent
import androidx.compose.web.events.SyntheticEvent
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import org.jetbrains.compose.web.events.*
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.events.EventTarget

private typealias SyntheticMouseEventListener = (SyntheticMouseEvent) -> Unit
private typealias SyntheticWheelEventListener = (SyntheticWheelEvent) -> Unit
private typealias SyntheticDragEventListener = (SyntheticDragEvent) -> Unit

/**
 * [EventsListenerBuilder] is used most often not directly but via [AttrsBuilder].
 * Its purpose is to add events to the element. For all most frequently used events there
 * exist dedicated method. In case you need to support event that doesn't have such method,
 * use [addEventListener]
 */
open class EventsListenerBuilder {

    protected val listeners = mutableListOf<SyntheticEventListener<*>>()

    /* Mouse Events */

    fun onContextMenu(listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(CONTEXTMENU, listener))
    }

    fun onClick(listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(CLICK, listener))
    }

    fun onDoubleClick(listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(DBLCLICK, listener))
    }

    fun onMouseDown(listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSEDOWN, listener))
    }

    fun onMouseUp(listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSEUP, listener))
    }

    fun onMouseEnter(listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSEENTER, listener))
    }

    fun onMouseLeave(listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSELEAVE, listener))
    }

    fun onMouseMove(listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSEMOVE, listener))
    }

    fun onMouseOut(listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSEOUT, listener))
    }

    fun onMouseOver(listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSEOVER, listener))
    }

    fun onWheel(listener: SyntheticWheelEventListener) {
        listeners.add(MouseWheelEventListener(WHEEL, listener))
    }

    /* Drag Events */

    fun onDrag(listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DRAG, listener))
    }

    fun onDrop(listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DROP, listener))
    }

    fun onDragStart(listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DRAGSTART, listener))
    }

    fun onDragEnd(listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DRAGEND, listener))
    }

    fun onDragOver(listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DRAGOVER, listener))
    }

    fun onDragEnter(listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DRAGENTER, listener))
    }

    fun onDragLeave(listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DRAGLEAVE, listener))
    }

    /* End of Drag Events */

    /* Clipboard Events */

    fun onCopy(listener: (SyntheticClipboardEvent) -> Unit) {
        listeners.add(ClipboardEventListener(COPY, listener))
    }

    fun onCut(listener: (SyntheticClipboardEvent) -> Unit) {
        listeners.add(ClipboardEventListener(CUT, listener))
    }

    fun onPaste(listener: (SyntheticClipboardEvent) -> Unit) {
        listeners.add(ClipboardEventListener(PASTE, listener))
    }

    /* End of Clipboard Events */

    /* Keyboard Events */

    fun onKeyDown(listener: (SyntheticKeyboardEvent) -> Unit) {
        listeners.add(KeyboardEventListener(KEYDOWN, listener))
    }

    fun onKeyUp(listener: (SyntheticKeyboardEvent) -> Unit) {
        listeners.add(KeyboardEventListener(KEYUP, listener))
    }

    /* End of Keyboard Events */

    /* Focus Events */

    fun onFocus(listener: (SyntheticFocusEvent) -> Unit) {
        listeners.add(FocusEventListener(FOCUS, listener))
    }

    fun onBlur(listener: (SyntheticFocusEvent) -> Unit) {
        listeners.add(FocusEventListener(BLUR, listener))
    }

    fun onFocusIn(listener: (SyntheticFocusEvent) -> Unit) {
        listeners.add(FocusEventListener(FOCUSIN, listener))
    }

    fun onFocusOut(listener: (SyntheticFocusEvent) -> Unit) {
        listeners.add(FocusEventListener(FOCUSOUT, listener))
    }

    /* End of Focus Events */

    /* Touch Events */

    fun onTouchCancel(listener: (SyntheticTouchEvent) -> Unit) {
        listeners.add(TouchEventListener(TOUCHCANCEL, listener))
    }

    fun onTouchMove(listener: (SyntheticTouchEvent) -> Unit) {
        listeners.add(TouchEventListener(TOUCHMOVE, listener))
    }

    fun onTouchEnd(listener: (SyntheticTouchEvent) -> Unit) {
        listeners.add(TouchEventListener(TOUCHEND, listener))
    }

    fun onTouchStart(listener: (SyntheticTouchEvent) -> Unit) {
        listeners.add(TouchEventListener(TOUCHSTART, listener))
    }

    /* End of Touch Events */

    /* Animation Events */

    fun onAnimationEnd(listener: (SyntheticAnimationEvent) -> Unit) {
        listeners.add(AnimationEventListener(ANIMATIONEND, listener))
    }

    fun onAnimationIteration(listener: (SyntheticAnimationEvent) -> Unit) {
        listeners.add(AnimationEventListener(ANIMATIONITERATION, listener))
    }

    fun onAnimationStart(listener: (SyntheticAnimationEvent) -> Unit) {
        listeners.add(AnimationEventListener(ANIMATIONSTART, listener))
    }

    /* End of Animation Events */

    fun onScroll(listener: (SyntheticEvent<EventTarget>) -> Unit) {
        listeners.add(SyntheticEventListener(SCROLL, listener))
    }

    internal fun collectListeners(): List<SyntheticEventListener<*>> = listeners

    /**
     * [addEventListener] used for adding arbitrary events to the element. It resembles the standard DOM addEventListener method
     * @param eventName - the name of the event
     * @param options - as of now this param is always equal to Options.DEFAULT
     * @listener - event handler
     */
    fun <T : SyntheticEvent<out EventTarget>> addEventListener(
        eventName: String,
        listener: (T) -> Unit
    ) {
        listeners.add(SyntheticEventListener(eventName, listener))
    }

    fun addEventListener(
        eventName: String,
        listener: (SyntheticEvent<EventTarget>) -> Unit
    ) {
        listeners.add(SyntheticEventListener(eventName, listener))
    }

    internal fun copyListenersFrom(from: EventsListenerBuilder) {
        listeners.addAll(from.listeners)
    }

    companion object {
        const val COPY = "copy"
        const val CUT = "cut"
        const val PASTE = "paste"
        const val CONTEXTMENU = "contextmenu"

        const val CLICK = "click"
        const val DBLCLICK = "dblclick"
        const val FOCUS = "focus"
        const val BLUR = "blur"
        const val FOCUSIN = "focusin"
        const val FOCUSOUT = "focusout"

        const val KEYDOWN = "keydown"
        const val KEYUP = "keyup"
        const val MOUSEDOWN = "mousedown"
        const val MOUSEUP = "mouseup"
        const val MOUSEENTER = "mouseenter"
        const val MOUSELEAVE = "mouseleave"
        const val MOUSEMOVE = "mousemove"
        const val MOUSEOUT = "mouseout"
        const val MOUSEOVER = "mouseover"
        const val WHEEL = "wheel"
        const val SCROLL = "scroll"
        const val SELECT = "select"

        const val TOUCHCANCEL = "touchcancel"
        const val TOUCHEND = "touchend"
        const val TOUCHMOVE = "touchmove"
        const val TOUCHSTART = "touchstart"

        const val ANIMATIONCANCEL = "animationcancel" // firefox and safari only
        const val ANIMATIONEND = "animationend"
        const val ANIMATIONITERATION = "animationiteration"
        const val ANIMATIONSTART = "animationstart"

        const val BEFOREINPUT = "beforeinput"
        const val INPUT = "input"
        const val CHANGE = "change"
        const val INVALID = "invalid"

        const val DRAG = "drag"
        const val DROP = "drop"
        const val DRAGSTART = "dragstart"
        const val DRAGEND = "dragend"
        const val DRAGOVER = "dragover"
        const val DRAGENTER = "dragenter"
        const val DRAGLEAVE = "dragleave"

        const val SUBMIT = "submit"
        const val RESET = "reset"
    }
}
