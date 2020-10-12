/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.input.pointer

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Uptime

internal fun PointerInputEventData(
    id: Int,
    uptime: Uptime,
    position: Offset?,
    down: Boolean
): PointerInputEventData {
    val pointerInputData = PointerInputData(
        uptime,
        position,
        down
    )
    return PointerInputEventData(PointerId(id.toLong()), pointerInputData)
}

internal fun PointerInputEvent(
    id: Int,
    uptime: Uptime,
    position: Offset?,
    down: Boolean
): PointerInputEvent {
    return PointerInputEvent(
        uptime,
        listOf(PointerInputEventData(id, uptime, position, down)),
        MotionEventDouble
    )
}

internal fun PointerInputEvent(
    uptime: Uptime,
    pointers: List<PointerInputEventData>
) = PointerInputEvent(
    uptime,
    pointers,
    MotionEventDouble
)

internal fun catchThrowable(lambda: () -> Unit): Throwable? {
    var exception: Throwable? = null

    try {
        lambda()
    } catch (theException: Throwable) {
        exception = theException
    }

    return exception
}

/**
 * To be used to construct types that require a MotionEvent but where no details of the MotionEvent
 * are actually needed.
 */
internal val MotionEventDouble = MotionEvent.obtain(0L, 0L, ACTION_DOWN, 0f, 0f, 0)

internal fun Modifier.spyGestureFilter(
    callback: (PointerEventPass) -> Unit
): Modifier = composed {
    val modifier = remember { SpyGestureModifier() }
    modifier.callback = callback
    modifier
}

internal class SpyGestureModifier : PointerInputModifier {

    lateinit var callback: (PointerEventPass) -> Unit

    override val pointerInputFilter: PointerInputFilter =
        object : PointerInputFilter() {

            override fun onPointerEvent(
                pointerEvent: PointerEvent,
                pass: PointerEventPass,
                bounds: IntSize
            ) {
                callback.invoke(pass)
            }

            override fun onCancel() {
                // Nothing
            }

            override fun onCustomEvent(customEvent: CustomEvent, pass: PointerEventPass) {
                // Nothing
            }
        }

    // We only need this because IR compiler doesn't like converting lambdas to Runnables
    @Suppress("DEPRECATION")
    internal fun androidx.test.rule.ActivityTestRule<*>.runOnUiThreadIR(block: () -> Unit) {
        val runnable = Runnable { block() }
        runOnUiThread(runnable)
    }
}

/**
 * Creates a simple [MotionEvent].
 *
 * @param dispatchTarget The [View] that the [MotionEvent] is going to be dispatched to. This
 * guarantees that the MotionEvent is created correctly for both Compose (which relies on raw
 * coordinates being correct) and Android (which requires that local coordinates are correct).
 */
@Suppress("TestFunctionName")
internal fun MotionEvent(
    eventTime: Int,
    action: Int,
    numPointers: Int,
    actionIndex: Int,
    pointerProperties: Array<MotionEvent.PointerProperties>,
    pointerCoords: Array<MotionEvent.PointerCoords>,
    dispatchTarget: View
): MotionEvent {

    val locationOnScreen = IntArray(2) { 0 }
    dispatchTarget.getLocationOnScreen(locationOnScreen)

    pointerCoords.forEach {
        it.x += locationOnScreen[0]
        it.y += locationOnScreen[1]
    }

    val motionEvent = MotionEvent.obtain(
        0,
        eventTime.toLong(),
        action + (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
        numPointers,
        pointerProperties,
        pointerCoords,
        0,
        0,
        0f,
        0f,
        0,
        0,
        0,
        0
    ).apply {
        offsetLocation(-locationOnScreen[0].toFloat(), -locationOnScreen[1].toFloat())
    }

    pointerCoords.forEach {
        it.x -= locationOnScreen[0]
        it.y -= locationOnScreen[1]
    }

    return motionEvent
}

@Suppress("TestFunctionName")
internal fun PointerProperties(id: Int) =
    MotionEvent.PointerProperties().apply { this.id = id }

@Suppress("TestFunctionName")
internal fun PointerCoords(x: Float, y: Float) =
    MotionEvent.PointerCoords().apply {
        this.x = x
        this.y = y
    }

internal fun PointerEvent.deepCopy() =
    PointerEvent(
        changes.map {
            it.deepCopy()
        },
        motionEvent = motionEvent
    )

internal fun PointerInputChange.deepCopy() =
    PointerInputChange(
        id,
        current.copy(),
        previous.copy(),
        consumed.copy()
    )

internal fun pointerEventOf(
    vararg changes: PointerInputChange,
    motionEvent: MotionEvent = MotionEventDouble
) = PointerEvent(changes.toList(), motionEvent)

internal class PointerInputFilterMock(
    val log: MutableList<LogEntry> = mutableListOf(),
    val initHandler: ((CustomEventDispatcher) -> Unit)? = null,
    val pointerEventHandler: PointerEventHandler? = null,
    val onCustomEvent: ((CustomEvent, PointerEventPass) -> Unit)? = null,
    layoutCoordinates: LayoutCoordinates? = null
) :
    PointerInputFilter() {

    init {
        this.layoutCoordinates = layoutCoordinates ?: LayoutCoordinatesStub(true)
    }

    override fun onInit(customEventDispatcher: CustomEventDispatcher) {
        log.add(OnInitEntry())
        initHandler?.invoke(customEventDispatcher)
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        log.add(
            OnPointerEventEntry(
                this,
                pointerEvent.deepCopy(),
                pass,
                bounds
            )
        )
        pointerEventHandler?.invokeOverPass(pointerEvent, pass, bounds)
    }

    override fun onCancel() {
        log.add(OnCancelEntry(this))
    }

    override fun onCustomEvent(customEvent: CustomEvent, pass: PointerEventPass) {
        log.add(
            OnCustomEventEntry(
                this,
                customEvent,
                pass
            )
        )
        onCustomEvent?.invoke(customEvent, pass)
    }
}

internal fun List<LogEntry>.getOnInitLog() = filterIsInstance<OnInitEntry>()

internal fun List<LogEntry>.getOnPointerEventLog() = filterIsInstance<OnPointerEventEntry>()

internal fun List<LogEntry>.getOnCancelLog() = filterIsInstance<OnCancelEntry>()

internal fun List<LogEntry>.getOnCustomEventLog() = filterIsInstance<OnCustomEventEntry>()

internal sealed class LogEntry

internal class OnInitEntry : LogEntry()

internal data class OnPointerEventEntry (
    val pointerInputFilter: PointerInputFilter,
    val pointerEvent: PointerEvent,
    val pass: PointerEventPass,
    val bounds: IntSize
) : LogEntry()

internal class OnCancelEntry (
    val pointerInputFilter: PointerInputFilter
) : LogEntry()

internal data class OnCustomEventEntry (
    val pointerInputFilter: PointerInputFilter,
    val customEvent: CustomEvent,
    val pass: PointerEventPass
) : LogEntry()

internal fun internalPointerEventOf(vararg changes: PointerInputChange) =
    InternalPointerEvent(changes.toList().associateBy { it.id }.toMutableMap(), MotionEventDouble)