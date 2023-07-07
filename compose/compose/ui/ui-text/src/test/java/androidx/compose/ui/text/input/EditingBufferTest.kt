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
import androidx.compose.ui.text.matchers.assertThat
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EditingBufferTest {

    @Test
    fun insert() {
        val eb = EditingBuffer("", TextRange.Zero)

        eb.replace(0, 0, "A")

        assertThat(eb).hasChars("A")
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        // Keep inserting text to the end of string. Cursor should follow.
        eb.replace(1, 1, "BC")
        assertThat(eb).hasChars("ABC")
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        // Insert into middle position. Cursor should be end of inserted text.
        eb.replace(1, 1, "D")
        assertThat(eb).hasChars("ADBC")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.selectionStart).isEqualTo(2)
        assertThat(eb.selectionEnd).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)
    }

    @Test
    fun delete() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.replace(0, 1, "")

        // Delete the left character at the cursor.
        assertThat(eb).hasChars("BCDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        // Delete the text before the cursor
        eb.replace(0, 2, "")
        assertThat(eb).hasChars("DE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        // Delete end of the text.
        eb.replace(1, 2, "")
        assertThat(eb).hasChars("D")
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)
    }

    @Test
    fun setSelection() {
        val eb = EditingBuffer("ABCDE", TextRange(0, 3))
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.cursor).isEqualTo(-1)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.setSelection(0, 5) // Change the selection
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.cursor).isEqualTo(-1)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(5)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.replace(0, 3, "X") // replace function cancel the selection and place cursor.
        assertThat(eb).hasChars("XDE")
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.setSelection(0, 2) // Set the selection again
        assertThat(eb).hasChars("XDE")
        assertThat(eb.cursor).isEqualTo(-1)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)
    }

    @Test fun setSelection_throws_whenNegativeStart() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        assertFailsWith<IndexOutOfBoundsException> {
            eb.setSelection(-1, 0)
        }
    }

    @Test fun setSelection_throws_whenNegativeEnd() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        assertFailsWith<IndexOutOfBoundsException> {
            eb.setSelection(0, -1)
        }
    }

    @Test
    fun setCompostion_and_cancelComposition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(0, 5) // Make all text as composition
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(5)

        eb.replace(2, 3, "X") // replace function cancel the composition text.
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.setComposition(2, 4) // set composition again
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(4)

        eb.cancelComposition() // cancel the composition
        assertThat(eb).hasChars("ABE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.selectionStart).isEqualTo(2)
        assertThat(eb.selectionEnd).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)
    }

    @Test
    fun setCompostion_and_commitComposition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(0, 5) // Make all text as composition
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(5)

        eb.replace(2, 3, "X") // replace function cancel the composition text.
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.setComposition(2, 4) // set composition again
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(4)

        eb.commitComposition() // commit the composition
        assertThat(eb).hasChars("ABXDE")
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)
    }

    @Test
    fun setCursor_and_get_cursor() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.cursor = 1
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.cursor = 2
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.selectionStart).isEqualTo(2)
        assertThat(eb.selectionEnd).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)

        eb.cursor = 5
        assertThat(eb).hasChars("ABCDE")
        assertThat(eb.cursor).isEqualTo(5)
        assertThat(eb.selectionStart).isEqualTo(5)
        assertThat(eb.selectionEnd).isEqualTo(5)
        assertThat(eb.hasComposition()).isFalse()
        assertThat(eb.compositionStart).isEqualTo(-1)
        assertThat(eb.compositionEnd).isEqualTo(-1)
    }

    @Test
    fun delete_preceding_cursor_no_composition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.delete(1, 2)
        assertThat(eb).hasChars("ACDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun delete_trailing_cursor_no_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.delete(1, 2)
        assertThat(eb).hasChars("ACDE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun delete_preceding_selection_no_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(0, 1))

        eb.delete(1, 2)
        assertThat(eb).hasChars("ACDE")
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun delete_trailing_selection_no_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(4, 5))

        eb.delete(1, 2)
        assertThat(eb).hasChars("ACDE")
        assertThat(eb.selectionStart).isEqualTo(3)
        assertThat(eb.selectionEnd).isEqualTo(4)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun delete_covered_cursor() {
        // AB[]CDE
        val eb = EditingBuffer("ABCDE", TextRange(2, 2))

        eb.delete(1, 3)
        // A[]DE
        assertThat(eb).hasChars("ADE")
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(1)
    }

    @Test
    fun delete_covered_selection() {
        // A[BC]DE
        val eb = EditingBuffer("ABCDE", TextRange(1, 3))

        eb.delete(0, 4)
        // []E
        assertThat(eb).hasChars("E")
        assertThat(eb.selectionStart).isEqualTo(0)
        assertThat(eb.selectionEnd).isEqualTo(0)
    }

    @Test
    fun delete_intersects_first_half_of_selection() {
        // AB[CD]E
        val eb = EditingBuffer("ABCDE", TextRange(2, 4))

        eb.delete(1, 3)
        // A[D]E
        assertThat(eb).hasChars("ADE")
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(2)
    }

    @Test
    fun delete_intersects_second_half_of_selection() {
        // A[BCD]EFG
        val eb = EditingBuffer("ABCDEFG", TextRange(1, 4))

        eb.delete(3, 5)
        // A[BC]FG
        assertThat(eb).hasChars("ABCFG")
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(3)
    }

    @Test
    fun delete_preceding_composition_no_intersection() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 2)
        eb.delete(2, 3)

        assertThat(eb).hasChars("ABDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.compositionStart).isEqualTo(1)
        assertThat(eb.compositionEnd).isEqualTo(2)
    }

    @Test
    fun delete_trailing_composition_no_intersection() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(3, 4)
        eb.delete(2, 3)

        assertThat(eb).hasChars("ABDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(3)
    }

    @Test
    fun delete_preceding_composition_intersection() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 3)
        eb.delete(2, 4)

        assertThat(eb).hasChars("ABE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.compositionStart).isEqualTo(1)
        assertThat(eb.compositionEnd).isEqualTo(2)
    }

    @Test
    fun delete_trailing_composition_intersection() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(3, 5)
        eb.delete(2, 4)

        assertThat(eb).hasChars("ABE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(3)
    }

    @Test
    fun delete_composition_contains_delrange() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(2, 5)
        eb.delete(3, 4)

        assertThat(eb).hasChars("ABCE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(4)
    }

    @Test
    fun delete_delrange_contains_composition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(3, 4)
        eb.delete(2, 5)

        assertThat(eb).hasChars("AB")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }
}