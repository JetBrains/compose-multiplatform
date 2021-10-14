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

import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

internal fun PointerInputEventData(
    id: Int,
    uptime: Long,
    position: Offset,
    down: Boolean
): PointerInputEventData {
    return PointerInputEventData(
        PointerId(id.toLong()),
        uptime,
        position,
        position,
        down,
        PointerType.Touch
    )
}

internal fun PointerInputEvent(
    id: Int,
    uptime: Long,
    position: Offset,
    down: Boolean
): PointerInputEvent {
    return PointerInputEvent(
        uptime,
        listOf(PointerInputEventData(id, uptime, position, down)),
        MotionEventDouble
    )
}

internal fun PointerInputEvent(
    uptime: Long,
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

/**
 * To be used to construct types that require a MotionEvent but where only the ACTION_UP
 * type is needed.
 */
internal val MotionEventUp = MotionEvent.obtain(0L, 0L, ACTION_UP, 0f, 0f, 0)

/**
 * To be used to construct types that require a MotionEvent but where we only care if the event
 * is a hover event.
 */
internal val MotionEventHover = createHoverMotionEvent(ACTION_HOVER_MOVE, 0f, 0f)

fun createHoverMotionEvent(action: Int, x: Float, y: Float): MotionEvent {
    val pointerProperties = MotionEvent.PointerProperties().apply {
        toolType = MotionEvent.TOOL_TYPE_MOUSE
    }
    val pointerCoords = MotionEvent.PointerCoords().also {
        it.x = x
        it.y = y
    }
    return MotionEvent.obtain(
        0L /* downTime */,
        0L /* eventTime */,
        action,
        1 /* pointerCount */,
        arrayOf(pointerProperties),
        arrayOf(pointerCoords),
        0 /* metaState */,
        0 /* buttonState */,
        0f /* xPrecision */,
        0f /* yPrecision */,
        0 /* deviceId */,
        0 /* edgeFlags */,
        InputDevice.SOURCE_MOUSE,
        0 /* flags */
    )
}

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
internal fun PointerProperties(id: Int, toolType: Int = MotionEvent.TOOL_TYPE_FINGER) =
    MotionEvent.PointerProperties().apply {
        this.id = id
        this.toolType = toolType
    }

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
    ).also { it.type = type }

internal fun pointerEventOf(
    vararg changes: PointerInputChange,
    motionEvent: MotionEvent = MotionEventDouble
) = PointerEvent(changes.toList(), motionEvent)

