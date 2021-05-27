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
class SetComposingTextCommandTest {

    @Test
    fun test_insert_empty() {
        val eb = EditingBuffer("", TextRange.Zero)

        SetComposingTextCommand("X", 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("X")
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(1)
    }

    @Test
    fun test_insert_cursor_tail() {
        val eb = EditingBuffer("A", TextRange(1))

        SetComposingTextCommand("X", 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("AX")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(1)
        assertThat(eb.compositionEnd).isEqualTo(2)
    }

    @Test
    fun test_insert_cursor_head() {
        val eb = EditingBuffer("A", TextRange(1))

        SetComposingTextCommand("X", 0).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("AX")
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(1)
        assertThat(eb.compositionEnd).isEqualTo(2)
    }

    @Test
    fun test_insert_cursor_far_tail() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        SetComposingTextCommand("X", 2).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("AXBCDE")
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(1)
        assertThat(eb.compositionEnd).isEqualTo(2)
    }

    @Test
    fun test_insert_cursor_far_head() {
        val eb = EditingBuffer("ABCDE", TextRange(4))

        SetComposingTextCommand("X", -2).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDXE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(4)
        assertThat(eb.compositionEnd).isEqualTo(5)
    }

    @Test
    fun test_insert_empty_text_cursor_head() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        SetComposingTextCommand("", 0).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_insert_empty_text_cursor_tail() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        SetComposingTextCommand("", 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_insert_empty_text_cursor_far_tail() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        SetComposingTextCommand("", 2).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_insert_empty_text_cursor_far_head() {
        val eb = EditingBuffer("ABCDE", TextRange(4))

        SetComposingTextCommand("", -2).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_cancel_composition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 4) // Mark "BCD" as composition
        SetComposingTextCommand("X", 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("AXE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(1)
        assertThat(eb.compositionEnd).isEqualTo(2)
    }

    @Test
    fun test_replace_selection() {
        val eb = EditingBuffer("ABCDE", TextRange(1, 4)) // select "BCD"

        SetComposingTextCommand("X", 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("AXE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(1)
        assertThat(eb.compositionEnd).isEqualTo(2)
    }

    @Test
    fun test_composition_and_selection() {
        val eb = EditingBuffer("ABCDE", TextRange(1, 3)) // select "BC"

        eb.setComposition(2, 4) // Mark "CD" as composition
        SetComposingTextCommand("X", 1).applyTo(eb)

        // If composition and selection exists at the same time, replace composition and cancel
        // selection and place cursor.
        assertThat(eb.toString()).isEqualTo("ABXE")
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(3)
    }

    @Test
    fun test_cursor_position_too_small() {
        val eb = EditingBuffer("ABCDE", TextRange(5))

        SetComposingTextCommand("X", -1000).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDEX")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(5)
        assertThat(eb.compositionEnd).isEqualTo(6)
    }

    @Test
    fun test_cursor_position_too_large() {
        val eb = EditingBuffer("ABCDE", TextRange(5))

        SetComposingTextCommand("X", 1000).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABCDEX")
        assertThat(eb.cursor).isEqualTo(6)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.compositionStart).isEqualTo(5)
        assertThat(eb.compositionEnd).isEqualTo(6)
    }
}