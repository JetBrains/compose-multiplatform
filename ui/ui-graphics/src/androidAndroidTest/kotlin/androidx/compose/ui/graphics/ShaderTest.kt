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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@SmallTest
@RunWith(AndroidJUnit4::class)
class ShaderTest {

    @Test
    fun testLinearGradient() {
        val imageBitmap = ImageBitmap(100, 100)
        imageBitmap.drawInto {
            drawRect(
                brush = Brush.linearGradient(
                    0.0f to Color.Red,
                    0.5f to Color.Red,
                    0.5f to Color.Blue,
                    1.0f to Color.Blue,
                    start = Offset.Zero,
                    end = Offset(0.0f, 100f),
                    tileMode = TileMode.Clamp
                )
            )
        }

        val pixelMap = imageBitmap.toPixelMap()
        val centerX = imageBitmap.width / 2
        val centerY = imageBitmap.height / 2
        assertEquals(Color.Red, pixelMap[centerX, centerY - 5])
        assertEquals(Color.Blue, pixelMap[centerX, centerY + 5])
        assertEquals(Color.Red, pixelMap[5, centerY - 5])
        assertEquals(Color.Blue, pixelMap[5, centerY + 5])
        assertEquals(Color.Red, pixelMap[imageBitmap.width - 5, centerY - 5])
        assertEquals(Color.Blue, pixelMap[imageBitmap.width - 5, centerY + 5])
    }

    @Test
    fun testRadialGradient() {
        val imageBitmap = ImageBitmap(100, 100)

        imageBitmap.drawInto {
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color.Red,
                    0.5f to Color.Red,
                    0.5f to Color.Blue,
                    1.0f to Color.Blue,
                    center = Offset(50f, 50f),
                    radius = 50f,
                    tileMode = TileMode.Clamp
                )
            )
        }

        val pixelMap = imageBitmap.toPixelMap()

        assertEquals(Color.Red, pixelMap[50, 50])
        assertEquals(Color.Red, pixelMap[50, 30])
        assertEquals(Color.Red, pixelMap[70, 50])
        assertEquals(Color.Red, pixelMap[50, 70])
        assertEquals(Color.Red, pixelMap[30, 50])

        assertEquals(Color.Blue, pixelMap[50, 20])
        assertEquals(Color.Blue, pixelMap[80, 50])
        assertEquals(Color.Blue, pixelMap[50, 80])
        assertEquals(Color.Blue, pixelMap[20, 50])
    }

    @Test
    fun testSweepGradient() {
        val imageBitmap = ImageBitmap(100, 100)
        val center = Offset(50f, 50f)
        imageBitmap.drawInto {
            drawRect(
                brush = Brush.sweepGradient(
                    0.0f to Color.Red,
                    0.5f to Color.Red,
                    0.5f to Color.Blue,
                    1.0f to Color.Blue,
                    center = center
                )
            )
        }

        val pixelMap = imageBitmap.toPixelMap()
        val centerX = center.x.roundToInt()
        val centerY = center.y.roundToInt()
        assertEquals(Color.Red, pixelMap[centerX, centerY + 5])
        assertEquals(Color.Blue, pixelMap[centerX, centerY - 5])
        assertEquals(Color.Red, pixelMap[centerX * 2 - 5, centerY + 5])
        assertEquals(Color.Blue, pixelMap[centerX * 2 - 5, centerY - 5])
        assertEquals(Color.Red, pixelMap[5, centerY + 5])
        assertEquals(Color.Blue, pixelMap[5, centerY - 5])
    }

    @Test
    fun testLinearGradientIntrinsicSize() {
        assertEquals(
            Size(100f, 200f),
            Brush.linearGradient(
                listOf(Color.Red, Color.Blue),
                start = Offset(200f, 100f),
                end = Offset(300f, 300f)
            ).intrinsicSize
        )
    }

    @Test
    fun testLinearGradientNegativePosition() {
        assertEquals(
            Size(100f, 200f),
            Brush.linearGradient(
                listOf(Color.Red, Color.Blue),
                start = Offset(200f, 100f),
                end = Offset(100f, -100f)
            ).intrinsicSize
        )
    }

    @Test
    fun testLinearGradientInfiniteWidth() {
        assertEquals(
            Size(Float.NaN, 200f),
            Brush.linearGradient(
                listOf(Color.Red, Color.Blue),
                start = Offset(Float.POSITIVE_INFINITY, 100f),
                end = Offset(Float.POSITIVE_INFINITY, 300f)
            ).intrinsicSize
        )
    }

    @Test
    fun testLinearGradientInfiniteHeight() {
        assertEquals(
            Size(100f, Float.NaN),
            Brush.linearGradient(
                listOf(Color.Red, Color.Blue),
                start = Offset(100f, 0f),
                end = Offset(200f, Float.POSITIVE_INFINITY)
            ).intrinsicSize
        )
    }

    @Test
    fun testSweepGradientIntrinsicSize() {
        // Sweep gradients do not have an intrinsic size as they sweep/fill the geometry they are
        // drawn with
        assertEquals(
            Size.Unspecified,
            Brush.sweepGradient(listOf(Color.Red, Color.Blue)).intrinsicSize
        )
    }

    @Test
    fun testRadialGradientIntrinsicSize() {
        assertEquals(
            Size(100f, 100f),
            Brush.radialGradient(
                listOf(Color.Red, Color.Blue),
                radius = 50f
            ).intrinsicSize
        )
    }

    @Test
    fun testRadialGradientInfiniteSize() {
        assertEquals(
            Size.Unspecified,
            Brush.radialGradient(listOf(Color.Red, Color.Blue)).intrinsicSize
        )
    }

    private fun ImageBitmap.drawInto(
        block: DrawScope.() -> Unit
    ) = CanvasDrawScope().draw(
        Density(1.0f),
        LayoutDirection.Ltr,
        Canvas(this),
        Size(width.toFloat(), height.toFloat()),
        block
    )
}