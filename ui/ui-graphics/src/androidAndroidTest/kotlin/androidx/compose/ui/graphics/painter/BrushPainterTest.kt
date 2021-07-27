/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.graphics.painter

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class BrushPainterTest {

    private fun createImageBitmap(): ImageBitmap {
        val image = ImageBitmap(100, 100)
        Canvas(image).drawRect(
            0f,
            0f,
            100f,
            100f,
            Paint().apply { color = Color.White }
        )
        return image
    }

    @Test
    fun testBrushPainter() {
        val brushPainter = BrushPainter(
            Brush.verticalGradient(
                0.0f to Color.Red,
                0.5f to Color.Red,
                0.5f to Color.Blue,
                1.0f to Color.Blue
            )
        )
        val image = createImageBitmap()
        drawPainter(brushPainter, Canvas(image), Size(100f, 100f))
        val pixelMap = image.toPixelMap()
        assertEquals(Color.Red, pixelMap[0, 0])
        assertEquals(Color.Red, pixelMap[99, 0])
        assertEquals(Color.Red, pixelMap[0, 49])
        assertEquals(Color.Red, pixelMap[99, 49])

        assertEquals(Color.Blue, pixelMap[0, 50])
        assertEquals(Color.Blue, pixelMap[99, 50])
        assertEquals(Color.Blue, pixelMap[0, 99])
        assertEquals(Color.Blue, pixelMap[99, 99])
    }

    @Test
    fun testBrushPainterAlphaApplied() {
        val brushPainter = BrushPainter(
            Brush.verticalGradient(
                0.0f to Color.Red,
                0.5f to Color.Red,
                0.5f to Color.Blue,
                1.0f to Color.Blue
            )
        )
        val image = createImageBitmap()
        drawPainter(brushPainter, Canvas(image), Size(100f, 100f), alpha = 0.5f)

        val expectedRed = Color(
            alpha = 0.5f,
            red = Color.Red.red,
            green = 0f,
            blue = 0f
        ).compositeOver(Color.White)

        val expectedBlue = Color(
            alpha = 0.5f,
            red = 0f,
            green = 0f,
            blue = Color.Blue.blue
        ).compositeOver(Color.White)

        val pixelMap = image.toPixelMap()
        assertEquals(expectedRed, pixelMap[0, 0])
        assertEquals(expectedRed, pixelMap[99, 0])
        assertEquals(expectedRed, pixelMap[0, 49])
        assertEquals(expectedRed, pixelMap[99, 49])

        assertEquals(expectedBlue, pixelMap[0, 50])
        assertEquals(expectedBlue, pixelMap[99, 50])
        assertEquals(expectedBlue, pixelMap[0, 99])
        assertEquals(expectedBlue, pixelMap[99, 99])
    }

    @Test
    fun testBrushPainterTint() {
        val brushPainter = BrushPainter(
            Brush.verticalGradient(
                0.0f to Color.Red,
                0.5f to Color.Red,
                0.5f to Color.Blue,
                1.0f to Color.Blue
            )
        )
        val image = createImageBitmap()
        drawPainter(
            brushPainter,
            Canvas(image),
            Size(100f, 100f),
            colorFilter = ColorFilter.tint(Color.Cyan, BlendMode.SrcIn)
        )
        val pixelMap = image.toPixelMap()
        assertEquals(Color.Cyan, pixelMap[0, 0])
        assertEquals(Color.Cyan, pixelMap[99, 0])
        assertEquals(Color.Cyan, pixelMap[0, 49])
        assertEquals(Color.Cyan, pixelMap[99, 49])

        assertEquals(Color.Cyan, pixelMap[0, 50])
        assertEquals(Color.Cyan, pixelMap[99, 50])
        assertEquals(Color.Cyan, pixelMap[0, 99])
        assertEquals(Color.Cyan, pixelMap[99, 99])
    }

    @Test
    fun testBrushPainterEquals() {
        val brush1 = Brush.verticalGradient(
            0.0f to Color.Red,
            0.3f to Color.Blue,
            0.7f to Color.Green
        )

        val brush2 = Brush.verticalGradient(
            0.0f to Color.Red,
            0.3f to Color.Blue,
            0.7f to Color.Green
        )

        assertEquals(BrushPainter(brush1), BrushPainter(brush2))
    }

    @Test
    fun testBrushPainterHashCode() {
        val brush = Brush.horizontalGradient(listOf(Color.Red, Color.Blue, Color.Yellow))
        assertEquals(BrushPainter(brush).hashCode(), brush.hashCode())
    }

    @Test
    fun testBrushPainterToString() {
        val brush = Brush.verticalGradient(
            listOf(Color.White, Color.Black, Color.Gray, Color.LightGray)
        )
        assertEquals("BrushPainter(brush=$brush)", BrushPainter(brush).toString())
    }

    @Test
    fun testBrushPainterIntrinsicSize() {
        val brush = Brush.verticalGradient(
            listOf(Color.White, Color.Black),
            startY = 0f,
            endY = 100f
        )
        assertEquals(Size(0.0f, 100f), BrushPainter(brush).intrinsicSize)
    }
}