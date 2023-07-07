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
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IntOffsetTest {
    @Test
    fun lerpPosition() {
        val a = IntOffset(3, 10)
        val b = IntOffset(5, 8)
        assertEquals(IntOffset(4, 9), lerp(a, b, 0.5f))
        assertEquals(IntOffset(3, 10), lerp(a, b, 0f))
        assertEquals(IntOffset(5, 8), lerp(a, b, 1f))
    }

    @Test
    fun positionMinus() {
        val a = IntOffset(3, 10)
        val b = IntOffset(5, 8)
        assertEquals(IntOffset(-2, 2), a - b)
        assertEquals(IntOffset(2, -2), b - a)
    }

    @Test
    fun positionPlus() {
        val a = IntOffset(3, 10)
        val b = IntOffset(5, 8)
        assertEquals(IntOffset(8, 18), a + b)
        assertEquals(IntOffset(8, 18), b + a)
    }

    @Test
    fun toOffset() {
        assertEquals(Offset(3f, 10f), IntOffset(3, 10).toOffset())
    }
}