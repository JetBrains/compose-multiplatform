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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TransformGestureDetectorTest(val panZoomLock: Boolean) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters() = arrayOf(false, true)
    }

    private var centroid = Offset.Zero
    private var panned = false
    private var panAmount = Offset.Zero
    private var rotated = false
    private var rotateAmount = 0f
    private var zoomed = false
    private var zoomAmount = 1f

    private val util = SuspendingGestureTestUtil {
        detectTransformGestures(panZoomLock = panZoomLock) { c, pan, gestureZoom, gestureAngle ->
            centroid = c
            if (gestureAngle != 0f) {
                rotated = true
                rotateAmount += gestureAngle
            }
            if (gestureZoom != 1f) {
                zoomed = true
                zoomAmount *= gestureZoom
            }
            if (pan != Offset.Zero) {
                panned = true
                panAmount += pan
            }
        }
    }

    @Before
    fun setup() {
        panned = false
        panAmount = Offset.Zero
        rotated = false
        rotateAmount = 0f
        zoomed = false
        zoomAmount = 1f
    }

    /**
     * Single finger pan.
     */
    @Test
    fun singleFingerPan() = util.executeInComposition {
        val down = down(5f, 5f)
        assertFalse(down.isConsumed)

        assertFalse(panned)

        val move1 = down.moveBy(Offset(12.7f, 12.7f))
        assertFalse(move1.isConsumed)

        assertFalse(panned)

        val move2 = move1.moveBy(Offset(0.1f, 0.1f))
        assertTrue(move2.isConsumed)

        assertEquals(17.7f, centroid.x, 0.1f)
        assertEquals(17.7f, centroid.y, 0.1f)
        assertTrue(panned)
        assertFalse(zoomed)
        assertFalse(rotated)

        assertTrue(panAmount.getDistance() < 1f)

        panAmount = Offset.Zero
        val move3 = move2.moveBy(Offset(1f, 0f))
        assertTrue(move3.isConsumed)

        assertEquals(Offset(1f, 0f), panAmount)

        move3.up().also { assertFalse(it.isConsumed) }

        assertFalse(rotated)
        assertFalse(zoomed)
    }

    /**
     * Multi-finger pan
     */
    @Test
    fun multiFingerPanZoom() = util.executeInComposition {
        val downA = down(5f, 5f)
        assertFalse(downA.isConsumed)

        val downB = down(25f, 25f)
        assertFalse(downB.isConsumed)

        assertFalse(panned)

        val moveA1 = downA.moveBy(Offset(12.8f, 12.8f))
        assertFalse(moveA1.isConsumed)

        val moveB1 = downB.moveBy(Offset(12.8f, 12.8f))
        // Now we've averaged enough movement
        assertTrue(moveB1.isConsumed)

        assertEquals((5f + 25f + 12.8f) / 2f, centroid.x, 0.1f)
        assertEquals((5f + 25f + 12.8f) / 2f, centroid.y, 0.1f)
        assertTrue(panned)
        assertTrue(zoomed)
        assertFalse(rotated)

        assertEquals(6.4f, panAmount.x, 0.1f)
        assertEquals(6.4f, panAmount.y, 0.1f)

        moveA1.up()
        moveB1.up()
    }

    /**
     * 2-pointer zoom
     */
    @Test
    fun zoom2Pointer() = util.executeInComposition {
        val downA = down(5f, 5f)
        assertFalse(downA.isConsumed)

        val downB = down(25f, 5f)
        assertFalse(downB.isConsumed)

        val moveB1 = downB.moveBy(Offset(35.95f, 0f))
        assertFalse(moveB1.isConsumed)

        val moveB2 = moveB1.moveBy(Offset(0.1f, 0f))
        assertTrue(moveB2.isConsumed)

        assertTrue(panned)
        assertTrue(zoomed)
        assertFalse(rotated)

        // both should be small movements
        assertTrue(panAmount.getDistance() < 1f)
        assertTrue(zoomAmount in 1f..1.1f)

        zoomAmount = 1f
        panAmount = Offset.Zero

        val moveA1 = downA.moveBy(Offset(-1f, 0f))
        assertTrue(moveA1.isConsumed)

        val moveB3 = moveB2.moveBy(Offset(1f, 0f))
        assertTrue(moveB3.isConsumed)

        assertEquals(0f, panAmount.x, 0.01f)
        assertEquals(0f, panAmount.y, 0.01f)

        assertEquals(48f / 46f, zoomAmount, 0.01f)

        moveA1.up()
        moveB3.up()
    }

    /**
     * 4-pointer zoom
     */
    @Test
    fun zoom4Pointer() = util.executeInComposition {
        val downA = down(0f, 50f)
        // just get past the touch slop
        val slop1 = downA.moveBy(Offset(-1000f, 0f))
        val slop2 = slop1.moveBy(Offset(1000f, 0f))

        panned = false
        panAmount = Offset.Zero

        val downB = down(100f, 50f)
        val downC = down(50f, 0f)
        val downD = down(50f, 100f)

        val moveA = slop2.moveBy(Offset(-50f, 0f))
        val moveB = downB.moveBy(Offset(50f, 0f))

        assertTrue(zoomed)
        assertTrue(panned)

        assertEquals(0f, panAmount.x, 0.1f)
        assertEquals(0f, panAmount.y, 0.1f)
        assertEquals(1.5f, zoomAmount, 0.1f)

        val moveC = downC.moveBy(Offset(0f, -50f))
        val moveD = downD.moveBy(Offset(0f, 50f))

        assertEquals(0f, panAmount.x, 0.1f)
        assertEquals(0f, panAmount.y, 0.1f)
        assertEquals(2f, zoomAmount, 0.1f)

        moveA.up()
        moveB.up()
        moveC.up()
        moveD.up()
    }

    /**
     * 2 pointer rotation.
     */
    @Test
    fun rotation2Pointer() = util.executeInComposition {
        val downA = down(0f, 50f)
        val downB = down(100f, 50f)
        val moveA = downA.moveBy(Offset(50f, -50f))
        val moveB = downB.moveBy(Offset(-50f, 50f))

        // assume some of the above was touch slop
        assertTrue(rotated)
        rotateAmount = 0f
        rotated = false
        zoomAmount = 1f
        panAmount = Offset.Zero

        // now do the real rotation:
        val moveA2 = moveA.moveBy(Offset(-50f, 50f))
        val moveB2 = moveB.moveBy(Offset(50f, -50f))

        moveA2.up()
        moveB2.up()

        assertTrue(rotated)
        assertEquals(-90f, rotateAmount, 0.01f)
        assertEquals(0f, panAmount.x, 0.1f)
        assertEquals(0f, panAmount.y, 0.1f)
        assertEquals(1f, zoomAmount, 0.1f)
    }

    /**
     * 2 pointer rotation, with early panning.
     */
    @Test
    fun rotation2PointerLock() = util.executeInComposition {
        val downA = down(0f, 50f)
        // just get past the touch slop with panning
        val slop1 = downA.moveBy(Offset(-1000f, 0f))
        val slop2 = slop1.moveBy(Offset(1000f, 0f))

        val downB = down(100f, 50f)

        // now do the rotation:
        val moveA2 = slop2.moveBy(Offset(50f, -50f))
        val moveB2 = downB.moveBy(Offset(-50f, 50f))

        moveA2.up()
        moveB2.up()

        if (panZoomLock) {
            assertFalse(rotated)
        } else {
            assertTrue(rotated)
            assertEquals(90f, rotateAmount, 0.01f)
        }
        assertEquals(0f, panAmount.x, 0.1f)
        assertEquals(0f, panAmount.y, 0.1f)
        assertEquals(1f, zoomAmount, 0.1f)
    }

    /**
     * Adding or removing a pointer won't change the current values
     */
    @Test
    fun noChangeOnPointerDownUp() = util.executeInComposition {
        val downA = down(0f, 50f)
        val downB = down(100f, 50f)
        val moveA = downA.moveBy(Offset(50f, -50f))
        val moveB = downB.moveBy(Offset(-50f, 50f))

        // now we've gotten past the touch slop
        rotated = false
        panned = false
        zoomed = false

        val downC = down(0f, 50f)

        assertFalse(rotated)
        assertFalse(panned)
        assertFalse(zoomed)

        val downD = down(100f, 50f)
        assertFalse(rotated)
        assertFalse(panned)
        assertFalse(zoomed)

        moveA.up()
        moveB.up()
        downC.up()
        downD.up()

        assertFalse(rotated)
        assertFalse(panned)
        assertFalse(zoomed)
    }

    /**
     * Consuming position during touch slop will cancel the current gesture.
     */
    @Test
    fun touchSlopCancel() = util.executeInComposition {
        down(5f, 5f)
            .moveBy(Offset(50f, 0f)) { consume() }
            .up()

        assertFalse(panned)
        assertFalse(zoomed)
        assertFalse(rotated)
    }

    /**
     * Consuming position after touch slop will cancel the current gesture.
     */
    @Test
    fun afterTouchSlopCancel() = util.executeInComposition {
        down(5f, 5f)
            .moveBy(Offset(50f, 0f))
            .moveBy(Offset(50f, 0f)) { consume() }
            .up()

        assertTrue(panned)
        assertFalse(zoomed)
        assertFalse(rotated)
        assertEquals(50f, panAmount.x, 0.1f)
    }
}