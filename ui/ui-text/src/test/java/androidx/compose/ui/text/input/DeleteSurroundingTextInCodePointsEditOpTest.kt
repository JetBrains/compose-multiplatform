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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DeleteSurroundingTextInCodePointsEditOpTest {
    val CH1 = "\uD83D\uDE00" // U+1F600
    val CH2 = "\uD83D\uDE01" // U+1F601
    val CH3 = "\uD83D\uDE02" // U+1F602
    val CH4 = "\uD83D\uDE03" // U+1F603
    val CH5 = "\uD83D\uDE04" // U+1F604

    @Test
    fun test_delete_after() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(2))

        DeleteSurroundingTextInCodePointsEditOp(0, 1).process(eb)

        assertEquals("$CH1$CH3$CH4$CH5", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_before() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(2))

        DeleteSurroundingTextInCodePointsEditOp(1, 0).process(eb)

        assertEquals("$CH2$CH3$CH4$CH5", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_both() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        DeleteSurroundingTextInCodePointsEditOp(1, 1).process(eb)

        assertEquals("$CH1$CH2$CH5", eb.toString())
        assertEquals(4, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_after_multiple() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(4))

        DeleteSurroundingTextInCodePointsEditOp(0, 2).process(eb)

        assertEquals("$CH1$CH2$CH5", eb.toString())
        assertEquals(4, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_before_multiple() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        DeleteSurroundingTextInCodePointsEditOp(2, 0).process(eb)

        assertEquals("$CH1$CH4$CH5", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_both_multiple() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        DeleteSurroundingTextInCodePointsEditOp(2, 2).process(eb)

        assertEquals("$CH1", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_selection_preserve() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(4, 8))

        DeleteSurroundingTextInCodePointsEditOp(1, 1).process(eb)

        assertEquals("$CH1$CH3$CH4", eb.toString())
        assertEquals(2, eb.selectionStart)
        assertEquals(6, eb.selectionEnd)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_before_too_many() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        DeleteSurroundingTextInCodePointsEditOp(1000, 0).process(eb)

        assertEquals("$CH4$CH5", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_after_too_many() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        DeleteSurroundingTextInCodePointsEditOp(0, 1000).process(eb)

        assertEquals("$CH1$CH2$CH3", eb.toString())
        assertEquals(6, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_both_too_many() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        DeleteSurroundingTextInCodePointsEditOp(1000, 1000).process(eb)

        assertEquals("", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_composition_no_intersection_preceding_composition() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(0, 2)

        DeleteSurroundingTextInCodePointsEditOp(1, 1).process(eb)

        assertEquals("$CH1$CH2$CH5", eb.toString())
        assertEquals(4, eb.cursor)
        assertEquals(0, eb.compositionStart)
        assertEquals(2, eb.compositionEnd)
    }

    @Test
    fun test_delete_composition_no_intersection_trailing_composition() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(8, 10)

        DeleteSurroundingTextInCodePointsEditOp(1, 1).process(eb)

        assertEquals("$CH1$CH2$CH5", eb.toString())
        assertEquals(4, eb.cursor)
        assertEquals(4, eb.compositionStart)
        assertEquals(6, eb.compositionEnd)
    }

    @Test
    fun test_delete_composition_intersection_preceding_composition() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(0, 6)

        DeleteSurroundingTextInCodePointsEditOp(1, 1).process(eb)

        assertEquals("$CH1$CH2$CH5", eb.toString())
        assertEquals(4, eb.cursor)
        assertEquals(0, eb.compositionStart)
        assertEquals(4, eb.compositionEnd)
    }

    @Test
    fun test_delete_composition_intersection_trailing_composition() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(6, 10)

        DeleteSurroundingTextInCodePointsEditOp(1, 1).process(eb)

        assertEquals("$CH1$CH2$CH5", eb.toString())
        assertEquals(4, eb.cursor)
        assertEquals(4, eb.compositionStart)
        assertEquals(6, eb.compositionEnd)
    }

    @Test
    fun test_delete_covered_composition() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(4, 6)

        DeleteSurroundingTextInCodePointsEditOp(1, 1).process(eb)

        assertEquals("$CH1$CH2$CH5", eb.toString())
        assertEquals(4, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_composition_covered() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(0, 10)

        DeleteSurroundingTextInCodePointsEditOp(1, 1).process(eb)

        assertEquals("$CH1$CH2$CH5", eb.toString())
        assertEquals(4, eb.cursor)
        assertEquals(0, eb.compositionStart)
        assertEquals(6, eb.compositionEnd)
    }
}