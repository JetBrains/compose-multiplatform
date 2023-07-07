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

package androidx.compose.ui.graphics.painter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
class PainterTest {

    val size = Size(100.0f, 100.0f)

    @Test
    fun testPainterDidDraw() {
        val p = object : Painter() {

            var didDraw: Boolean = false

            override val intrinsicSize: Size
                get() = size

            override fun DrawScope.onDraw() {
                didDraw = true
            }
        }

        assertEquals(size, p.intrinsicSize)
        assertFalse(p.didDraw)

        drawPainter(
            p,
            Canvas(ImageBitmap(100, 100)),
            Size(100f, 100f)
        )
        assertTrue(p.didDraw)
    }

    @Test
    fun testPainterRtl() {
        val p = object : Painter() {

            var color = Color.Black

            override val intrinsicSize: Size
                get() = size

            override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean {
                color = if (layoutDirection == LayoutDirection.Rtl) Color.Red else Color.Cyan
                return true
            }

            override fun DrawScope.onDraw() {
                drawRect(color = color)
            }
        }

        val image = ImageBitmap(100, 100)

        drawPainter(
            p,
            Canvas(image),
            Size(100f, 100f),
            layoutDirection = LayoutDirection.Rtl
        )

        assertEquals(Color.Red, image.toPixelMap()[50, 50])
    }

    @Test
    fun testPainterAlpha() {
        val p = object : Painter() {

            override val intrinsicSize: Size
                get() = size

            override fun DrawScope.onDraw() {
                drawRect(color = Color.Red)
            }
        }

        val image = ImageBitmap(100, 100)
        val canvas = Canvas(image)

        val paint = Paint().apply { this.color = Color.White }
        canvas.drawRect(Rect(Offset.Zero, Size(100.0f, 100.0f)), paint)

        drawPainter(
            p,
            canvas,
            size,
            alpha = 0.5f
        )

        val expected = Color(
            alpha = 0.5f,
            red = Color.Red.red,
            green = Color.Red.green,
            blue = Color.Red.blue
        ).compositeOver(Color.White)

        val result = image.toPixelMap()[50, 50]
        assertEquals(expected.red, result.red, 0.01f)
        assertEquals(expected.green, result.green, 0.01f)
        assertEquals(expected.blue, result.blue, 0.01f)
        assertEquals(expected.alpha, result.alpha, 0.01f)
    }

    @Test
    fun testPainterCustomAlpha() {
        val p = object : Painter() {

            var color = Color.Red

            override fun applyAlpha(alpha: Float): Boolean {
                color =
                    Color(
                        alpha = alpha,
                        red = Color.Red.red,
                        blue = Color.Red.blue,
                        green = Color.Red.green
                    )
                return true
            }

            override val intrinsicSize: Size
                get() = size

            override fun DrawScope.onDraw() {
                drawRect(color = color)
            }
        }

        assertEquals(Color.Red, p.color)
        val image = ImageBitmap(100, 100)
        val canvas = Canvas(image)

        val paint = Paint().apply { this.color = Color.White }
        canvas.drawRect(Rect(Offset.Zero, Size(100.0f, 100.0f)), paint)

        drawPainter(
            p,
            canvas,
            size,
            alpha = 0.5f
        )

        val expected = Color(
            alpha = 0.5f,
            red = Color.Red.red,
            green = Color.Red.green,
            blue = Color.Red.blue
        ).compositeOver(Color.White)

        val result = image.toPixelMap()[50, 50]
        assertEquals(expected.red, result.red, 0.01f)
        assertEquals(expected.green, result.green, 0.01f)
        assertEquals(expected.blue, result.blue, 0.01f)
        assertEquals(expected.alpha, result.alpha, 0.01f)
    }

    @Test
    fun testColorFilter() {
        val p = object : Painter() {

            var colorFilter: ColorFilter? = ColorFilter.tint(Color.Red, BlendMode.SrcIn)

            override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
                this.colorFilter = colorFilter
                return true
            }

            override val intrinsicSize: Size
                get() = size

            override fun DrawScope.onDraw() {
                drawRect(color = Color.Black, colorFilter = colorFilter)
            }
        }

        val image = ImageBitmap(100, 100)

        drawPainter(
            p,
            Canvas(image),
            size,
            colorFilter = ColorFilter.tint(Color.Blue, BlendMode.SrcIn)
        )
        assertEquals(Color.Blue, image.toPixelMap()[50, 50])
    }
}