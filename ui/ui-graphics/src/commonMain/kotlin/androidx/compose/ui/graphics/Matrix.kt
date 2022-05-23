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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// TODO(mount): This class needs some optimization
@kotlin.jvm.JvmInline
value class Matrix(
    val values: FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )
) {
    inline operator fun get(row: Int, column: Int) = values[(row * 4) + column]

    inline operator fun set(row: Int, column: Int, v: Float) {
        values[(row * 4) + column] = v
    }

    /**
     * Does the 3D transform on [point] and returns the `x` and `y` values in an [Offset].
     */
    fun map(point: Offset): Offset {
        val x = point.x
        val y = point.y
        val z = this[0, 3] * x + this[1, 3] * y + this[3, 3]
        val inverseZ = 1 / z
        val pZ = if (inverseZ.isFinite()) inverseZ else 0f

        return Offset(
            x = pZ * (this[0, 0] * x + this[1, 0] * y + this[3, 0]),
            y = pZ * (this[0, 1] * x + this[1, 1] * y + this[3, 1])
        )
    }

    /**
     * Does a 3D transform on [rect] and returns its bounds after the transform.
     */
    fun map(rect: Rect): Rect {
        val p0 = map(Offset(rect.left, rect.top))
        val p1 = map(Offset(rect.left, rect.bottom))
        val p3 = map(Offset(rect.right, rect.top))
        val p4 = map(Offset(rect.right, rect.bottom))

        val left = min(min(p0.x, p1.x), min(p3.x, p4.x))
        val top = min(min(p0.y, p1.y), min(p3.y, p4.y))
        val right = max(max(p0.x, p1.x), max(p3.x, p4.x))
        val bottom = max(max(p0.y, p1.y), max(p3.y, p4.y))
        return Rect(left, top, right, bottom)
    }

    /**
     * Does a 3D transform on [rect], transforming [rect] with the results.
     */
    fun map(rect: MutableRect) {
        val p0 = map(Offset(rect.left, rect.top))
        val p1 = map(Offset(rect.left, rect.bottom))
        val p3 = map(Offset(rect.right, rect.top))
        val p4 = map(Offset(rect.right, rect.bottom))

        rect.left = min(min(p0.x, p1.x), min(p3.x, p4.x))
        rect.top = min(min(p0.y, p1.y), min(p3.y, p4.y))
        rect.right = max(max(p0.x, p1.x), max(p3.x, p4.x))
        rect.bottom = max(max(p0.y, p1.y), max(p3.y, p4.y))
    }

    /**
     * Multiply this matrix by [m] and assign the result to this matrix.
     */
    operator fun timesAssign(m: Matrix) {
        val v00 = dot(this, 0, m, 0)
        val v01 = dot(this, 0, m, 1)
        val v02 = dot(this, 0, m, 2)
        val v03 = dot(this, 0, m, 3)
        val v10 = dot(this, 1, m, 0)
        val v11 = dot(this, 1, m, 1)
        val v12 = dot(this, 1, m, 2)
        val v13 = dot(this, 1, m, 3)
        val v20 = dot(this, 2, m, 0)
        val v21 = dot(this, 2, m, 1)
        val v22 = dot(this, 2, m, 2)
        val v23 = dot(this, 2, m, 3)
        val v30 = dot(this, 3, m, 0)
        val v31 = dot(this, 3, m, 1)
        val v32 = dot(this, 3, m, 2)
        val v33 = dot(this, 3, m, 3)
        this[0, 0] = v00
        this[0, 1] = v01
        this[0, 2] = v02
        this[0, 3] = v03
        this[1, 0] = v10
        this[1, 1] = v11
        this[1, 2] = v12
        this[1, 3] = v13
        this[2, 0] = v20
        this[2, 1] = v21
        this[2, 2] = v22
        this[2, 3] = v23
        this[3, 0] = v30
        this[3, 1] = v31
        this[3, 2] = v32
        this[3, 3] = v33
    }

    override fun toString(): String {
        return """
            |${this[0, 0]} ${this[0, 1]} ${this[0, 2]} ${this[0, 3]}|
            |${this[1, 0]} ${this[1, 1]} ${this[1, 2]} ${this[1, 3]}|
            |${this[2, 0]} ${this[2, 1]} ${this[2, 2]} ${this[2, 3]}|
            |${this[3, 0]} ${this[3, 1]} ${this[3, 2]} ${this[3, 3]}|
        """.trimIndent()
    }

    /**
     * Invert `this` Matrix.
     */
    fun invert() {
        val a00 = this[0, 0]
        val a01 = this[0, 1]
        val a02 = this[0, 2]
        val a03 = this[0, 3]
        val a10 = this[1, 0]
        val a11 = this[1, 1]
        val a12 = this[1, 2]
        val a13 = this[1, 3]
        val a20 = this[2, 0]
        val a21 = this[2, 1]
        val a22 = this[2, 2]
        val a23 = this[2, 3]
        val a30 = this[3, 0]
        val a31 = this[3, 1]
        val a32 = this[3, 2]
        val a33 = this[3, 3]
        val b00 = a00 * a11 - a01 * a10
        val b01 = a00 * a12 - a02 * a10
        val b02 = a00 * a13 - a03 * a10
        val b03 = a01 * a12 - a02 * a11
        val b04 = a01 * a13 - a03 * a11
        val b05 = a02 * a13 - a03 * a12
        val b06 = a20 * a31 - a21 * a30
        val b07 = a20 * a32 - a22 * a30
        val b08 = a20 * a33 - a23 * a30
        val b09 = a21 * a32 - a22 * a31
        val b10 = a21 * a33 - a23 * a31
        val b11 = a22 * a33 - a23 * a32
        val det =
            (b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06)
        if (det == 0.0f) {
            return
        }
        val invDet = 1.0f / det
        this[0, 0] = ((a11 * b11 - a12 * b10 + a13 * b09) * invDet)
        this[0, 1] = ((-a01 * b11 + a02 * b10 - a03 * b09) * invDet)
        this[0, 2] = ((a31 * b05 - a32 * b04 + a33 * b03) * invDet)
        this[0, 3] = ((-a21 * b05 + a22 * b04 - a23 * b03) * invDet)
        this[1, 0] = ((-a10 * b11 + a12 * b08 - a13 * b07) * invDet)
        this[1, 1] = ((a00 * b11 - a02 * b08 + a03 * b07) * invDet)
        this[1, 2] = ((-a30 * b05 + a32 * b02 - a33 * b01) * invDet)
        this[1, 3] = ((a20 * b05 - a22 * b02 + a23 * b01) * invDet)
        this[2, 0] = ((a10 * b10 - a11 * b08 + a13 * b06) * invDet)
        this[2, 1] = ((-a00 * b10 + a01 * b08 - a03 * b06) * invDet)
        this[2, 2] = ((a30 * b04 - a31 * b02 + a33 * b00) * invDet)
        this[2, 3] = ((-a20 * b04 + a21 * b02 - a23 * b00) * invDet)
        this[3, 0] = ((-a10 * b09 + a11 * b07 - a12 * b06) * invDet)
        this[3, 1] = ((a00 * b09 - a01 * b07 + a02 * b06) * invDet)
        this[3, 2] = ((-a30 * b03 + a31 * b01 - a32 * b00) * invDet)
        this[3, 3] = ((a20 * b03 - a21 * b01 + a22 * b00) * invDet)
    }

    /**
     * Resets the `this` to the identity matrix.
     */
    fun reset() {
        for (c in 0..3) {
            for (r in 0..3) {
                this.set(r, c, if (c == r) 1f else 0f)
            }
        }
    }

    /** Sets the entire matrix to the matrix in [matrix]. */
    fun setFrom(matrix: Matrix) {
        for (i in 0..15) {
            values[i] = matrix.values[i]
        }
    }

    /**
     * Applies a [degrees] rotation around X to `this`.
     */
    fun rotateX(degrees: Float) {
        val c = cos(degrees * PI / 180.0).toFloat()
        val s = sin(degrees * PI / 180.0).toFloat()

        val a01 = this[0, 1]
        val a02 = this[0, 2]
        val v01 = a01 * c - a02 * s
        val v02 = a01 * s + a02 * c

        val a11 = this[1, 1]
        val a12 = this[1, 2]
        val v11 = a11 * c - a12 * s
        val v12 = a11 * s + a12 * c

        val a21 = this[2, 1]
        val a22 = this[2, 2]
        val v21 = a21 * c - a22 * s
        val v22 = a21 * s + a22 * c

        val a31 = this[3, 1]
        val a32 = this[3, 2]
        val v31 = a31 * c - a32 * s
        val v32 = a31 * s + a32 * c

        this[0, 1] = v01
        this[0, 2] = v02
        this[1, 1] = v11
        this[1, 2] = v12
        this[2, 1] = v21
        this[2, 2] = v22
        this[3, 1] = v31
        this[3, 2] = v32
    }

    /**
     * Applies a [degrees] rotation around Y to `this`.
     */
    fun rotateY(degrees: Float) {
        val c = cos(degrees * PI / 180.0).toFloat()
        val s = sin(degrees * PI / 180.0).toFloat()

        val a00 = this[0, 0]
        val a02 = this[0, 2]
        val v00 = a00 * c + a02 * s
        val v02 = -a00 * s + a02 * c

        val a10 = this[1, 0]
        val a12 = this[1, 2]
        val v10 = a10 * c + a12 * s
        val v12 = -a10 * s + a12 * c

        val a20 = this[2, 0]
        val a22 = this[2, 2]
        val v20 = a20 * c + a22 * s
        val v22 = -a20 * s + a22 * c

        val a30 = this[3, 0]
        val a32 = this[3, 2]
        val v30 = a30 * c + a32 * s
        val v32 = -a30 * s + a32 * c

        this[0, 0] = v00
        this[0, 2] = v02
        this[1, 0] = v10
        this[1, 2] = v12
        this[2, 0] = v20
        this[2, 2] = v22
        this[3, 0] = v30
        this[3, 2] = v32
    }

    /**
     * Applies a [degrees] rotation around Z to `this`.
     */
    fun rotateZ(degrees: Float) {
        val c = cos(degrees * PI / 180.0).toFloat()
        val s = sin(degrees * PI / 180.0).toFloat()

        val a00 = this[0, 0]
        val a10 = this[1, 0]
        val v00 = c * a00 + s * a10
        val v10 = -s * a00 + c * a10

        val a01 = this[0, 1]
        val a11 = this[1, 1]
        val v01 = c * a01 + s * a11
        val v11 = -s * a01 + c * a11

        val a02 = this[0, 2]
        val a12 = this[1, 2]
        val v02 = c * a02 + s * a12
        val v12 = -s * a02 + c * a12

        val a03 = this[0, 3]
        val a13 = this[1, 3]
        val v03 = c * a03 + s * a13
        val v13 = -s * a03 + c * a13

        this[0, 0] = v00
        this[0, 1] = v01
        this[0, 2] = v02
        this[0, 3] = v03
        this[1, 0] = v10
        this[1, 1] = v11
        this[1, 2] = v12
        this[1, 3] = v13
    }

    /** Scale this matrix by [x], [y], [z] */
    fun scale(x: Float = 1f, y: Float = 1f, z: Float = 1f) {
        this[0, 0] *= x
        this[0, 1] *= x
        this[0, 2] *= x
        this[0, 3] *= x
        this[1, 0] *= y
        this[1, 1] *= y
        this[1, 2] *= y
        this[1, 3] *= y
        this[2, 0] *= z
        this[2, 1] *= z
        this[2, 2] *= z
        this[2, 3] *= z
    }

    /** Translate this matrix by [x], [y], [z] */
    fun translate(x: Float = 0f, y: Float = 0f, z: Float = 0f) {
        val t1 = this[0, 0] * x +
            this[1, 0] * y +
            this[2, 0] * z +
            this[3, 0]
        val t2 = this[0, 1] * x +
            this[1, 1] * y +
            this[2, 1] * z +
            this[3, 1]
        val t3 = this[0, 2] * x +
            this[1, 2] * y +
            this[2, 2] * z +
            this[3, 2]
        val t4 = this[0, 3] * x +
            this[1, 3] * y +
            this[2, 3] * z +
            this[3, 3]
        this[3, 0] = t1
        this[3, 1] = t2
        this[3, 2] = t3
        this[3, 3] = t4
    }

    companion object {
        /**
         * Index of the flattened array that represents the scale factor along the X axis
         */
        const val ScaleX = 0

        /**
         * Index of the flattened array that represents the skew factor along the Y axis
         */
        const val SkewY = 1

        /**
         * Index of the flattened array that represents the perspective factor along the X axis
         */
        const val Perspective0 = 3

        /**
         * Index of the flattened array that represents the skew factor along the X axis
         */
        const val SkewX = 4

        /**
         * Index of the flattened array that represents the scale factor along the Y axis
         */
        const val ScaleY = 5

        /**
         * Index of the flattened array that represents the perspective factor along the Y axis
         */
        const val Perspective1 = 7

        /**
         * Index of the flattened array that represents the scale factor along the Z axis
         */
        const val ScaleZ = 10

        /**
         * Index of the flattened array that represents the translation along the X axis
         */
        const val TranslateX = 12

        /**
         * Index of the flattened array that represents the translation along the Y axis
         */
        const val TranslateY = 13

        /**
         * Index of the flattened array that represents the translation along the Z axis
         */
        const val TranslateZ = 14

        /**
         * Index of the flattened array that represents the perspective factor along the Z axis
         */
        const val Perspective2 = 15
    }
}

private fun dot(m1: Matrix, row: Int, m2: Matrix, column: Int): Float {
    return m1[row, 0] * m2[0, column] +
        m1[row, 1] * m2[1, column] +
        m1[row, 2] * m2[2, column] +
        m1[row, 3] * m2[3, column]
}

/** Whether the given matrix is the identity matrix. */
fun Matrix.isIdentity(): Boolean {
    for (row in 0..3) {
        for (column in 0..3) {
            val expected = if (row == column) 1f else 0f
            if (this[row, column] != expected) {
                return false
            }
        }
    }
    return true
}