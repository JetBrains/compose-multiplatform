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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.graphics.vectormath

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// TODO(mount): This class needs some optimization
data class Matrix4(
    var x: Vector4 = Vector4(x = 1.0f),
    var y: Vector4 = Vector4(y = 1.0f),
    var z: Vector4 = Vector4(z = 1.0f),
    var w: Vector4 = Vector4(w = 1.0f)
) {
    constructor(right: Vector3, up: Vector3, forward: Vector3, position: Vector3 = Vector3()) :
            this(Vector4(right), Vector4(up), Vector4(forward), Vector4(position, 1.0f))

    constructor(m: Matrix4) : this(m.x.copy(), m.y.copy(), m.z.copy(), m.w.copy())

    companion object {
        fun of(vararg a: Float): Matrix4 {
            require(a.size >= 16)
            return Matrix4(
                Vector4(a[0], a[4], a[8], a[12]),
                Vector4(a[1], a[5], a[9], a[13]),
                Vector4(a[2], a[6], a[10], a[14]),
                Vector4(a[3], a[7], a[11], a[15])
            )
        }

        fun zero() = diagonal3Values(0.0f, 0.0f, 0.0f)

        fun identity() = Matrix4()

        fun diagonal3(scale: Vector3): Matrix4 {
            return diagonal3Values(x = scale.x, y = scale.y, z = scale.z)
        }

        fun diagonal3Values(x: Float, y: Float, z: Float): Matrix4 {
            return Matrix4(
                Vector4(x, 0.0f, 0.0f, 0.0f),
                Vector4(0.0f, y, 0.0f, 0.0f),
                Vector4(0.0f, 0.0f, z, 0.0f),
                Vector4(0.0f, 0.0f, 0.0f, 1.0f)
            )
        }

        /** Rotation of [radians_] around X. */
        fun rotationX(radians: Float) = zero().apply {
            set(3, 3, 1.0f)
            rotateX(radians)
        }

        /** Rotation of [radians_] around Y. */
        fun rotationY(radians: Float) = zero().apply {
            set(3, 3, 1.0f)
            rotateY(radians)
        }

        fun rotationZ(radians: Float) = zero().apply {
            set(3, 3, 1.0f)
            rotateZ(radians)
        }

        // / Translation matrix.
        fun translation(translation: Vector3) = identity().apply {
            setTranslationRaw(x = translation.x, y = translation.y, z = translation.z)
        }

        fun translationValues(x: Float, y: Float, z: Float) =
            identity().apply { setTranslationRaw(x, y, z) }
    }

    inline val m4storage: List<Float>
        get() = x.v4storage + y.v4storage + z.v4storage + w.v4storage

    inline var right: Vector3
        get() = x.xyz
        set(value) {
            x.xyz = value
        }
    inline var up: Vector3
        get() = y.xyz
        set(value) {
            y.xyz = value
        }
    inline var forward: Vector3
        get() = z.xyz
        set(value) {
            z.xyz = value
        }
    inline var position: Vector3
        get() = w.xyz
        set(value) {
            w.xyz = value
        }

    inline val scale: Vector3
        get() = Vector3(length(x.xyz), length(y.xyz), length(z.xyz))
    inline val translation: Vector3
        get() = w.xyz
    val rotation: Vector3
        get() {
            val x = normalize(right)
            val y = normalize(up)
            val z = normalize(forward)

            return when {
                z.y <= -1.0f -> Vector3(degrees(-HALF_PI), 0.0f, degrees(atan2(x.z, y.z)))
                z.y >= 1.0f -> Vector3(degrees(HALF_PI), 0.0f, degrees(atan2(-x.z, -y.z)))
                else -> Vector3(
                    degrees(-asin(z.y)), degrees(-atan2(z.x, z.z)), degrees(atan2(x.y, y.y))
                )
            }
        }

    inline val upperLeft: Matrix3
        get() = Matrix3(x.xyz, y.xyz, z.xyz)

    operator fun get(column: Int) = when (column) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> throw IllegalArgumentException("column must be in 0..3")
    }

    operator fun get(column: Int, row: Int) = get(column)[row]

    operator fun get(column: MatrixColumn) = when (column) {
        MatrixColumn.X -> x
        MatrixColumn.Y -> y
        MatrixColumn.Z -> z
        MatrixColumn.W -> w
    }

    operator fun get(column: MatrixColumn, row: Int) = get(column)[row]

    fun getRow(row: Int): Vector4 {
        return Vector4(x[row], y[row], z[row], w[row])
    }

    operator fun set(column: Int, v: Vector4) {
        this[column].xyzw = v
    }

    operator fun set(column: Int, row: Int, v: Float) {
        this[column][row] = v
    }

    operator fun unaryMinus() = Matrix4(-x, -y, -z, -w)
    operator fun inc() = Matrix4(this).apply {
        ++x
        ++y
        ++z
        ++w
    }

    operator fun dec() = Matrix4(this).apply {
        --x
        --y
        --z
        --w
    }

    operator fun plus(v: Float) = Matrix4(x + v, y + v, z + v, w + v)
    operator fun minus(v: Float) = Matrix4(x - v, y - v, z - v, w - v)
    operator fun times(v: Float) = Matrix4(x * v, y * v, z * v, w * v)
    operator fun div(v: Float) = Matrix4(x / v, y / v, z / v, w / v)

    operator fun times(m: Matrix4): Matrix4 {
        val t = transpose(this)
        return Matrix4(
            Vector4(dot(t.x, m.x), dot(t.y, m.x), dot(t.z, m.x), dot(t.w, m.x)),
            Vector4(dot(t.x, m.y), dot(t.y, m.y), dot(t.z, m.y), dot(t.w, m.y)),
            Vector4(dot(t.x, m.z), dot(t.y, m.z), dot(t.z, m.z), dot(t.w, m.z)),
            Vector4(dot(t.x, m.w), dot(t.y, m.w), dot(t.z, m.w), dot(t.w, m.w))
        )
    }

    operator fun times(v: Vector4): Vector4 {
        val t = transpose(this)
        return Vector4(dot(t.x, v), dot(t.y, v), dot(t.z, v), dot(t.w, v))
    }

    operator fun timesAssign(m: Matrix4) {
        assignColumns(this * m)
    }

    fun assignColumns(other: Matrix4) {
        this.x = other.x
        this.y = other.y
        this.z = other.z
        this.w = other.w
    }

    fun assignFromStorage(storage: List<Float>) {
        check(storage.size == 16)
        x.assignFromStorage(storage.subList(0, 4))
        y.assignFromStorage(storage.subList(4, 8))
        z.assignFromStorage(storage.subList(8, 12))
        w.assignFromStorage(storage.subList(12, 16))
    }

    fun toFloatArray() = floatArrayOf(
        x.x, y.x, z.x, w.x,
        x.y, y.y, z.y, w.y,
        x.z, y.z, z.z, w.z,
        x.w, y.w, z.w, w.w
    )

    override fun toString(): String {
        return """
            |${x.x} ${y.x} ${z.x} ${w.x}|
            |${x.y} ${y.y} ${z.y} ${w.y}|
            |${x.z} ${y.z} ${z.z} ${w.z}|
            |${x.w} ${y.w} ${z.w} ${w.w}|
            """.trimIndent()
    }

    // ***** Required methods from dart's matrix4 *****

    /**
     * Transform [arg] of type [Vector3] using the perspective transformation
     * defined by [this].
     */
    fun perspectiveTransform(arg: Vector3): Vector3 {
        val argStorage = arg.v3storage
        val x_ = m4storage[0] * argStorage[0] +
                m4storage[4] * argStorage[1] +
                m4storage[8] * argStorage[2] +
                m4storage[12]
        val y_ = m4storage[1] * argStorage[0] +
                m4storage[5] * argStorage[1] +
                m4storage[9] * argStorage[2] +
                m4storage[13]
        val z_ = m4storage[2] * argStorage[0] +
                m4storage[6] * argStorage[1] +
                m4storage[10] * argStorage[2] +
                m4storage[14]
        val w_ = 1.0f / (m4storage[3] * argStorage[0] +
                m4storage[7] * argStorage[1] +
                m4storage[11] * argStorage[2] +
                m4storage[15])
        arg.x = x_ * w_
        arg.y = y_ * w_
        arg.z = z_ * w_
        return arg
    }

    /** Returns the determinant of this matrix. */
    val determinant: Float
        get() {
            val det2_01_01 = m4storage[0] * m4storage[5] - m4storage[1] * m4storage[4]
            val det2_01_02 = m4storage[0] * m4storage[6] - m4storage[2] * m4storage[4]
            val det2_01_03 = m4storage[0] * m4storage[7] - m4storage[3] * m4storage[4]
            val det2_01_12 = m4storage[1] * m4storage[6] - m4storage[2] * m4storage[5]
            val det2_01_13 = m4storage[1] * m4storage[7] - m4storage[3] * m4storage[5]
            val det2_01_23 = m4storage[2] * m4storage[7] - m4storage[3] * m4storage[6]
            val det3_201_012 = m4storage[8] * det2_01_12 - m4storage[9] * det2_01_02 +
                    m4storage[10] * det2_01_01
            val det3_201_013 = m4storage[8] * det2_01_13 - m4storage[9] * det2_01_03 +
                    m4storage[11] * det2_01_01
            val det3_201_023 = m4storage[8] * det2_01_23 - m4storage[10] * det2_01_03 +
                    m4storage[11] * det2_01_02
            val det3_201_123 = m4storage[9] * det2_01_23 - m4storage[10] * det2_01_13 +
                    m4storage[11] * det2_01_12
            return -det3_201_123 * m4storage[12] + det3_201_023 * m4storage[13] -
                    det3_201_013 * m4storage[14] + det3_201_012 * m4storage[15]
        }

    /** Invert [this]. */
    fun invert() = copyInverse(this)

    /** Set this matrix to be the inverse of [arg] */
    fun copyInverse(arg: Matrix4): Float {
        val argStorage = arg.m4storage
        val a00 = argStorage[0]
        val a01 = argStorage[1]
        val a02 = argStorage[2]
        val a03 = argStorage[3]
        val a10 = argStorage[4]
        val a11 = argStorage[5]
        val a12 = argStorage[6]
        val a13 = argStorage[7]
        val a20 = argStorage[8]
        val a21 = argStorage[9]
        val a22 = argStorage[10]
        val a23 = argStorage[11]
        val a30 = argStorage[12]
        val a31 = argStorage[13]
        val a32 = argStorage[14]
        val a33 = argStorage[15]
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
            setFrom(arg)
            return 0.0f
        }
        val invDet = 1.0f / det
        val newStorage = MutableList(16) { 0.0f }
        newStorage[0] = ((a11 * b11 - a12 * b10 + a13 * b09) * invDet)
        newStorage[1] = ((-a01 * b11 + a02 * b10 - a03 * b09) * invDet)
        newStorage[2] = ((a31 * b05 - a32 * b04 + a33 * b03) * invDet)
        newStorage[3] = ((-a21 * b05 + a22 * b04 - a23 * b03) * invDet)
        newStorage[4] = ((-a10 * b11 + a12 * b08 - a13 * b07) * invDet)
        newStorage[5] = ((a00 * b11 - a02 * b08 + a03 * b07) * invDet)
        newStorage[6] = ((-a30 * b05 + a32 * b02 - a33 * b01) * invDet)
        newStorage[7] = ((a20 * b05 - a22 * b02 + a23 * b01) * invDet)
        newStorage[8] = ((a10 * b10 - a11 * b08 + a13 * b06) * invDet)
        newStorage[9] = ((-a00 * b10 + a01 * b08 - a03 * b06) * invDet)
        newStorage[10] = ((a30 * b04 - a31 * b02 + a33 * b00) * invDet)
        newStorage[11] = ((-a20 * b04 + a21 * b02 - a23 * b00) * invDet)
        newStorage[12] = ((-a10 * b09 + a11 * b07 - a12 * b06) * invDet)
        newStorage[13] = ((a00 * b09 - a01 * b07 + a02 * b06) * invDet)
        newStorage[14] = ((-a30 * b03 + a31 * b01 - a32 * b00) * invDet)
        newStorage[15] = ((a20 * b03 - a21 * b01 + a22 * b00) * invDet)
        assignFromStorage(newStorage)
        return det
    }

    /** Sets the entire matrix to the matrix in [arg]. */
    fun setFrom(arg: Matrix4) {
        assignFromStorage(arg.m4storage)
    }

    fun rotateX(radians: Float) {
        val c = cos(radians)
        val s = sin(radians)
        val newStorage = m4storage.toMutableList()
        newStorage[0] = 1.0f
        newStorage[1] = 0.0f
        newStorage[2] = 0.0f
        newStorage[4] = 0.0f
        newStorage[5] = c
        newStorage[6] = s
        newStorage[8] = 0.0f
        newStorage[9] = -s
        newStorage[10] = c
        newStorage[3] = 0.0f
        newStorage[7] = 0.0f
        newStorage[11] = 0.0f
        assignFromStorage(newStorage)
    }

    /** Sets the upper 3x3 to a rotation of [radians] around Y */
    fun rotateY(radians: Float) {
        val c = cos(radians)
        val s = sin(radians)
        val newStorage = m4storage.toMutableList()
        newStorage[0] = c
        newStorage[1] = 0.0f
        newStorage[2] = -s
        newStorage[4] = 0.0f
        newStorage[5] = 1.0f
        newStorage[6] = 0.0f
        newStorage[8] = s
        newStorage[9] = 0.0f
        newStorage[10] = c
        newStorage[3] = 0.0f
        newStorage[7] = 0.0f
        newStorage[11] = 0.0f
        assignFromStorage(newStorage)
    }

    /** Sets the upper 3x3 to a rotation of [radians] around Z */
    fun rotateZ(radians: Float) {
        val c = cos(radians)
        val s = sin(radians)
        val newStorage = m4storage.toMutableList()
        newStorage[0] = c
        newStorage[1] = s
        newStorage[2] = 0.0f
        newStorage[4] = -s
        newStorage[5] = c
        newStorage[6] = 0.0f
        newStorage[8] = 0.0f
        newStorage[9] = 0.0f
        newStorage[10] = 1.0f
        newStorage[3] = 0.0f
        newStorage[7] = 0.0f
        newStorage[11] = 0.0f
        assignFromStorage(newStorage)
    }

    /** Sets the translation vector in this homogeneous transformation matrix. */
    fun setTranslationRaw(x: Float, y: Float, z: Float) {
        set(3, 0, x)
        set(3, 1, y)
        set(3, 2, z)
    }

    /** Scale this matrix by a [Vector3], [Vector4], or x,y,z */
    fun scale(x: Any, y: Float? = null, z: Float? = null) {
        var sx: Float? = null
        var sy: Float? = null
        var sz: Float? = null
        val sw = if (x is Vector4) x.w else 1.0f
        if (x is Vector3) {
            sx = x.x
            sy = x.y
            sz = x.z
        } else if (x is Vector4) {
            sx = x.x
            sy = x.y
            sz = x.z
        } else if (x is Float) {
            sx = x
            sy = if (y != null) y else x
            sz = if (z != null) z else x
        }
        sx as Float
        sy as Float
        sz as Float
        val newStorage = m4storage.toMutableList()
        newStorage[0] *= sx
        newStorage[1] *= sx
        newStorage[2] *= sx
        newStorage[3] *= sx
        newStorage[4] *= sy
        newStorage[5] *= sy
        newStorage[6] *= sy
        newStorage[7] *= sy
        newStorage[8] *= sz
        newStorage[9] *= sz
        newStorage[10] *= sz
        newStorage[11] *= sz
        newStorage[12] *= sw
        newStorage[13] *= sw
        newStorage[14] *= sw
        newStorage[15] *= sw
        assignFromStorage(newStorage)
    }

    /** Translate this matrix by a [Vector3], [Vector4], or x,y,z */
    fun translate(x: Any, y: Float = 0.0f, z: Float = 0.0f) {
        var tx: Float? = null
        var ty: Float? = null
        var tz: Float? = null
        var tw = if (x is Vector4) x.w else 1.0f
        if (x is Vector3) {
            tx = x.x
            ty = x.y
            tz = x.z
        } else if (x is Vector4) {
            tx = x.x
            ty = x.y
            tz = x.z
        } else if (x is Float) {
            tx = x
            ty = y
            tz = z
        }
        tx as Float
        ty as Float
        tz as Float
        val newStorage = m4storage.toMutableList()
        val t1 = newStorage[0] * tx +
                newStorage[4] * ty +
                newStorage[8] * tz +
                newStorage[12] * tw
        val t2 = newStorage[1] * tx +
                newStorage[5] * ty +
                newStorage[9] * tz +
                newStorage[13] * tw
        val t3 = newStorage[2] * tx +
                newStorage[6] * ty +
                newStorage[10] * tz +
                newStorage[14] * tw
        val t4 = newStorage[3] * tx +
                newStorage[7] * ty +
                newStorage[11] * tz +
                newStorage[15] * tw
        newStorage[12] = t1
        newStorage[13] = t2
        newStorage[14] = t3
        newStorage[15] = t4
        assignFromStorage(newStorage)
    }
}

