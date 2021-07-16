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

open class EventsListenerBuilder {

    protected val listeners = mutableListOf<SyntheticEventListener<*>>()

    /* Mouse Events */

    fun onContextMenu(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(CONTEXTMENU, options, listener))
    }

    fun onClick(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(CLICK, options, listener))
    }

    fun onDoubleClick(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(DBLCLICK, options, listener))
    }

    fun onMouseDown(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSEDOWN, options, listener))
    }

    fun onMouseUp(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSEUP, options, listener))
    }

    fun onMouseEnter(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSEENTER, options, listener))
    }

    fun onMouseLeave(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSELEAVE, options, listener))
    }

    fun onMouseMove(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSEMOVE, options, listener))
    }

    fun onMouseOut(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSEOUT, options, listener))
    }

    fun onMouseOver(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(MouseEventListener(MOUSEOVER, options, listener))
    }

    fun onWheel(options: Options = Options.DEFAULT, listener: SyntheticWheelEventListener) {
        listeners.add(MouseWheelEventListener(WHEEL, options, listener))
    }

    /* Drag Events */

    fun onDrag(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DRAG, options, listener))
    }

    fun onDrop(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DROP, options, listener))
    }

    fun onDragStart(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DRAGSTART, options, listener))
    }

    fun onDragEnd(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DRAGEND, options, listener))
    }

    fun onDragOver(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DRAGOVER, options, listener))
    }

    fun onDragEnter(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DRAGENTER, options, listener))
    }

    fun onDragLeave(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(DragEventListener(DRAGLEAVE, options, listener))
    }

    /* End of Drag Events */

    /* Clipboard Events */

    fun onCopy(options: Options = Options.DEFAULT, listener: (SyntheticClipboardEvent) -> Unit) {
        listeners.add(ClipboardEventListener(COPY, options, listener))
    }

    fun onCut(options: Options = Options.DEFAULT, listener: (SyntheticClipboardEvent) -> Unit) {
        listeners.add(ClipboardEventListener(CUT, options, listener))
    }

    fun onPaste(options: Options = Options.DEFAULT, listener: (SyntheticClipboardEvent) -> Unit) {
        listeners.add(ClipboardEventListener(PASTE, options, listener))
    }

    /* End of Clipboard Events */

    /* Keyboard Events */

    fun onKeyDown(options: Options = Options.DEFAULT, listener: (SyntheticKeyboardEvent) -> Unit) {
        listeners.add(KeyboardEventListener(KEYDOWN, options, listener))
    }

    fun onKeyUp(options: Options = Options.DEFAULT, listener: (SyntheticKeyboardEvent) -> Unit) {
        listeners.add(KeyboardEventListener(KEYUP, options, listener))
    }

    /* End of Keyboard Events */

    /* Focus Events */

    fun onFocus(options: Options = Options.DEFAULT, listener: (SyntheticFocusEvent) -> Unit) {
        listeners.add(FocusEventListener(FOCUS, options, listener))
    }

    fun onBlur(options: Options = Options.DEFAULT, listener: (SyntheticFocusEvent) -> Unit) {
        listeners.add(FocusEventListener(BLUR, options, listener))
    }

    fun onFocusIn(options: Options = Options.DEFAULT, listener: (SyntheticFocusEvent) -> Unit) {
        listeners.add(FocusEventListener(FOCUSIN, options, listener))
    }

    fun onFocusOut(options: Options = Options.DEFAULT, listener: (SyntheticFocusEvent) -> Unit) {
        listeners.add(FocusEventListener(FOCUSOUT, options, listener))
    }

    /* End of Focus Events */

    /* Touch Events */

    fun onTouchCancel(options: Options = Options.DEFAULT, listener: (SyntheticTouchEvent) -> Unit) {
        listeners.add(TouchEventListener(TOUCHCANCEL, options, listener))
    }

    fun onTouchMove(options: Options = Options.DEFAULT, listener: (SyntheticTouchEvent) -> Unit) {
        listeners.add(TouchEventListener(TOUCHMOVE, options, listener))
    }

    fun onTouchEnd(options: Options = Options.DEFAULT, listener: (SyntheticTouchEvent) -> Unit) {
        listeners.add(TouchEventListener(TOUCHEND, options, listener))
    }

    fun onTouchStart(options: Options = Options.DEFAULT, listener: (SyntheticTouchEvent) -> Unit) {
        listeners.add(TouchEventListener(TOUCHSTART, options, listener))
    }

    /* End of Touch Events */

    /* Animation Events */

    fun onAnimationEnd(options: Options = Options.DEFAULT, listener: (SyntheticAnimationEvent) -> Unit) {
        listeners.add(AnimationEventListener(ANIMATIONEND, options, listener))
    }

    fun onAnimationIteration(options: Options = Options.DEFAULT, listener: (SyntheticAnimationEvent) -> Unit) {
        listeners.add(AnimationEventListener(ANIMATIONITERATION, options, listener))
    }

    fun onAnimationStart(options: Options = Options.DEFAULT, listener: (SyntheticAnimationEvent) -> Unit) {
        listeners.add(AnimationEventListener(ANIMATIONSTART, options, listener))
    }

    /* End of Animation Events */

    fun onScroll(options: Options = Options.DEFAULT, listener: (SyntheticEvent<EventTarget>) -> Unit) {
        listeners.add(SyntheticEventListener(SCROLL, options, listener))
    }

    fun collectListeners(): List<SyntheticEventListener<*>> = listeners

    fun <T : SyntheticEvent<out EventTarget>> addEventListener(
        eventName: String,
        options: Options = Options.DEFAULT,
        listener: (T) -> Unit
    ) {
        listeners.add(SyntheticEventListener(eventName, options, listener))
    }

    fun addEventListener(
        eventName: String,
        options: Options = Options.DEFAULT,
        listener: (SyntheticEvent<EventTarget>) -> Unit
    ) {
        listeners.add(SyntheticEventListener(eventName, options, listener))
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
