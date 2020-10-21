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

package androidx.compose.ui.graphics.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DesktopGraphicsTest
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.VertexMode
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.imageFromResource
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Test

class DesktopCanvasTest : DesktopGraphicsTest() {
    private val canvas: Canvas = initCanvas(widthPx = 16, heightPx = 16)

    @Test
    fun drawArc() {
        canvas.drawArc(
            left = 0f,
            top = 0f,
            right = 16f,
            bottom = 16f,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true,
            paint = redPaint
        )
        canvas.drawArc(
            left = 0f,
            top = 0f,
            right = 16f,
            bottom = 16f,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = true,
            paint = bluePaint
        )
        canvas.drawArc(
            left = 0f,
            top = 0f,
            right = 16f,
            bottom = 16f,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            paint = greenPaint
        )
        canvas.drawArc(
            left = 0f,
            top = 0f,
            right = 16f,
            bottom = 16f,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            paint = cyanPaint
        )

        screenshotRule.snap(surface)
    }

    @Test
    fun drawCircle() {
        canvas.drawCircle(Offset(8f, 8f), radius = 4f, paint = redPaint)

        screenshotRule.snap(surface)
    }

    @Test
    fun drawImage() {
        canvas.drawImage(
            image = imageFromResource("androidx/compose/desktop/test.png"),
            topLeftOffset = Offset(2f, 4f),
            paint = redPaint
        )
        canvas.drawImage(
            image = imageFromResource("androidx/compose/desktop/test.png"),
            topLeftOffset = Offset(-2f, 0f),
            paint = redPaint
        )

        screenshotRule.snap(surface)
    }

    @Test
    fun drawImageRect() {
        canvas.drawImageRect(
            image = imageFromResource("androidx/compose/desktop/test.png"),
            srcOffset = IntOffset(0, 2),
            srcSize = IntSize(2, 4),
            dstOffset = IntOffset(0, 4),
            dstSize = IntSize(4, 12),
            paint = redPaint.apply {
                filterQuality = FilterQuality.None
            }
        )

        screenshotRule.snap(surface)
    }

    @Test
    fun drawLine() {
        canvas.drawLine(
            Offset(-4f, -4f), Offset(4f, 4f),
            Paint().apply {
                color = Color.Red
                strokeWidth = 1f
                strokeCap = StrokeCap.Butt
            }
        )
        canvas.drawLine(
            Offset(8f, 4f), Offset(8f, 12f),
            Paint().apply {
                color = Color.Blue
                strokeWidth = 4f
                strokeCap = StrokeCap.Butt
            }
        )
        canvas.drawLine(
            Offset(12f, 4f), Offset(12f, 12f),
            Paint().apply {
                color = Color.Green
                strokeWidth = 4f
                strokeCap = StrokeCap.Round
            }
        )
        canvas.drawLine(
            Offset(4f, 4f), Offset(4f, 12f),
            Paint().apply {
                color = Color.Black.copy(alpha = 0.5f)
                strokeWidth = 4f
                strokeCap = StrokeCap.Square
            }
        )

        // should draw antialiased two-pixel line
        canvas.drawLine(
            Offset(4f, 4f), Offset(4f, 12f),
            Paint().apply {
                color = Color.Yellow
                strokeWidth = 1f
                strokeCap = StrokeCap.Butt
            }
        )

        // should draw aliased one-pixel line
        canvas.drawLine(
            Offset(4f, 4f), Offset(4f, 12f),
            Paint().apply {
                color = Color.Yellow
                strokeWidth = 1f
                strokeCap = StrokeCap.Butt
                isAntiAlias = false
            }
        )

        // shouldn't draw any line
        canvas.drawLine(
            Offset(4f, 4f), Offset(4f, 12f),
            Paint().apply {
                color = Color.Yellow
                strokeWidth = 0f
                strokeCap = StrokeCap.Butt
                isAntiAlias = false
            }
        )

        screenshotRule.snap(surface)
    }

