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

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DragGestureDetectorTest(dragType: GestureType) {
    enum class GestureType {
        VerticalDrag,
        HorizontalDrag,
        AwaitVerticalDragOrCancel,
        AwaitHorizontalDragOrCancel,
        AwaitDragOrCancel,
        DragWithVertical,
        DragWithHorizontal,
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters() = GestureType.values()
    }

    private var dragDistance = 0f
    private var dragged = false
    private var gestureEnded = false
    private var gestureStarted = false
    private var gestureCanceled = false
    private var consumePositiveOnly = false
    private var sloppyDetector = false
    private var startOrder = -1
    private var endOrder = -1
    private var cancelOrder = -1
    private var dragOrder = -1

    private val DragTouchSlopUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        var count = 0
        detectDragGestures(
            onDragStart = {
                gestureStarted = true
                startOrder = count++
            },
            onDragEnd = {
                gestureEnded = true
                endOrder = count++
            },
            onDragCancel = {
                gestureCanceled = true
                cancelOrder = count++
            }
        ) { change, dragAmount ->
            val positionChange = change.positionChange()
            dragOrder = count++
            if (positionChange.x > 0f || positionChange.y > 0f || !consumePositiveOnly) {
                change.consume()
                dragged = true
                dragDistance += dragAmount.getDistance()
            }
        }
    }

    private val VerticalTouchSlopUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        var count = 0
        detectVerticalDragGestures(
            onDragStart = {
                gestureStarted = true
                startOrder = count++
            },
            onDragEnd = {
                gestureEnded = true
                endOrder = count++
            },
            onDragCancel = {
                gestureCanceled = true
                cancelOrder = count++
            }
        ) { change, dragAmount ->
            dragOrder = count++
            if (change.positionChange().y > 0f || !consumePositiveOnly) {
                dragged = true
                dragDistance += dragAmount
            }
        }
    }

    private val HorizontalTouchSlopUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        var count = 0
        detectHorizontalDragGestures(
            onDragStart = {
                gestureStarted = true
                startOrder = count++
            },
            onDragEnd = {
                gestureEnded = true
                endOrder = count++
            },
            onDragCancel = {
                gestureCanceled = true
                cancelOrder = count++
            }
        ) { change, dragAmount ->
            dragOrder = count++
            if (change.positionChange().x > 0f || !consumePositiveOnly) {
                dragged = true
                dragDistance += dragAmount
            }
        }
    }

    private val AwaitVerticalDragUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        forEachGesture {
            awaitPointerEventScope {
                val down = awaitFirstDown()
                val slopChange = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                    if (change.positionChange().y > 0f || !consumePositiveOnly) {
                        dragged = true
                        dragDistance = overSlop
                        change.consume()
                    }
                }
                if (slopChange != null || sloppyDetector) {
                    gestureStarted = true
                    var pointer = if (sloppyDetector) down.id else slopChange!!.id
                    do {
                        val change = awaitVerticalDragOrCancellation(pointer)
                        if (change == null) {
                            gestureCanceled = true
                        } else {
                            dragDistance += change.positionChange().y
                            change.consume()
                            if (change.changedToUpIgnoreConsumed()) {
                                gestureEnded = true
                            }
                            pointer = change.id
                        }
                    } while (!gestureEnded && !gestureCanceled)
                }
            }
        }
    }

    private val AwaitHorizontalDragUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        forEachGesture {
            awaitPointerEventScope {
                val down = awaitFirstDown()
                val slopChange =
                    awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                        if (change.positionChange().x > 0f || !consumePositiveOnly) {
                            dragged = true
                            dragDistance = overSlop
                            change.consume()
                        }
                    }
                if (slopChange != null || sloppyDetector) {
                    gestureStarted = true
                    var pointer = if (sloppyDetector) down.id else slopChange!!.id
                    do {
                        val change = awaitHorizontalDragOrCancellation(pointer)
                        if (change == null) {
                            gestureCanceled = true
                        } else {
                            dragDistance += change.positionChange().x
                            change.consume()
                            if (change.changedToUpIgnoreConsumed()) {
                                gestureEnded = true
                            }
                            pointer = change.id
                        }
                    } while (!gestureEnded && !gestureCanceled)
                }
            }
        }
    }

    private val AwaitDragUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        forEachGesture {
            awaitPointerEventScope {
                val down = awaitFirstDown()
                val slopChange = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
                    val positionChange = change.positionChange()
                    if (positionChange.x > 0f || positionChange.y > 0f || !consumePositiveOnly) {
                        dragged = true
                        dragDistance = overSlop.getDistance()
                        change.consume()
                    }
                }
                if (slopChange != null || sloppyDetector) {
                    gestureStarted = true
                    var pointer = if (sloppyDetector) down.id else slopChange!!.id
                    do {
                        val change = awaitDragOrCancellation(pointer)
                        if (change == null) {
                            gestureCanceled = true
                        } else {
                            dragDistance += change.positionChange().getDistance()
                            change.consume()
                            if (change.changedToUpIgnoreConsumed()) {
                                gestureEnded = true
                            }
                            pointer = change.id
                        }
                    } while (!gestureEnded && !gestureCanceled)
                }
            }
        }
    }

    private val util = when (dragType) {
        GestureType.VerticalDrag -> VerticalTouchSlopUtil
        GestureType.HorizontalDrag -> HorizontalTouchSlopUtil
        GestureType.AwaitVerticalDragOrCancel -> AwaitVerticalDragUtil
        GestureType.AwaitHorizontalDragOrCancel -> AwaitHorizontalDragUtil
        GestureType.AwaitDragOrCancel -> AwaitDragUtil
        GestureType.DragWithVertical -> DragTouchSlopUtil
        GestureType.DragWithHorizontal -> DragTouchSlopUtil
    }

    private val dragMotion = when (dragType) {
        GestureType.VerticalDrag,
        GestureType.AwaitVerticalDragOrCancel,
        GestureType.DragWithVertical -> Offset(0f, 18f)
        else -> Offset(18f, 0f)
    }

    private val crossDragMotion = when (dragType) {
        GestureType.VerticalDrag,
        GestureType.AwaitVerticalDragOrCancel,
        GestureType.DragWithVertical -> Offset(18f, 0f)
        else -> Offset(0f, 18f)
    }

    private val twoAxisDrag = when (dragType) {
        GestureType.DragWithVertical,
        GestureType.DragWithHorizontal,
        GestureType.AwaitDragOrCancel -> true
        else -> false
    }

    private val supportsSloppyGesture = when (dragType) {
        GestureType.AwaitVerticalDragOrCancel,
        GestureType.AwaitHorizontalDragOrCancel,
        GestureType.AwaitDragOrCancel -> true
        else -> false
    }

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
        assertTrue(gestureStarted)
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
            assertFalse(gestureStarted)
            assertFalse(dragged)
            assertFalse(gestureEnded)

            // now try a normal drag to ensure that it is still working.
            down().moveBy(dragMotion).up()
            assertTrue(gestureStarted)
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

        assertFalse(gestureStarted)
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

        assertTrue(gestureStarted)
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
        down().moveBy(dragMotion) { consume() }.moveBy(dragMotion).up()
        assertFalse(gestureStarted)
        assertFalse(dragged)
        assertFalse(gestureEnded)
        assertFalse(gestureCanceled) // only canceled if the touch slop was crossed first
    }

    /**
     * Cancel drag after touch slop
     */
    @Test
    fun cancelDragAfterSlop() = util.executeInComposition {
        down().moveBy(dragMotion).moveBy(dragMotion) { consume() }.up()
        assertTrue(gestureStarted)
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
        if (!twoAxisDrag) {
            down().moveBy(
                (dragMotion * 2f) + crossDragMotion,
                final = {
                    // This should have priority because it has moved more than the other direction.
                    assertTrue(isConsumed)
                }
            )
                .up()
            assertTrue(gestureStarted)
            assertTrue(dragged)
            assertTrue(gestureEnded)
            assertFalse(gestureCanceled)
            assertEquals(18f, dragDistance)
        }
    }

    /**
     * When a drag is not consumed, it should lead to the touch slop being reset. This is
     * important when you drag your finger to
     */
    @Test
    fun dragBackAndForth() = util.executeInComposition {
        if (supportsSloppyGesture) {
            try {
                consumePositiveOnly = true

                val back = down().moveBy(-dragMotion)

                assertFalse(gestureStarted)
                assertFalse(dragged)
                back.moveBy(dragMotion).up()

                assertTrue(gestureStarted)
                assertTrue(dragged)
            } finally {
                consumePositiveOnly = false
            }
        }
    }

    /**
     * When gesture detectors use the wrong pointer for the drag, it should just not
     * detect the touch.
     */
    @Test
    fun pointerUpTooQuickly() = util.executeInComposition {
        if (supportsSloppyGesture) {
            try {
                sloppyDetector = true

                val finger1 = down()
                val finger2 = down()
                finger1.up()
                finger2.moveBy(dragMotion).up()

                // The sloppy detector doesn't know to look at finger2
                assertTrue(gestureCanceled)
            } finally {
                sloppyDetector = false
            }
        }
    }

    @Test
    fun dragGestureCallbackOrder_normalFinish() = util.executeInComposition {
        if (!supportsSloppyGesture) {
            val progress = down().moveBy(Offset(50f, 50f))
            assertTrue(startOrder < dragOrder)
            progress.up()
            assertTrue(startOrder < dragOrder)
            assertTrue(dragOrder < endOrder)
            assertTrue(cancelOrder == -1)
        }
    }

    @Test
    fun dragGestureCallbackOrder_cancel() = util.executeInComposition {
        if (!supportsSloppyGesture) {
            down().moveBy(dragMotion).moveBy(dragMotion) { consume() }
            assertTrue(startOrder < dragOrder)
            assertTrue(dragOrder < cancelOrder)
            assertTrue(endOrder == -1)
        }
    }

    // An error in the pointer input stream should stop the gesture without error.
    @Test
    fun interruptedBeforeDrag() = util.executeInComposition {
        down()
        clearPointerStream()
        // The next stream doesn't have the existing pointer, so we lose the dragging
        down().up()
        assertFalse(gestureStarted)
    }

    // An error in the pointer input stream should stop the gesture without error.
    @Test
    fun interruptedBeforeTouchSlop() = util.executeInComposition {
        down().moveBy(dragMotion / 2f)
        clearPointerStream()
        // The next stream doesn't have the existing pointer, so we lose the dragging
        down().up()
        assertFalse(gestureStarted)
    }

    // An error in the pointer input stream should end in a drag cancellation.
    @Test
    fun interruptedAfterTouchSlop() = util.executeInComposition {
        down().moveBy(dragMotion * 2f)
        clearPointerStream()
        // The next stream doesn't have the existing pointer, so we lose the dragging
        down().up()
        assertTrue(gestureCanceled)
    }
}

@RunWith(JUnit4::class)
class DragGestureOrderTest {
    var startCount = -1
    var stopCount = -1
    var dragCount = -1
    private val DragOrderUtil = SuspendingGestureTestUtil(width = 100, height = 100) {
        var counter = 0
        detectDragGestures(
            onDragStart = { startCount = counter++ },
            onDragEnd = { stopCount = counter++ }
        ) { _, _ ->
            dragCount = counter++
        }
    }

    @Test
    fun dragGestureCallbackOrder() = DragOrderUtil.executeInComposition {
        val progress = down().moveBy(Offset(50f, 50f))
        assertTrue(startCount < dragCount)
        progress.up()
        assertTrue(startCount < dragCount)
        assertTrue(dragCount < stopCount)
    }
}