internal class PointerInputFilterMock(
    val log: MutableList<LogEntry> = mutableListOf(),
    val pointerEventHandler: PointerEventHandler? = null,
    layoutCoordinates: LayoutCoordinates? = null
) :
    PointerInputFilter() {

    init {
        this.layoutCoordinates = layoutCoordinates ?: LayoutCoordinatesStub(true)
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
}

internal fun List<LogEntry>.getOnPointerEventLog() = filterIsInstance<OnPointerEventEntry>()

internal fun List<LogEntry>.getOnCancelLog() = filterIsInstance<OnCancelEntry>()

internal sealed class LogEntry

internal data class OnPointerEventEntry(
    val pointerInputFilter: PointerInputFilter,
    val pointerEvent: PointerEvent,
    val pass: PointerEventPass,
    val bounds: IntSize
) : LogEntry()

internal class OnCancelEntry(
    val pointerInputFilter: PointerInputFilter
) : LogEntry()

internal fun internalPointerEventOf(vararg changes: PointerInputChange): InternalPointerEvent {
    val event = if (changes.any { it.changedToUpIgnoreConsumed() }) {
        MotionEventUp
    } else {
        MotionEventDouble
    }

    return InternalPointerEvent(changes.toList().associateBy { it.id }.toMutableMap(), event)
}

internal fun hoverInternalPointerEvent(
    action: Int = ACTION_HOVER_MOVE,
    x: Float = 0f,
    y: Float = 0f
): InternalPointerEvent {
    val change = PointerInputChange(
        PointerId(0),
        0L,
        Offset(0f, 0f),
        false,
        0L,
        Offset(0f, 0f),
        false,
        ConsumedData(),
        PointerType.Mouse
    )

    return InternalPointerEvent(
        mutableMapOf(change.id to change),
        createHoverMotionEvent(action, x, y)
    )
}

internal class PointerEventSubject(
    metaData: FailureMetadata,
    val actual: PointerEvent
) : Subject(metaData, actual) {
    companion object {
        private val Factory =
            Factory<PointerEventSubject, PointerEvent> { metadata, actual ->
                PointerEventSubject(metadata, actual)
            }

        fun assertThat(actual: PointerEvent): PointerEventSubject {
            return Truth.assertAbout(Factory).that(actual)
        }
    }

    fun isStructurallyEqualTo(expected: PointerEvent) {
        check("motionEvent").that(actual.motionEvent).isEqualTo(expected.motionEvent)
        val actualChanges = actual.changes
        val expectedChanges = expected.changes
        check("changes.size").that(actualChanges.size).isEqualTo(expectedChanges.size)
        actualChanges.forEachIndexed { i, _ ->
            check("id").that(actualChanges[i].id).isEqualTo(expectedChanges[i].id)
            check("currentPosition")
                .that(actualChanges[i].position)
                .isEqualTo(expectedChanges[i].position)
            check("currentTime")
                .that(actualChanges[i].uptimeMillis)
                .isEqualTo(expectedChanges[i].uptimeMillis)
            check("currentPressed")
                .that(actualChanges[i].pressed)
                .isEqualTo(expectedChanges[i].pressed)
            check("previousTime")
                .that(actualChanges[i].previousUptimeMillis)
                .isEqualTo(expectedChanges[i].previousUptimeMillis)
            check("previousPosition")
                .that(actualChanges[i].previousPosition)
                .isEqualTo(expectedChanges[i].previousPosition)
            check("previousPressed")
                .that(actualChanges[i].previousPressed)
                .isEqualTo(expectedChanges[i].previousPressed)
            check("consumed.downChange")
                .that(actualChanges[i].consumed.downChange)
                .isEqualTo(expectedChanges[i].consumed.downChange)
            check("consumed.positionChange")
                .that(actualChanges[i].consumed.positionChange)
                .isEqualTo(expectedChanges[i].consumed.positionChange)
        }
    }
}

internal class PointerInputChangeSubject(
    metaData: FailureMetadata,
    val actual: PointerInputChange
) : Subject(metaData, actual) {

    companion object {

        private val Factory =
            Factory<PointerInputChangeSubject, PointerInputChange> { metadata, actual ->
                PointerInputChangeSubject(metadata, actual)
            }

        fun assertThat(actual: PointerInputChange?): PointerInputChangeSubject {
            return Truth.assertAbout(Factory).that(actual)
        }
    }

    fun nothingConsumed() {
        downNotConsumed()
        positionChangeNotConsumed()
    }

    fun downConsumed() {
        check("consumed.downChange").that(actual.consumed.downChange).isEqualTo(true)
    }

    fun downNotConsumed() {
        check("consumed.downChange").that(actual.consumed.downChange).isEqualTo(false)
    }

    fun positionChangeConsumed() {
        check("consumed.positionChangeConsumed")
            .that(actual.consumed.positionChange).isEqualTo(true)
    }

    fun positionChangeNotConsumed() {
        check("consumed.positionChange not Consumed")
            .that(actual.consumed.positionChange).isEqualTo(false)
    }

    fun isStructurallyEqualTo(expected: PointerInputChange) {
        check("id").that(actual.id).isEqualTo(expected.id)
        check("currentPosition")
            .that(actual.position)
            .isEqualTo(expected.position)
        check("previousPosition")
            .that(actual.previousPosition)
            .isEqualTo(expected.previousPosition)
        check("currentTime")
            .that(actual.uptimeMillis)
            .isEqualTo(expected.uptimeMillis)
        check("previousTime")
            .that(actual.previousUptimeMillis)
            .isEqualTo(expected.previousUptimeMillis)
        check("currentPressed")
            .that(actual.pressed)
            .isEqualTo(expected.pressed)
        check("previousPressed")
            .that(actual.previousPressed)
            .isEqualTo(expected.previousPressed)
        check("consumed.downChange")
            .that(actual.consumed.downChange)
            .isEqualTo(expected.consumed.downChange)
        check("consumed.positionChange")
            .that(actual.consumed.positionChange)
            .isEqualTo(expected.consumed.positionChange)
    }
}

internal fun PointerInputChange.deepCopy() = copy(
    consumed = ConsumedData(consumed.positionChange, consumed.downChange)
)
