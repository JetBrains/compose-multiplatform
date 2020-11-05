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
class EditingBufferTest {

    private fun assertStrWithChars(expected: String, eb: EditingBuffer) {
        assertEquals(expected.length, eb.length)
        assertEquals(expected, eb.toString())
        for (i in 0 until expected.length) {
            assertEquals(expected[i], eb[i])
        }
    }

    @Test
    fun test_insert() {
        val eb = EditingBuffer("", TextRange.Zero)

        eb.replace(0, 0, "A")

        assertStrWithChars("A", eb)
        assertEquals(1, eb.cursor)
        assertEquals(1, eb.selectionStart)
        assertEquals(1, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        // Keep inserting text to the end of string. Cursor should follow.
        eb.replace(1, 1, "BC")
        assertStrWithChars("ABC", eb)
        assertEquals(3, eb.cursor)
        assertEquals(3, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        // Insert into middle position. Cursor should be end of inserted text.
        eb.replace(1, 1, "D")
        assertStrWithChars("ADBC", eb)
        assertEquals(2, eb.cursor)
        assertEquals(2, eb.selectionStart)
        assertEquals(2, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)
    }

    @Test
    fun test_delete() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.replace(0, 1, "")

        // Delete the left character at the cursor.
        assertStrWithChars("BCDE", eb)
        assertEquals(0, eb.cursor)
        assertEquals(0, eb.selectionStart)
        assertEquals(0, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        // Delete the text before the cursor
        eb.replace(0, 2, "")
        assertStrWithChars("DE", eb)
        assertEquals(0, eb.cursor)
        assertEquals(0, eb.selectionStart)
        assertEquals(0, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        // Delete end of the text.
        eb.replace(1, 2, "")
        assertStrWithChars("D", eb)
        assertEquals(1, eb.cursor)
        assertEquals(1, eb.selectionStart)
        assertEquals(1, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)
    }

    @Test
    fun test_setSelection() {
        val eb = EditingBuffer("ABCDE", TextRange(0, 3))
        assertStrWithChars("ABCDE", eb)
        assertEquals(-1, eb.cursor)
        assertEquals(0, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        eb.setSelection(0, 5) // Change the selection
        assertStrWithChars("ABCDE", eb)
        assertEquals(-1, eb.cursor)
        assertEquals(0, eb.selectionStart)
        assertEquals(5, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        eb.replace(0, 3, "X") // replace function cancel the selection and place cursor.
        assertStrWithChars("XDE", eb)
        assertEquals(1, eb.cursor)
        assertEquals(1, eb.selectionStart)
        assertEquals(1, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        eb.setSelection(0, 2) // Set the selection again
        assertStrWithChars("XDE", eb)
        assertEquals(-1, eb.cursor)
        assertEquals(0, eb.selectionStart)
        assertEquals(2, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)
    }

    @Test
    fun test_setCompostion_and_cancelComposition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(0, 5) // Make all text as composition
        assertStrWithChars("ABCDE", eb)
        assertEquals(0, eb.cursor)
        assertEquals(0, eb.selectionStart)
        assertEquals(0, eb.selectionEnd)
        assertTrue(eb.hasComposition())
        assertEquals(0, eb.compositionStart)
        assertEquals(5, eb.compositionEnd)

        eb.replace(2, 3, "X") // replace function cancel the composition text.
        assertStrWithChars("ABXDE", eb)
        assertEquals(3, eb.cursor)
        assertEquals(3, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        eb.setComposition(2, 4) // set composition again
        assertStrWithChars("ABXDE", eb)
        assertEquals(3, eb.cursor)
        assertEquals(3, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertTrue(eb.hasComposition())
        assertEquals(2, eb.compositionStart)
        assertEquals(4, eb.compositionEnd)

        eb.cancelComposition() // cancel the composition
        assertStrWithChars("ABE", eb)
        assertEquals(2, eb.cursor)
        assertEquals(2, eb.selectionStart)
        assertEquals(2, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)
    }

    @Test
    fun test_setCompostion_and_commitComposition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(0, 5) // Make all text as composition
        assertStrWithChars("ABCDE", eb)
        assertEquals(0, eb.cursor)
        assertEquals(0, eb.selectionStart)
        assertEquals(0, eb.selectionEnd)
        assertTrue(eb.hasComposition())
        assertEquals(0, eb.compositionStart)
        assertEquals(5, eb.compositionEnd)

        eb.replace(2, 3, "X") // replace function cancel the composition text.
        assertStrWithChars("ABXDE", eb)
        assertEquals(3, eb.cursor)
        assertEquals(3, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        eb.setComposition(2, 4) // set composition again
        assertStrWithChars("ABXDE", eb)
        assertEquals(3, eb.cursor)
        assertEquals(3, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertTrue(eb.hasComposition())
        assertEquals(2, eb.compositionStart)
        assertEquals(4, eb.compositionEnd)

        eb.commitComposition() // commit the composition
        assertStrWithChars("ABXDE", eb)
        assertEquals(3, eb.cursor)
        assertEquals(3, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)
    }

    @Test
    fun test_setCursor_and_get_cursor() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.cursor = 1
        assertStrWithChars("ABCDE", eb)
        assertEquals(1, eb.cursor)
        assertEquals(1, eb.selectionStart)
        assertEquals(1, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        eb.cursor = 2
        assertStrWithChars("ABCDE", eb)
        assertEquals(2, eb.cursor)
        assertEquals(2, eb.selectionStart)
        assertEquals(2, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)

        eb.cursor = 5
        assertStrWithChars("ABCDE", eb)
        assertEquals(5, eb.cursor)
        assertEquals(5, eb.selectionStart)
        assertEquals(5, eb.selectionEnd)
        assertFalse(eb.hasComposition())
        assertEquals(-1, eb.compositionStart)
        assertEquals(-1, eb.compositionEnd)
    }

    @Test
    fun test_delete_preceding_cursor_no_composition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.delete(1, 2)
        assertStrWithChars("ACDE", eb)
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_trailing_cursor_no_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.delete(1, 2)
        assertStrWithChars("ACDE", eb)
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_preceding_selection_no_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(0, 1))

        eb.delete(1, 2)
        assertStrWithChars("ACDE", eb)
        assertEquals(0, eb.selectionStart)
        assertEquals(1, eb.selectionEnd)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_trailing_selection_no_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(4, 5))

        eb.delete(1, 2)
        assertStrWithChars("ACDE", eb)
        assertEquals(3, eb.selectionStart)
        assertEquals(4, eb.selectionEnd)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_preceding_composition_no_intersection() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 2)
        eb.delete(2, 3)

        assertStrWithChars("ABDE", eb)
        assertEquals(0, eb.cursor)
        assertEquals(1, eb.compositionStart)
        assertEquals(2, eb.compositionEnd)
    }

    @Test
    fun test_delete_trailing_composition_no_intersection() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(3, 4)
        eb.delete(2, 3)

        assertStrWithChars("ABDE", eb)
        assertEquals(0, eb.cursor)
        assertEquals(2, eb.compositionStart)
        assertEquals(3, eb.compositionEnd)
    }

    @Test
    fun test_delete_preceding_composition_intersection() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 3)
        eb.delete(2, 4)

        assertStrWithChars("ABE", eb)
        assertEquals(0, eb.cursor)
        assertEquals(1, eb.compositionStart)
        assertEquals(2, eb.compositionEnd)
    }

    @Test
    fun test_delete_trailing_composition_intersection() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(3, 5)
        eb.delete(2, 4)

        assertStrWithChars("ABE", eb)
        assertEquals(0, eb.cursor)
        assertEquals(2, eb.compositionStart)
        assertEquals(3, eb.compositionEnd)
    }

    @Test
    fun test_delete_composition_contains_delrange() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(2, 5)
        eb.delete(3, 4)

        assertStrWithChars("ABCE", eb)
        assertEquals(0, eb.cursor)
        assertEquals(2, eb.compositionStart)
        assertEquals(4, eb.compositionEnd)
    }

    @Test
    fun test_delete_delrange_contains_composition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(3, 4)
        eb.delete(2, 5)

        assertStrWithChars("AB", eb)
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }
}
