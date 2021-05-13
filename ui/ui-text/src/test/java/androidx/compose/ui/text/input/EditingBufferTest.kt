
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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EditingBufferTest {

    private fun assertStrWithChars(expected: String, eb: EditingBuffer) {
        assertThat(eb.length).isEqualTo(expected.length)
        assertThat(eb.toString()).isEqualTo(expected)
        for (i in 0 until expected.length) {
            assertThat(eb[i]).isEqualTo(expected[i])
        }
    }

    @Test
    fun test_insert() {
        val eb = EditingBuffer("", TextRange.Zero)

        eb.replace(0, 0, "A")

        assertStrWithChars("A", eb)
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        // Keep inserting text to the end of string. Cursor should follow.
        eb.replace(1, 1, "BC")
        assertStrWithChars("ABC", eb)
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        // Insert into middle position. Cursor should be end of inserted text.
        eb.replace(1, 1, "D")
        assertStrWithChars("ADBC", eb)
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.selectionStart).isEqualTo(2)
        assertThat(eb.selectionEnd).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)
    }

    @Test
    fun test_delete() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.replace(0, 1, "")

        // Delete the left character at the cursor.
        assertStrWithChars("BCDE", eb)
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        // Delete the text before the cursor
        eb.replace(0, 2, "")
        assertStrWithChars("DE", eb)
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        // Delete end of the text.
        eb.replace(1, 2, "")
        assertStrWithChars("D", eb)
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)
    }

    @Test
    fun test_setSelection() {
        val eb = EditingBuffer("ABCDE", TextRange(0, 3))
        assertStrWithChars("ABCDE", eb)
        assertThat(eb.cursor).isEqualTo(-1)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.setSelection(0, 5) // Change the selection
        assertStrWithChars("ABCDE", eb)
        assertThat(eb.cursor).isEqualTo(-1)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(5)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.replace(0, 3, "X") // replace function cancel the selection and place cursor.
        assertStrWithChars("XDE", eb)
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.setSelection(0, 2) // Set the selection again
        assertStrWithChars("XDE", eb)
        assertThat(eb.cursor).isEqualTo(-1)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)
    }

    @Test
    fun test_setCompostion_and_cancelComposition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(0, 5) // Make all text as composition
        assertStrWithChars("ABCDE", eb)
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(5)

        eb.replace(2, 3, "X") // replace function cancel the composition text.
        assertStrWithChars("ABXDE", eb)
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.setComposition(2, 4) // set composition again
        assertStrWithChars("ABXDE", eb)
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(4)

        eb.cancelComposition() // cancel the composition
        assertStrWithChars("ABE", eb)
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.selectionStart).isEqualTo(2)
        assertThat(eb.selectionEnd).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)
    }

    @Test
    fun test_setCompostion_and_commitComposition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(0, 5) // Make all text as composition
        assertStrWithChars("ABCDE", eb)
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(5)

        eb.replace(2, 3, "X") // replace function cancel the composition text.
        assertStrWithChars("ABXDE", eb)
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.setComposition(2, 4) // set composition again
        assertStrWithChars("ABXDE", eb)
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(4)

        eb.commitComposition() // commit the composition
        assertStrWithChars("ABXDE", eb)
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)
    }

    @Test
    fun test_setCursor_and_get_cursor() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.cursor = 1
        assertStrWithChars("ABCDE", eb)
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.cursor = 2
        assertStrWithChars("ABCDE", eb)
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.selectionStart).isEqualTo(2)
        assertThat(eb.selectionEnd).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.cursor = 5
        assertStrWithChars("ABCDE", eb)
        assertThat(eb.cursor).isEqualTo(5)
        assertThat(eb.selectionStart).isEqualTo(5)
        assertThat(eb.selectionEnd).isEqualTo(5)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)
    }

    @Test
    fun test_delete_preceding_cursor_no_composition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.delete(1, 2)
        assertStrWithChars("ACDE", eb)
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_trailing_cursor_no_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.delete(1, 2)
        assertStrWithChars("ACDE", eb)
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_preceding_selection_no_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(0, 1))

        eb.delete(1, 2)
        assertStrWithChars("ACDE", eb)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_trailing_selection_no_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(4, 5))

        eb.delete(1, 2)
        assertStrWithChars("ACDE", eb)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(4)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_covered_cursor() {
        // AB[]CDE
        val eb = EditingBuffer("ABCDE", TextRange(2, 2))

        eb.delete(1, 3)
        // A[]DE
        assertStrWithChars("ADE", eb)
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(1)
    }

    @Test
    fun test_delete_covered_selection() {
        // A[BC]DE
        val eb = EditingBuffer("ABCDE", TextRange(1, 3))

        eb.delete(0, 4)
        // []E
        assertStrWithChars("E", eb)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(0)
    }

    @Test
    fun test_delete_intersects_first_half_of_selection() {
        // AB[CD]E
        val eb = EditingBuffer("ABCDE", TextRange(2, 4))

        eb.delete(1, 3)
        // A[D]E
        assertStrWithChars("ADE", eb)
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(2)
    }

    @Test
    fun test_delete_intersects_second_half_of_selection() {
        // A[BCD]EFG
        val eb = EditingBuffer("ABCDEFG", TextRange(1, 4))

        eb.delete(3, 5)
        // A[BC]FG
        assertStrWithChars("ABCFG", eb)
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(3)
    }

    @Test
    fun test_delete_preceding_composition_no_intersection() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 2)
        eb.delete(2, 3)

        assertStrWithChars("ABDE", eb)
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.compositionStart).isEqualTo(1)
        assertThat(eb.compositionEnd).isEqualTo(2)
    }

    @Test
    fun test_delete_trailing_composition_no_intersection() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(3, 4)
        eb.delete(2, 3)

        assertStrWithChars("ABDE", eb)
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(3)
    }

    @Test
    fun test_delete_preceding_composition_intersection() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 3)
        eb.delete(2, 4)

        assertStrWithChars("ABE", eb)
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.compositionStart).isEqualTo(1)
        assertThat(eb.compositionEnd).isEqualTo(2)
    }

    @Test
    fun test_delete_trailing_composition_intersection() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(3, 5)
        eb.delete(2, 4)

        assertStrWithChars("ABE", eb)
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(3)
    }

    @Test
    fun test_delete_composition_contains_delrange() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(2, 5)
        eb.delete(3, 4)

        assertStrWithChars("ABCE", eb)
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(4)
    }

    @Test
    fun test_delete_delrange_contains_composition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(3, 4)
        eb.delete(2, 5)

        assertStrWithChars("AB", eb)
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }
}