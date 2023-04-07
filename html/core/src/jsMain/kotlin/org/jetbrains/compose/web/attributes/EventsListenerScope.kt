package org.jetbrains.compose.web.attributes

// if you change API for this interface, note that there is a link to this file in this tutorial: https://github.com/JetBrains/compose-jb/blob/master/tutorials/Web/Events_Handling/README.md#other-event-handlers

import androidx.compose.web.events.SyntheticDragEvent
import androidx.compose.web.events.SyntheticEvent
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import org.jetbrains.compose.web.events.*
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.w3c.dom.events.EventTarget

private typealias SyntheticMouseEventListener = (SyntheticMouseEvent) -> Unit
private typealias SyntheticWheelEventListener = (SyntheticWheelEvent) -> Unit
private typealias SyntheticDragEventListener = (SyntheticDragEvent) -> Unit

@Deprecated(
    message = "Renamed to EventsListenerScopeBuilder",
    replaceWith = ReplaceWith("EventsListenerScopeBuilder", "org.jetbrains.compose.web.attributes.EventsListenerScopeBuilder")
)
typealias EventsListenerBuilder = EventsListenerScopeBuilder

/**
 * [EventsListenerScope] is used most often not directly but via [AttrsScope].
 * Its purpose is to add events to the element. For all most frequently used events there
 * exist dedicated method. In case you need to support event that doesn't have such method,
 * use [addEventListener]
 */
interface EventsListenerScope {
    @ComposeWebInternalApi
    fun registerEventListener(listener: SyntheticEventListener<*>)

    /* Mouse Events */

    fun onContextMenu(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(CONTEXTMENU, listener))
    }

    fun onClick(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(CLICK, listener))
    }

    fun onDoubleClick(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(DBLCLICK, listener))
    }

    fun onMouseDown(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(MOUSEDOWN, listener))
    }

    fun onMouseUp(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(MOUSEUP, listener))
    }

    fun onMouseEnter(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(MOUSEENTER, listener))
    }

    fun onMouseLeave(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(MOUSELEAVE, listener))
    }

    fun onMouseMove(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(MOUSEMOVE, listener))
    }

    fun onMouseOut(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(MOUSEOUT, listener))
    }

    fun onMouseOver(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(MOUSEOVER, listener))
    }

    fun onWheel(listener: SyntheticWheelEventListener) {
        registerEventListener(MouseWheelEventListener(WHEEL, listener))
    }

    /* Drag Events */

    fun onDrag(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(DRAG, listener))
    }

    fun onDrop(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(DROP, listener))
    }

    fun onDragStart(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(DRAGSTART, listener))
    }

    fun onDragEnd(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(DRAGEND, listener))
    }

    fun onDragOver(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(DRAGOVER, listener))
    }

    fun onDragEnter(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(DRAGENTER, listener))
    }

    fun onDragLeave(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(DRAGLEAVE, listener))
    }

    /* End of Drag Events */

    /* Clipboard Events */

    fun onCopy(listener: (SyntheticClipboardEvent) -> Unit) {
        registerEventListener(ClipboardEventListener(COPY, listener))
    }

    fun onCut(listener: (SyntheticClipboardEvent) -> Unit) {
        registerEventListener(ClipboardEventListener(CUT, listener))
    }

    fun onPaste(listener: (SyntheticClipboardEvent) -> Unit) {
        registerEventListener(ClipboardEventListener(PASTE, listener))
    }

    /* End of Clipboard Events */

    /* Keyboard Events */

    fun onKeyDown(listener: (SyntheticKeyboardEvent) -> Unit) {
        registerEventListener(KeyboardEventListener(KEYDOWN, listener))
    }

    fun onKeyUp(listener: (SyntheticKeyboardEvent) -> Unit) {
        registerEventListener(KeyboardEventListener(KEYUP, listener))
    }

    /* End of Keyboard Events */

    /* Focus Events */

    fun onFocus(listener: (SyntheticFocusEvent) -> Unit) {
        registerEventListener(FocusEventListener(FOCUS, listener))
    }

    fun onBlur(listener: (SyntheticFocusEvent) -> Unit) {
        registerEventListener(FocusEventListener(BLUR, listener))
    }

    fun onFocusIn(listener: (SyntheticFocusEvent) -> Unit) {
        registerEventListener(FocusEventListener(FOCUSIN, listener))
    }

    fun onFocusOut(listener: (SyntheticFocusEvent) -> Unit) {
        registerEventListener(FocusEventListener(FOCUSOUT, listener))
    }

    /* End of Focus Events */

    /* Touch Events */

    fun onTouchCancel(listener: (SyntheticTouchEvent) -> Unit) {
        registerEventListener(TouchEventListener(TOUCHCANCEL, listener))
    }

    fun onTouchMove(listener: (SyntheticTouchEvent) -> Unit) {
        registerEventListener(TouchEventListener(TOUCHMOVE, listener))
    }

    fun onTouchEnd(listener: (SyntheticTouchEvent) -> Unit) {
        registerEventListener(TouchEventListener(TOUCHEND, listener))
    }

    fun onTouchStart(listener: (SyntheticTouchEvent) -> Unit) {
        registerEventListener(TouchEventListener(TOUCHSTART, listener))
    }

    /* End of Touch Events */

    /* Animation Events */

    fun onAnimationEnd(listener: (SyntheticAnimationEvent) -> Unit) {
        registerEventListener(AnimationEventListener(ANIMATIONEND, listener))
    }

    fun onAnimationIteration(listener: (SyntheticAnimationEvent) -> Unit) {
        registerEventListener(AnimationEventListener(ANIMATIONITERATION, listener))
    }

    fun onAnimationStart(listener: (SyntheticAnimationEvent) -> Unit) {
        registerEventListener(AnimationEventListener(ANIMATIONSTART, listener))
    }

    /* End of Animation Events */

    fun onScroll(listener: (SyntheticEvent<EventTarget>) -> Unit) {
        registerEventListener(SyntheticEventListener(SCROLL, listener))
    }

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
        registerEventListener(SyntheticEventListener(eventName, listener))
    }

    fun addEventListener(
        eventName: String,
        listener: (SyntheticEvent<EventTarget>) -> Unit
    ) {
        registerEventListener(SyntheticEventListener(eventName, listener))
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

open class EventsListenerScopeBuilder : EventsListenerScope {
    private val listeners: MutableList<SyntheticEventListener<*>> = mutableListOf()

    override fun registerEventListener(listener: SyntheticEventListener<*>) {
        listeners.add(listener)
    }

    internal fun copyListenersFrom(from: EventsListenerScopeBuilder) {
        listeners.addAll(from.listeners)
    }

    internal fun collectListeners(): List<SyntheticEventListener<*>> = listeners
}
