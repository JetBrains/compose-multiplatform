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

package androidx.compose.ui.unit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PxTest {

    @Test
    fun compareDimension2() {
        assertTrue(PxSquared(0f) < PxSquared(Float.MIN_VALUE))
        assertTrue(PxSquared(1f) < PxSquared(3f))
        assertTrue(PxSquared(1f) == PxSquared(1f))
        assertTrue(PxSquared(1f) > PxSquared(0f))
    }

    @Test
    fun compareDimension3() {
        assertTrue(PxCubed(0f) < PxCubed(Float.MIN_VALUE))
        assertTrue(PxCubed(1f) < PxCubed(3f))
        assertTrue(PxCubed(1f) == PxCubed(1f))
        assertTrue(PxCubed(1f) > PxCubed(0f))
    }

    @Test
    fun compareDimensionInverse() {
        assertTrue(PxInverse(0f) < PxInverse(Float.MIN_VALUE))
        assertTrue(PxInverse(1f) < PxInverse(3f))
        assertTrue(PxInverse(1f) == PxInverse(1f))
        assertTrue(PxInverse(1f) > PxInverse(0f))
    }

    @Test
    fun positionDistance() {
        val position = Offset(3f, 4f)
        assertEquals(5f, position.getDistance())
    }

    @Test
    fun lerpPosition() {
        val a = Offset(3f, 10f)
        val b = Offset(5f, 8f)
        assertEquals(Offset(4f, 9f), lerp(a, b, 0.5f))
        assertEquals(Offset(3f, 10f), lerp(a, b, 0f))
        assertEquals(Offset(5f, 8f), lerp(a, b, 1f))
    }

    @Test
    fun positionMinus() {
        val a = Offset(3f, 10f)
        val b = Offset(5f, 8f)
        assertEquals(Offset(-2f, 2f), a - b)
        assertEquals(Offset(2f, -2f), b - a)
    }

    @Test
    fun positionPlus() {
        val a = Offset(3f, 10f)
        val b = Offset(5f, 8f)
        assertEquals(Offset(8f, 18f), a + b)
        assertEquals(Offset(8f, 18f), b + a)
    }

    @Test
    fun pxPositionMinusIntPxPosition() {
        val a = Offset(3f, 10f)
        val b = IntOffset(5, 8)
        assertEquals(Offset(-2f, 2f), a - b)
    }

    @Test
    fun pxPositionPlusIntPxPosition() {
        val a = Offset(3f, 10f)
        val b = IntOffset(5, 8)
        assertEquals(Offset(8f, 18f), a + b)
    }
}