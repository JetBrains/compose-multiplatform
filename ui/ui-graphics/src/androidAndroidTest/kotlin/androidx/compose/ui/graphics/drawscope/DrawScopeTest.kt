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

package androidx.compose.ui.graphics.drawscope

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@SmallTest
@RunWith(AndroidJUnit4::class)
class DrawScopeTest {

    private val width: Int = 100
    private val height: Int = 100
    private val dstSize = Size(width.toFloat(), height.toFloat())

    private fun createTestDstImage(): ImageBitmap {
        val dst = ImageBitmap(width, height)
        val dstCanvas = Canvas(dst)
        val dstPaint = Paint().apply {
            this.color = Color.White
        }
        dstCanvas.drawRect(
            Rect(Offset.Zero, Size(200.0f, 200.0f)),
            dstPaint
        )
        return dst
    }

    @Test
    fun testDrawRectColor() {
        val img = createTestDstImage()
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            // Verify that the overload that consumes a color parameter
            // fills the canvas with red color
            drawRect(color = Color.Red)
        }

        val pixelMap = img.toPixelMap()
        for (i in 0 until pixelMap.width) {
            for (j in 0 until pixelMap.height) {
                assertEquals(Color.Red, pixelMap[i, j])
            }
        }
    }

    @Test
    fun testDrawRectBrushColor() {
        val img = createTestDstImage()
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            // Verify that the overload that consumes a brush parameter
            // fills the canvas with red color
            drawRect(brush = SolidColor(Color.Red))
        }

        val pixelMap = img.toPixelMap()
        for (i in 0 until pixelMap.width) {
            for (j in 0 until pixelMap.height) {
                assertEquals(Color.Red, pixelMap[i, j])
            }
        }
    }

    @Test
    fun testDrawOvalBrush() {
        val width = 200
        val height = 200

        // Test that colors are rendered with the correct stroke parameters
        testDrawScopeAndCanvasAreEquivalent(
            width,
            height,
            {
                drawOval(
                    brush = Brush.linearGradient(listOf(Color.Red, Color.Blue)),
                    topLeft = Offset(10f, 10f),
                    size = Size(width - 20f, height - 20f)
                )
            },
            { canvas ->
                canvas.drawOval(
                    10f,
                    10f,
                    width - 10f,
                    height - 10f,
                    Paint().apply {
                        shader =
                            LinearGradientShader(
                                Offset.Zero,
                                Offset(width.toFloat(), height.toFloat()),
                                listOf(Color.Red, Color.Blue)
                            )
                    }
                )
            }
        )
    }

    @Test
    fun testDrawOvalColor() {
        val width = 200
        val height = 200

        // Test that colors are rendered with the correct stroke parameters
        testDrawScopeAndCanvasAreEquivalent(
            width,
            height,
            {
                drawOval(
                    color = Color.Cyan,
                    topLeft = Offset(10f, 10f),
                    size = Size(width - 20f, height - 20f)
                )
            },
            { canvas ->
                canvas.drawOval(
                    10f,
                    10f,
                    width - 10f,
                    height - 10f,
                    Paint().apply { color = Color.Cyan }
                )
            }
        )
    }

    @Test
    fun testDrawRectColorAlpha() {
        val img = createTestDstImage()
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            // Verify that the overload that consumes a color parameter
            // fills the canvas with red color
            drawRect(color = Color.Red, alpha = 0.5f)
        }

        val expected = Color(
            alpha = 0.5f,
            red = Color.Red.red,
            green = Color.Red.green,
            blue = Color.Red.blue
        ).compositeOver(Color.White)

        val pixelMap = img.toPixelMap()
        for (i in 0 until pixelMap.width) {
            for (j in 0 until pixelMap.height) {
                val result = pixelMap[i, j]
                assertEquals(expected.red, result.red, 0.01f)
                assertEquals(expected.green, result.green, 0.01f)
                assertEquals(expected.blue, result.blue, 0.01f)
                assertEquals(expected.alpha, result.alpha, 0.01f)
            }
        }
    }

    @Test
    fun testDrawRectBrushColorAlpha() {
        val img = createTestDstImage()
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            // Verify that the overload that consumes a brush parameter
            // fills the canvas with red color
            drawRect(brush = SolidColor(Color.Red), alpha = 0.5f)
        }

        val expected = Color(
            alpha = 0.5f,
            red = Color.Red.red,
            green = Color.Red.green,
            blue = Color.Red.blue
        ).compositeOver(Color.White)

        val pixelMap = img.toPixelMap()
        for (i in 0 until pixelMap.width) {
            for (j in 0 until pixelMap.height) {
                val result = pixelMap[i, j]
                assertEquals(expected.red, result.red, 0.01f)
                assertEquals(expected.green, result.green, 0.01f)
                assertEquals(expected.blue, result.blue, 0.01f)
                assertEquals(expected.alpha, result.alpha, 0.01f)
            }
        }
    }

    @Test
    fun testDrawRectColorIntrinsicAlpha() {
        val img = createTestDstImage()
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            // Verify that the overload that consumes a color parameter
            // fills the canvas with red color
            drawRect(
                color =
                    Color.Red.copy(
                        alpha = 0.5f,
                        red = Color.Red.red,
                        green = Color.Red.green,
                        blue = Color.Red.blue
                    )
            )
        }

        val expected = Color(
            alpha = 0.5f,
            red = Color.Red.red,
            green = Color.Red.green,
            blue = Color.Red.blue
        ).compositeOver(Color.White)

        val pixelMap = img.toPixelMap()
        for (i in 0 until pixelMap.width) {
            for (j in 0 until pixelMap.height) {
                val result = pixelMap[i, j]
                assertEquals(expected.red, result.red, 0.01f)
                assertEquals(expected.green, result.green, 0.01f)
                assertEquals(expected.blue, result.blue, 0.01f)
                assertEquals(expected.alpha, result.alpha, 0.01f)
            }
        }
    }

    @Test
    fun testDrawRectBrushColorIntrinsicAlpha() {
        val img = createTestDstImage()
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            // Verify that the overload that consumes a brush parameter
            // fills the canvas with red color
            drawRect(
                brush =
                    SolidColor(
                        Color.Red.copy(
                            alpha = 0.5f,
                            red = Color.Red.red,
                            green = Color.Red.green,
                            blue = Color.Red.blue
                        )
                    )
            )
        }

        val expected = Color(
            alpha = 0.5f,
            red = Color.Red.red,
            green = Color.Red.green,
            blue = Color.Red.blue
        ).compositeOver(Color.White)

        val pixelMap = img.toPixelMap()
        for (i in 0 until pixelMap.width) {
            for (j in 0 until pixelMap.height) {
                val result = pixelMap[i, j]
                assertEquals(expected.red, result.red, 0.01f)
                assertEquals(expected.green, result.green, 0.01f)
                assertEquals(expected.blue, result.blue, 0.01f)
                assertEquals(expected.alpha, result.alpha, 0.01f)
            }
        }
    }

    @Test
    fun testDrawTranslatedRect() {
        val img = createTestDstImage()
        val insetLeft = 10.0f
        val insetTop = 12.0f
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            translate(insetLeft, insetTop) {
                drawRect(color = Color.Red)
            }
        }

        val pixelMap = img.toPixelMap()
        for (i in 0 until pixelMap.width) {
            for (j in 0 until pixelMap.height) {
                val expectedColor =
                    if (i >= insetLeft && j >= insetTop) {
                        Color.Red
                    } else {
                        Color.White
                    }
                assertEquals("Coordinate: " + i + ", " + j, expectedColor, pixelMap[i, j])
            }
        }
    }

    @Test
    fun testDrawInsetRect() {
        val img = createTestDstImage()
        val insetLeft = 10.0f
        val insetTop = 12.0f
        val insetRight = 11.0f
        val insetBottom = 13.0f
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            inset(insetLeft, insetTop, insetRight, insetBottom) {
                drawRect(color = Color.Red)
            }
        }

        val pixelMap = img.toPixelMap()
        for (i in 0 until pixelMap.width) {
            for (j in 0 until pixelMap.height) {
                val expectedColor =
                    if (i >= insetLeft && i < pixelMap.width - insetRight &&
                        j >= insetTop && j < pixelMap.height - insetBottom
                    ) {
                        Color.Red
                    } else {
                        Color.White
                    }
                assertEquals("Coordinate: " + i + ", " + j, expectedColor, pixelMap[i, j])
            }
        }
    }

    @Test
    fun testDrawInsetHorizontalVertical() {
        val img = createTestDstImage()
        val insetHorizontal = 10.0f
        val insetVertical = 12.0f
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            inset(insetHorizontal, insetVertical, insetHorizontal, insetVertical) {
                drawRect(color = Color.Red)
            }
        }

        val pixelMap = img.toPixelMap()
        for (i in 0 until pixelMap.width) {
            for (j in 0 until pixelMap.height) {
                val expectedColor =
                    if (i >= insetHorizontal && i < pixelMap.width - insetHorizontal &&
                        j >= insetVertical && j < pixelMap.height - insetVertical
                    ) {
                        Color.Red
                    } else {
                        Color.White
                    }
                assertEquals("Coordinate: " + i + ", " + j, expectedColor, pixelMap[i, j])
            }
        }
    }

    @Test
    fun testDrawInsetAll() {
        val img = createTestDstImage()
        val insetAll = 10.0f
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            inset(insetAll) {
                drawRect(color = Color.Red)
            }
        }

        val pixelMap = img.toPixelMap()
        for (i in 0 until pixelMap.width) {
            for (j in 0 until pixelMap.height) {
                val expectedColor =
                    if (i >= insetAll && i < pixelMap.width - insetAll &&
                        j >= insetAll && j < pixelMap.height - insetAll
                    ) {
                        Color.Red
                    } else {
                        Color.White
                    }
                assertEquals("Coordinate: " + i + ", " + j, expectedColor, pixelMap[i, j])
            }
        }
    }

    @Test
    fun testInsetRestoredAfterScopedInsetDraw() {
        val img = createTestDstImage()
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            // Verify that the overload that consumes a color parameter
            // fills the canvas with red color
            val left = 10.0f
            val top = 30.0f
            val right = 20.0f
            val bottom = 12.0f
            inset(left, top, right, bottom) {
                drawRect(color = Color.Red)
                assertEquals(dstSize.width - (left + right), size.width)
                assertEquals(dstSize.height - (top + bottom), size.height)
            }

            assertEquals(dstSize.width, size.width)
            assertEquals(dstSize.height, size.height)
        }
    }

    @Test
    fun testFillOverwritesOldAlpha() {
        val img = createTestDstImage()
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            // Verify that the alpha parameter used in the first draw call is overridden
            // in the subsequent call that does not specify an alpha value
            drawRect(color = Color.Blue, alpha = 0.5f)
            drawRect(color = Color.Red)
        }

        val pixelMap = img.toPixelMap()
        for (i in 0 until pixelMap.width) {
            for (j in 0 until pixelMap.height) {
                assertEquals(Color.Red, pixelMap[i, j])
            }
        }
    }

    @Test
    fun testFillOverwritesOldPaintBrushAlpha() {
        val img = createTestDstImage()
        CanvasDrawScope().draw(Canvas(img), dstSize) {
            // Verify that the alpha parameter used in the first draw call is overridden
            // in the subsequent call that does not specify an alpha value that goes through
            // a different code path for configuration of the underlying paint
            drawRect(color = Color.Blue, alpha = 0.5f)
            drawRect(brush = SolidColor(Color.Red))
        }

        val pixelMap = img.toPixelMap()
        for (i in 0 until pixelMap.width) {
            for (j in 0 until pixelMap.height) {
                assertEquals(Color.Red, pixelMap[i, j])
            }
        }
    }

    @Test
    fun testScaleTopLeftPivot() {
        val canvasScope = CanvasDrawScope()

        val width = 200
        val height = 200
        val size = Size(width.toFloat(), height.toFloat())
        val imageBitmap = ImageBitmap(width, height)

        canvasScope.draw(Canvas(imageBitmap), size) {
            drawRect(color = Color.Red)
            scale(0.5f, pivot = Offset.Zero) {
                drawRect(color = Color.Blue)
            }
        }

        val pixelMap = imageBitmap.toPixelMap()
        assertEquals(Color.Blue, pixelMap[0, 0])
        assertEquals(Color.Blue, pixelMap[99, 0])
        assertEquals(Color.Blue, pixelMap[0, 99])
        assertEquals(Color.Blue, pixelMap[99, 99])

        assertEquals(Color.Red, pixelMap[0, 100])
        assertEquals(Color.Red, pixelMap[100, 0])
        assertEquals(Color.Red, pixelMap[100, 100])
        assertEquals(Color.Red, pixelMap[100, 99])
        assertEquals(Color.Red, pixelMap[99, 100])
    }

    @Test
    fun testScaleCenterDefaultPivot() {
        val canvasScope = CanvasDrawScope()

        val width = 200
        val height = 200
        val size = Size(width.toFloat(), height.toFloat())
        val imageBitmap = ImageBitmap(width, height)

        canvasScope.draw(Canvas(imageBitmap), size) {
            drawRect(color = Color.Red)
            scale(0.5f) {
                drawRect(color = Color.Blue)
            }
        }

        val pixelMap = imageBitmap.toPixelMap()
        val left = width / 2 - 50
        val top = height / 2 - 50
        val right = width / 2 + 50 - 1
        val bottom = height / 2 + 50 - 1
        assertEquals(Color.Blue, pixelMap[left, top])
        assertEquals(Color.Blue, pixelMap[right, top])
        assertEquals(Color.Blue, pixelMap[left, bottom])
        assertEquals(Color.Blue, pixelMap[right, bottom])

        assertEquals(Color.Red, pixelMap[left - 1, top - 1])
        assertEquals(Color.Red, pixelMap[left - 1, top])
        assertEquals(Color.Red, pixelMap[left, top - 1])

        assertEquals(Color.Red, pixelMap[right + 1, top - 1])
        assertEquals(Color.Red, pixelMap[right + 1, top])
        assertEquals(Color.Red, pixelMap[right, top - 1])

        assertEquals(Color.Red, pixelMap[left - 1, bottom + 1])
        assertEquals(Color.Red, pixelMap[left - 1, bottom])
        assertEquals(Color.Red, pixelMap[left, bottom + 1])

        assertEquals(Color.Red, pixelMap[right + 1, bottom + 1])
        assertEquals(Color.Red, pixelMap[right + 1, bottom])
        assertEquals(Color.Red, pixelMap[right, bottom + 1])
    }

    @Test
    fun testInsetNegativeWidthThrows() {
        val canvasScope = CanvasDrawScope()

        val width = 200
        val height = 200
        val size = Size(width.toFloat(), height.toFloat())
        val imageBitmap = ImageBitmap(width, height)

        try {
            canvasScope.draw(Canvas(imageBitmap), size) {
                inset(100.0f, 0.0f, 101.0f, 0.0f) {
                    drawRect(color = Color.Red)
                }
            }
            fail("Width must be greater than or equal to zero after applying inset")
        } catch (e: IllegalArgumentException) {
            // no-op
        }
    }

    @Test
    fun testInsetNegativeHeightThrows() {
        val canvasScope = CanvasDrawScope()

        val width = 200
        val height = 200
        val size = Size(width.toFloat(), height.toFloat())
        val imageBitmap = ImageBitmap(width, height)

        try {
            canvasScope.draw(Canvas(imageBitmap), size) {
                inset(0.0f, 100.0f, 0.0f, 101.0f) {
                    drawRect(color = Color.Red)
                }
            }
            fail("Height must be greater than or equal to zero after applying inset")
        } catch (e: IllegalArgumentException) {
            // no-op
        }
    }

    @Test
    fun testInsetZeroHeight() {
        // Verify that the inset call does not crash even though we are adding an inset
        // to the right and bottom such that the drawing size after the inset is zero in both
        // dimensions.
        // This is useful for animations that slowly reveal the drawing bounds of the area.
        // Alternatively this could happen if the a sibling UI element
        val canvasScope = CanvasDrawScope()

        val width = 200
        val height = 200
        val size = Size(width.toFloat(), height.toFloat())
        val imageBitmap = ImageBitmap(width, height)

        try {
            canvasScope.draw(Canvas(imageBitmap), size) {
                inset(0.0f, 100.0f, 0.0f, 100.0f) {
                    drawRect(color = Color.Red)
                }
            }
        } catch (e: IllegalArgumentException) {
            fail("Zero height after applying inset is allowed")
        }
    }

    @Test
    fun testInsetZeroWidth() {
        // Verify that the inset call does not crash even though we are adding an inset
        // to the right and bottom such that the drawing size after the inset is zero in both
        // dimensions.
        // This is useful for animations that slowly reveal the drawing bounds of the area.
        // Alternatively this could happen if the a sibling UI element
        val canvasScope = CanvasDrawScope()

        val width = 200
        val height = 200
        val size = Size(width.toFloat(), height.toFloat())
        val imageBitmap = ImageBitmap(width, height)

        try {
            canvasScope.draw(Canvas(imageBitmap), size) {
                inset(100.0f, 0.0f, 100.0f, 0.0f) {
                    drawRect(color = Color.Red)
                }
            }
        } catch (e: IllegalArgumentException) {
            fail("Zero width after applying inset is allowed")
        }
    }

    @Test
    fun testScaleBottomRightPivot() {
        val canvasScope = CanvasDrawScope()

        val width = 200
        val height = 200
        val size = Size(width.toFloat(), height.toFloat())
        val imageBitmap = ImageBitmap(width, height)

        canvasScope.draw(Canvas(imageBitmap), size) {
            drawRect(color = Color.Red)
            scale(0.5f, 0.5f, Offset(width.toFloat(), height.toFloat())) {
                drawRect(color = Color.Blue)
            }
        }

        val pixelMap = imageBitmap.toPixelMap()

        val left = width - 100
        val top = height - 100
        val right = width - 1
        val bottom = height - 1
        assertEquals(Color.Blue, pixelMap[left, top])
        assertEquals(Color.Blue, pixelMap[right, top])
        assertEquals(Color.Blue, pixelMap[left, bottom])
        assertEquals(Color.Blue, pixelMap[left, right])

        assertEquals(Color.Red, pixelMap[left, top - 1])
        assertEquals(Color.Red, pixelMap[left - 1, top])
        assertEquals(Color.Red, pixelMap[left - 1, top - 1])

        assertEquals(Color.Red, pixelMap[right, top - 1])
        assertEquals(Color.Red, pixelMap[left - 1, bottom])
    }

    @Test
    fun testRotationCenterPivot() {
        val width = 200
        val height = 200
        val size = Size(width.toFloat(), height.toFloat())
        val imageBitmap = ImageBitmap(width, height)
        CanvasDrawScope().draw(Canvas(imageBitmap), size) {
            drawRect(color = Color.Red)
            rotate(180.0f) {
                drawRect(
                    topLeft = Offset(100.0f, 100.0f),
                    size = Size(100.0f, 100.0f),
                    color = Color.Blue
                )
            }
        }

        val pixelMap = imageBitmap.toPixelMap()
        assertEquals(Color.Blue, pixelMap[0, 0])
        assertEquals(Color.Blue, pixelMap[99, 0])
        assertEquals(Color.Blue, pixelMap[0, 99])
        assertEquals(Color.Blue, pixelMap[99, 99])

        assertEquals(Color.Red, pixelMap[0, 100])
        assertEquals(Color.Red, pixelMap[100, 0])
        assertEquals(Color.Red, pixelMap[100, 100])
        assertEquals(Color.Red, pixelMap[100, 99])
        assertEquals(Color.Red, pixelMap[99, 100])
    }

    @Test
    fun testRotationCenterPivotRad() {
        val width = 200
        val height = 200
        val size = Size(width.toFloat(), height.toFloat())
        val imageBitmap = ImageBitmap(width, height)
        CanvasDrawScope().draw(Canvas(imageBitmap), size) {
            drawRect(color = Color.Red)
            rotateRad(kotlin.math.PI.toFloat()) {
                drawRect(
                    topLeft = Offset(100.0f, 100.0f),
                    size = Size(100.0f, 100.0f),
                    color = Color.Blue
                )
            }
        }

        val pixelMap = imageBitmap.toPixelMap()
        assertEquals(Color.Blue, pixelMap[0, 0])
        assertEquals(Color.Blue, pixelMap[99, 0])
        assertEquals(Color.Blue, pixelMap[0, 99])
        assertEquals(Color.Blue, pixelMap[99, 99])

        assertEquals(Color.Red, pixelMap[0, 100])
        assertEquals(Color.Red, pixelMap[100, 0])
        assertEquals(Color.Red, pixelMap[100, 100])
        assertEquals(Color.Red, pixelMap[100, 99])
        assertEquals(Color.Red, pixelMap[99, 100])
    }

    @Test
    fun testRotationTopLeftPivot() {
        val width = 200
        val height = 200
        val size = Size(width.toFloat(), height.toFloat())
        val imageBitmap = ImageBitmap(width, height)
        CanvasDrawScope().draw(Canvas(imageBitmap), size) {
            drawRect(color = Color.Red)
            rotate(-45.0f, Offset.Zero) {
                drawRect(
                    size = Size(100.0f, 100.0f),
                    color = Color.Blue
                )
            }
        }

        val pixelMap = imageBitmap.toPixelMap()
        assertEquals(Color.Blue, pixelMap[2, 0])
        assertEquals(Color.Blue, pixelMap[50, 49])
        assertEquals(Color.Blue, pixelMap[70, 0])
        assertEquals(Color.Blue, pixelMap[70, 68])

        assertEquals(Color.Red, pixelMap[50, 51])
        assertEquals(Color.Red, pixelMap[75, 76])
    }

    @Test
    @LargeTest
    fun testBatchTransformEquivalent() {
        val width = 200
        val height = 200
        val size = Size(width.toFloat(), height.toFloat())
        val imageBitmap1 = ImageBitmap(width, height)
        CanvasDrawScope().draw(Canvas(imageBitmap1), size) {
            drawRect(color = Color.Red)
            inset(20.0f, 12.0f, 10.0f, 8.0f) {
                scale(2.0f, 0.5f) {
                    rotate(-45.0f, Offset.Zero) {
                        translate(7.0f, 9.0f) {
                            drawRect(
                                size = Size(100.0f, 100.0f),
                                color = Color.Blue
                            )
                        }
                    }
                }
            }
        }

        val imageBitmap2 = ImageBitmap(width, height)
        val saveCountCanvas = SaveCountCanvas(Canvas(imageBitmap2))
        CanvasDrawScope().draw(saveCountCanvas, size) {
            drawRect(color = Color.Red)
            withTransform({
                inset(20.0f, 12.0f, 10.0f, 8.0f)
                scale(2.0f, 0.5f)
                rotate(-45.0f, Offset.Zero)
                translate(7.0f, 9.0f)
            }) {
                // 2 saves at this point, the initial draw call does a save
                // as well as the withTransform call
                assertEquals(2, saveCountCanvas.saveCount)
                drawRect(
                    size = Size(100.0f, 100.0f),
                    color = Color.Blue
                )
            }

            // Restore to the save count of the initial CanvasScope.draw call
            assertEquals(1, saveCountCanvas.saveCount)
        }

        val pixelMap1 = imageBitmap1.toPixelMap()
        val pixelMap2 = imageBitmap2.toPixelMap()
        assertEquals(pixelMap1.width, pixelMap2.width)
        assertEquals(pixelMap1.height, pixelMap2.height)
        assertEquals(pixelMap1.stride, pixelMap2.stride)
        assertEquals(pixelMap1.bufferOffset, pixelMap2.bufferOffset)
        for (x in 0 until pixelMap1.width) {
            for (y in 0 until pixelMap1.height) {
                assertEquals(
                    "coordinate: " + x + ", " + y + " expected: " +
                        pixelMap1[x, y] + " actual: " + pixelMap2[x, y],
                    pixelMap1[x, y],
                    pixelMap2[x, y]
                )
            }
        }
    }

    @Test
    @LargeTest
    fun testDrawLineStrokeParametersAreApplied() {
        val width = 200
        val height = 200
        val start = Offset.Zero
        val end = Offset(width.toFloat(), height.toFloat())
        val strokeWidth = 10.0f
        // Test that colors are rendered with the correct stroke parameters
        testDrawScopeAndCanvasAreEquivalent(
            width,
            height,
            {
                drawLine(
                    Color.Cyan,
                    start,
                    end,
                    strokeWidth,
                    StrokeCap.Round
                )
            },
            { canvas ->
                canvas.drawLine(
                    start, end,
                    Paint().apply {
                        this.color = Color.Cyan
                        this.strokeWidth = strokeWidth
                        this.strokeCap = StrokeCap.Round
                    }
                )
            }
        )

        // ... now test that Brush parameters are also rendered with the correct stroke parameters
        testDrawScopeAndCanvasAreEquivalent(
            width,
            height,
            {
                drawLine(
                    SolidColor(Color.Cyan),
                    start,
                    end,
                    strokeWidth,
                    StrokeCap.Round
                )
            },
            { canvas ->
                canvas.drawLine(
                    start, end,
                    Paint().apply {
                        this.color = Color.Cyan
                        this.strokeWidth = strokeWidth
                        this.strokeCap = StrokeCap.Round
                    }
                )
            }
        )
    }

    @Test
    @LargeTest
    fun testDrawPointStrokeParametersAreApplied() {
        val width = 200
        val height = 200
        val points = listOf(
            Offset.Zero,
            Offset(10f, 10f),
            Offset(25f, 25f),
            Offset(40f, 40f),
            Offset(50f, 50f),
            Offset(75f, 75f),
            Offset(150f, 150f)
        )
        // Test first that colors are rendered with the correct stroke parameters
        testDrawScopeAndCanvasAreEquivalent(
            width,
            height,
            {
                drawPoints(
                    points,
                    PointMode.Points,
                    Color.Magenta,
                    strokeWidth = 15.0f,
                    cap = StrokeCap.Butt
                )
            },
            { canvas ->
                canvas.drawPoints(
                    PointMode.Points,
                    points,
                    Paint().apply {
                        this.color = Color.Magenta
                        this.strokeWidth = 15.0f
                        this.strokeCap = StrokeCap.Butt
                    }
                )
            }
        )

        // ... now verify that Brush parameters are also rendered with the correct stroke parameters
        testDrawScopeAndCanvasAreEquivalent(
            width,
            height,
            {
                drawPoints(
                    points,
                    PointMode.Points,
                    SolidColor(Color.Magenta),
                    strokeWidth = 15.0f,
                    cap = StrokeCap.Butt
                )
            },
            { canvas ->
                canvas.drawPoints(
                    PointMode.Points,
                    points,
                    Paint().apply {
                        this.color = Color.Magenta
                        this.strokeWidth = 15.0f
                        this.strokeCap = StrokeCap.Butt
                    }
                )
            }
        )
    }

    @Test
    fun testDensityAndLayoutDirectionConfigured() {
        val canvas = Canvas(ImageBitmap(1, 1))
        CanvasDrawScope().draw(
            Density(density = 2.0f, fontScale = 3.0f),
            LayoutDirection.Rtl,
            canvas,
            Size(1f, 1f)
        ) {
            assertEquals(2.0f, density)
            assertEquals(3.0f, fontScale)
            assertEquals(LayoutDirection.Rtl, layoutDirection)
            assertEquals(Size(1.0f, 1.0f), size)
        }
    }

    @Test
    fun testParametersRestoredAfterDraw() {
        val canvas1 = Canvas(ImageBitmap(200, 300))
        val canvas2 = Canvas(ImageBitmap(100, 200))

        val size1 = Size(200f, 300f)
        val size2 = Size(100f, 200f)

        val layoutDirection1 = LayoutDirection.Ltr
        val layoutDirection2 = LayoutDirection.Rtl

        val density1 = Density(2.0f, 3.0f)
        val density2 = Density(5.0f, 7.0f)

        val canvasDrawScope = CanvasDrawScope()
        canvasDrawScope.draw(
            density1,
            layoutDirection1,
            canvas1,
            size1
        ) {
            assertEquals(size1, size)
            assertEquals(density1, Density(density, fontScale))
            assertTrue(canvas1 === drawContext.canvas)
            assertEquals(LayoutDirection.Ltr, layoutDirection)

            canvasDrawScope.draw(
                density2,
                layoutDirection2,
                canvas2,
                size2
            ) {
                assertEquals(size2, size)
                assertTrue(canvas2 === drawContext.canvas)
                assertEquals(density2, Density(density, fontScale))
                assertEquals(layoutDirection2, layoutDirection)
            }

            assertEquals(size1, size)
            assertEquals(density1, Density(density, fontScale))
            assertTrue(canvas1 === drawContext.canvas)
            assertEquals(LayoutDirection.Ltr, layoutDirection)
        }
    }

    @Test
    fun testDefaultClipIntersectParams() {
        testDrawTransformDefault {
            clipPath(Path())
            assertEquals(ClipOp.Intersect, this.clipOp)
        }
    }

    @Test
    fun testDefaultClipRectParams() {
        testDrawTransformDefault {
            clipRect()
            assertEquals(0f, clipLeft)
            assertEquals(0f, clipTop)
            assertEquals(size.width, clipRight)
            assertEquals(size.height, clipBottom)
            assertEquals(ClipOp.Intersect, clipOp)
        }
    }

    @Test
    fun testDefaultTranslateParams() {
        testDrawTransformDefault {
            translate()
            assertEquals(0f, left)
            assertEquals(0f, top)
        }
    }

    @Test
    fun testDefaultRotationPivotParam() {
        testDrawTransformDefault {
            rotate(7f)
            assertEquals(center, this.pivot)
        }
    }

    @Test
    fun testDefaultScalePivotParam() {
        testDrawTransformDefault {
            scale(0.5f, 0.5f)
            assertEquals(center, this.pivot)
        }
    }

    @Test
    fun testLinearGradient() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(Brush.linearGradient(listOf(Color.Red, Color.Green, Color.Blue)))
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = LinearGradientShader(
                        Offset.Zero,
                        Offset(100f, 100f),
                        listOf(Color.Red, Color.Green, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testLinearGradientBottomEnd() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Red, Color.Green, Color.Blue),
                        end = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = LinearGradientShader(
                        Offset.Zero,
                        Offset(0f, 100f),
                        listOf(Color.Red, Color.Green, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testLinearGradientRightEnd() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Red, Color.Green, Color.Blue),
                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                    )
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = LinearGradientShader(
                        Offset.Zero,
                        Offset(100f, 0f),
                        listOf(Color.Red, Color.Green, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testLinearGradientBottomStart() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Red, Color.Green, Color.Blue),
                        start = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = LinearGradientShader(
                        Offset(0f, 100f),
                        Offset(100f, 100f),
                        listOf(Color.Red, Color.Green, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testLinearGradientRightStart() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Red, Color.Green, Color.Blue),
                        start = Offset(Float.POSITIVE_INFINITY, 0f)
                    )
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = LinearGradientShader(
                        Offset(100f, 0f),
                        Offset(100f, 100f),
                        listOf(Color.Red, Color.Green, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testLinearGradientWithStops() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.linearGradient(
                        0.0f to Color.Red,
                        0.1f to Color.Green,
                        0.8f to Color.Blue,
                        start = Offset(10.0f, 10f),
                        tileMode = TileMode.Repeated
                    )
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = LinearGradientShader(
                        Offset(10f, 10f),
                        Offset(100f, 100f),
                        colors = listOf(Color.Red, Color.Green, Color.Blue),
                        colorStops = listOf(0.0f, 0.1f, 0.8f),
                        tileMode = TileMode.Repeated
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testHorizontalGradient() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Green, Color.Blue)))
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = LinearGradientShader(
                        Offset.Zero,
                        Offset(100f, 0f),
                        listOf(Color.Red, Color.Green, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testHorizontalGradientWithStops() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.horizontalGradient(
                        0.0f to Color.Red,
                        0.1f to Color.Green,
                        0.8f to Color.Blue,
                        startX = 10f,
                        tileMode = TileMode.Repeated
                    )
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = LinearGradientShader(
                        Offset(10f, 0f),
                        Offset(100f, 0f),
                        colors = listOf(Color.Red, Color.Green, Color.Blue),
                        colorStops = listOf(0.0f, 0.1f, 0.8f),
                        tileMode = TileMode.Repeated
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testVerticalGradient() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(Brush.verticalGradient(listOf(Color.Red, Color.Green, Color.Blue)))
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = LinearGradientShader(
                        Offset.Zero,
                        Offset(0f, 100f),
                        listOf(Color.Red, Color.Green, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testVerticalGradientWithStops() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.verticalGradient(
                        0.0f to Color.Red,
                        0.1f to Color.Green,
                        0.8f to Color.Blue,
                        startY = 10f,
                        tileMode = TileMode.Repeated
                    )
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = LinearGradientShader(
                        Offset(0f, 10f),
                        Offset(0f, 100f),
                        colors = listOf(Color.Red, Color.Green, Color.Blue),
                        colorStops = listOf(0.0f, 0.1f, 0.8f),
                        tileMode = TileMode.Repeated
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testRadialGradient() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.radialGradient(listOf(Color.Red, Color.Green, Color.Blue))
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = RadialGradientShader(
                        Offset(50f, 50f),
                        50f,
                        colors = listOf(Color.Red, Color.Green, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testRadialGradientOutsideDrawingBounds() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                val offsetRadialGradient = Brush.radialGradient(
                    listOf(Color.Red, Color.Blue),
                    center = Offset(150f, 150f),
                    radius = 50f
                )
                drawRect(offsetRadialGradient)
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = RadialGradientShader(
                        Offset(150f, 150f),
                        radius = 50f,
                        colors = listOf(Color.Red, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testRadialGradientBottomRight() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                val offsetRadialGradient = Brush.radialGradient(
                    listOf(Color.Red, Color.Blue),
                    center = Offset.Infinite
                )
                drawRect(offsetRadialGradient)
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = RadialGradientShader(
                        Offset(100f, 100f),
                        radius = 50f,
                        colors = listOf(Color.Red, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testRadialGradientRight() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                val offsetRadialGradient = Brush.radialGradient(
                    listOf(Color.Red, Color.Blue),
                    center = Offset(Float.POSITIVE_INFINITY, 0f)
                )
                drawRect(offsetRadialGradient)
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = RadialGradientShader(
                        Offset(100f, 0f),
                        radius = 50f,
                        colors = listOf(Color.Red, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testRadialGradientBottom() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                val offsetRadialGradient = Brush.radialGradient(
                    listOf(Color.Red, Color.Blue),
                    center = Offset(0f, Float.POSITIVE_INFINITY)
                )
                drawRect(offsetRadialGradient)
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = RadialGradientShader(
                        Offset(0f, 100f),
                        radius = 50f,
                        colors = listOf(Color.Red, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testRadialGradientWithStops() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.radialGradient(
                        0.0f to Color.Red,
                        0.1f to Color.Green,
                        0.8f to Color.Blue,
                        radius = 10f,
                        tileMode = TileMode.Mirror
                    )
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = RadialGradientShader(
                        Offset(50f, 50f),
                        10f,
                        colors = listOf(Color.Red, Color.Green, Color.Blue),
                        colorStops = listOf(0.0f, 0.1f, 0.8f),
                        tileMode = TileMode.Mirror
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testSweepGradient() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.sweepGradient(listOf(Color.Red, Color.Green, Color.Blue))
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = SweepGradientShader(
                        Offset(50f, 50f),
                        colors = listOf(Color.Red, Color.Green, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testSweepGradientBottomRight() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.sweepGradient(
                        listOf(Color.Red, Color.Green, Color.Blue),
                        center = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = SweepGradientShader(
                        Offset(100f, 100f),
                        colors = listOf(Color.Red, Color.Green, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testSweepGradientBottom() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.sweepGradient(
                        listOf(Color.Red, Color.Green, Color.Blue),
                        center = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = SweepGradientShader(
                        Offset(0f, 100f),
                        colors = listOf(Color.Red, Color.Green, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testSweepGradientRight() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.sweepGradient(
                        listOf(Color.Red, Color.Green, Color.Blue),
                        center = Offset(Float.POSITIVE_INFINITY, 0f)
                    )
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = SweepGradientShader(
                        Offset(100f, 0f),
                        colors = listOf(Color.Red, Color.Green, Color.Blue)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testSweepGradientWithStops() {
        testDrawScopeAndCanvasAreEquivalent(
            100,
            100,
            {
                drawRect(
                    Brush.sweepGradient(
                        0.0f to Color.Red,
                        0.1f to Color.Green,
                        0.8f to Color.Blue
                    )
                )
            },
            { canvas ->
                val paint = Paint().apply {
                    shader = SweepGradientShader(
                        Offset(50f, 50f),
                        colors = listOf(Color.Red, Color.Green, Color.Blue),
                        colorStops = listOf(0.0f, 0.1f, 0.8f)
                    )
                }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
            }
        )
    }

    @Test
    fun testDrawScopeWithAlternatingPathEffectParameters() {
        val strokeWidth = 2f
        val strokeColor = Color.Red
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f), 1f)
        val width = 100f
        val height = 100f
        // Verify that DrawScope is correctly updating internal Paint parameters
        // across draw commands with a stroke
        testDrawScopeAndCanvasAreEquivalent(
            width.toInt(),
            height.toInt(),
            {
                drawCircle(
                    strokeColor,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = pathEffect
                    )
                )
                drawLine(
                    color = strokeColor,
                    start = Offset(size.width / 2, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = strokeWidth
                )
            },
            { canvas ->
                canvas.drawCircle(
                    Offset(width / 2f, height / 2f),
                    radius = 50f,
                    Paint().apply {
                        this.color = strokeColor
                        this.style = PaintingStyle.Stroke
                        this.strokeWidth = strokeWidth
                        this.pathEffect = pathEffect
                    }
                )

                canvas.drawLine(
                    Offset(width / 2, height / 2),
                    Offset(width, height / 2),
                    Paint().apply {
                        this.color = strokeColor
                        this.style = PaintingStyle.Stroke
                        this.strokeWidth = strokeWidth
                    }
                )
            }
        )
    }

    @Test
    fun testDrawImageWithFilterQualityNone() {
        // Create a small 3x3 ImageBitmap that has red, blue and green squares
        // along the diagonal. Drawing this bitmap scaled into any square drawing area
        // with FilterQuality.None should draw 3 equivalently sized squares each red, blue and green
        // without any interpolation/anti-aliasing algorithm applied to the result
        val width = 90f
        val height = 90f
        val sampleBitmap = ImageBitmap(3, 3)
        val canvas = androidx.compose.ui.graphics.Canvas(sampleBitmap)
        val samplePaint = Paint().apply {
            color = Color.White
        }

        canvas.drawRect(0f, 0f, 3f, 3f, samplePaint)

        samplePaint.color = Color.Red
        canvas.drawRect(0f, 0f, 1f, 1f, samplePaint)

        samplePaint.color = Color.Blue
        canvas.drawRect(1f, 1f, 2f, 2f, samplePaint)

        samplePaint.color = Color.Green
        canvas.drawRect(2f, 2f, 3f, 3f, samplePaint)

        testDrawScopeAndCanvasAreEquivalent(
            width.toInt(),
            height.toInt(),
            {
                drawRect(Color.White)
                drawImage(
                    sampleBitmap,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.None
                )
            },
            {
                // Compare the result above with a similarly rendered result that is 30x larger
                // In this case, scaling up the original 3x3 ImageBitmap will generate the
                // same exact output
                val canvasPaint = Paint().apply { color = Color.White }
                it.drawRect(0f, 0f, width, height, canvasPaint)

                canvasPaint.color = Color.Red
                it.drawRect(0f, 0f, 30f, 30f, canvasPaint)

                canvasPaint.color = Color.Blue
                it.drawRect(30f, 30f, 60f, 60f, canvasPaint)

                canvasPaint.color = Color.Green
                it.drawRect(60f, 60f, 90f, 90f, canvasPaint)
            }
        )
    }

    private inline fun testDrawTransformDefault(block: WrappedDrawTransform.() -> Unit) {
        val width = 100
        val height = 150
        TestDrawScopeTransform().draw(
            Canvas(ImageBitmap(width, height)),
            Size(width.toFloat(), height.toFloat())
        ) {
            withWrappedTransform({
                block(this)
            }) { /* no-op */ }
        }
    }

    /**
     * Helper method used  to confirm both DrawScope rendered content and Canvas drawn
     * content are identical
     */
    private fun testDrawScopeAndCanvasAreEquivalent(
        width: Int,
        height: Int,
        drawScopeBlock: DrawScope.() -> Unit,
        canvasBlock: (Canvas) -> Unit
    ) {
        val size = Size(width.toFloat(), height.toFloat())
        val imageBitmap1 = ImageBitmap(width, height)
        CanvasDrawScope().draw(Canvas(imageBitmap1), size) {
            drawScopeBlock()
        }

        val imageBitmap2 = ImageBitmap(width, height)
        canvasBlock(Canvas(imageBitmap2))

        val pixelMap1 = imageBitmap1.toPixelMap()
        val pixelMap2 = imageBitmap2.toPixelMap()
        assertEquals(pixelMap1.width, pixelMap2.width)
        assertEquals(pixelMap1.height, pixelMap2.height)
        assertEquals(pixelMap1.stride, pixelMap2.stride)
        assertEquals(pixelMap1.bufferOffset, pixelMap2.bufferOffset)
        for (x in 0 until pixelMap1.width) {
            for (y in 0 until pixelMap1.height) {
                assertEquals(
                    "coordinate: " + x + ", " + y + " expected: " +
                        pixelMap1[x, y] + " actual: " + pixelMap2[x, y],
                    pixelMap1[x, y],
                    pixelMap2[x, y]
                )
            }
        }
    }

    class SaveCountCanvas(val canvas: Canvas) : Canvas by canvas {

        var saveCount: Int = 0

        override fun save() {
            saveCount++
        }

        override fun restore() {
            saveCount--
        }
    }

    /**
     * Helper test method with defaults for density and layout direction
     */
    private inline fun CanvasDrawScope.draw(
        canvas: Canvas,
        size: Size,
        block: DrawScope.() -> Unit
    ) = this.draw(Density(1.0f, 1.0f), LayoutDirection.Ltr, canvas, size, block)

    private inline fun DrawScope.withWrappedTransform(
        transformBlock: WrappedDrawTransform.() -> Unit,
        drawBlock: DrawScope.() -> Unit
    ) {
        withTransform(
            { transformBlock((this as WrappedDrawTransform)) },
            drawBlock
        )
    }

    private class TestDrawScopeTransform(
        val drawScope: CanvasDrawScope = CanvasDrawScope()
    ) : DrawScope by drawScope {

        override val drawContext = object : DrawContext {
            override var size: Size
                get() = drawScope.drawContext.size
                set(value) {
                    drawScope.drawContext.size = value
                }
            override val canvas: Canvas
                get() = drawScope.drawContext.canvas
            override val transform: DrawTransform =
                WrappedDrawTransform(drawScope.drawContext.transform)
        }

        inline fun draw(canvas: Canvas, size: Size, block: DrawScope.() -> Unit) {
            drawScope.draw(
                Density(1.0f, 1.0f),
                LayoutDirection.Ltr,
                canvas,
                size
            ) {
                this@TestDrawScopeTransform.block()
            }
        }
    }

    /**
     * DrawTransform implementation that caches its parameter values to ensure proper defaults
     * are being provided.
     */
    class WrappedDrawTransform(val drawTransform: DrawTransform) : DrawTransform by drawTransform {

        var clipLeft: Float = -1f
        var clipTop: Float = -1f
        var clipRight: Float = -1f
        var clipBottom: Float = -1f
        var clipOp: ClipOp? = null

        var left: Float = -1f
        var top: Float = -1f

        var pivot: Offset = Offset(-1f, -1f)

        override fun clipRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            clipOp: ClipOp
        ) {
            clipLeft = left
            clipTop = top
            clipRight = right
            clipBottom = bottom
            this.clipOp = clipOp
            drawTransform.clipRect(left, top, right, bottom, clipOp)
        }

        override fun clipPath(path: Path, clipOp: ClipOp) {
            this.clipOp = clipOp
            drawTransform.clipPath(path, clipOp)
        }

        override fun translate(left: Float, top: Float) {
            this.left = left
            this.top = top
            drawTransform.translate(left, top)
        }

        override fun rotate(degrees: Float, pivot: Offset) {
            this.pivot = pivot
            drawTransform.rotate(degrees, pivot)
        }

        override fun scale(scaleX: Float, scaleY: Float, pivot: Offset) {
            this.pivot = pivot
            drawTransform.scale(scaleX, scaleY, pivot)
        }
    }
}
