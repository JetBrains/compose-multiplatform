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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SetComposingTextEditOpTest {

    @Test
    fun test_insert_empty() {
        val eb = EditingBuffer("", TextRange.Zero)

        SetComposingTextEditOp("X", 1).process(eb)

        assertEquals("X", eb.toString())
        assertEquals(1, eb.cursor)
        assertTrue(eb.hasComposition())
        assertEquals(0, eb.compositionStart)
        assertEquals(1, eb.compositionEnd)
    }

    @Test
    fun test_insert_cursor_tail() {
        val eb = EditingBuffer("A", TextRange(1))

        SetComposingTextEditOp("X", 1).process(eb)

        assertEquals("AX", eb.toString())
        assertEquals(2, eb.cursor)
        assertTrue(eb.hasComposition())
        assertEquals(1, eb.compositionStart)
        assertEquals(2, eb.compositionEnd)
    }

    @Test
    fun test_insert_cursor_head() {
        val eb = EditingBuffer("A", TextRange(1))

        SetComposingTextEditOp("X", 0).process(eb)

        assertEquals("AX", eb.toString())
        assertEquals(1, eb.cursor)
        assertTrue(eb.hasComposition())
        assertEquals(1, eb.compositionStart)
        assertEquals(2, eb.compositionEnd)
    }

    @Test
    fun test_insert_cursor_far_tail() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        SetComposingTextEditOp("X", 2).process(eb)

        assertEquals("AXBCDE", eb.toString())
        assertEquals(3, eb.cursor)
        assertTrue(eb.hasComposition())
        assertEquals(1, eb.compositionStart)
        assertEquals(2, eb.compositionEnd)
    }

    @Test
    fun test_insert_cursor_far_head() {
        val eb = EditingBuffer("ABCDE", TextRange(4))

        SetComposingTextEditOp("X", -2).process(eb)

        assertEquals("ABCDXE", eb.toString())
        assertEquals(2, eb.cursor)
        assertTrue(eb.hasComposition())
        assertEquals(4, eb.compositionStart)
        assertEquals(5, eb.compositionEnd)
    }

    @Test
    fun test_insert_empty_text_cursor_head() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        SetComposingTextEditOp("", 0).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(1, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_insert_empty_text_cursor_tail() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        SetComposingTextEditOp("", 1).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(1, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_insert_empty_text_cursor_far_tail() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        SetComposingTextEditOp("", 2).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_insert_empty_text_cursor_far_head() {
        val eb = EditingBuffer("ABCDE", TextRange(4))

        SetComposingTextEditOp("", -2).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_cancel_composition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 4) // Mark "BCD" as composition
        SetComposingTextEditOp("X", 1).process(eb)

        assertEquals("AXE", eb.toString())
        assertEquals(2, eb.cursor)
        assertTrue(eb.hasComposition())
        assertEquals(1, eb.compositionStart)
        assertEquals(2, eb.compositionEnd)
    }

    @Test
    fun test_replace_selection() {
        val eb = EditingBuffer("ABCDE", TextRange(1, 4)) // select "BCD"

        SetComposingTextEditOp("X", 1).process(eb)

        assertEquals("AXE", eb.toString())
        assertEquals(2, eb.cursor)
        assertTrue(eb.hasComposition())
        assertEquals(1, eb.compositionStart)
        assertEquals(2, eb.compositionEnd)
    }

    @Test
    fun test_composition_and_selection() {
        val eb = EditingBuffer("ABCDE", TextRange(1, 3)) // select "BC"

        eb.setComposition(2, 4) // Mark "CD" as composition
        SetComposingTextEditOp("X", 1).process(eb)

        // If composition and selection exists at the same time, replace composition and cancel
        // selection and place cursor.
        assertEquals("ABXE", eb.toString())
        assertEquals(3, eb.cursor)
        assertTrue(eb.hasComposition())
        assertEquals(2, eb.compositionStart)
        assertEquals(3, eb.compositionEnd)
    }

    @Test
    fun test_cursor_position_too_small() {
        val eb = EditingBuffer("ABCDE", TextRange(5))

        SetComposingTextEditOp("X", -1000).process(eb)

        assertEquals("ABCDEX", eb.toString())
        assertEquals(0, eb.cursor)
        assertTrue(eb.hasComposition())
        assertEquals(5, eb.compositionStart)
        assertEquals(6, eb.compositionEnd)
    }

    @Test
    fun test_cursor_position_too_large() {
        val eb = EditingBuffer("ABCDE", TextRange(5))

        SetComposingTextEditOp("X", 1000).process(eb)

        assertEquals("ABCDEX", eb.toString())
        assertEquals(6, eb.cursor)
        assertTrue(eb.hasComposition())
        assertEquals(5, eb.compositionStart)
        assertEquals(6, eb.compositionEnd)
    }
}