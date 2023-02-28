package androidx.compose.web.events

import org.jetbrains.compose.web.NativeEventExtension
import org.w3c.dom.DataTransfer
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
open class SyntheticMouseEvent internal constructor(
    nativeEvent: MouseEvent
) : SyntheticEvent<EventTarget>(nativeEvent) {

    private val mouseEvent = nativeEvent

    val altKey: Boolean = nativeEvent.altKey
    val button: Short = nativeEvent.button
    val buttons: Short = nativeEvent.buttons
    val clientX: Int = nativeEvent.clientX
    val clientY: Int = nativeEvent.clientY
    val ctrlKey: Boolean = nativeEvent.ctrlKey
    val metaKey: Boolean = nativeEvent.metaKey

    // https://github.com/JetBrains/compose-jb/issues/1053
    // movementX and movementY are undefined in SafariMobile MouseEvent
    val movementX: Int = (nativeEvent as NativeEventExtension).movementX
    val movementY: Int = (nativeEvent as NativeEventExtension).movementY

    val offsetX: Double = nativeEvent.offsetX
    val offsetY: Double = nativeEvent.offsetY
    val pageX: Double = nativeEvent.pageX
    val pageY: Double = nativeEvent.pageY
    val region: String? = nativeEvent.region
    val relatedTarget: EventTarget? = nativeEvent.relatedTarget
    val screenX: Int = nativeEvent.screenX
    val screenY: Int = nativeEvent.screenY
    val shiftKey: Boolean = nativeEvent.shiftKey
    val x: Double = nativeEvent.x
    val y: Double = nativeEvent.y

    fun getModifierState(keyArg: String): Boolean = mouseEvent.getModifierState(keyArg)
}


/**
 * https://developer.mozilla.org/en-US/docs/Web/API/WheelEvent
 */
class SyntheticWheelEvent(
    nativeEvent: WheelEvent
) : SyntheticMouseEvent(
    nativeEvent
) {
    val deltaX: Double = nativeEvent.deltaX
    val deltaY: Double = nativeEvent.deltaY
    val deltaZ: Double = nativeEvent.deltaZ
    val deltaMode: Int = nativeEvent.deltaMode
}

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/DragEvent
 */
class SyntheticDragEvent(
    nativeEvent: DragEvent
) : SyntheticMouseEvent(
    nativeEvent
) {
    val dataTransfer: DataTransfer? = nativeEvent.dataTransfer
}
