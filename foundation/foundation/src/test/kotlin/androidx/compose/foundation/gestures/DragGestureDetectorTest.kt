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

@file:OptIn(ExperimentalPointerInput::class)

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.input.pointer.anyPositionChangeConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.positionChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@OptIn(ExperimentalPointerInput::class)
class DragGestureDetectorTest(dragType: GestureType) {
    enum class GestureType {
        VerticalDrag,
        HorizontalDrag,
        VerticalDragLock,
        HorizontalDragLock,
        AwaitVerticalDragOrCancel,
        AwaitHorizontalDragOrCancel,
        DragWithVertical,
        DragWithHorizontal
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters() = GestureType.values()
    }

    private var dragDistance = 0f
    private var dragged = false
    private var gestureEnded = false
    private var gestureCanceled = false

    private val DragTouchSlopUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        detectDragGestures(
            onDragEnd = { gestureEnded = true },
            onDragCancel = { gestureCanceled = true }
        ) { change, dragAmount ->
            change.consumeAllChanges()
            dragged = true
            dragDistance += dragAmount.getDistance()
        }
    }

    private val VerticalTouchSlopUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        detectVerticalDragGestures(
            onDragEnd = { gestureEnded = true },
            onDragCancel = { gestureCanceled = true }
        ) { change, dragAmount ->
            change.consumeAllChanges()
            dragged = true
            dragDistance += dragAmount
        }
    }

    private val HorizontalTouchSlopUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        detectHorizontalDragGestures(
            onDragEnd = { gestureEnded = true },
            onDragCancel = { gestureCanceled = true }
        ) { change, dragAmount ->
            change.consumeAllChanges()
            dragged = true
            dragDistance += dragAmount
        }
    }

    private val VerticalDragLockUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        detectVerticalDragGestures(
            orientationLock = true,
            onDragEnd = { gestureEnded = true },
            onDragCancel = { gestureCanceled = true }
        ) { change, dragAmount ->
            change.consumeAllChanges()
            dragged = true
            dragDistance += dragAmount
        }
    }

    private val HorizontalDragLockUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        detectHorizontalDragGestures(
            orientationLock = true,
            onDragEnd = { gestureEnded = true },
            onDragCancel = { gestureCanceled = true }
        ) { change, dragAmount ->
            change.consumeAllChanges()
            dragged = true
            dragDistance += dragAmount
        }
    }

    private val AwaitVerticalDragUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        handlePointerInput {
            var pointer = awaitVerticalTouchSlop { change, overSlop ->
                dragged = true
                dragDistance = overSlop
                change.consumeAllChanges()
            }
            do {
                val change = awaitVerticalDragOrCancel(pointer)
                if (change == null) {
                    gestureCanceled = true
                } else {
                    dragDistance += change.positionChange().y
                    change.consumeAllChanges()
                    if (change.changedToUpIgnoreConsumed()) {
                        gestureEnded = true
                    }
                    pointer = change.id
                }
            } while (!gestureEnded && !gestureCanceled)
        }
    }

    private val AwaitHorizontalDragUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        handlePointerInput {
            var pointer = awaitHorizontalTouchSlop { change, overSlop ->
                dragged = true
                dragDistance = overSlop
                change.consumeAllChanges()
            }
            do {
                val change = awaitHorizontalDragOrCancel(pointer)
                if (change == null) {
                    gestureCanceled = true
                } else {
                    dragDistance += change.positionChange().x
                    change.consumeAllChanges()
                    if (change.changedToUpIgnoreConsumed()) {
                        gestureEnded = true
                    }
                    pointer = change.id
                }
            } while (!gestureEnded && !gestureCanceled)
        }
    }

    private val util = when (dragType) {
        GestureType.VerticalDrag -> VerticalTouchSlopUtil
        GestureType.HorizontalDrag -> HorizontalTouchSlopUtil
        GestureType.VerticalDragLock -> VerticalDragLockUtil
        GestureType.HorizontalDragLock -> HorizontalDragLockUtil
        GestureType.AwaitVerticalDragOrCancel -> AwaitVerticalDragUtil
        GestureType.AwaitHorizontalDragOrCancel -> AwaitHorizontalDragUtil
        GestureType.DragWithVertical -> DragTouchSlopUtil
        GestureType.DragWithHorizontal -> DragTouchSlopUtil
    }

    private val dragMotion = when (dragType) {
        GestureType.VerticalDrag,
        GestureType.VerticalDragLock,
        GestureType.AwaitVerticalDragOrCancel,
        GestureType.DragWithVertical -> Offset(0f, 18f)
        else -> Offset(18f, 0f)
    }

    private val crossDragMotion = when (dragType) {
        GestureType.VerticalDrag,
        GestureType.VerticalDragLock,
        GestureType.AwaitVerticalDragOrCancel,
        GestureType.DragWithVertical -> Offset(18f, 0f)
        else -> Offset(0f, 18f)
    }

    private val isLock = when (dragType) {
        GestureType.VerticalDragLock, GestureType.HorizontalDragLock -> true
        else -> false
    }

    private val twoAxisDrag = dragType == GestureType.DragWithVertical ||
        dragType == GestureType.DragWithHorizontal

    @Before
    fun setup() {
        dragDistance = 0f
        dragged = false
        gestureEnded = false
    }

    /**
     * A normal drag, just to ensure that the drag worked.
     */
    @Test
    fun normalDrag() = util.executeInComposition {
        val move = down().moveBy(dragMotion)
        assertTrue(dragged)
        assertEquals(0f, dragDistance)
        val move2 = move.moveBy(dragMotion)
        assertEquals(18f, dragDistance)
        assertFalse(gestureEnded)
        move2.up()
        assertTrue(gestureEnded)
    }

    /**
     * A drag in the opposite direction doesn't cause a drag event.
     */
    @Test
    fun crossDrag() = util.executeInComposition {
        if (!twoAxisDrag) {
            down().moveBy(crossDragMotion).up()
            assertFalse(dragged)
            assertFalse(gestureEnded)

            // now try a normal drag to ensure that it is still working.
            down().moveBy(dragMotion).up()
            assertTrue(dragged)
            assertEquals(0f, dragDistance)
            assertTrue(gestureEnded)
        }
    }

    /**
     * Use two fingers and lift the finger before the touch slop is reached.
     */
    @Test
    fun twoFingerDrag_upBeforeSlop() = util.executeInComposition {
        val finger1 = down()
        val finger2 = down()

        // second finger shouldn't cause a drag. It should follow finger1
        val moveFinger2 = finger2.moveBy(dragMotion)

        assertFalse(dragged)

        // now it should move to finger 2
        finger1.up()

        moveFinger2.moveBy(dragMotion).moveBy(dragMotion).up()

        assertTrue(dragged)
        assertEquals(18f, dragDistance)
        assertTrue(gestureEnded)
    }

    /**
     * Use two fingers and lift the finger after the touch slop is reached.
     */
    @Test
    fun twoFingerDrag_upAfterSlop() = util.executeInComposition {
        val finger1 = down()
        val finger2 = down()

        finger1.moveBy(dragMotion).up()

        assertTrue(dragged)
        assertEquals(0f, dragDistance)
        assertFalse(gestureEnded)

        finger2.moveBy(dragMotion).up()

        assertEquals(18f, dragDistance)
        assertTrue(gestureEnded)
    }

    /**
     * Cancel drag during touch slop
     */
    @Test
    fun cancelDragDuringSlop() = util.executeInComposition {
        down().moveBy(dragMotion) { consumeAllChanges() }.moveBy(dragMotion).up()
        assertFalse(dragged)
        assertFalse(gestureEnded)
        assertFalse(gestureCanceled) // only canceled if the touch slop was crossed first
    }

    /**
     * Cancel drag after touch slop
     */
    @Test
    fun cancelDragAfterSlop() = util.executeInComposition {
        down().moveBy(dragMotion).moveBy(dragMotion) { consumeAllChanges() }.up()
        assertTrue(dragged)
        assertFalse(gestureEnded)
        assertTrue(gestureCanceled)
        assertEquals(0f, dragDistance)
    }

    /**
     * When this drag direction is more than the other drag direction, it should have priority
     * in locking the orientation.
     */
    @Test
    fun dragLockedWithPriority() = util.executeInComposition {
        if (isLock) {
            down().moveBy(
                (dragMotion * 2f) + crossDragMotion,
                final = {
                    // This should have priority because it has moved more than the other direction.
                    assertTrue(anyPositionChangeConsumed())
                }
            )
                .up()
            assertTrue(dragged)
            assertTrue(gestureEnded)
            assertFalse(gestureCanceled)
            assertEquals(18f, dragDistance)
        }
    }

    /**
     * When this drag direction is less than the other drag direction, it should wait
     * before locking the orientation.
     */
    @Test
    fun dragLockedWithLowPriority() = util.executeInComposition {
        if (isLock) {
            down().moveBy(
                dragMotion + (crossDragMotion * 2f),
                final = {
                    // The other direction should have priority, but it should consume the
                    // in-direction position change
                    assertEquals(dragMotion, consumed.positionChange)

                    // but it shouldn't have called the callback, yet
                    assertFalse(dragged)
                }
            )
                .up()
            assertTrue(dragged)
            assertTrue(gestureEnded)
            assertFalse(gestureCanceled)
            assertEquals(0f, dragDistance)
        }
    }

    /**
     * When this drag direction is less than the other drag direction, it should wait
     * before locking the orientation. When the other direction locks, it should not drag.
     */
    @Test
    fun dragLockFailWithLowPriority() = util.executeInComposition {
        if (isLock) {
            down().moveBy(
                dragMotion + (crossDragMotion * 2f),
                final = {
                    consumeAllChanges()
                }
            )
                .up()
            assertFalse(dragged)
            assertFalse(gestureEnded)
            assertFalse(gestureCanceled)
        }
    }

    /**
     * When this drag direction is less than the other drag direction, it should wait
     * before locking the orientation. When the other direction locks, it should not drag.
     */
    @Test
    fun dragLockFailNested() = util.executeInComposition {
        if (isLock) {
            down().moveBy(
                dragMotion + (crossDragMotion * 2f),
                final = {
                    assertEquals(crossDragMotion * 2f, positionChange())
                    consumeAllChanges()
                }
            ).also {
                assertEquals(dragMotion, it.positionChange())
            }
                .up()
            assertFalse(dragged)
            assertFalse(gestureEnded)
            assertFalse(gestureCanceled)
        }
    }
}