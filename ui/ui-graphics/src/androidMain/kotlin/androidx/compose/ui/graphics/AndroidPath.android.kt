/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect

actual fun Path(): Path = AndroidPath()

/**
 * Convert the [android.graphics.Path] instance into a compose compatible Path
 */
fun android.graphics.Path.asComposePath(): Path = AndroidPath(this)

/**
 * @Throws UnsupportedOperationException if this Path is not backed by an android.graphics.Path
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Path.asAndroidPath(): android.graphics.Path =
    if (this is AndroidPath) {
        internalPath
    } else {
        throw UnsupportedOperationException("Unable to obtain android.graphics.Path")
    }

/* actual */ class AndroidPath(
    val internalPath: android.graphics.Path = android.graphics.Path()
) : Path {

    // Temporary value holders to reuse an object (not part of a state):
    private val rectF = android.graphics.RectF()
    private val radii = FloatArray(8)
    private val mMatrix = android.graphics.Matrix()

    override var fillType: PathFillType
        get() {
            if (internalPath.fillType == android.graphics.Path.FillType.EVEN_ODD) {
                return PathFillType.EvenOdd
            } else {
                return PathFillType.NonZero
            }
        }

        set(value) {
            internalPath.fillType =
                if (value == PathFillType.EvenOdd) {
                    android.graphics.Path.FillType.EVEN_ODD
                } else {
                    android.graphics.Path.FillType.WINDING
                }
        }

    override fun moveTo(x: Float, y: Float) {
        internalPath.moveTo(x, y)
    }

    override fun relativeMoveTo(dx: Float, dy: Float) {
        internalPath.rMoveTo(dx, dy)
    }

    override fun lineTo(x: Float, y: Float) {
        internalPath.lineTo(x, y)
    }

    override fun relativeLineTo(dx: Float, dy: Float) {
        internalPath.rLineTo(dx, dy)
    }

    override fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        internalPath.quadTo(x1, y1, x2, y2)
    }

    override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        internalPath.rQuadTo(dx1, dy1, dx2, dy2)
    }

    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        internalPath.cubicTo(
            x1, y1,
            x2, y2,
            x3, y3
        )
    }

    override fun relativeCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float
    ) {
        internalPath.rCubicTo(
            dx1, dy1,
            dx2, dy2,
            dx3, dy3
        )
    }

    override fun arcTo(
        rect: Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float,
        forceMoveTo: Boolean
    ) {
        val left = rect.left
        val top = rect.top
        val right = rect.right
        val bottom = rect.bottom
        rectF.set(left, top, right, bottom)
        internalPath.arcTo(
            rectF,
            startAngleDegrees,
            sweepAngleDegrees,
            forceMoveTo
        )
    }

    override fun addRect(rect: Rect) {
        check(_rectIsValid(rect))
        rectF.set(rect.toAndroidRect())
        // TODO(njawad) figure out what to do with Path Direction,
        // Flutter does not use it, Platform does
        internalPath.addRect(rectF, android.graphics.Path.Direction.CCW)
    }

    override fun addOval(oval: Rect) {
        rectF.set(oval.toAndroidRect())
        internalPath.addOval(rectF, android.graphics.Path.Direction.CCW)
    }

    override fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float) {
        addArc(oval, degrees(startAngleRadians), degrees(sweepAngleRadians))
    }

    override fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) {
        check(_rectIsValid(oval))
        rectF.set(oval.toAndroidRect())
        internalPath.addArc(rectF, startAngleDegrees, sweepAngleDegrees)
    }

    override fun addRoundRect(roundRect: RoundRect) {
        rectF.set(roundRect.left, roundRect.top, roundRect.right, roundRect.bottom)
        radii[0] = roundRect.topLeftCornerRadius.x
        radii[1] = roundRect.topLeftCornerRadius.y

        radii[2] = roundRect.topRightCornerRadius.x
        radii[3] = roundRect.topRightCornerRadius.y

        radii[4] = roundRect.bottomRightCornerRadius.x
        radii[5] = roundRect.bottomRightCornerRadius.y

        radii[6] = roundRect.bottomLeftCornerRadius.x
        radii[7] = roundRect.bottomLeftCornerRadius.y
        internalPath.addRoundRect(rectF, radii, android.graphics.Path.Direction.CCW)
    }

    override fun addPath(path: Path, offset: Offset) {
        internalPath.addPath(path.asAndroidPath(), offset.x, offset.y)
    }

    override fun close() {
        internalPath.close()
    }

    override fun reset() {
        internalPath.reset()
    }

    override fun translate(offset: Offset) {
        mMatrix.reset()
        mMatrix.setTranslate(offset.x, offset.y)
        internalPath.transform(mMatrix)
    }

    override fun getBounds(): Rect {
        internalPath.computeBounds(rectF, true)
        return Rect(
            rectF.left,
            rectF.top,
            rectF.right,
            rectF.bottom
        )
    }

    override fun op(
        path1: Path,
        path2: Path,
        operation: PathOperation
    ): Boolean {
        val op = when (operation) {
            PathOperation.Difference -> android.graphics.Path.Op.DIFFERENCE
            PathOperation.Intersect -> android.graphics.Path.Op.INTERSECT
            PathOperation.ReverseDifference -> android.graphics.Path.Op.REVERSE_DIFFERENCE
            PathOperation.Union -> android.graphics.Path.Op.UNION
            else -> android.graphics.Path.Op.XOR
        }
        return internalPath.op(path1.asAndroidPath(), path2.asAndroidPath(), op)
    }

    @Suppress("DEPRECATION") // Path.isConvex
    override val isConvex: Boolean get() = internalPath.isConvex

    override val isEmpty: Boolean get() = internalPath.isEmpty

    private fun _rectIsValid(rect: Rect): Boolean {
        check(!rect.left.isNaN()) {
            "Rect.left is NaN"
        }
        check(!rect.top.isNaN()) {
            "Rect.top is NaN"
        }
        check(!rect.right.isNaN()) {
            "Rect.right is NaN"
        }
        check(!rect.bottom.isNaN()) {
            "Rect.bottom is NaN"
        }
        return true
    }
}
