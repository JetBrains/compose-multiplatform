/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.ui.geometry

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RectTest {

    companion object {
        private const val DELTA = 0.01f
    }

    @Test
    fun `rect accessors`() {
        val r = Rect(1.0f, 3.0f, 5.0f, 7.0f)
        assertEquals(1.0f, r.left, DELTA)
        assertEquals(3.0f, r.top, DELTA)
        assertEquals(5.0f, r.right, DELTA)
        assertEquals(7.0f, r.bottom, DELTA)
    }

    @Test
    fun `rect created by width and height`() {
        val r = Rect(Offset(1.0f, 3.0f), Size(5.0f, 7.0f))
        assertEquals(1.0f, r.left, DELTA)
        assertEquals(3.0f, r.top, DELTA)
        assertEquals(6.0f, r.right, DELTA)
        assertEquals(10.0f, r.bottom, DELTA)
    }

    @Test
    fun `rect intersection`() {
        val r1 = Rect(0.0f, 0.0f, 100.0f, 100.0f)
        val r2 = Rect(50.0f, 50.0f, 200.0f, 200.0f)
        val r3 = r1.intersect(r2)
        assertEquals(50.0f, r3.left, DELTA)
        assertEquals(50.0f, r3.top, DELTA)
        assertEquals(100.0f, r3.right, DELTA)
        assertEquals(100.0f, r3.bottom, DELTA)
        val r4 = r2.intersect(r1)
        assertEquals(r3, r4)
    }
}