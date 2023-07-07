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
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SizeTest {

    @Test
    fun sizeTimesInt() {
        Assert.assertEquals(
            Size(10f, 10f),
            Size(2.5f, 2.5f) * 4f
        )
        Assert.assertEquals(
            Size(10f, 10f),
            4f * Size(2.5f, 2.5f)
        )
    }

    @Test
    fun sizeDivInt() {
        Assert.assertEquals(
            Size(10f, 10f),
            Size(40f, 40f) / 4f
        )
    }

    @Test
    fun sizeTimesFloat() {
        Assert.assertEquals(Size(10f, 10f), Size(4f, 4f) * 2.5f)
        Assert.assertEquals(Size(10f, 10f), 2.5f * Size(4f, 4f))
    }

    @Test
    fun sizeDivFloat() {
        Assert.assertEquals(Size(10f, 10f), Size(40f, 40f) / 4f)
    }

    @Test
    fun sizeTimesDouble() {
        Assert.assertEquals(Size(10f, 10f), Size(4f, 4f) * 2.5f)
        Assert.assertEquals(Size(10f, 10f), 2.5f * Size(4f, 4f))
    }

    @Test
    fun sizeDivDouble() {
        Assert.assertEquals(
            Size(10f, 10f),
            Size(40f, 40f) / 4.0f
        )
    }

    @Test
    fun testSizeCopy() {
        val size = Size(100f, 200f)
        Assert.assertEquals(size, size.copy())
    }

    @Test
    fun testSizeCopyOverwriteWidth() {
        val size = Size(100f, 200f)
        val copy = size.copy(width = 50f)
        Assert.assertEquals(50f, copy.width)
        Assert.assertEquals(200f, copy.height)
    }

    @Test
    fun testSizeCopyOverwriteHeight() {
        val size = Size(100f, 200f)
        val copy = size.copy(height = 300f)
        Assert.assertEquals(100f, copy.width)
        Assert.assertEquals(300f, copy.height)
    }

    @Test
    fun testUnspecifiedWidthQueryThrows() {
        try {
            Size.Unspecified.width
            fail("Size.Unspecified.width is not allowed")
        } catch (t: Throwable) {
            // no-op
        }
    }

    @Test
    fun testUnspecifiedHeightQueryThrows() {
        try {
            Size.Unspecified.height
            fail("Size.Unspecified.height is not allowed")
        } catch (t: Throwable) {
            // no-op
        }
    }

    @Test
    fun testUnspecifiedCopyThrows() {
        try {
            Size.Unspecified.copy(width = 100f)
            Size.Unspecified.copy(height = 70f)
            fail("Size.Unspecified.copy is not allowed")
        } catch (t: Throwable) {
            // no-op
        }
    }

    @Test
    fun testUnspecifiedComponentAssignmentThrows() {
        try {
            val (_, _) = Size.Unspecified
            fail("Size.Unspecified component assignment is not allowed")
        } catch (t: Throwable) {
            // no-op
        }
    }

    @Test
    fun testSizeLerp() {
        val size1 = Size(100f, 200f)
        val size2 = Size(300f, 500f)
        Assert.assertEquals(Size(200f, 350f), lerp(size1, size2, 0.5f))
    }

    @Test
    fun testIsSpecified() {
        Assert.assertFalse(Size.Unspecified.isSpecified)
        Assert.assertTrue(Size(1f, 1f).isSpecified)
    }

    @Test
    fun testIsUnspecified() {
        Assert.assertTrue(Size.Unspecified.isUnspecified)
        Assert.assertFalse(Size(1f, 1f).isUnspecified)
    }

    @Test
    fun testTakeOrElseTrue() {
        Assert.assertTrue(Size(1f, 1f).takeOrElse { Size.Unspecified }.isSpecified)
    }

    @Test
    fun testTakeOrElseFalse() {
        Assert.assertTrue(Size.Unspecified.takeOrElse { Size(1f, 1f) }.isSpecified)
    }

    @Test
    fun testUnspecifiedSizeToString() {
        Assert.assertEquals("Size.Unspecified", Size.Unspecified.toString())
    }

    @Test
    fun testSpecifiedSizeToString() {
        Assert.assertEquals("Size(10.0, 20.0)", Size(10f, 20f).toString())
    }
}