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

import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.InputDispatcher
import androidx.compose.ui.test.MultiModalInjectionScopeImpl
import androidx.compose.ui.test.SemanticsNodeInteraction
import com.google.common.collect.Ordering
import com.google.common.truth.FloatSubject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.sign

const val TypeFinger = MotionEvent.TOOL_TYPE_FINGER
const val TypeMouse = MotionEvent.TOOL_TYPE_MOUSE
const val SourceTouchscreen = InputDevice.SOURCE_TOUCHSCREEN
const val SourceMouse = InputDevice.SOURCE_MOUSE

internal fun SemanticsNodeInteraction.assertNoTouchGestureInProgress() {
    val failMessage = "Can't verify if a touch is in progress: failed to create an injection scope"
    val node = fetchSemanticsNode(failMessage)
    val scope = MultiModalInjectionScopeImpl(node, testContext)
    assertThat(scope.inputDispatcher.isTouchInProgress).isFalse()
}

internal fun InputDispatcher.assertNoTouchGestureInProgress() {
    assertThat(isTouchInProgress).isFalse()
}

/**
 * Asserts that all event times are after their corresponding down time, and that the event
 * stream has increasing event times.
 */
internal fun InputEventRecorder.assertHasValidEventTimes() {
    events.fold(Pair(0L, 0L)) { (lastDownTime, lastEventTime), event ->
        var downTime: Long = 0

        when (event) {
            is MotionEvent -> downTime = event.downTime
            is KeyEvent -> downTime = event.downTime
            else -> AssertionError("Given InputEvent must be a MotionEvent or KeyEvent" +
                " not ${event::class.simpleName}")
        }

        assertWithMessage("monotonically increasing downTime")
            .that(downTime).isAtLeast(lastDownTime)
        assertWithMessage("monotonically increasing eventTime")
            .that(event.eventTime).isAtLeast(lastEventTime)
        assertWithMessage("downTime <= eventTime")
            .that(downTime).isAtMost(event.eventTime)
        Pair(downTime, event.eventTime)
    }
}

internal fun InputEvent.verifyTouchEvent(
    expectedPointerCount: Int,
    expectedAction: Int,
    expectedActionIndex: Int,
    expectedRelativeTime: Long
) {
    if (this is MotionEvent) {
        assertThat(pointerCount).isEqualTo(expectedPointerCount)
        assertThat(actionMasked).isEqualTo(expectedAction)
        assertThat(actionIndex).isEqualTo(expectedActionIndex)
        assertThat(relativeTime).isEqualTo(expectedRelativeTime)
        assertThat(source).isEqualTo(SourceTouchscreen)
    } else {
        throw AssertionError("A touch event must be of type MotionEvent, " +
            "not ${this::class.simpleName}")
    }
}

internal fun InputEvent.verifyTouchPointer(
    expectedPointerId: Int,
    expectedPosition: Offset
) {
    if (this is MotionEvent) {
        var index = -1
        for (i in 0 until pointerCount) {
            if (getPointerId(i) == expectedPointerId) {
                index = i
                break
            }
        }
        assertThat(index).isAtLeast(0)
        assertThat(getX(index)).isEqualTo(expectedPosition.x)
        assertThat(getY(index)).isEqualTo(expectedPosition.y)
        assertThat(getToolType(index)).isEqualTo(TypeFinger)
    } else {
        throw AssertionError("A touch event must be of type MotionEvent, " +
            "not ${this::class.simpleName}")
    }
}

internal fun InputEvent.verifyMouseEvent(
    expectedAction: Int,
    expectedRelativeTime: Long,
    expectedPosition: Offset,
    expectedButtonState: Int,
    vararg expectedAxisValues: Pair<Int, Float>, // <axis, value>
    expectedMetaState: Int = 0,
) {
    if (this is MotionEvent) {
        assertWithMessage("pointerCount").that(pointerCount).isEqualTo(1)
        assertWithMessage("pointerId").that(getPointerId(0)).isEqualTo(0)
        assertWithMessage("actionMasked").that(actionMasked).isEqualTo(expectedAction)
        assertWithMessage("actionIndex").that(actionIndex).isEqualTo(0)
        assertWithMessage("relativeTime").that(relativeTime).isEqualTo(expectedRelativeTime)
        assertWithMessage("x").that(x).isEqualTo(expectedPosition.x)
        assertWithMessage("y").that(y).isEqualTo(expectedPosition.y)
        assertWithMessage("buttonState").that(buttonState).isEqualTo(expectedButtonState)
        assertWithMessage("source").that(source).isEqualTo(SourceMouse)
        assertWithMessage("toolType").that(getToolType(0)).isEqualTo(TypeMouse)
        assertWithMessage("metaState").that(metaState).isEqualTo(expectedMetaState)
        expectedAxisValues.forEach { (axis, expectedValue) ->
            assertWithMessage("axisValue($axis)").that(getAxisValue(axis)).isEqualTo(expectedValue)
        }
    } else {
        throw AssertionError("A mouse event must be of type MotionEvent, " +
            "not ${this::class.simpleName}")
    }
}