/**
 * Returns the given [transform] matrix as an [Offset], if the matrix is
 * nothing but a 2D translation.
 *
 * Otherwise, returns null.
 */
fun Matrix4.getAsTranslation(): Offset? {
    val values = m4storage
    // Values are stored in column-major order.
    return if (values[0] == 1.0f && // col 1
        values[1] == 0.0f &&
        values[2] == 0.0f &&
        values[3] == 0.0f &&
        values[4] == 0.0f && // col 2
        values[5] == 1.0f &&
        values[6] == 0.0f &&
        values[7] == 0.0f &&
        values[8] == 0.0f && // col 3
        values[9] == 0.0f &&
        values[10] == 1.0f &&
        values[11] == 0.0f &&
        values[14] == 0.0f && // bottom of col 4 (values 12 and 13 are the x and y offsets)

        values[15] == 1.0f) {
        Offset(values[12], values[13])
    } else null
}

/**
 * Returns the given [transform] matrix as a [Float] describing a uniform
 * scale, if the matrix is nothing but a symmetric 2D scale transform.
 *
 * Otherwise, returns null.
 */
fun Matrix4.getAsScale(): Float? {
    val values = m4storage
    // Values are stored in column-major order.
    return if (values[1] == 0.0f && // col 1 (value 0 is the scale)
        values[2] == 0.0f &&
        values[3] == 0.0f &&
        values[4] == 0.0f && // col 2 (value 5 is the scale)
        values[6] == 0.0f &&
        values[7] == 0.0f &&
        values[8] == 0.0f && // col 3
        values[9] == 0.0f &&
        values[10] == 1.0f &&
        values[11] == 0.0f &&
        values[12] == 0.0f && // col 4
        values[13] == 0.0f &&
        values[14] == 0.0f &&
        values[15] == 1.0f &&
        values[0] == values[5]) { // uniform scale
        values[0]
    } else null
}

