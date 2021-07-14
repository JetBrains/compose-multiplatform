package org.jetbrains.compose.web.attributes

import androidx.compose.web.events.SyntheticDragEvent
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import org.jetbrains.compose.web.events.WrappedClipboardEvent
import org.jetbrains.compose.web.events.WrappedEvent
import org.jetbrains.compose.web.events.WrappedFocusEvent
import org.jetbrains.compose.web.events.WrappedInputEvent
import org.jetbrains.compose.web.events.WrappedKeyboardEvent
import org.jetbrains.compose.web.events.WrappedTouchEvent
import org.jetbrains.compose.web.events.GenericWrappedEvent

private typealias SyntheticMouseEventListener = (SyntheticMouseEvent) -> Unit
private typealias SyntheticWheelEventListener = (SyntheticWheelEvent) -> Unit
private typealias SyntheticDragEventListener = (SyntheticDragEvent) -> Unit

open class EventsListenerBuilder {

    protected val listeners = mutableListOf<WrappedEventListener<*>>()

    /* Mouse Events */

    private fun createMouseEventListener(
        name: String, options: Options, listener: SyntheticMouseEventListener
    ): MouseEventListener {
        return MouseEventListener(
            event = name,
            options = options,
            listener = {
                listener(SyntheticMouseEvent(it.nativeEvent))
            }
        )
    }

    private fun createMouseWheelEventListener(
        name: String, options: Options, listener: SyntheticWheelEventListener
    ): MouseWheelEventListener {
        return MouseWheelEventListener(
            event = name,
            options = options,
            listener = {
                listener(SyntheticWheelEvent(it.nativeEvent))
            }
        )
    }

    fun onContextMenu(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(createMouseEventListener(CONTEXTMENU, options, listener))
    }

    fun onClick(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(createMouseEventListener(CLICK, options, listener))
    }

    fun onDoubleClick(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(createMouseEventListener(DBLCLICK, options, listener))
    }

    fun onMouseDown(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(createMouseEventListener(MOUSEDOWN, options, listener))
    }

    fun onMouseUp(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(createMouseEventListener(MOUSEUP, options, listener))
    }

    fun onMouseEnter(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(createMouseEventListener(MOUSEENTER, options, listener))
    }

    fun onMouseLeave(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(createMouseEventListener(MOUSELEAVE, options, listener))
    }

    fun onMouseMove(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(createMouseEventListener(MOUSEMOVE, options, listener))
    }

    fun onMouseOut(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(createMouseEventListener(MOUSEOUT, options, listener))
    }

    fun onMouseOver(options: Options = Options.DEFAULT, listener: SyntheticMouseEventListener) {
        listeners.add(createMouseEventListener(MOUSEOVER, options, listener))
    }

    fun onWheel(options: Options = Options.DEFAULT, listener: (SyntheticWheelEvent) -> Unit) {
        listeners.add(createMouseWheelEventListener(WHEEL, options, listener))
    }

    /* Drag Events */

    private fun createDragEventListener(
        name: String, options: Options, listener: SyntheticDragEventListener
    ): DragEventListener {
        return DragEventListener(
            event = name,
            options = options,
            listener = {
                listener(SyntheticDragEvent(it.nativeEvent))
            }
        )
    }

    fun onDrag(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(createDragEventListener(DRAG, options, listener))
    }

    fun onDrop(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(createDragEventListener(DROP, options, listener))
    }

    fun onDragStart(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(createDragEventListener(DRAGSTART, options, listener))
    }

    fun onDragEnd(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(createDragEventListener(DRAGEND, options, listener))
    }

    fun onDragOver(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(createDragEventListener(DRAGOVER, options, listener))
    }

    fun onDragEnter(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(createDragEventListener(DRAGENTER, options, listener))
    }

    fun onDragLeave(options: Options = Options.DEFAULT, listener: SyntheticDragEventListener) {
        listeners.add(createDragEventListener(DRAGLEAVE, options, listener))
    }

    /* End of Drag Events */

    fun onCopy(options: Options = Options.DEFAULT, listener: (WrappedClipboardEvent) -> Unit) {
        listeners.add(ClipboardEventListener(COPY, options, listener))
    }

    fun onCut(options: Options = Options.DEFAULT, listener: (WrappedClipboardEvent) -> Unit) {
        listeners.add(ClipboardEventListener(CUT, options, listener))
    }

    fun onPaste(options: Options = Options.DEFAULT, listener: (WrappedClipboardEvent) -> Unit) {
        listeners.add(ClipboardEventListener(PASTE, options, listener))
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
