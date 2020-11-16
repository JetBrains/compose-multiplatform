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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopPathTest : DesktopGraphicsTest() {
    private val canvas: Canvas = initCanvas(widthPx = 16, heightPx = 16)

    @Test
    fun arc() {
        val path = Path().apply {
            addArc(Rect(0f, 0f, 16f, 16f), 0f, 90f)
            arcTo(Rect(0f, 0f, 16f, 16f), 90f, 90f, true)
        }

        canvas.drawPath(path, redPaint)

        screenshotRule.snap(surface)
    }

    @Test
    fun clipPath() {
        val path = Path().apply {
            addOval(Rect(0f, 0f, 16f, 8f))
        }

        canvas.withSave {
            canvas.clipPath(path, ClipOp.Intersect)
            canvas.drawRect(0f, 0f, 16f, 16f, redPaint)
        }

        canvas.withSave {
            canvas.clipPath(path, ClipOp.Difference)
            canvas.drawRect(0f, 0f, 16f, 16f, bluePaint)
        }

        screenshotRule.snap(surface)
    }

    @Test
    fun bezier() {
        val path = Path().apply {
            quadraticBezierTo(0f, 16f, 16f, 16f)
        }

        canvas.drawPath(path, redPaint)

        screenshotRule.snap(surface)
    }

    @Test
    fun cubic() {
        val path = Path().apply {
            cubicTo(0f, 12f, 0f, 16f, 16f, 16f)
        }

        canvas.drawPath(path, redPaint)

        screenshotRule.snap(surface)
    }

    @Test
    fun figures() {
        val path = Path().apply {
            addOval(Rect(0f, 0f, 8f, 4f))
            addRect(Rect(12f, 0f, 16f, 8f))
            addRoundRect(RoundRect(0f, 8f, 4f, 16f, 4f, 4f))
        }

        canvas.drawPath(path, redPaint)

        screenshotRule.snap(surface)
    }

    @Test
    fun fillTypeEvenOdd() {
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, 8f, 8f))
            addRect(Rect(4f, 4f, 12f, 12f))
        }

        canvas.drawPath(path, redPaint)

        screenshotRule.snap(surface)
    }

    @Test
    fun fillTypeNonZero() {
        val path = Path().apply {
            addRect(Rect(0f, 0f, 8f, 8f))
            addRect(Rect(4f, 4f, 12f, 12f))
        }

        assertEquals(PathFillType.NonZero, path.fillType)
        canvas.drawPath(path, redPaint)

        screenshotRule.snap(surface)
    }

    @Test
    fun linesFill() {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(8f, 8f)
            lineTo(0f, 8f)

            moveTo(8f, 8f)
            lineTo(8f, 16f)
            lineTo(16f, 16f)
            relativeLineTo(0f, -8f)
        }

        assertEquals(PaintingStyle.Fill, redPaint.style)
        canvas.drawPath(path, redPaint)

        screenshotRule.snap(surface)
    }

    @Test
    fun linesStroke() {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(8f, 8f)
            lineTo(0f, 8f)
            close()

            moveTo(8f, 8f)
            lineTo(8f, 16f)
            lineTo(16f, 16f)
            relativeLineTo(0f, -8f)
        }

        canvas.drawPath(
            path,
            redPaint.apply {
                style = PaintingStyle.Stroke
                strokeWidth = 2f
            }
        )

        screenshotRule.snap(surface)
    }

    @Test
    fun isEmpty() {
        val path = Path()
        assertEquals(true, path.isEmpty)

        path.addRect(Rect(0f, 0f, 16f, 16f))
        assertEquals(false, path.isEmpty)
    }

    @Test
    fun isConvex() {
        val path = Path()
        assertEquals(true, path.isConvex)

        path.addRect(Rect(0f, 0f, 8f, 8f))
        assertEquals(true, path.isConvex)

        path.addRect(Rect(8f, 8f, 16f, 16f))
        assertEquals(false, path.isConvex)
    }

    @Test
    fun getBounds() {
        val path = Path()
        assertEquals(Rect(0f, 0f, 0f, 0f), path.getBounds())

        path.addRect(Rect(0f, 0f, 8f, 8f))
        assertEquals(Rect(0f, 0f, 8f, 8f), path.getBounds())

        path.addRect(Rect(8f, 8f, 16f, 16f))
        assertEquals(Rect(0f, 0f, 16f, 16f), path.getBounds())
    }

    @Test
    fun `initial parameters`() {
        val path = Path()

        assertEquals(PathFillType.NonZero, path.fillType)
    }

    @Test
    fun `reset should preserve fillType`() {
        val path = Path()

        path.fillType = PathFillType.EvenOdd
        path.reset()

        assertEquals(PathFillType.EvenOdd, path.fillType)
    }
}
