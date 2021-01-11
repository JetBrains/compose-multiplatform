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
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.PointerCoords
import androidx.compose.ui.gesture.PointerProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PointerInteropUtilsTest {

    @Test(expected = IllegalArgumentException::class)
    fun toMotionEventScope_noPlatformEvent_throws() {
        val pointerEvent = PointerEvent(
            listOf(),
            motionEvent = null
        )
        pointerEvent.toMotionEventScope(Offset.Zero) {}
    }

    @Test
    fun toMotionEventScope_1stPointerDownEvent_motionEventIsCorrect() {
        val expected =
            MotionEvent(
                2,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(3f, 4f))
            )
        val pointerEvent = PointerEvent(
            listOf(
                down(
                    1,
                    2,
                    3f,
                    4f
                )
            ),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toMotionEventScope(Offset.Zero) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toMotionEventScope_1stPointerUpEvent_motionEventIsCorrect() {
        val expected =
            MotionEvent(
                5,
                MotionEvent.ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(3f, 4f))
            )
        val pointerEvent = PointerEvent(
            listOf(down(1, 2, 3f, 4f).up(5)),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toMotionEventScope(Offset.Zero) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toMotionEventScope_2ndPointerDownEventAs1stPointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f).moveTo(7, 3f, 4f)
        val pointer2 = down(8, 7, 10f, 11f)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(1), PointerProperties(8)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(10f, 11f))
            )
        val pointerEvent = PointerEvent(
            listOf(
                pointer1,
                pointer2
            ),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toMotionEventScope(Offset.Zero) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toMotionEventScope_2ndPointerDownEventAs2ndPointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f).moveTo(7, 3f, 4f)
        val pointer2 = down(8, 7, 10f, 11f)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                0,
                arrayOf(PointerProperties(8), PointerProperties(1)),
                arrayOf(PointerCoords(10f, 11f), PointerCoords(3f, 4f))
            )
        val pointerEvent = PointerEvent(
            listOf(
                pointer2,
                pointer1
            ),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toMotionEventScope(Offset.Zero) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toMotionEventScope_2ndPointerUpEventAs1stPointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f).moveTo(7, 3f, 4f)
        val pointer2 = down(8, 2, 10f, 11f).up(7)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_POINTER_UP,
                2,
                1,
                arrayOf(PointerProperties(1), PointerProperties(8)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(10f, 11f))
            )
        val pointerEvent = PointerEvent(
            listOf(
                pointer1,
                pointer2
            ),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toMotionEventScope(Offset.Zero) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toMotionEventScope_2ndPointerUpEventAs2ndPointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f).moveTo(7, 3f, 4f)
        val pointer2 = down(8, 2, 10f, 11f).up(7)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_POINTER_UP,
                2,
                0,
                arrayOf(PointerProperties(8), PointerProperties(1)),
                arrayOf(PointerCoords(10f, 11f), PointerCoords(3f, 4f))
            )
        val pointerEvent = PointerEvent(
            listOf(
                pointer2,
                pointer1
            ),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toMotionEventScope(Offset.Zero) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toMotionEventScope_moveEvent1Pointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f).moveTo(7, 8f, 9f)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(8f, 9f))
            )
        val pointerEvent = PointerEvent(
            listOf(pointer1),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toMotionEventScope(Offset.Zero) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toMotionEventScope_moveEvent2Pointers_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f).moveTo(7, 8f, 9f)
        val pointer2 = down(11, 12, 13f, 14f).moveTo(17, 18f, 19f)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_MOVE,
                2,
                0,
                arrayOf(PointerProperties(1), PointerProperties(11)),
                arrayOf(PointerCoords(8f, 9f), PointerCoords(18f, 19f))
            )
        val pointerEvent = PointerEvent(
            listOf(
                pointer1,
                pointer2
            ),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toMotionEventScope(Offset.Zero) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toMotionEventScope_globalOffsetsSet1Pointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f)
        val expected =
            MotionEvent(
                2,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(13f, 104f))
            ).apply { offsetLocation(-10f, -100f) }
        val pointerEvent = PointerEvent(
            listOf(pointer1),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toMotionEventScope(Offset(10f, 100f)) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toMotionEventScope_globalOffsetsSet2Pointers_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f).moveTo(7, 3f, 4f)
        val pointer2 = down(8, 7, 10f, 11f)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                0,
                arrayOf(PointerProperties(8), PointerProperties(1)),
                arrayOf(PointerCoords(110f, 1011f), PointerCoords(103f, 1004f))
            ).apply { offsetLocation(-100f, -1000f) }
        val pointerEvent = PointerEvent(
            listOf(
                pointer2,
                pointer1
            ),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toMotionEventScope(Offset(100f, 1000f)) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun toCancelMotionEventScope_noPlatformEvent_throws() {
        val pointerEvent = PointerEvent(
            listOf(),
            motionEvent = null
        )
        pointerEvent.toCancelMotionEventScope(Offset.Zero) {}
    }

    @Test
    fun toCancelMotionEventScope_1Pointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f).moveTo(7, 8f, 9f)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_CANCEL,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(8f, 9f))
            )
        val pointerEvent = PointerEvent(
            listOf(pointer1),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toCancelMotionEventScope(Offset.Zero) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toCancelMotionEventScope_2Pointers_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f).moveTo(7, 8f, 9f)
        val pointer2 = down(11, 12, 13f, 14f).moveTo(17, 18f, 19f)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_CANCEL,
                2,
                0,
                arrayOf(PointerProperties(1), PointerProperties(11)),
                arrayOf(PointerCoords(8f, 9f), PointerCoords(18f, 19f))
            )
        val pointerEvent = PointerEvent(
            listOf(
                pointer1,
                pointer2
            ),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toCancelMotionEventScope(Offset.Zero) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toCancelMotionEventScope_2PointersAltOrder_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f).moveTo(7, 8f, 9f)
        val pointer2 = down(11, 12, 13f, 14f).moveTo(7, 18f, 19f)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_CANCEL,
                2,
                0,
                arrayOf(PointerProperties(11), PointerProperties(1)),
                arrayOf(PointerCoords(18f, 19f), PointerCoords(8f, 9f))
            )
        val pointerEvent = PointerEvent(
            listOf(
                pointer2,
                pointer1
            ),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toCancelMotionEventScope(Offset.Zero) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toCancelMotionEventScope_globalOffsetsSet1Pointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f)
        val expected =
            MotionEvent(
                2,
                MotionEvent.ACTION_CANCEL,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(13f, 104f))
            ).apply { offsetLocation(-10f, -100f) }
        val pointerEvent = PointerEvent(
            listOf(pointer1),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toCancelMotionEventScope(Offset(10f, 100f)) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun toCancelMotionEventScope_globalOffsetsSet2Pointers_motionEventIsCorrect() {
        val pointer1 = down(1, 2, 3f, 4f).moveTo(7, 3f, 4f)
        val pointer2 = down(8, 7, 10f, 11f)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_CANCEL,
                2,
                0,
                arrayOf(PointerProperties(8), PointerProperties(1)),
                arrayOf(PointerCoords(110f, 1011f), PointerCoords(103f, 1004f))
            ).apply { offsetLocation(-100f, -1000f) }
        val pointerEvent = PointerEvent(
            listOf(
                pointer2,
                pointer1
            ),
            expected
        )

        lateinit var actual: MotionEvent
        pointerEvent.toCancelMotionEventScope(Offset(100f, 1000f)) {
            actual = it
        }

        assertThat(actual).isSameInstanceAs(expected)
    }

    @Test
    fun emptyCancelMotionEventScope_motionEventIsCorrect() {
        val expected =
            MotionEvent(
                76,
                MotionEvent.ACTION_CANCEL,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(0f, 0f)),
                76
            )
        lateinit var actual: MotionEvent

        emptyCancelMotionEventScope(76) {
            actual = it
        }

        assertEquals(actual, expected)
    }
}

