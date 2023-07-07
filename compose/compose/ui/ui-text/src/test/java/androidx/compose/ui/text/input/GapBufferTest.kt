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

import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.matchers.assertThat
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(InternalTextApi::class)
@RunWith(JUnit4::class)
class GapBufferTest {

    @Test
    fun insertTest_insert_to_empty_string() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "A")
            }
        ).hasChars("A")
    }

    @Test
    fun insertTest_insert_and_append() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "A")
                replace(0, 0, "B")
            }
        ).hasChars("BA")
    }

    @Test
    fun insertTest_insert_and_prepend() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "A")
                replace(1, 1, "B")
            }
        ).hasChars("AB")
    }

    @Test
    fun insertTest_insert_and_insert_into_middle() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "AA")
                replace(1, 1, "B")
            }
        ).hasChars("ABA")
    }

    @Test
    fun insertTest_intoExistingText_prepend() {
        assertThat(
            PartialGapBuffer("XX").apply {
                replace(0, 0, "A")
            }
        ).hasChars("AXX")
    }

    @Test
    fun insertTest_intoExistingText_insert_into_middle() {
        assertThat(
            PartialGapBuffer("XX").apply {
                replace(1, 1, "A")
            }
        ).hasChars("XAX")
    }

    @Test
    fun insertTest_intoExistingText_append() {
        assertThat(
            PartialGapBuffer("XX").apply {
                replace(2, 2, "A")
            }
        ).hasChars("XXA")
    }

    @Test
    fun insertTest_intoExistingText_prepend_and_prepend() {
        assertThat(
            PartialGapBuffer("XX").apply {
                replace(0, 0, "A")
                replace(0, 0, "B")
            }
        ).hasChars("BAXX")
    }

    @Test
    fun insertTest_intoExistingText_prepend_and_append() {
        assertThat(
            PartialGapBuffer("XX").apply {
                replace(0, 0, "A")
                replace(1, 1, "B")
            }
        ).hasChars("ABXX")
    }

    @Test
    fun insertTest_intoExistingText_prepend_and_insert_middle() {
        assertThat(
            PartialGapBuffer("XX").apply {
                replace(0, 0, "A")
                replace(2, 2, "B")
            }
        ).hasChars("AXBX")
    }

    @Test
    fun insertTest_intoExistingText_insert_two_chars_and_append() {
        assertThat(
            PartialGapBuffer("XX").apply {
                replace(0, 0, "AA")
                replace(1, 1, "B")
            }
        ).hasChars("ABAXX")
    }

    @Test
    fun deleteTest_insert_and_delete_from_head() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 1, "")
            }
        ).hasChars("BC")
    }

    @Test
    fun deleteTest_insert_and_delete_middle() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 2, "")
            }
        ).hasChars("AC")
    }

    @Test
    fun deleteTest_insert_and_delete_tail() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(2, 3, "")
            }
        ).hasChars("AB")
    }

    @Test
    fun deleteTest_insert_and_delete_two_head() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 2, "")
            }
        ).hasChars("C")
    }

    @Test
    fun deleteTest_insert_and_delete_two_tail() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 3, "")
            }
        ).hasChars("A")
    }

    @Test
    fun deleteTest_insert_and_delete_with_two_instruction_from_haed() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 1, "")
                replace(0, 1, "")
            }
        ).hasChars("C")
    }

    @Test
    fun deleteTest_insert_and_delet_with_two_instruction_from_head_and_tail() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 1, "")
                replace(1, 2, "")
            }
        ).hasChars("B")
    }

    @Test
    fun deleteTest_insert_and_delet_with_two_instruction_from_tail() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 2, "")
                replace(1, 2, "")
            }
        ).hasChars("A")
    }

    @Test
    fun deleteTest_insert_and_delete_three_chars() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 3, "")
            }
        ).hasChars("")
    }

    @Test
    fun deleteTest_insert_and_delete_three_chars_with_three_instructions() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 1, "")
                replace(0, 1, "")
                replace(0, 1, "")
            }
        ).hasChars("")
    }

    @Test
    fun deleteTest_fromExistingText_from_head() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(0, 1, "")
            }
        ).hasChars("BC")
    }

    @Test
    fun deleteTest_fromExistingText_from_middle() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(1, 2, "")
            }
        ).hasChars("AC")
    }

    @Test
    fun deleteTest_fromExistingText_from_tail() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(2, 3, "")
            }
        ).hasChars("AB")
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_from_head() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(0, 2, "")
            }
        ).hasChars("C")
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_from_tail() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(1, 3, "")
            }
        ).hasChars("A")
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_with_two_instruction_from_head() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(0, 1, "")
                replace(0, 1, "")
            }
        ).hasChars("C")
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_with_two_instruction_from_head_and_tail() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(0, 1, "")
                replace(1, 2, "")
            }
        ).hasChars("B")
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_with_two_instruction_from_tail() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(1, 2, "")
                replace(1, 2, "")
            }
        ).hasChars("A")
    }

    @Test
    fun deleteTest_fromExistingText_delete_three_chars() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(0, 3, "")
            }
        ).hasChars("")
    }

    @Test
    fun deleteTest_fromExistingText_delete_three_chars_with_three_instructions() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(0, 1, "")
                replace(0, 1, "")
                replace(0, 1, "")
            }
        ).hasChars("")
    }

    @Test
    fun replaceTest_head() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 1, "X")
            }
        ).hasChars("XBC")
    }

    @Test
    fun replaceTest_middle() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 2, "X")
            }
        ).hasChars("AXC")
    }

    @Test
    fun replaceTest_tail() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(2, 3, "X")
            }
        ).hasChars("ABX")
    }

    @Test
    fun replaceTest_head_two_chars() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 2, "X")
            }
        ).hasChars("XC")
    }

    @Test
    fun replaceTest_middle_two_chars() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 3, "X")
            }
        ).hasChars("AX")
    }

    @Test
    fun replaceTest_three_chars() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 3, "X")
            }
        ).hasChars("X")
    }

    @Test
    fun replaceTest_one_char_with_two_chars_from_head() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 1, "XY")
            }
        ).hasChars("XYBC")
    }

    @Test
    fun replaceTest_one_char_with_two_chars_from_middle() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 2, "XY")
            }
        ).hasChars("AXYC")
    }

    @Test
    fun replaceTest_one_char_with_two_chars_from_tail() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(2, 3, "XY")
            }
        ).hasChars("ABXY")
    }

    @Test
    fun replaceTest_two_chars_with_two_chars_from_head() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 2, "XY")
            }
        ).hasChars("XYC")
    }

    @Test
    fun replaceTest_two_chars_with_two_chars_from_tail() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 3, "XY")
            }
        ).hasChars("AXY")
    }

    @Test
    fun replaceTest_three_chars_with_two_char() {
        assertThat(
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 3, "XY")
            }
        ).hasChars("XY")
    }

    @Test
    fun replaceTest_fromExistingText_head() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(0, 1, "X")
            }
        ).hasChars("XBC")
    }

    @Test
    fun replaceTest_fromExistingText_middle() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(1, 2, "X")
            }
        ).hasChars("AXC")
    }

    @Test
    fun replaceTest_fromExistingText_tail() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(2, 3, "X")
            }
        ).hasChars("ABX")
    }

    @Test
    fun replaceTest_fromExistingText_two_chars_with_one_char_from_head() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(0, 2, "X")
            }
        ).hasChars("XC")
    }

    @Test
    fun replaceTest_fromExistingText_two_chars_with_one_char_from_tail() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(1, 3, "X")
            }
        ).hasChars("AX")
    }

    @Test
    fun replaceTest_fromExistingText_three_chars() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(0, 3, "X")
            }
        ).hasChars("X")
    }

    @Test
    fun replaceTest_fromExistingText_one_char_with_two_chars_from_head() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(0, 1, "XY")
            }
        ).hasChars("XYBC")
    }

    @Test
    fun replaceTest_fromExistingText_one_char_with_two_chars_from_middle() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(1, 2, "XY")
            }
        ).hasChars("AXYC")
    }

    @Test
    fun replaceTest_fromExistingText_one_char_with_two_chars_from_tail() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(2, 3, "XY")
            }
        ).hasChars("ABXY")
    }

    @Test
    fun replaceTest_fromExistingText_two_chars_with_two_chars_from_head() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(0, 2, "XY")
            }
        ).hasChars("XYC")
    }

    @Test
    fun replaceTest_fromExistingText_two_chars_with_two_chars_from_tail() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(1, 3, "XY")
            }
        ).hasChars("AXY")
    }

    @Test
    fun replaceTest_fromExistingText_three_chars_with_three_chars() {
        assertThat(
            PartialGapBuffer("ABC").apply {
                replace(0, 3, "XY")
            }
        ).hasChars("XY")
    }

    @Test
    fun replace_throws_whenStartGreaterThanEnd() {
        val buffer = PartialGapBuffer("ABCD")

        val error = assertFailsWith<IllegalArgumentException> {
            buffer.replace(3, 2, "")
        }
        assertThat(error).hasMessageThat().contains("3 > 2")
    }

    @Test
    fun replace_throws_whenStartNegative() {
        val buffer = PartialGapBuffer("ABCD")

        val error = assertFailsWith<IllegalArgumentException> {
            buffer.replace(-1, 2, "XY")
        }
        assertThat(error).hasMessageThat().contains("-1")
    }

    // Compare with the result of StringBuffer. We trust the StringBuffer works correctly
    private fun assertReplace(
        start: Int,
        end: Int,
        str: String,
        sb: StringBuffer,
        gb: PartialGapBuffer
    ) {
        sb.replace(start, end, str)
        gb.replace(start, end, str)
        assertThat(gb).hasChars(sb.toString())
    }

    private val LONG_INIT_TEXT = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".repeat(256)
    private val SHORT_TEXT = "A"
    private val MEDIUM_TEXT = "Hello, World"
    private val LONG_TEXT = "abcdefghijklmnopqrstuvwxyz".repeat(16)

    @Test
    fun longTextTest_keep_insertion() {
        val sb = StringBuffer(LONG_INIT_TEXT)
        val gb = PartialGapBuffer(LONG_INIT_TEXT)

        var c = 256 // cursor
        assertReplace(c, c, SHORT_TEXT, sb, gb)
        c += SHORT_TEXT.length
        assertReplace(c, c, MEDIUM_TEXT, sb, gb)
        c += MEDIUM_TEXT.length
        assertReplace(c, c, LONG_TEXT, sb, gb)
        c += LONG_TEXT.length
        assertReplace(c, c, MEDIUM_TEXT, sb, gb)
        c += MEDIUM_TEXT.length
        assertReplace(c, c, SHORT_TEXT, sb, gb)
    }

    @Test
    fun longTextTest_keep_deletion() {
        val sb = StringBuffer(LONG_INIT_TEXT)
        val gb = PartialGapBuffer(LONG_INIT_TEXT)

        var c = 2048 // cursor
        // Forward deletion
        assertReplace(c, c + 10, "", sb, gb)
        assertReplace(c, c + 100, "", sb, gb)
        assertReplace(c, c + 1000, "", sb, gb)

        // Backspacing
        assertReplace(c - 10, c, "", sb, gb)
        c -= 10
        assertReplace(c - 100, c, "", sb, gb)
        c -= 100
        assertReplace(c - 1000, c, "", sb, gb)
    }

    @Test
    fun longTextTest_farInput() {
        val sb = StringBuffer(LONG_INIT_TEXT)
        val gb = PartialGapBuffer(LONG_INIT_TEXT)

        assertReplace(1024, 1024, "Hello, World", sb, gb)
        assertReplace(128, 128, LONG_TEXT, sb, gb)
    }

    @Test
    fun randomInsertDeleteStressTest() {
        val sb = StringBuffer(LONG_INIT_TEXT)
        val gb = PartialGapBuffer(LONG_INIT_TEXT)

        val r = Random(10 /* fix the seed for reproduction */)

        val insertTexts = arrayOf(SHORT_TEXT, MEDIUM_TEXT, LONG_TEXT)
        val delLengths = arrayOf(1, 10, 100)

        var c = LONG_INIT_TEXT.length / 2

        for (i in 0..100) {
            when (r.nextInt() % 4) {
                0 -> { // insert
                    val txt = insertTexts.random(r)
                    assertReplace(c, c, txt, sb, gb)
                    c += txt.length
                }
                1 -> { // forward delete
                    assertReplace(c, c + delLengths.random(r), "", sb, gb)
                }
                2 -> { // backspacing
                    val len = delLengths.random(r)
                    assertReplace(c - len, c, "", sb, gb)
                    c -= len
                }
                3 -> { // replacing
                    val txt = insertTexts.random(r)
                    val len = delLengths.random(r)

                    assertReplace(c, c + len, txt, sb, gb)
                }
            }
        }
    }
}