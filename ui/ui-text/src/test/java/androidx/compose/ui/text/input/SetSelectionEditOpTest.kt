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
class SetSelectionEditOpTest {

    @Test
    fun test_set() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        SetSelectionEditOp(1, 4).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(1, eb.selectionStart)
        assertEquals(4, eb.selectionEnd)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_preserve_ongoing_composition() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 3)

        SetSelectionEditOp(2, 4).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(2, eb.selectionStart)
        assertEquals(4, eb.selectionEnd)
        assertTrue(eb.hasComposition())
        assertEquals(1, eb.compositionStart)
        assertEquals(3, eb.compositionEnd)
    }

    @Test
    fun test_cancel_ongoing_selection() {
        val eb = EditingBuffer("ABCDE", TextRange(1, 4))

        SetSelectionEditOp(2, 5).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(2, eb.selectionStart)
        assertEquals(5, eb.selectionEnd)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_set_reversed() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        SetSelectionEditOp(4, 1).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(1, eb.selectionStart)
        assertEquals(4, eb.selectionEnd)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_set_too_small() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        SetSelectionEditOp(-1000, -1000).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_set_too_large() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        SetSelectionEditOp(1000, 1000).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(5, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_set_too_small_too_large() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        SetSelectionEditOp(-1000, 1000).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(0, eb.selectionStart)
        assertEquals(5, eb.selectionEnd)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_set_too_small_too_large_reversed() {
        val eb = EditingBuffer("ABCDE", TextRange.Zero)

        SetSelectionEditOp(1000, -1000).process(eb)

        assertEquals("ABCDE", eb.toString())
        assertEquals(0, eb.selectionStart)
        assertEquals(5, eb.selectionEnd)
        assertFalse(eb.hasComposition())
    }
}