private fun MotionEvent(
    eventTime: Long,
    action: Int,
    numPointers: Int,
    actionIndex: Int,
    pointerProperties: Array<MotionEvent.PointerProperties>,
    pointerCoords: Array<MotionEvent.PointerCoords>,
    downtime: Long = 0L
) = MotionEvent.obtain(
    downtime,
    eventTime,
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
)

private fun assertEquals(actual: MotionEvent, expected: MotionEvent) {
    assertThat(actual.downTime).isEqualTo(expected.downTime)
    assertThat(actual.eventTime).isEqualTo(expected.eventTime)
    assertThat(actual.actionMasked).isEqualTo(expected.actionMasked)
    assertThat(actual.actionIndex).isEqualTo(expected.actionIndex)
    assertThat(actual.pointerCount).isEqualTo(expected.pointerCount)

    assertEqualToolTypes(actual, expected)

    // Equal pointer properties
    assertEqualPointerProperties(actual, expected)

    // Equal pointer coords relative to local region.
    assertEqualPointerCoords(actual, expected)

    // Equal pointer coords relative to screen.
    assertEqualPointerCoords(
        actual.asOffsetToScreen(),
        expected.asOffsetToScreen()
    )
}

private fun assertEqualToolTypes(actual: MotionEvent, expected: MotionEvent) {
    repeat(expected.pointerCount) { index ->
        assertThat(actual.getToolType(index)).isEqualTo(expected.getToolType(index))
    }
}

private fun assertEqualPointerProperties(actual: MotionEvent, expected: MotionEvent) {
    val actualPointerProperties = MotionEvent.PointerProperties()
    val expectedPointerProperties = MotionEvent.PointerProperties()
    repeat(expected.pointerCount) { index ->
        actual.getPointerProperties(index, actualPointerProperties)
        expected.getPointerProperties(index, expectedPointerProperties)
        assertThat(actualPointerProperties).isEqualTo(expectedPointerProperties)
    }
}

/**
 * Asserts that 2 [MotionEvent]s' [PointerCoords] are the same.
 */
private fun assertEqualPointerCoords(actual: MotionEvent, expected: MotionEvent) {
    val actualPointerCoords = MotionEvent.PointerCoords()
    val expectedPointerCoords = MotionEvent.PointerCoords()
    repeat(expected.pointerCount) { index ->
        actual.getPointerCoords(index, actualPointerCoords)
        expected.getPointerCoords(index, expectedPointerCoords)
        assertThat(actualPointerCoords.x).isEqualTo(expectedPointerCoords.x)
        assertThat(actualPointerCoords.y).isEqualTo(expectedPointerCoords.y)
    }
}

/**
 * Creates a new [MotionEvent] that is offset to the screen instead of the [View] it was
 * dispatched to.
 */
private fun MotionEvent.asOffsetToScreen() =
    MotionEvent.obtain(this).also { motionEvent ->
        motionEvent.offsetLocation(rawX - x, rawY - y)
    }
