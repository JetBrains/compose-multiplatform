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

data class Vector4(
    var x: Float = 0.0f,
    var y: Float = 0.0f,
    var z: Float = 0.0f,
    var w: Float = 0.0f
) {
    constructor(v: Vector2, z: Float = 0.0f, w: Float = 0.0f) : this(v.x, v.y, z, w)
    constructor(v: Vector3, w: Float = 0.0f) : this(v.x, v.y, v.z, w)
    constructor(v: Vector4) : this(v.x, v.y, v.z, v.w)

    inline val v4storage: List<Float>
        get() = listOf(x, y, z, w)

    inline var r: Float
        get() = x
        set(value) {
            x = value
        }
    inline var g: Float
        get() = y
        set(value) {
            y = value
        }
    inline var b: Float
        get() = z
        set(value) {
            z = value
        }
    inline var a: Float
        get() = w
        set(value) {
            w = value
        }

    inline var s: Float
        get() = x
        set(value) {
            x = value
        }
    inline var t: Float
        get() = y
        set(value) {
            y = value
        }
    inline var p: Float
        get() = z
        set(value) {
            z = value
        }
    inline var q: Float
        get() = w
        set(value) {
            w = value
        }

    inline var xy: Vector2
        get() = Vector2(x, y)
        set(value) {
            x = value.x
            y = value.y
        }
    inline var rg: Vector2
        get() = Vector2(x, y)
        set(value) {
            x = value.x
            y = value.y
        }
    inline var st: Vector2
        get() = Vector2(x, y)
        set(value) {
            x = value.x
            y = value.y
        }

    inline var rgb: Vector3
        get() = Vector3(x, y, z)
        set(value) {
            x = value.x
            y = value.y
            z = value.z
        }
    inline var xyz: Vector3
        get() = Vector3(x, y, z)
        set(value) {
            x = value.x
            y = value.y
            z = value.z
        }
    inline var stp: Vector3
        get() = Vector3(x, y, z)
        set(value) {
            x = value.x
            y = value.y
            z = value.z
        }

    inline var rgba: Vector4
        get() = Vector4(x, y, z, w)
        set(value) {
            x = value.x
            y = value.y
            z = value.z
            w = value.w
        }
    inline var xyzw: Vector4
        get() = Vector4(x, y, z, w)
        set(value) {
            x = value.x
            y = value.y
            z = value.z
            w = value.w
        }
    inline var stpq: Vector4
        get() = Vector4(x, y, z, w)
        set(value) {
            x = value.x
            y = value.y
            z = value.z
            w = value.w
        }

    operator fun get(index: VectorComponent) = when (index) {
        VectorComponent.X, VectorComponent.R, VectorComponent.S -> x
        VectorComponent.Y, VectorComponent.G, VectorComponent.T -> y
        VectorComponent.Z, VectorComponent.B, VectorComponent.P -> z
        VectorComponent.W, VectorComponent.A, VectorComponent.Q -> w
    }

    operator fun get(index1: VectorComponent, index2: VectorComponent): Vector2 {
        return Vector2(get(index1), get(index2))
    }
    operator fun get(
        index1: VectorComponent,
        index2: VectorComponent,
        index3: VectorComponent
    ): Vector3 {
        return Vector3(get(index1), get(index2), get(index3))
    }
    operator fun get(
        index1: VectorComponent,
        index2: VectorComponent,
        index3: VectorComponent,
        index4: VectorComponent
    ): Vector4 {
        return Vector4(get(index1), get(index2), get(index3), get(index4))
    }

    operator fun get(index: Int) = when (index) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> throw IllegalArgumentException("index must be in 0..3")
    }

    operator fun get(index1: Int, index2: Int) = Vector2(get(index1), get(index2))
    operator fun get(index1: Int, index2: Int, index3: Int): Vector3 {
        return Vector3(get(index1), get(index2), get(index3))
    }
    operator fun get(index1: Int, index2: Int, index3: Int, index4: Int): Vector4 {
        return Vector4(get(index1), get(index2), get(index3), get(index4))
    }

    operator fun set(index: Int, v: Float) = when (index) {
        0 -> x = v
        1 -> y = v
        2 -> z = v
        3 -> w = v
        else -> throw IllegalArgumentException("index must be in 0..3")
    }

    operator fun set(index1: Int, index2: Int, v: Float) {
        set(index1, v)
        set(index2, v)
    }

    operator fun set(index1: Int, index2: Int, index3: Int, v: Float) {
        set(index1, v)
        set(index2, v)
        set(index3, v)
    }

    operator fun set(index1: Int, index2: Int, index3: Int, index4: Int, v: Float) {
        set(index1, v)
        set(index2, v)
        set(index3, v)
        set(index4, v)
    }

    operator fun set(index: VectorComponent, v: Float) = when (index) {
        VectorComponent.X, VectorComponent.R, VectorComponent.S -> x = v
        VectorComponent.Y, VectorComponent.G, VectorComponent.T -> y = v
        VectorComponent.Z, VectorComponent.B, VectorComponent.P -> z = v
        VectorComponent.W, VectorComponent.A, VectorComponent.Q -> w = v
    }

    operator fun set(index1: VectorComponent, index2: VectorComponent, v: Float) {
        set(index1, v)
        set(index2, v)
    }

    operator fun set(
        index1: VectorComponent,
        index2: VectorComponent,
        index3: VectorComponent,
        v: Float
    ) {
        set(index1, v)
        set(index2, v)
        set(index3, v)
    }

    operator fun set(
        index1: VectorComponent,
        index2: VectorComponent,
        index3: VectorComponent,
        index4: VectorComponent,
        v: Float
    ) {
        set(index1, v)
        set(index2, v)
        set(index3, v)
        set(index4, v)
    }

    operator fun unaryMinus() = Vector4(-x, -y, -z, -w)
    operator fun inc() = Vector4(this).apply {
        ++x
        ++y
        ++z
        ++w
    }
    operator fun dec() = Vector4(this).apply {
        --x
        --y
        --z
        --w
    }

    inline operator fun plus(v: Float) = Vector4(x + v, y + v, z + v, w + v)
    inline operator fun minus(v: Float) = Vector4(x - v, y - v, z - v, w - v)
    inline operator fun times(v: Float) = Vector4(x * v, y * v, z * v, w * v)
    inline operator fun div(v: Float) = Vector4(x / v, y / v, z / v, w / v)

    inline operator fun plus(v: Vector2) = Vector4(x + v.x, y + v.y, z, w)
    inline operator fun minus(v: Vector2) = Vector4(x - v.x, y - v.y, z, w)
    inline operator fun times(v: Vector2) = Vector4(x * v.x, y * v.y, z, w)
    inline operator fun div(v: Vector2) = Vector4(x / v.x, y / v.y, z, w)

    inline operator fun plus(v: Vector3) = Vector4(x + v.x, y + v.y, z + v.z, w)
    inline operator fun minus(v: Vector3) = Vector4(x - v.x, y - v.y, z - v.z, w)
    inline operator fun times(v: Vector3) = Vector4(x * v.x, y * v.y, z * v.z, w)
    inline operator fun div(v: Vector3) = Vector4(x / v.x, y / v.y, z / v.z, w)

    inline operator fun plus(v: Vector4) = Vector4(x + v.x, y + v.y, z + v.z, w + v.w)
    inline operator fun minus(v: Vector4) = Vector4(x - v.x, y - v.y, z - v.z, w - v.w)
    inline operator fun times(v: Vector4) = Vector4(x * v.x, y * v.y, z * v.z, w * v.w)
    inline operator fun div(v: Vector4) = Vector4(x / v.x, y / v.y, z / v.z, w / v.w)

    inline fun transform(block: (Float) -> Float): Vector4 {
        x = block(x)
        y = block(y)
        z = block(z)
        w = block(w)
        return this
    }

    fun assignFromStorage(storage: List<Float>) {
        check(storage.size >= 4)
        x = storage[0]
        y = storage[1]
        z = storage[2]
        w = storage[3]
    }

    override fun toString() = "$x,$y,$z,$w"
}
