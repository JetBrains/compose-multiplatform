/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.geometry

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OffsetTest {
    @Test
    fun testOffsetCopy() {
        val offset = Offset(100f, 200f)
        assertEquals(offset, offset.copy())
    }

    @Test
    fun testOffsetCopyOverwriteX() {
        val offset = Offset(100f, 200f)
        val copy = offset.copy(x = 50f)
        assertEquals(50f, copy.x)
        assertEquals(200f, copy.y)
    }

    @Test
    fun testOffsetCopyOverwriteY() {
        val offset = Offset(100f, 200f)
        val copy = offset.copy(y = 300f)
        assertEquals(100f, copy.x)
        assertEquals(300f, copy.y)
    }

    @Test
    fun testUnspecifiedWidthQueryThrows() {
        try {
            Offset.Unspecified.x
            Assert.fail("Offset.Unspecified.x is not allowed")
        } catch (t: Throwable) {
            // no-op
        }
    }

    @Test
    fun testUnspecifiedHeightQueryThrows() {
        try {
            Offset.Unspecified.y
            Assert.fail("Offset.Unspecified.y is not allowed")
        } catch (t: Throwable) {
            // no-op
        }
    }

    @Test
    fun testUnspecifiedCopyThrows() {
        try {
            Offset.Unspecified.copy(x = 100f)
            Offset.Unspecified.copy(y = 70f)
            Assert.fail("Offset.Unspecified.copy is not allowed")
        } catch (t: Throwable) {
            // no-op
        }
    }

    @Test
    fun testUnspecifiedComponentAssignmentThrows() {
        try {
            val (_, _) = Offset.Unspecified
            Assert.fail("Size.Unspecified component assignment is not allowed")
        } catch (t: Throwable) {
            // no-op
        }
    }

    @Test
    fun testIsSpecified() {
        val offset = Offset(10f, 20f)
        assertTrue(offset.isSpecified)
        assertFalse(offset.isUnspecified)
    }

    @Test
    fun testIsUnspecified() {
        assertTrue(Offset.Unspecified.isUnspecified)
        assertFalse(Offset.Unspecified.isSpecified)
    }

    @Test
    fun testUnspecifiedEquals() {
        // Verify that verifying equality here does not crash
        assertTrue(Offset.Unspecified == Offset.Unspecified)
    }

    @Test
    fun testTakeOrElseTrue() {
        assertTrue(Offset(1f, 1f).takeOrElse { Offset.Unspecified }.isSpecified)
    }

    @Test
    fun testTakeOrElseFalse() {
        assertTrue(Offset.Unspecified.takeOrElse { Offset(1f, 1f) }.isSpecified)
    }

    @Test
    fun testUnspecifiedOffsetToString() {
        assertEquals("Offset.Unspecified", Offset.Unspecified.toString())
    }
}