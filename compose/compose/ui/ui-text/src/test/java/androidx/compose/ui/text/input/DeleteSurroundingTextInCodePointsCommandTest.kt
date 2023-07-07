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
class DeleteSurroundingTextInCodePointsCommandTest {
    val CH1 = "\uD83D\uDE00" // U+1F600
    val CH2 = "\uD83D\uDE01" // U+1F601
    val CH3 = "\uD83D\uDE02" // U+1F602
    val CH4 = "\uD83D\uDE03" // U+1F603
    val CH5 = "\uD83D\uDE04" // U+1F604

    @Test
    fun test_delete_after() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(2))

        DeleteSurroundingTextInCodePointsCommand(0, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH1$CH3$CH4$CH5")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_before() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(2))

        DeleteSurroundingTextInCodePointsCommand(1, 0).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH2$CH3$CH4$CH5")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_both() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        DeleteSurroundingTextInCodePointsCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.cursor).isEqualTo(4)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_after_multiple() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(4))

        DeleteSurroundingTextInCodePointsCommand(0, 2).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.cursor).isEqualTo(4)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_before_multiple() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        DeleteSurroundingTextInCodePointsCommand(2, 0).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH1$CH4$CH5")
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_both_multiple() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        DeleteSurroundingTextInCodePointsCommand(2, 2).applyTo(eb)

        assertThat(eb.toString()).isEqualTo(CH1)
        assertThat(eb.cursor).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_selection_preserve() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(4, 8))

        DeleteSurroundingTextInCodePointsCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH1$CH3$CH4")
        assertThat(eb.selectionStart).isEqualTo(2)
        assertThat(eb.selectionEnd).isEqualTo(6)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_before_too_many() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        DeleteSurroundingTextInCodePointsCommand(1000, 0).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH4$CH5")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_after_too_many() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        DeleteSurroundingTextInCodePointsCommand(0, 1000).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH3")
        assertThat(eb.cursor).isEqualTo(6)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_both_too_many() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        DeleteSurroundingTextInCodePointsCommand(1000, 1000).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("")
        assertThat(eb.cursor).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_composition_no_intersection_preceding_composition() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(0, 2)

        DeleteSurroundingTextInCodePointsCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.cursor).isEqualTo(4)
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(2)
    }

    @Test
    fun test_delete_composition_no_intersection_trailing_composition() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(8, 10)

        DeleteSurroundingTextInCodePointsCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.cursor).isEqualTo(4)
        assertThat(eb.compositionStart).isEqualTo(4)
        assertThat(eb.compositionEnd).isEqualTo(6)
    }

    @Test
    fun test_delete_composition_intersection_preceding_composition() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(0, 6)

        DeleteSurroundingTextInCodePointsCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.cursor).isEqualTo(4)
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(4)
    }

    @Test
    fun test_delete_composition_intersection_trailing_composition() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(6, 10)

        DeleteSurroundingTextInCodePointsCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.cursor).isEqualTo(4)
        assertThat(eb.compositionStart).isEqualTo(4)
        assertThat(eb.compositionEnd).isEqualTo(6)
    }

    @Test
    fun test_delete_covered_composition() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(4, 6)

        DeleteSurroundingTextInCodePointsCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.cursor).isEqualTo(4)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_composition_covered() {
        val eb = EditingBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(0, 10)

        DeleteSurroundingTextInCodePointsCommand(1, 1).applyTo(eb)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.cursor).isEqualTo(4)
        assertThat(eb.compositionStart).isEqualTo(0)
        assertThat(eb.compositionEnd).isEqualTo(6)
    }

    @Test
    fun throws_whenLengthBeforeInvalid() {
        val error = assertFailsWith<IllegalArgumentException> {
            DeleteSurroundingTextInCodePointsCommand(
                lengthBeforeCursor = -42,
                lengthAfterCursor = 0
            )
        }
        assertThat(error).hasMessageThat().contains("-42")
    }

    @Test
    fun throws_whenLengthAfterInvalid() {
        val error = assertFailsWith<IllegalArgumentException> {
            DeleteSurroundingTextInCodePointsCommand(
                lengthBeforeCursor = 0,
                lengthAfterCursor = -42
            )
        }
        assertThat(error).hasMessageThat().contains("-42")
    }
}