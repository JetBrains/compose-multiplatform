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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Stub implementation of [Canvas] to be used to ensure
 * the internal canvas object within [DrawScope] is never
 * null. All methods here are no-ops to ensure no
 * null pointer exceptions are thrown at runtime. During
 * normal use, the canvas used within [DrawScope] is
 * consuming a valid Canvas that draws content
 * into a valid destination
 */
internal class EmptyCanvas : Canvas {

    override fun save() {
        throw UnsupportedOperationException()
    }

    override fun restore() {
        throw UnsupportedOperationException()
    }

    override fun saveLayer(bounds: Rect, paint: Paint) {
        throw UnsupportedOperationException()
    }

    override fun translate(dx: Float, dy: Float) {
        throw UnsupportedOperationException()
    }

    override fun scale(sx: Float, sy: Float) {
        throw UnsupportedOperationException()
    }

    override fun rotate(degrees: Float) {
        throw UnsupportedOperationException()
    }

    override fun skew(sx: Float, sy: Float) {
        throw UnsupportedOperationException()
    }

    override fun concat(matrix: Matrix) {
        throw UnsupportedOperationException()
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        throw UnsupportedOperationException()
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
        throw UnsupportedOperationException()
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        throw UnsupportedOperationException()
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        throw UnsupportedOperationException()
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
        paint: Paint
    ) {
        throw UnsupportedOperationException()
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        throw UnsupportedOperationException()
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        throw UnsupportedOperationException()
    }

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        throw UnsupportedOperationException()
    }

    override fun drawPath(path: Path, paint: Paint) {
        throw UnsupportedOperationException()
    }

    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) {
        throw UnsupportedOperationException()
    }

    override fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint
    ) {
        throw UnsupportedOperationException()
    }

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        throw UnsupportedOperationException()
    }

    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {
        throw UnsupportedOperationException()
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {
        throw UnsupportedOperationException()
    }

    override fun enableZ() {
        throw UnsupportedOperationException()
    }

    override fun disableZ() {
        throw UnsupportedOperationException()
    }
}