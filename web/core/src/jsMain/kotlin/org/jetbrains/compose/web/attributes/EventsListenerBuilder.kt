package org.jetbrains.compose.web.attributes

import org.jetbrains.compose.web.events.WrappedCheckBoxInputEvent
import org.jetbrains.compose.web.events.WrappedClipboardEvent
import org.jetbrains.compose.web.events.WrappedDragEvent
import org.jetbrains.compose.web.events.WrappedEvent
import org.jetbrains.compose.web.events.WrappedFocusEvent
import org.jetbrains.compose.web.events.WrappedInputEvent
import org.jetbrains.compose.web.events.WrappedKeyboardEvent
import org.jetbrains.compose.web.events.WrappedMouseEvent
import org.jetbrains.compose.web.events.WrappedRadioInputEvent
import org.jetbrains.compose.web.events.WrappedTextInputEvent
import org.jetbrains.compose.web.events.WrappedTouchEvent
import org.jetbrains.compose.web.events.WrappedWheelEvent
import org.jetbrains.compose.web.events.GenericWrappedEvent

open class EventsListenerBuilder {

    protected val listeners = mutableListOf<WrappedEventListener<*>>()

    fun onCopy(options: Options = Options.DEFAULT, listener: (WrappedClipboardEvent) -> Unit) {
        listeners.add(ClipboardEventListener(COPY, options, listener))
    }

    fun onCut(options: Options = Options.DEFAULT, listener: (WrappedClipboardEvent) -> Unit) {
        listeners.add(ClipboardEventListener(CUT, options, listener))
    }

    fun onPaste(options: Options = Options.DEFAULT, listener: (WrappedClipboardEvent) -> Unit) {
        listeners.add(ClipboardEventListener(PASTE, options, listener))
    }

    fun onContextMenu(options: Options = Options.DEFAULT, listener: (WrappedMouseEvent) -> Unit) {
        listeners.add(MouseEventListener(CONTEXTMENU, options, listener))
    }

    fun onClick(options: Options = Options.DEFAULT, listener: (WrappedMouseEvent) -> Unit) {
        listeners.add(MouseEventListener(CLICK, options, listener))
    }

    fun onDoubleClick(options: Options = Options.DEFAULT, listener: (WrappedMouseEvent) -> Unit) {
        listeners.add(MouseEventListener(DBLCLICK, options, listener))
    }

    fun onGenericInput(
        options: Options = Options.DEFAULT,
        listener: (GenericWrappedEvent<*>) -> Unit
    ) {
        listeners.add(WrappedEventListener(INPUT, options, listener))
    }

    fun onChange(options: Options = Options.DEFAULT, listener: (WrappedEvent) -> Unit) {
        listeners.add(WrappedEventListener(CHANGE, options, listener))
    }

    fun onInvalid(options: Options = Options.DEFAULT, listener: (WrappedEvent) -> Unit) {
        listeners.add(WrappedEventListener(INVALID, options, listener))
    }

    fun onSearch(options: Options = Options.DEFAULT, listener: (WrappedEvent) -> Unit) {
        listeners.add(WrappedEventListener(SEARCH, options, listener))
    }

    fun onFocus(options: Options = Options.DEFAULT, listener: (WrappedFocusEvent) -> Unit) {
        listeners.add(FocusEventListener(FOCUS, options, listener))
    }

    fun onBlur(options: Options = Options.DEFAULT, listener: (WrappedFocusEvent) -> Unit) {
        listeners.add(FocusEventListener(BLUR, options, listener))
    }

    fun onFocusIn(options: Options = Options.DEFAULT, listener: (WrappedFocusEvent) -> Unit) {
        listeners.add(FocusEventListener(FOCUSIN, options, listener))
    }

    fun onFocusOut(options: Options = Options.DEFAULT, listener: (WrappedFocusEvent) -> Unit) {
        listeners.add(FocusEventListener(FOCUSOUT, options, listener))
    }

    fun onKeyDown(options: Options = Options.DEFAULT, listener: (WrappedKeyboardEvent) -> Unit) {
        listeners.add(KeyboardEventListener(KEYDOWN, options, listener))
    }

    fun onKeyUp(options: Options = Options.DEFAULT, listener: (WrappedKeyboardEvent) -> Unit) {
        listeners.add(KeyboardEventListener(KEYUP, options, listener))
    }

    fun onMouseDown(options: Options = Options.DEFAULT, listener: (WrappedMouseEvent) -> Unit) {
        listeners.add(MouseEventListener(MOUSEDOWN, options, listener))
    }

    fun onMouseUp(options: Options = Options.DEFAULT, listener: (WrappedMouseEvent) -> Unit) {
        listeners.add(MouseEventListener(MOUSEUP, options, listener))
    }

    fun onMouseEnter(options: Options = Options.DEFAULT, listener: (WrappedMouseEvent) -> Unit) {
        listeners.add(MouseEventListener(MOUSEENTER, options, listener))
    }

