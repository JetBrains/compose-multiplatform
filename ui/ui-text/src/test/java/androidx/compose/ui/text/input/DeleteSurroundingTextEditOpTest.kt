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
class DeleteSurroundingTextEditOpTest {

    @Test
    fun test_delete_after() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        DeleteSurroundingTextEditOp(0, 1).process(eb)

        assertEquals("ACDE", eb.toString())
        assertEquals(1, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_before() {
        val eb = EditingBuffer("ABCDE", TextRange(1))

        DeleteSurroundingTextEditOp(1, 0).process(eb)

        assertEquals("BCDE", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_both() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        DeleteSurroundingTextEditOp(1, 1).process(eb)

        assertEquals("ABE", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_after_multiple() {
        val eb = EditingBuffer("ABCDE", TextRange(2))

        DeleteSurroundingTextEditOp(0, 2).process(eb)

        assertEquals("ABE", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_before_multiple() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        DeleteSurroundingTextEditOp(2, 0).process(eb)

        assertEquals("ADE", eb.toString())
        assertEquals(1, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_both_multiple() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        DeleteSurroundingTextEditOp(2, 2).process(eb)

        assertEquals("A", eb.toString())
        assertEquals(1, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_selection_preserve() {
        val eb = EditingBuffer("ABCDE", TextRange(2, 4))

        DeleteSurroundingTextEditOp(1, 1).process(eb)

        assertEquals("ACD", eb.toString())
        assertEquals(1, eb.selectionStart)
        assertEquals(3, eb.selectionEnd)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_before_too_many() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        DeleteSurroundingTextEditOp(1000, 0).process(eb)

        assertEquals("DE", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_after_too_many() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        DeleteSurroundingTextEditOp(0, 1000).process(eb)

        assertEquals("ABC", eb.toString())
        assertEquals(3, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_both_too_many() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        DeleteSurroundingTextEditOp(1000, 1000).process(eb)

        assertEquals("", eb.toString())
        assertEquals(0, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_composition_no_intersection_preceding_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.setComposition(0, 1)

        DeleteSurroundingTextEditOp(1, 1).process(eb)

        assertEquals("ABE", eb.toString())
        assertEquals(2, eb.cursor)
        assertEquals(0, eb.compositionStart)
        assertEquals(1, eb.compositionEnd)
    }

    @Test
    fun test_delete_composition_no_intersection_trailing_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.setComposition(4, 5)

        DeleteSurroundingTextEditOp(1, 1).process(eb)

        assertEquals("ABE", eb.toString())
        assertEquals(2, eb.cursor)
        assertEquals(2, eb.compositionStart)
        assertEquals(3, eb.compositionEnd)
    }

    @Test
    fun test_delete_composition_intersection_preceding_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.setComposition(0, 3)

        DeleteSurroundingTextEditOp(1, 1).process(eb)

        assertEquals("ABE", eb.toString())
        assertEquals(2, eb.cursor)
        assertEquals(0, eb.compositionStart)
        assertEquals(2, eb.compositionEnd)
    }

    @Test
    fun test_delete_composition_intersection_trailing_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.setComposition(3, 5)

        DeleteSurroundingTextEditOp(1, 1).process(eb)

        assertEquals("ABE", eb.toString())
        assertEquals(2, eb.cursor)
        assertEquals(2, eb.compositionStart)
        assertEquals(3, eb.compositionEnd)
    }

    @Test
    fun test_delete_covered_composition() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.setComposition(2, 3)

        DeleteSurroundingTextEditOp(1, 1).process(eb)

        assertEquals("ABE", eb.toString())
        assertEquals(2, eb.cursor)
        assertFalse(eb.hasComposition())
    }

    @Test
    fun test_delete_composition_covered() {
        val eb = EditingBuffer("ABCDE", TextRange(3))

        eb.setComposition(0, 5)

        DeleteSurroundingTextEditOp(1, 1).process(eb)

        assertEquals("ABE", eb.toString())
        assertEquals(2, eb.cursor)
        assertEquals(0, eb.compositionStart)
        assertEquals(3, eb.compositionEnd)
    }
}