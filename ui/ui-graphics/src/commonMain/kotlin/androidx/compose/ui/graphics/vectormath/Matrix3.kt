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
package androidx.compose.ui.graphics.vectormath

data class Matrix3(
    var x: Vector3 = Vector3(x = 1.0f),
    var y: Vector3 = Vector3(y = 1.0f),
    var z: Vector3 = Vector3(z = 1.0f)
) {
    constructor(m: Matrix3) : this(m.x.copy(), m.y.copy(), m.z.copy())

    companion object {
        fun of(vararg a: Float): Matrix3 {
            require(a.size >= 9)
            return Matrix3(
                    Vector3(a[0], a[3], a[6]),
                    Vector3(a[1], a[4], a[7]),
                    Vector3(a[2], a[5], a[8])
            )
        }

        fun identity() = Matrix3()
    }

    inline val m3storage: List<Float>
        get() = x.v3storage + y.v3storage + z.v3storage

    operator fun get(column: Int) = when (column) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IllegalArgumentException("column must be in 0..2")
    }
    operator fun get(column: Int, row: Int) = get(column)[row]

    operator fun get(column: MatrixColumn) = when (column) {
        MatrixColumn.X -> x
        MatrixColumn.Y -> y
        MatrixColumn.Z -> z
        else -> throw IllegalArgumentException("column must be X, Y or Z")
    }
    operator fun get(column: MatrixColumn, row: Int) = get(column)[row]

    operator fun set(column: Int, v: Vector3) {
        this[column].xyz = v
    }
    operator fun set(column: Int, row: Int, v: Float) {
        this[column][row] = v
    }

    operator fun unaryMinus() = Matrix3(-x, -y, -z)
    operator fun inc() = Matrix3(this).apply {
        ++x
        ++y
        ++z
    }
    operator fun dec() = Matrix3(this).apply {
        --x
        --y
        --z
    }

    operator fun plus(v: Float) = Matrix3(x + v, y + v, z + v)
    operator fun minus(v: Float) = Matrix3(x - v, y - v, z - v)
    operator fun times(v: Float) = Matrix3(x * v, y * v, z * v)
    operator fun div(v: Float) = Matrix3(x / v, y / v, z / v)

    operator fun times(m: Matrix3): Matrix3 {
        val t = transpose(this)
        return Matrix3(
                Vector3(dot(t.x, m.x), dot(t.y, m.x), dot(t.z, m.x)),
                Vector3(dot(t.x, m.y), dot(t.y, m.y), dot(t.z, m.y)),
                Vector3(dot(t.x, m.z), dot(t.y, m.z), dot(t.z, m.z))
        )
    }

    operator fun times(v: Vector3): Vector3 {
        val t = transpose(this)
        return Vector3(dot(t.x, v), dot(t.y, v), dot(t.z, v))
    }

    fun toFloatArray() = floatArrayOf(
            x.x, y.x, z.x,
            x.y, y.y, z.y,
            x.z, y.z, z.z
    )

    override fun toString(): String {
        return """
            |${x.x} ${y.x} ${z.x}|
            |${x.y} ${y.y} ${z.y}|
            |${x.z} ${y.z} ${z.z}|
            """.trimIndent()
    }
}
