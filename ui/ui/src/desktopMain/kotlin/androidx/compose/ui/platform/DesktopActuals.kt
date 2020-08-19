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

package androidx.compose.ui.platform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.isIdentity

actual class NativeRectF {
    actual var left: Float = 0f
    actual var right: Float = 0f
    actual var top: Float = 0f
    actual var bottom: Float = 0f

    actual fun set(left: Float, right: Float, top: Float, bottom: Float) {
        this.left = left
        this.right = right
        this.top = top
        this.bottom = bottom
    }

    /**
     * copy of android.graphics.RectF.intersect(float, float, float, float)
     */
    actual fun intersect(left: Float, right: Float, top: Float, bottom: Float): Boolean {
        if (this.left < right && left < this.right && this.top < bottom && top < this.bottom) {
            if (this.left < left) {
                this.left = left
            }
            if (this.top < top) {
                this.top = top
            }
            if (this.right > right) {
                this.right = right
            }
            if (this.bottom > bottom) {
                this.bottom = bottom
            }
            return true
        }
        return false
    }

    actual fun setEmpty() {
        left = 0f
        right = 0f
        top = 0f
        bottom = 0f
    }
}

actual class NativeMatrix {
    private var matrix = Matrix()

    actual fun isIdentity(): Boolean = matrix.isIdentity()

    actual fun invert(inverted: NativeMatrix): Boolean {
        matrix.setFrom(inverted.matrix)
        matrix.invert()
        return true
    }

    actual fun mapPoints(points: FloatArray) {
        for (i in points.indices step 2) {
            val original = Offset(points[i], points[i + 1])
            val result = matrix.map(original)
            points[i] = result.x
            points[i + 1] = result.y
        }
    }

    actual fun mapRect(rect: NativeRectF): Boolean {
        val tmp = Rect(rect.left, rect.top, rect.right, rect.bottom)
        val result = matrix.map(tmp)
        rect.left = result.left
        rect.top = result.top
        rect.right = result.right
        rect.bottom = result.bottom
        return true
    }
}