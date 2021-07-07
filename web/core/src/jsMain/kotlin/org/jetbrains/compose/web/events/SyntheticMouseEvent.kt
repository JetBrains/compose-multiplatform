package androidx.compose.web.events

import org.w3c.dom.DataTransfer
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent
 */
open class SyntheticMouseEvent(
    target: HTMLElement,
    nativeEvent: MouseEvent
) : SyntheticEvent<HTMLElement>(target, nativeEvent) {

    constructor(nativeEvent: MouseEvent): this(
        target = nativeEvent.target as HTMLElement,
        nativeEvent = nativeEvent
    )

    private val mouseEvent = nativeEvent

    val altKey: Boolean = nativeEvent.altKey
    val button: Short = nativeEvent.button
    val buttons: Short = nativeEvent.buttons
    val clientX: Int = nativeEvent.clientX
    val clientY: Int = nativeEvent.clientY
    val ctrlKey: Boolean = nativeEvent.ctrlKey
    val metaKey: Boolean = nativeEvent.metaKey
    val movementX: Int = nativeEvent.asDynamic().movementX as Int
    val movementY: Int = nativeEvent.asDynamic().movementY as Int
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
    target: HTMLElement,
    nativeEvent: WheelEvent
) : SyntheticMouseEvent(
    target,
    nativeEvent
) {

    constructor(nativeEvent: WheelEvent): this(
        target = nativeEvent.target as HTMLElement,
        nativeEvent = nativeEvent
    )

    val deltaX: Double = nativeEvent.deltaX
    val deltaY: Double = nativeEvent.deltaY
    val deltaZ: Double = nativeEvent.deltaZ
    val deltaMode: Int = nativeEvent.deltaMode
}

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/DragEvent
 */
class SyntheticDragEvent(
    target: HTMLElement,
    nativeEvent: DragEvent
) : SyntheticMouseEvent(
    target,
    nativeEvent
) {

    constructor(nativeEvent: DragEvent): this(
        target = nativeEvent.target as HTMLElement,
        nativeEvent = nativeEvent
    )

    val dataTransfer: DataTransfer? = nativeEvent.dataTransfer
}