    @Test
    fun drawOval() {
        canvas.drawOval(left = 0f, top = 4f, right = 16f, bottom = 12f, paint = redPaint)

        screenshotRule.snap(surface)
    }

    @Test
    fun drawRect() {
        canvas.drawRect(left = 2f, top = 0f, right = 6f, bottom = 8f, paint = redPaint)
        canvas.drawRect(left = -4f, top = -4f, right = 4f, bottom = 4f, paint = bluePaint)
        canvas.drawRect(left = 1f, top = 1f, right = 1f, bottom = 1f, paint = bluePaint)

        screenshotRule.snap(surface)
    }

    @Test
    fun drawRoundRect() {
        canvas.drawRoundRect(
            left = 0f,
            top = 4f,
            right = 16f,
            bottom = 12f,
            radiusX = 6f,
            radiusY = 4f,
            paint = redPaint
        )

        screenshotRule.snap(surface)
    }

    @Test
    fun saveLayer() {
        canvas.drawRect(0f, 0f, 16f, 16f, redPaint)

        canvas.withSaveLayer(
            Rect(left = 4f, top = 8f, right = 12f, bottom = 16f),
            redPaint.apply {
                blendMode = BlendMode.Plus
            }
        ) {
            canvas.drawLine(Offset(4f, 0f), Offset(4f, 16f), bluePaint)
        }

        screenshotRule.snap(surface)
    }

    @Test
    fun transform() {
        canvas.withSave {
            canvas.translate(4f, 2f)
            canvas.drawRect(left = 0f, top = 0f, right = 4f, bottom = 4f, paint = redPaint)

            canvas.rotate(45f)
            canvas.drawLine(Offset(0f, 0f), Offset(4f, 0f), paint = bluePaint)
        }

        canvas.withSave {
            canvas.rotate(90f)
            canvas.translate(8f, 0f)
            canvas.drawRect(left = 0f, top = -4f, right = 4f, bottom = 0f, paint = greenPaint)
        }

        canvas.withSave {
            canvas.translate(8f, 8f)
            canvas.skew(1f, 0f)
            canvas.drawRect(left = 0f, top = 0f, right = 4f, bottom = 4f, paint = yellowPaint)
        }

        canvas.withSave {
            canvas.translate(8f, 8f)
            canvas.skew(0f, 1f)
            canvas.drawRect(left = 0f, top = 0f, right = 4f, bottom = 4f, paint = magentaPaint)
        }

        canvas.withSave {
            canvas.concat(
                Matrix().apply {
                    translate(12f, 2f)
                }
            )
            canvas.drawRect(left = 0f, top = 0f, right = 4f, bottom = 4f, paint = cyanPaint)
        }

        screenshotRule.snap(surface)
    }

    @Test
    fun clipRect() {
        canvas.withSave {
            canvas.clipRect(
                left = 4f,
                top = 4f,
                right = 12f,
                bottom = 12f,
                clipOp = ClipOp.Intersect
            )
            canvas.drawRect(left = -4f, top = -4f, right = 20f, bottom = 20f, paint = redPaint)
        }

        canvas.drawRect(left = 4f, top = 0f, right = 8f, bottom = 4f, paint = bluePaint)

        canvas.clipRect(left = 2f, top = 2f, right = 14f, bottom = 14f, clipOp = ClipOp.Difference)
        canvas.drawRect(left = -4f, top = -4f, right = 20f, bottom = 20f, paint = greenPaint)

        screenshotRule.snap(surface)
    }

    @Test
    fun drawVertices() {
        val positions = listOf(
            Offset(0f, 0f),
            Offset(16f, 0f),
            Offset(8f, 8f),
            Offset(16f, 8f),
            Offset(0f, 16f),
            Offset(16f, 16f),
        )
        val colors = listOf(
            Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Magenta, Color.Yellow
        )
        val indices = listOf(0, 1, 2, 3, 4, 5)

        canvas.drawVertices(
            Vertices(VertexMode.Triangles, positions, positions, colors, indices),
            BlendMode.SrcOver,
            Paint()
        )

        screenshotRule.snap(surface)
    }
}