/**
 * Returns true if the given matrices are exactly equal, and false
 * otherwise. Null values are assumed to be the identity matrix.
 */
fun matrixEquals(a: Matrix4?, b: Matrix4?): Boolean {
    if (a === b)
        return true
    check(a != null || b != null)
    if (a == null)
        return b!!.isIdentity()
    if (b == null) {
        return a.isIdentity()
    }
    val astorage = a.m4storage
    val bstorage = b.m4storage
    return astorage.subList(0, 16) == bstorage.subList(0, 16)
}

/** Whether the given matrix is the identity matrix. */
fun Matrix4.isIdentity(): Boolean {
    val storage = m4storage
    return (storage[0] == 1.0f && // col 1
            storage[1] == 0.0f &&
            storage[2] == 0.0f &&
            storage[3] == 0.0f &&
            storage[4] == 0.0f && // col 2
            storage[5] == 1.0f &&
            storage[6] == 0.0f &&
            storage[7] == 0.0f &&
            storage[8] == 0.0f && // col 3
            storage[9] == 0.0f &&
            storage[10] == 1.0f &&
            storage[11] == 0.0f &&
            storage[12] == 0.0f && // col 4
            storage[13] == 0.0f &&
            storage[14] == 0.0f &&
            storage[15] == 1.0f)
}