internal fun InputEvent.verifyKeyEvent(
    expectedAction: Int,
    expectedKeyCode: Int,
    expectedEventTime: Long = 0,
    expectedDownTime: Long = 0,
    expectedMetaState: Int = 0,
    expectedRepeat: Int = 0,
) {
    if (this is KeyEvent) {
        assertWithMessage("action").that(action).isEqualTo(expectedAction)
        assertWithMessage("keyCode").that(keyCode).isEqualTo(expectedKeyCode)
        assertWithMessage("eventTime").that(eventTime).isEqualTo(expectedEventTime)
        assertWithMessage("downTime").that(downTime).isEqualTo(expectedDownTime)
        assertWithMessage("metaState").that(metaState).isEqualTo(expectedMetaState)
        assertWithMessage("repeat").that(repeatCount).isEqualTo(expectedRepeat)
    } else {
        throw AssertionError("A keyboard event must be of type KeyEvent, " +
            "not ${this::class.simpleName}")
    }
}

/**
 * Returns a list of all events between [t0] and [t1], excluding [t0] and including [t1].
 */
fun List<MotionEvent>.between(t0: Long, t1: Long): List<MotionEvent> {
    return dropWhile { it.relativeTime <= t0 }.takeWhile { it.relativeTime <= t1 }
}

/**
 * Checks that the coordinates are progressing in a monotonous direction
 */
fun List<MotionEvent>.isMonotonicBetween(start: Offset, end: Offset) {
    map { it.x }.isMonotonicBetween(start.x, end.x, 1e-6f)
    map { it.y }.isMonotonicBetween(start.y, end.y, 1e-6f)
}

/**
 * Verifies that the MotionEvents in this list are equidistant from each other in time between
 * [t0] and [t1], with a duration between them that is as close to the [desiredDuration] as
 * possible, given that the sequence is splitting the total duration between [t0] and [t1].
 */
fun List<MotionEvent>.splitsDurationEquallyInto(t0: Long, t1: Long, desiredDuration: Long) {
    val totalDuration = t1 - t0
    if (totalDuration < desiredDuration) {
        assertThat(this).hasSize(1)
        assertThat(first().relativeTime - t0).isEqualTo(totalDuration)
        return
    }

    // Either `desiredDuration` divides `totalDuration` perfectly, or it doesn't.
    // If it doesn't, `desiredDuration * size` must be as close to `totalDuration` as possible.
    // Verify that `desiredDuration * size` for any other number of events will be further away
    // from `totalDuration`. If the diff with `totalDuration` is the same, the higher value gets
    // precedence.
    val actualDiff = abs(totalDuration - desiredDuration * size)
    val oneLessDiff = abs(totalDuration - desiredDuration * (size - 1))
    val oneMoreDiff = abs(totalDuration - desiredDuration * (size + 1))
    assertThat(actualDiff).isAtMost(oneLessDiff)
    assertThat(actualDiff).isLessThan(oneMoreDiff)

    // Check that the timestamps are within .5 of the unrounded splits
    forEachIndexed { i, event ->
        assertThat((event.relativeTime - t0).toFloat()).isWithin(.5f).of(
            ((i + 1) / size.toDouble() * totalDuration).toFloat()
        )
    }
}

private val MotionEvent.relativeTime get() = eventTime - downTime

/**
 * Checks if the subject is within [tolerance] of [f]. Shorthand for
 * `isWithin([tolerance]).of([f])`.
 */
fun FloatSubject.isAlmostEqualTo(f: Float, tolerance: Float = 1e-3f) {
    isWithin(tolerance).of(f)
}

/**
 * Verifies that the [Offset] is equal to the given position with some tolerance. The default
 * tolerance is 0.001.
 */
fun Offset.isAlmostEqualTo(position: Offset, tolerance: Float = 1e-3f) {
    assertThat(x).isAlmostEqualTo(position.x, tolerance)
    assertThat(y).isAlmostEqualTo(position.y, tolerance)
}

/**
 * Checks that the values are progressing in a monotonic direction between [a] and [b].
 * If [a] and [b] are equal, all values in the list should be that value too. The edges [a] and
 * [b] allow a [tolerance] for floating point imprecision, which is by default `0.001`.
 */
fun List<Float>.isMonotonicBetween(a: Float, b: Float, tolerance: Float = 1e-3f) {
    val expectedSign = sign(b - a)
    if (expectedSign == 0f) {
        forEach { assertThat(it).isAlmostEqualTo(a, tolerance) }
    } else {
        forEach { assertThat(it).isAlmostBetween(a, b, tolerance) }
        zipWithNext { curr, next -> sign(next - curr) }.forEach {
            if (it != 0f) assertThat(it).isEqualTo(expectedSign)
        }
    }
}

fun List<Float>.assertSame(tolerance: Float = 0f) {
    if (size <= 1) {
        return
    }
    assertThat(minOrNull()!!).isWithin(2 * tolerance).of(maxOrNull()!!)
}

/**
 * Checks that the float value is between [a] and [b], allowing a [tolerance] on either side.
 * The order of [a] and [b] doesn't matter, the float value must be _between_ them. The default
 * tolerance is `0.001`.
 */
fun FloatSubject.isAlmostBetween(a: Float, b: Float, tolerance: Float = 1e-3f) {
    if (a < b) {
        isAtLeast(a - tolerance)
        isAtMost(b + tolerance)
    } else {
        isAtLeast(b - tolerance)
        isAtMost(a + tolerance)
    }
}

fun <E : Comparable<E>> List<E>.assertIncreasing() {
    assertThat(this).isInOrder(Ordering.natural<E>())
}

fun <E : Comparable<E>> List<E>.assertDecreasing() {
    assertThat(this).isInOrder(Ordering.natural<E>().reverse<E>())
}
