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

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

inline operator fun Float.plus(v: Vector2) = Vector2(this + v.x, this + v.y)
inline operator fun Float.minus(v: Vector2) = Vector2(this - v.x, this - v.y)
inline operator fun Float.times(v: Vector2) = Vector2(this * v.x, this * v.y)
inline operator fun Float.div(v: Vector2) = Vector2(this / v.x, this / v.y)

inline fun abs(v: Vector2) = Vector2(abs(v.x), abs(v.y))
inline fun length(v: Vector2) = sqrt(v.x * v.x + v.y * v.y)
inline fun length2(v: Vector2) = v.x * v.x + v.y * v.y
inline fun distance(a: Vector2, b: Vector2) = length(a - b)
inline fun dot(a: Vector2, b: Vector2) = a.x * b.x + a.y * b.y
fun normalize(v: Vector2): Vector2 {
    val l = 1.0f / length(v)
    return Vector2(v.x * l, v.y * l)
}

inline fun reflect(i: Vector2, n: Vector2) = i - 2.0f * dot(n, i) * n
fun refract(i: Vector2, n: Vector2, eta: Float): Vector2 {
    val d = dot(n, i)
    val k = 1.0f - eta * eta * (1.0f - (d * d))
    return if (k < 0.0f) Vector2(0.0f) else eta * i - (eta * d + sqrt(k)) * n
}

inline fun Vector2.coerceIn(min: Float, max: Float): Vector2 {
    return Vector2(
        x.coerceIn(min, max),
        y.coerceIn(min, max)
    )
}

inline fun Vector2.coerceIn(min: Vector2, max: Vector2): Vector2 {
    return Vector2(
        x.coerceIn(min.x, max.x),
        y.coerceIn(min.y, max.y)
    )
}

inline fun min(v: Vector2) = min(v.x, v.y)
inline fun min(a: Vector2, b: Vector2) = Vector2(min(a.x, b.x), min(a.y, b.y))
inline fun max(v: Vector2) = max(v.x, v.y)
inline fun max(a: Vector2, b: Vector2) = Vector2(max(a.x, b.x), max(a.y, b.y))

inline fun transform(v: Vector2, block: (Float) -> Float) = v.copy().transform(block)

inline operator fun Float.plus(v: Vector3) = Vector3(this + v.x, this + v.y, this + v.z)
inline operator fun Float.minus(v: Vector3) = Vector3(this - v.x, this - v.y, this - v.z)
inline operator fun Float.times(v: Vector3) = Vector3(this * v.x, this * v.y, this * v.z)
inline operator fun Float.div(v: Vector3) = Vector3(this / v.x, this / v.y, this / v.z)

inline fun abs(v: Vector3) = Vector3(abs(v.x), abs(v.y), abs(v.z))
inline fun length(v: Vector3) = sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
inline fun length2(v: Vector3) = v.x * v.x + v.y * v.y + v.z * v.z
inline fun distance(a: Vector3, b: Vector3) = length(a - b)
inline fun dot(a: Vector3, b: Vector3) = a.x * b.x + a.y * b.y + a.z * b.z
inline fun cross(a: Vector3, b: Vector3): Vector3 {
    return Vector3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)
}
inline infix fun Vector3.x(v: Vector3): Vector3 {
    return Vector3(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x)
}
fun normalize(v: Vector3): Vector3 {
    val l = 1.0f / length(v)
    return Vector3(v.x * l, v.y * l, v.z * l)
}

inline fun reflect(i: Vector3, n: Vector3) = i - 2.0f * dot(n, i) * n
fun refract(i: Vector3, n: Vector3, eta: Float): Vector3 {
    val d = dot(n, i)
    val k = 1.0f - eta * eta * (1.0f - (d * d))
    return if (k < 0.0f) Vector3(0.0f) else eta * i - (eta * d + sqrt(k)) * n
}

inline fun Vector3.coerceIn(min: Float, max: Float): Vector3 {
    return Vector3(
        x.coerceIn(min, max),
        y.coerceIn(min, max),
        z.coerceIn(min, max)
    )
}

inline fun Vector3.coerceIn(min: Vector3, max: Vector3): Vector3 {
    return Vector3(
        x.coerceIn(min.x, max.x),
        y.coerceIn(min.y, max.y),
        z.coerceIn(min.z, max.z)
    )
}

inline fun min(v: Vector3) = min(v.x, min(v.y, v.z))
inline fun min(a: Vector3, b: Vector3) = Vector3(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))
inline fun max(v: Vector3) = max(v.x, max(v.y, v.z))
inline fun max(a: Vector3, b: Vector3) = Vector3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))

inline fun transform(v: Vector3, block: (Float) -> Float) = v.copy().transform(block)

inline operator fun Float.plus(v: Vector4) = Vector4(this + v.x, this + v.y,
        this + v.z, this + v.w)
inline operator fun Float.minus(v: Vector4) = Vector4(this - v.x, this - v.y,
        this - v.z, this - v.w)
inline operator fun Float.times(v: Vector4) = Vector4(this * v.x, this * v.y,
        this * v.z, this * v.w)
inline operator fun Float.div(v: Vector4) = Vector4(this / v.x, this / v.y, this / v.z, this / v.w)

inline fun abs(v: Vector4) = Vector4(abs(v.x), abs(v.y), abs(v.z), abs(v.w))
inline fun length(v: Vector4) = sqrt(v.x * v.x + v.y * v.y + v.z * v.z + v.w * v.w)
inline fun length2(v: Vector4) = v.x * v.x + v.y * v.y + v.z * v.z + v.w * v.w
inline fun distance(a: Vector4, b: Vector4) = length(a - b)
inline fun dot(a: Vector4, b: Vector4) = a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w
fun normalize(v: Vector4): Vector4 {
    val l = 1.0f / length(v)
    return Vector4(v.x * l, v.y * l, v.z * l, v.w * l)
}

inline fun Vector4.coerceIn(min: Float, max: Float): Vector4 {
    return Vector4(
        x.coerceIn(min, max),
        y.coerceIn(min, max),
        z.coerceIn(min, max),
        w.coerceIn(min, max)
    )
}

inline fun Vector4.coerceIn(min: Vector4, max: Vector4): Vector4 {
    return Vector4(
        x.coerceIn(min.x, max.x),
        y.coerceIn(min.y, max.y),
        z.coerceIn(min.z, max.z),
        w.coerceIn(min.w, max.w)
    )
}

inline fun min(v: Vector4) = min(v.x, min(v.y, min(v.z, v.w)))
inline fun min(a: Vector4, b: Vector4): Vector4 {
    return Vector4(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z), min(a.w, b.w))
}
inline fun max(v: Vector4) = max(v.x, max(v.y, max(v.z, v.w)))
inline fun max(a: Vector4, b: Vector4): Vector4 {
    return Vector4(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z), max(a.w, b.w))
}

inline fun transform(v: Vector4, block: (Float) -> Float) = v.copy().transform(block)
