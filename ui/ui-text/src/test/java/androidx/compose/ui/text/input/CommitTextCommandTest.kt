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
class CommitTextCommandTest {

    @Test
    fun test_insert_empty() {
        val eb = EditingBuffer("", TextRange.Zero)

        CommitTextCommand("X", 1).applyTo(eb)

        assertEquals("X", eb.toString())
        assertEquals(1, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_insert_cursor_tail() {
        val eb = EditingBuffer("A", TextRange(1))

        CommitTextCommand("X", 1).applyTo(eb)

        assertEquals("AX", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_insert_cursor_head() {
        val eb = EditingBuffer("A", TextRange(1))

        CommitTextCommand("X", 0).applyTo(eb)

        assertEquals("AX", eb.toString())
        assertEquals(1, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_insert_cursor_far_tail() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        CommitTextCommand("X", 2).applyTo(eb)

        assertEquals("AXBCDE", eb.toString())
        assertEquals(3, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_insert_cursor_far_head() {
        val eb = EditingBuffer("ABCDE", TextRange(4))

        CommitTextCommand("X", -2).applyTo(eb)

        assertEquals("ABCDXE", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_insert_empty_text_cursor_head() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        CommitTextCommand("", 0).applyTo(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(1, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_insert_empty_text_cursor_tail() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        CommitTextCommand("", 1).applyTo(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(1, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_insert_empty_text_cursor_far_tail() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        CommitTextCommand("", 2).applyTo(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_insert_empty_text_cursor_far_head() {
        val eb = EditingBuffer("ABCDE", TextRange(4))

        CommitTextCommand("", -2).applyTo(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_cancel_composition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 4) // Mark "BCD" as composition
        CommitTextCommand("X", 1).applyTo(eb)

        assertEquals("AXE", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_replace_selection() {
        val eb = EditingBuffer("ABCDE", TextRange(1, 4)) // select "BCD"

        CommitTextCommand("X", 1).applyTo(eb)

        assertEquals("AXE", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_composition_and_selection() {
        val eb = EditingBuffer("ABCDE", TextRange(1, 3)) // select "BC"

        eb.setComposition(2, 4) // Mark "CD" as composition
        CommitTextCommand("X", 1).applyTo(eb)

        // If composition and selection exists at the same time, replace composition and cancel
        // selection and place cursor.
        assertEquals("ABXE", eb.toString())
        assertEquals(3, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_cursor_position_too_small() {
        val eb = EditingBuffer("ABCDE", TextRange(5))

        CommitTextCommand("X", -1000).applyTo(eb)

        assertEquals("ABCDEX", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_cursor_position_too_large() {
        val eb = EditingBuffer("ABCDE", TextRange(5))

        CommitTextCommand("X", 1000).applyTo(eb)

        assertEquals("ABCDEX", eb.toString())
        assertEquals(6, eb.cursor)
        assertFalse(eb.hasComposition())
    }
}