/**
 * Applies the given matrix as a perspective transform to the given point.
 *
 * this function assumes the given point has a z-coordinate of 0.0. the
 * z-coordinate of the result is ignored.
 */
fun Matrix4.transformPoint(point: Offset): Offset {
    val position3 = Vector3(point.x, point.y, 0.0f)
    val transformed3 = perspectiveTransform(position3)
    return Offset(transformed3.x, transformed3.y)
}

/**
 * Returns a rect that bounds the result of applying the given matrix as a
 * perspective transform to the given rect.
 *
 * This function assumes the given rect is in the plane with z equals 0.0.
 * The transformed rect is then projected back into the plane with z equals
 * 0.0 before computing its bounding rect.
 */
fun Matrix4.transformRect(rect: Rect): Rect {
    val point1 = transformPoint(rect.topLeft)
    val point2 = transformPoint(rect.topRight)
    val point3 = transformPoint(rect.bottomLeft)
    val point4 = transformPoint(rect.bottomRight)
    return Rect(
        min4(point1.x, point2.x, point3.x, point4.x),
        min4(point1.y, point2.y, point3.y, point4.y),
        max4(point1.x, point2.x, point3.x, point4.x),
        max4(point1.y, point2.y, point3.y, point4.y)
    )
}

private fun min4(a: Float, b: Float, c: Float, d: Float): Float {
    return minOf(a, minOf(b, minOf(c, d)))
}

private fun max4(a: Float, b: Float, c: Float, d: Float): Float {
    return maxOf(a, maxOf(b, maxOf(c, d)))
}

/**
 * Returns a rect that bounds the result of applying the inverse of the given
 * matrix as a perspective transform to the given rect.
 *
 * This function assumes the given rect is in the plane with z equals 0.0.
 * The transformed rect is then projected back into the plane with z equals
 * 0.0 before computing its bounding rect.
 */
fun inverseTransformRect(transform: Matrix4, rect: Rect): Rect {
    check(transform.determinant != 0.0f)
    if (transform.isIdentity())
        return rect
    val inverted = Matrix4(transform).apply { invert() }
    return inverted.transformRect(rect)
}