    fun onMouseLeave(options: Options = Options.DEFAULT, listener: (WrappedMouseEvent) -> Unit) {
        listeners.add(MouseEventListener(MOUSELEAVE, options, listener))
    }

    fun onMouseMove(options: Options = Options.DEFAULT, listener: (WrappedMouseEvent) -> Unit) {
        listeners.add(MouseEventListener(MOUSEMOVE, options, listener))
    }

    fun onMouseOut(options: Options = Options.DEFAULT, listener: (WrappedMouseEvent) -> Unit) {
        listeners.add(MouseEventListener(MOUSEOUT, options, listener))
    }

    fun onMouseOver(options: Options = Options.DEFAULT, listener: (WrappedMouseEvent) -> Unit) {
        listeners.add(MouseEventListener(MOUSEOVER, options, listener))
    }

    fun onWheel(options: Options = Options.DEFAULT, listener: (WrappedWheelEvent) -> Unit) {
        listeners.add(MouseWheelEventListener(WHEEL, options, listener))
    }

    fun onScroll(options: Options = Options.DEFAULT, listener: (WrappedEvent) -> Unit) {
        listeners.add(WrappedEventListener(SCROLL, options, listener))
    }

    fun onSelect(options: Options = Options.DEFAULT, listener: (WrappedEvent) -> Unit) {
        listeners.add(WrappedEventListener(SELECT, options, listener))
    }

    fun onTouchCancel(options: Options = Options.DEFAULT, listener: (WrappedTouchEvent) -> Unit) {
        listeners.add(TouchEventListener(TOUCHCANCEL, options, listener))
    }

    fun onTouchMove(options: Options = Options.DEFAULT, listener: (WrappedTouchEvent) -> Unit) {
        listeners.add(TouchEventListener(TOUCHMOVE, options, listener))
    }

    fun onTouchEnd(options: Options = Options.DEFAULT, listener: (WrappedTouchEvent) -> Unit) {
        listeners.add(TouchEventListener(TOUCHEND, options, listener))
    }

    fun onTouchStart(options: Options = Options.DEFAULT, listener: (WrappedTouchEvent) -> Unit) {
        listeners.add(TouchEventListener(TOUCHSTART, options, listener))
    }

    fun onAnimationEnd(options: Options = Options.DEFAULT, listener: (WrappedTouchEvent) -> Unit) {
        listeners.add(WrappedEventListener(ANIMATIONEND, options, listener))
    }

    fun onAnimationIteration(options: Options = Options.DEFAULT, listener: (WrappedEvent) -> Unit) {
        listeners.add(WrappedEventListener(ANIMATIONITERATION, options, listener))
    }

    fun onAnimationStart(options: Options = Options.DEFAULT, listener: (WrappedEvent) -> Unit) {
        listeners.add(WrappedEventListener(ANIMATIONSTART, options, listener))
    }

    fun onBeforeInput(options: Options = Options.DEFAULT, listener: (WrappedInputEvent) -> Unit) {
        listeners.add(InputEventListener(BEFOREINPUT, options, listener))
    }

    fun onDrag(options: Options = Options.DEFAULT, listener: (WrappedDragEvent) -> Unit) {
        listeners.add(DragEventListener(DRAG, options, listener))
    }

    fun onDrop(options: Options = Options.DEFAULT, listener: (WrappedDragEvent) -> Unit) {
        listeners.add(DragEventListener(DROP, options, listener))
    }

    fun onDragStart(options: Options = Options.DEFAULT, listener: (WrappedDragEvent) -> Unit) {
        listeners.add(DragEventListener(DRAGSTART, options, listener))
    }

    fun onDragEnd(options: Options = Options.DEFAULT, listener: (WrappedDragEvent) -> Unit) {
        listeners.add(DragEventListener(DRAGEND, options, listener))
    }

    fun onDragOver(options: Options = Options.DEFAULT, listener: (WrappedDragEvent) -> Unit) {
        listeners.add(DragEventListener(DRAGOVER, options, listener))
    }

    fun onDragEnter(options: Options = Options.DEFAULT, listener: (WrappedDragEvent) -> Unit) {
        listeners.add(DragEventListener(DRAGENTER, options, listener))
    }

    fun onDragLeave(options: Options = Options.DEFAULT, listener: (WrappedDragEvent) -> Unit) {
        listeners.add(DragEventListener(DRAGLEAVE, options, listener))
    }

    fun collectListeners(): List<WrappedEventListener<*>> = listeners

    fun addEventListener(
        eventName: String,
        options: Options = Options.DEFAULT,
        listener: (WrappedEvent) -> Unit
    ) {
        listeners.add(WrappedEventListener(eventName, options, listener))
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
        const val SEARCH = "search"

        const val DRAG = "drag"
        const val DROP = "drop"
        const val DRAGSTART = "dragstart"
        const val DRAGEND = "dragend"
        const val DRAGOVER = "dragover"
        const val DRAGENTER = "dragenter"
        const val DRAGLEAVE = "dragleave"
    }
}
