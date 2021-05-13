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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.random.Random

@OptIn(InternalTextApi::class)
@RunWith(JUnit4::class)
class GapBufferTest {

    private fun assertStrWithChars(expected: String, pgb: PartialGapBuffer) {
        assertThat(pgb.toString()).isEqualTo(expected)
        for (i in expected.indices) {
            assertThat(pgb[i]).isEqualTo(expected[i])
        }
    }

    @Test
    fun insertTest_insert_to_empty_string() {
        assertStrWithChars(
            "A",
            PartialGapBuffer("").apply {
                replace(0, 0, "A")
            }
        )
    }

    @Test
    fun insertTest_insert_and_append() {
        assertStrWithChars(
            "BA",
            PartialGapBuffer("").apply {
                replace(0, 0, "A")
                replace(0, 0, "B")
            }
        )
    }

    @Test
    fun insertTest_insert_and_prepend() {
        assertStrWithChars(
            "AB",
            PartialGapBuffer("").apply {
                replace(0, 0, "A")
                replace(1, 1, "B")
            }
        )
    }

    @Test
    fun insertTest_insert_and_insert_into_middle() {
        assertStrWithChars(
            "ABA",
            PartialGapBuffer("").apply {
                replace(0, 0, "AA")
                replace(1, 1, "B")
            }
        )
    }

    @Test
    fun insertTest_intoExistingText_prepend() {
        assertStrWithChars(
            "AXX",
            PartialGapBuffer("XX").apply {
                replace(0, 0, "A")
            }
        )
    }

    @Test
    fun insertTest_intoExistingText_insert_into_middle() {
        assertStrWithChars(
            "XAX",
            PartialGapBuffer("XX").apply {
                replace(1, 1, "A")
            }
        )
    }

    @Test
    fun insertTest_intoExistingText_append() {
        assertStrWithChars(
            "XXA",
            PartialGapBuffer("XX").apply {
                replace(2, 2, "A")
            }
        )
    }

    @Test
    fun insertTest_intoExistingText_prepend_and_prepend() {
        assertStrWithChars(
            "BAXX",
            PartialGapBuffer("XX").apply {
                replace(0, 0, "A")
                replace(0, 0, "B")
            }
        )
    }

    @Test
    fun insertTest_intoExistingText_prepend_and_append() {
        assertStrWithChars(
            "ABXX",
            PartialGapBuffer("XX").apply {
                replace(0, 0, "A")
                replace(1, 1, "B")
            }
        )
    }

    @Test
    fun insertTest_intoExistingText_prepend_and_insert_middle() {
        assertStrWithChars(
            "AXBX",
            PartialGapBuffer("XX").apply {
                replace(0, 0, "A")
                replace(2, 2, "B")
            }
        )
    }

    @Test
    fun insertTest_intoExistingText_insert_two_chars_and_append() {
        assertStrWithChars(
            "ABAXX",
            PartialGapBuffer("XX").apply {
                replace(0, 0, "AA")
                replace(1, 1, "B")
            }
        )
    }

