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
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DeleteSurroundingTextCommandTest {

    @Test
    fun test_delete_after() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        DeleteSurroundingTextCommand(0, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ACDE")
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_before() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        DeleteSurroundingTextCommand(1, 0).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("BCDE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_both() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        DeleteSurroundingTextCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_after_multiple() {
        val eb = EditingBuffer("ABCDE", TextRange(2))

        DeleteSurroundingTextCommand(0, 2).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_before_multiple() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        DeleteSurroundingTextCommand(2, 0).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ADE")
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_both_multiple() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        DeleteSurroundingTextCommand(2, 2).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("A")
        assertThat(eb.cursor).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_selection_preserve() {
        val eb = EditingBuffer("ABCDE", TextRange(2, 4))

        DeleteSurroundingTextCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ACD")
        assertThat(eb.selectionStart).isEqualTo(1)
        assertThat(eb.selectionEnd).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_before_too_many() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        DeleteSurroundingTextCommand(1000, 0).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("DE")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_after_too_many() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        DeleteSurroundingTextCommand(0, 1000).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABC")
        assertThat(eb.cursor).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_both_too_many() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        DeleteSurroundingTextCommand(1000, 1000).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_composition_no_intersection_preceding_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.setComposition(0, 1)

        DeleteSurroundingTextCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(1)
    }

    @Test
    fun test_delete_composition_no_intersection_trailing_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.setComposition(4, 5)

        DeleteSurroundingTextCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(3)
    }

    @Test
    fun test_delete_composition_intersection_preceding_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.setComposition(0, 3)

        DeleteSurroundingTextCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(2)
    }

    @Test
    fun test_delete_composition_intersection_trailing_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.setComposition(3, 5)

        DeleteSurroundingTextCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.compositionStart).isEqualTo(2)
        assertThat(eb.compositionEnd).isEqualTo(3)
    }

    @Test
    fun test_delete_covered_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.setComposition(2, 3)

        DeleteSurroundingTextCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_composition_covered() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.setComposition(0, 5)

        DeleteSurroundingTextCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(3)
    }

    @Test
    fun throws_whenLengthBeforeInvalid() {
        val error = assertFailsWith<IllegalArgumentException> {
            DeleteSurroundingTextCommand(lengthBeforeCursor = -42, lengthAfterCursor = 0)
        }
        assertThat(error).hasMessageThat().contains("-42")
    }

    @Test
    fun throws_whenLengthAfterInvalid() {
        val error = assertFailsWith<IllegalArgumentException> {
            DeleteSurroundingTextCommand(lengthBeforeCursor = 0, lengthAfterCursor = -42)
        }
        assertThat(error).hasMessageThat().contains("-42")
    }
}