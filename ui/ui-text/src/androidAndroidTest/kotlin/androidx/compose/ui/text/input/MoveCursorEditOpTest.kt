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

package androidx.compose.ui.text.input

import androidx.compose.ui.text.TextRange
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
class MoveCursorEditOpTest {
    private val CH1 = "\uD83D\uDE00" // U+1F600
    private val CH2 = "\uD83D\uDE01" // U+1F601
    private val CH3 = "\uD83D\uDE02" // U+1F602
    private val CH4 = "\uD83D\uDE03" // U+1F603
    private val CH5 = "\uD83D\uDE04" // U+1F604

    // U+1F468 U+200D U+1F469 U+200D U+1F467 U+200D U+1F466
    private val FAMILY = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66"

    @Test
    fun test_left() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        MoveCursorEditOp(-1).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_left_multiple() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        MoveCursorEditOp(-2).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(1, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_left_from_offset0() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        MoveCursorEditOp(-1).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_right() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        MoveCursorEditOp(1).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(4, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_right_multiple() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        MoveCursorEditOp(2).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(5, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_right_from_offset_length() {
        val eb = EditingBuffer("ABCDE", TextRange(5))

        MoveCursorEditOp(1).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(5, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_left_surrogate_pair() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        MoveCursorEditOp(-1).process(eb)

        assertEquals("$CH1$CH2$CH3$CH4$CH5", eb.toString())
        assertEquals(4, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_left_multiple_surrogate_pair() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        MoveCursorEditOp(-2).process(eb)

        assertEquals("$CH1$CH2$CH3$CH4$CH5", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_right_surrogate_pair() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        MoveCursorEditOp(1).process(eb)

        assertEquals("$CH1$CH2$CH3$CH4$CH5", eb.toString())
        assertEquals(8, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_right_multiple_surrogate_pair() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        MoveCursorEditOp(2).process(eb)

        assertEquals("$CH1$CH2$CH3$CH4$CH5", eb.toString())
        assertEquals(10, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun test_left_emoji() {
        val eb = EditingBuffer("$FAMILY$FAMILY", TextRange(FAMILY.length))

        MoveCursorEditOp(-1).process(eb)

        assertEquals("$FAMILY$FAMILY", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun test_right_emoji() {
        val eb = EditingBuffer("$FAMILY$FAMILY", TextRange(FAMILY.length))

        MoveCursorEditOp(1).process(eb)

        assertEquals("$FAMILY$FAMILY", eb.toString())
        assertEquals(2 * FAMILY.length, eb.cursor)
        assertFalse(eb.hasComposition())
    }
}