    @Test
    fun deleteTest_insert_and_delete_from_head() {
        assertStrWithChars(
            "BC",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 1, "")
            }
        )
    }

    @Test
    fun deleteTest_insert_and_delete_middle() {
        assertStrWithChars(
            "AC",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 2, "")
            }
        )
    }

    @Test
    fun deleteTest_insert_and_delete_tail() {
        assertStrWithChars(
            "AB",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(2, 3, "")
            }
        )
    }

    @Test
    fun deleteTest_insert_and_delete_two_head() {
        assertStrWithChars(
            "C",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 2, "")
            }
        )
    }

    @Test
    fun deleteTest_insert_and_delete_two_tail() {
        assertStrWithChars(
            "A",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 3, "")
            }
        )
    }

    @Test
    fun deleteTest_insert_and_delete_with_two_instruction_from_haed() {
        assertStrWithChars(
            "C",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 1, "")
                replace(0, 1, "")
            }
        )
    }

    @Test
    fun deleteTest_insert_and_delet_with_two_instruction_from_head_and_tail() {
        assertStrWithChars(
            "B",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 1, "")
                replace(1, 2, "")
            }
        )
    }

    @Test
    fun deleteTest_insert_and_delet_with_two_instruction_from_tail() {
        assertStrWithChars(
            "A",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 2, "")
                replace(1, 2, "")
            }
        )
    }

    @Test
    fun deleteTest_insert_and_delete_three_chars() {
        assertStrWithChars(
            "",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 3, "")
            }
        )
    }

    @Test
    fun deleteTest_insert_and_delete_three_chars_with_three_instructions() {
        assertStrWithChars(
            "",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 1, "")
                replace(0, 1, "")
                replace(0, 1, "")
            }
        )
    }

    @Test
    fun deleteTest_fromExistingText_from_head() {
        assertStrWithChars(
            "BC",
            PartialGapBuffer("ABC").apply {
                replace(0, 1, "")
            }
        )
    }

    @Test
    fun deleteTest_fromExistingText_from_middle() {
        assertStrWithChars(
            "AC",
            PartialGapBuffer("ABC").apply {
                replace(1, 2, "")
            }
        )
    }

    @Test
    fun deleteTest_fromExistingText_from_tail() {
        assertStrWithChars(
            "AB",
            PartialGapBuffer("ABC").apply {
                replace(2, 3, "")
            }
        )
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_from_head() {
        assertStrWithChars(
            "C",
            PartialGapBuffer("ABC").apply {
                replace(0, 2, "")
            }
        )
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_from_tail() {
        assertStrWithChars(
            "A",
            PartialGapBuffer("ABC").apply {
                replace(1, 3, "")
            }
        )
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_with_two_instruction_from_head() {
        assertStrWithChars(
            "C",
            PartialGapBuffer("ABC").apply {
                replace(0, 1, "")
                replace(0, 1, "")
            }
        )
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_with_two_instruction_from_head_and_tail() {
        assertStrWithChars(
            "B",
            PartialGapBuffer("ABC").apply {
                replace(0, 1, "")
                replace(1, 2, "")
            }
        )
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_with_two_instruction_from_tail() {
        assertStrWithChars(
            "A",
            PartialGapBuffer("ABC").apply {
                replace(1, 2, "")
                replace(1, 2, "")
            }
        )
    }

    @Test
    fun deleteTest_fromExistingText_delete_three_chars() {
        assertStrWithChars(
            "",
            PartialGapBuffer("ABC").apply {
                replace(0, 3, "")
            }
        )
    }

    @Test
    fun deleteTest_fromExistingText_delete_three_chars_with_three_instructions() {
        assertStrWithChars(
            "",
            PartialGapBuffer("ABC").apply {
                replace(0, 1, "")
                replace(0, 1, "")
                replace(0, 1, "")
            }
        )
    }

    @Test
    fun replaceTest_head() {
        assertStrWithChars(
            "XBC",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 1, "X")
            }
        )
    }

    @Test
    fun replaceTest_middle() {
        assertStrWithChars(
            "AXC",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 2, "X")
            }
        )
    }

    @Test
    fun replaceTest_tail() {
        assertStrWithChars(
            "ABX",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(2, 3, "X")
            }
        )
    }

    @Test
    fun replaceTest_head_two_chars() {
        assertStrWithChars(
            "XC",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 2, "X")
            }
        )
    }

    @Test
    fun replaceTest_middle_two_chars() {
        assertStrWithChars(
            "AX",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 3, "X")
            }
        )
    }

    @Test
    fun replaceTest_three_chars() {
        assertStrWithChars(
            "X",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 3, "X")
            }
        )
    }

    @Test
    fun replaceTest_one_char_with_two_chars_from_head() {
        assertStrWithChars(
            "XYBC",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 1, "XY")
            }
        )
    }

    @Test
    fun replaceTest_one_char_with_two_chars_from_middle() {
        assertStrWithChars(
            "AXYC",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 2, "XY")
            }
        )
    }

    @Test
    fun replaceTest_one_char_with_two_chars_from_tail() {
        assertStrWithChars(
            "ABXY",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(2, 3, "XY")
            }
        )
    }

    @Test
    fun replaceTest_two_chars_with_two_chars_from_head() {
        assertStrWithChars(
            "XYC",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 2, "XY")
            }
        )
    }

    @Test
    fun replaceTest_two_chars_with_two_chars_from_tail() {
        assertStrWithChars(
            "AXY",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(1, 3, "XY")
            }
        )
    }

    @Test
    fun replaceTest_three_chars_with_two_char() {
        assertStrWithChars(
            "XY",
            PartialGapBuffer("").apply {
                replace(0, 0, "ABC")
                replace(0, 3, "XY")
            }
        )
    }

    @Test
    fun replaceTest_fromExistingText_head() {
        assertStrWithChars(
            "XBC",
            PartialGapBuffer("ABC").apply {
                replace(0, 1, "X")
            }
        )
    }

    @Test
    fun replaceTest_fromExistingText_middle() {
        assertStrWithChars(
            "AXC",
            PartialGapBuffer("ABC").apply {
                replace(1, 2, "X")
            }
        )
    }

    @Test
    fun replaceTest_fromExistingText_tail() {
        assertStrWithChars(
            "ABX",
            PartialGapBuffer("ABC").apply {
                replace(2, 3, "X")
            }
        )
    }

    @Test
    fun replaceTest_fromExistingText_two_chars_with_one_char_from_head() {
        assertStrWithChars(
            "XC",
            PartialGapBuffer("ABC").apply {
                replace(0, 2, "X")
            }
        )
    }

    @Test
    fun replaceTest_fromExistingText_two_chars_with_one_char_from_tail() {
        assertStrWithChars(
            "AX",
            PartialGapBuffer("ABC").apply {
                replace(1, 3, "X")
            }
        )
    }

    @Test
    fun replaceTest_fromExistingText_three_chars() {
        assertStrWithChars(
            "X",
            PartialGapBuffer("ABC").apply {
                replace(0, 3, "X")
            }
        )
    }

    @Test
    fun replaceTest_fromExistingText_one_char_with_two_chars_from_head() {
        assertStrWithChars(
            "XYBC",
            PartialGapBuffer("ABC").apply {
                replace(0, 1, "XY")
            }
        )
    }

    @Test
    fun replaceTest_fromExistingText_one_char_with_two_chars_from_middle() {
        assertStrWithChars(
            "AXYC",
            PartialGapBuffer("ABC").apply {
                replace(1, 2, "XY")
            }
        )
    }

    @Test
    fun replaceTest_fromExistingText_one_char_with_two_chars_from_tail() {
        assertStrWithChars(
            "ABXY",
            PartialGapBuffer("ABC").apply {
                replace(2, 3, "XY")
            }
        )
    }

    @Test
    fun replaceTest_fromExistingText_two_chars_with_two_chars_from_head() {
        assertStrWithChars(
            "XYC",
            PartialGapBuffer("ABC").apply {
                replace(0, 2, "XY")
            }
        )
    }

    @Test
    fun replaceTest_fromExistingText_two_chars_with_two_chars_from_tail() {
        assertStrWithChars(
            "AXY",
            PartialGapBuffer("ABC").apply {
                replace(1, 3, "XY")
            }
        )
    }

    @Test
    fun replaceTest_fromExistingText_three_chars_with_three_chars() {
        assertStrWithChars(
            "XY",
            PartialGapBuffer("ABC").apply {
                replace(0, 3, "XY")
            }
        )
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
        assertStrWithChars(sb.toString(), gb)
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