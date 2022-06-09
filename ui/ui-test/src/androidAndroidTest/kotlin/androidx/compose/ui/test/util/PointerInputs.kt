/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.test.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat

data class DataPoint(
    val id: PointerId,
    val timestamp: Long,
    val position: Offset,
    val down: Boolean,
    val pointerType: PointerType,
    val eventType: PointerEventType,
    val buttons: PointerButtons,
    val keyboardModifiers: PointerKeyboardModifiers,
) {
    constructor(change: PointerInputChange, event: PointerEvent) : this(
        change.id,
        change.uptimeMillis,
        change.position,
        change.pressed,
        change.type,
        event.type,
        event.buttons,
        event.keyboardModifiers,
    )

    val x get() = position.x
    val y get() = position.y
}

/**
 * A [PointerInputModifier] that records all [PointerEvent]s as they pass through the
 * [PointerEventPass.Initial] phase, without consuming anything. This modifier is supposed to be
 * completely transparent to the rest of the system.
 *
 * Does not support multiple pointers: all [PointerInputChange]s are flattened in the recorded
 * list.
 */
class SinglePointerInputRecorder : PointerInputModifier {
    private val _events = mutableListOf<DataPoint>()
    val events get() = _events as List<DataPoint>

    private val velocityTracker = VelocityTracker()
    val recordedVelocity get() = velocityTracker.calculateVelocity()

    override val pointerInputFilter = RecordingFilter { event ->
        event.changes.forEach {
            _events.add(DataPoint(it, event))
            velocityTracker.addPosition(it.uptimeMillis, it.position)
        }
    }
}

/**
 * A [PointerInputModifier] that records all [PointerEvent]s as they pass through the
 * [PointerEventPass.Initial] phase, without consuming anything. This modifier is supposed to be
 * completely transparent to the rest of the system.
 *
 * Supports multiple pointers: the set of [PointerInputChange]s from each event is kept together
 * in the recorded list.
 */
class MultiPointerInputRecorder : PointerInputModifier {
    data class Event(val pointers: List<DataPoint>) {
        val pointerCount: Int get() = pointers.size
        fun getPointer(index: Int) = pointers[index]
    }

    private val _events = mutableListOf<Event>()
    val events get() = _events as List<Event>

    override val pointerInputFilter = RecordingFilter { event ->
        _events.add(Event(event.changes.map { DataPoint(it, event) }))
    }
}

/**
 * A [PointerInputFilter] that [record]s each [PointerEvent][onPointerEvent] during the
 * [PointerEventPass.Initial] pass. Does not consume anything itself, although implementation can
 * (but really shouldn't).
 */
class RecordingFilter(
    private val record: (PointerEvent) -> Unit
) : PointerInputFilter() {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (pass == PointerEventPass.Initial) {
            record(pointerEvent)
        }
    }

    override fun onCancel() {
        // Do nothing
    }
}

val SinglePointerInputRecorder.downEvents get() = events.filter { it.down }

val SinglePointerInputRecorder.recordedDurationMillis: Long
    get() {
        check(events.isNotEmpty()) { "No events recorded" }
        return events.last().timestamp - events.first().timestamp
    }

fun SinglePointerInputRecorder.assertTimestampsAreIncreasing() {
    check(events.isNotEmpty()) { "No events recorded" }
    events.reduce { prev, curr ->
        assertThat(curr.timestamp).isAtLeast(prev.timestamp)
        curr
    }
}

fun MultiPointerInputRecorder.assertTimestampsAreIncreasing() {
    check(events.isNotEmpty()) { "No events recorded" }
    // Check that each event has the same timestamp
    events.forEach { event ->
        assertThat(event.pointerCount).isAtLeast(1)
        val currTime = event.pointers[0].timestamp
        for (i in 1 until event.pointerCount) {
            assertThat(event.pointers[i].timestamp).isEqualTo(currTime)
        }
    }
    // Check that the timestamps are ordered
    assertThat(events.map { it.pointers[0].timestamp }).isInOrder()
}

fun SinglePointerInputRecorder.assertOnlyLastEventIsUp() {
    check(events.isNotEmpty()) { "No events recorded" }
    assertThat(events.last().down).isFalse()
    assertThat(events.count { !it.down }).isEqualTo(1)
}

fun SinglePointerInputRecorder.assertUpSameAsLastMove() {
    check(events.isNotEmpty()) { "No events recorded" }
    events.last().let {
        assertThat(it.eventType).isEqualTo(Release)
        downEvents.last().verify(it.timestamp, it.id, true, it.position, it.pointerType, Move)
    }
}

fun SinglePointerInputRecorder.assertSinglePointer() {
    assertThat(events.map { it.id }.distinct()).hasSize(1)
}

fun DataPoint.verify(
    expectedTimestamp: Long?,
    expectedId: PointerId?,
    expectedDown: Boolean,
    expectedPosition: Offset,
    expectedPointerType: PointerType,
    expectedEventType: PointerEventType,
    expectedButtons: PointerButtons = PointerButtons(0),
    expectedKeyboardModifiers: PointerKeyboardModifiers = PointerKeyboardModifiers(0),
) {
    if (expectedTimestamp != null) {
        assertThat(timestamp).isEqualTo(expectedTimestamp)
    }
    if (expectedId != null) {
        assertThat(id).isEqualTo(expectedId)
    }
    assertThat(down).isEqualTo(expectedDown)
    assertThat(position).isEqualTo(expectedPosition)
    assertThat(pointerType).isEqualTo(expectedPointerType)
    assertThat(eventType).isEqualTo(expectedEventType)
    assertThat(buttons).isEqualTo(expectedButtons)
    assertThat(keyboardModifiers).isEqualTo(expectedKeyboardModifiers)
}

/**
 * Checks that the coordinates are progressing in a monotonous direction
 */
fun List<DataPoint>.isMonotonicBetween(start: Offset, end: Offset) {
    map { it.x }.isMonotonicBetween(start.x, end.x, 1e-3f)
    map { it.y }.isMonotonicBetween(start.y, end.y, 1e-3f)
}

fun List<DataPoint>.hasSameTimeBetweenEvents() {
    zipWithNext { a, b -> b.timestamp - a.timestamp }.sorted().apply {
        assertThat(last() - first()).isAtMost(1L)
    }
}

fun List<DataPoint>.areSampledFromCurve(curve: (Long) -> Offset) {
    val t0 = first().timestamp
    forEach {
        it.position.isAlmostEqualTo(curve(it.timestamp - t0))
    }
}
