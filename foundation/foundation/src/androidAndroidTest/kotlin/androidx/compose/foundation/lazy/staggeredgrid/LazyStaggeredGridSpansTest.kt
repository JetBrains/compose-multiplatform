/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lazy.staggeredgrid

import kotlin.test.assertEquals
import org.junit.Test

class LazyStaggeredGridSpansTest {
    private val spans = LazyStaggeredGridSpans()

    @Test
    fun emptySpan_unset() {
        assertEquals(LazyStaggeredGridSpans.Unset, spans.getSpan(0))
    }

    @Test
    fun setSpan() {
        spans.setSpan(0, 42)
        spans.setSpan(1, 0)

        assertEquals(42, spans.getSpan(0))
        assertEquals(0, spans.getSpan(1))
    }

    @Test
    fun setSpan_beyondBound() {
        val bound = spans.upperBound()
        spans.setSpan(bound - 1, 42)
        spans.setSpan(bound, 42)

        assertEquals(42, spans.getSpan(bound - 1))
        assertEquals(42, spans.getSpan(bound))
    }

    @Test
    fun setSpan_largeNumber() {
        spans.setSpan(Int.MAX_VALUE / 2, 42)

        assertEquals(42, spans.getSpan(Int.MAX_VALUE / 2))
    }

    @Test
    fun setSpan_decreaseBound() {
        spans.setSpan(Int.MAX_VALUE / 2, 42)
        spans.setSpan(0, 42)

        assertEquals(-1, spans.getSpan(Int.MAX_VALUE / 2))
        assertEquals(42, spans.getSpan(0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun setSpan_negative() {
        spans.setSpan(-1, 0)
    }
}