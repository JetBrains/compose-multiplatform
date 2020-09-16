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

package androidx.compose.ui.unit

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IntSizeTest {
    @Test
    fun constructor() {
        val size = IntSize(width = 5, height = 10)
        assertEquals(5, size.width)
        assertEquals(10, size.height)

        val size2 = IntSize(width = Int.MAX_VALUE, height = Int.MIN_VALUE)
        assertEquals(Int.MAX_VALUE, size2.width)
        assertEquals(Int.MIN_VALUE, size2.height)
    }

    @Test
    fun intSizeTimesInt() {
        assertEquals(IntSize(10, 10), IntSize(2, 2) * 5)
        assertEquals(IntSize(10, 10), 5 * IntSize(2, 2))
    }

    @Test
    fun intSizeDivInt() {
        assertEquals(IntSize(10, 10), IntSize(40, 40) / 4)
    }

    @Test
    fun sizeCenter() {
        val size = IntSize(width = 10, height = 20)
        assertEquals(IntOffset(5, 10), size.center)
    }

    @Test
    fun components() {
        val size = IntSize(width = 10, height = 20)
        val (w, h) = size
        assertEquals(10, w)
        assertEquals(20, h)
    }
}