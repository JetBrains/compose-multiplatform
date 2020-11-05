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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import org.jetbrains.skija.Matrix33
import org.jetbrains.skija.PathDirection
import org.jetbrains.skija.PathEffect
import org.jetbrains.skija.PathFillMode
import org.jetbrains.skija.PathOp

actual fun Path(): Path = DesktopPath()

actual typealias NativePathEffect = PathEffect

/**
 * @Throws UnsupportedOperationException if this Path is not backed by an org.jetbrains.skija.Path
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Path.asDesktopPath(): org.jetbrains.skija.Path =
    if (this is DesktopPath) {
        internalPath
    } else {
        throw UnsupportedOperationException("Unable to obtain org.jetbrains.skija.Path")
    }

class DesktopPath(
    internalPath: org.jetbrains.skija.Path = org.jetbrains.skija.Path()
) : Path {
    var internalPath = internalPath
        private set

    override var fillType: PathFillType
        get() {
            if (internalPath.fillMode == PathFillMode.EVEN_ODD) {
                return PathFillType.EvenOdd
            } else {
                return PathFillType.NonZero
            }
        }

        set(value) {
            internalPath.fillMode =
                if (value == PathFillType.EvenOdd) {
                    PathFillMode.EVEN_ODD
                } else {
                    PathFillMode.WINDING
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
        internalPath.arcTo(
            rect.toSkijaRect(),
            startAngleDegrees,
            sweepAngleDegrees,
            forceMoveTo
        )
    }

    override fun addRect(rect: Rect) {
        internalPath.addRect(rect.toSkijaRect(), PathDirection.COUNTER_CLOCKWISE)
    }

    override fun addOval(oval: Rect) {
        internalPath.addOval(oval.toSkijaRect(), PathDirection.COUNTER_CLOCKWISE)
    }

    override fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float) {
        addArc(oval, degrees(startAngleRadians), degrees(sweepAngleRadians))
    }

    override fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) {
        internalPath.addArc(oval.toSkijaRect(), startAngleDegrees, sweepAngleDegrees)
    }

    override fun addRoundRect(roundRect: RoundRect) {
        internalPath.addRRect(roundRect.toSkijaRRect(), PathDirection.COUNTER_CLOCKWISE)
    }

    override fun addPath(path: Path, offset: Offset) {
        internalPath.addPath(path.asDesktopPath(), offset.x, offset.y)
    }

    override fun close() {
        internalPath.closePath()
    }

    override fun reset() {
        internalPath.reset()
    }

    override fun translate(offset: Offset) {
        internalPath.transform(Matrix33.makeTranslate(offset.x, offset.y))
    }

    override fun getBounds(): Rect {
        val bounds = internalPath.bounds
        return Rect(
            bounds.left,
            bounds.top,
            bounds.right,
            bounds.bottom
        )
    }

    override fun op(
        path1: Path,
        path2: Path,
        operation: PathOperation
    ): Boolean {
        val path = org.jetbrains.skija.Path.makeCombining(
            path1.asDesktopPath(),
            path2.asDesktopPath(),
            operation.toSkijaOperation()
        )

        internalPath = path ?: internalPath
        return path != null
    }

    private fun PathOperation.toSkijaOperation() = when (this) {
        PathOperation.difference -> PathOp.DIFFERENCE
        PathOperation.intersect -> PathOp.INTERSECT
        PathOperation.union -> PathOp.UNION
        PathOperation.xor -> PathOp.XOR
        PathOperation.reverseDifference -> PathOp.REVERSE_DIFFERENCE
    }

    override val isConvex: Boolean get() = internalPath.isConvex

    override val isEmpty: Boolean get() = internalPath.isEmpty
}