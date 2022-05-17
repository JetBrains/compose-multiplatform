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

package androidx.compose.testutils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ImageAssertionsTest {
    companion object {
        private const val Red = 0xFFFF0000.toInt()
        private const val Green = 0xFF00FF00.toInt()
        private const val Height = 16
        private const val Width = 4

        private val testPixels = Array(Height) { y ->
            IntArray(Width) { x ->
                when (x) {
                    3 -> (0x10 * y).shl(24) // alpha
                    else -> (0x10 * y).shl(8 * x) or 0xFF.shl(24) // rgb
                }
            }
        }
    }

    private fun createTestBitmap(): ImageBitmap {
        val srcPixels = IntArray(Height * Width) { i ->
            val y = i / Width
            val x = i % Width
            testPixels[y][x]
        }
        val bitmap = Bitmap.createBitmap(Width, Height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(srcPixels, 0, Width, 0, 0, Width, Height)
        return bitmap.asImageBitmap()
    }

    /**
     * Tests if the size verification of assertPixels works
     */
    @Test
    fun assertPixels_size() {
        val bitmap = createTestBitmap()
        bitmap.assertPixels(IntSize(Width, Height)) { null }
    }

    /**
     * Tests if the color verification of assertPixels works.
     * Exercises all channels (RGBA) with values running from 0x00 to 0xF0.
     */
    @Test
    fun assertPixels_colors() {
        val bitmap = createTestBitmap()
        bitmap.assertPixels { Color(testPixels[it.y][it.x]) }
    }

    /**
     * Tests the error message when size verification of assertPixels detects a failure
     */
    @Test
    fun assertPixels_wrongSize() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
        expectError<AssertionError>(
            expectedMessage = "Bitmap size is wrong! Expected '2 x 2' but got '1 x 1'.*"
        ) {
            bitmap.assertPixels(IntSize(2, 2)) { null }
        }
    }

    /**
     * Tests the error message when color verification of assertPixels detects a failure
     */
    @Test
    fun assertPixels_wrongColor() {
        val rawBitmap = Bitmap.createBitmap(1, 2, Bitmap.Config.ARGB_8888)
        rawBitmap.setPixel(0, 0, Red)
        rawBitmap.setPixel(0, 1, Green)
        val bitmap = rawBitmap.asImageBitmap()

        expectError<AssertionError>(
            expectedMessage = "Pixel\\(0, 1\\) expected to be " +
                "Color\\(1.0, 0.0, 0.0, 1.0, .*\\), but was " +
                "Color\\(0.0, 1.0, 0.0, 1.0, .*\\).*"
        ) {
            bitmap.assertPixels { Color(Red) }
        }
    }

    /**
     * Tests if assertContainsColor works
     */
    @Test
    fun assertContainsColor() {
        val rawBitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        rawBitmap.setPixel(1, 1, Red)
        val bitmap = rawBitmap.asImageBitmap()
        bitmap.assertContainsColor(Color(Red))
    }

    /**
     * Tests the error message when assertContainsColor detects a failure
     */
    @Test
    fun assertContainsColor_wrongColor() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).asImageBitmap()
        expectError<AssertionError>(
            expectedMessage = "The given color Color\\(1.0, 0.0, 0.0, 1.0, .*\\) " +
                "was not found in the bitmap."
        ) {
            bitmap.assertContainsColor(Color(Red))
        }
    }
}
