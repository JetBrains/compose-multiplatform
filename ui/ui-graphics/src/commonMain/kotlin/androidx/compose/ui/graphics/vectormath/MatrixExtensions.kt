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

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

fun transpose(m: Matrix3) = Matrix3(
        Vector3(m.x.x, m.y.x, m.z.x),
        Vector3(m.x.y, m.y.y, m.z.y),
        Vector3(m.x.z, m.y.z, m.z.z)
)
fun inverse(m: Matrix3): Matrix3 {
    val a = m.x.x
    val b = m.x.y
    val c = m.x.z
    val d = m.y.x
    val e = m.y.y
    val f = m.y.z
    val g = m.z.x
    val h = m.z.y
    val i = m.z.z

    val A = e * i - f * h
    val B = f * g - d * i
    val C = d * h - e * g

    val det = a * A + b * B + c * C

    return Matrix3.of(
            A / det, B / det, C / det,
            (c * h - b * i) / det, (a * i - c * g) / det, (b * g - a * h) / det,
            (b * f - c * e) / det, (c * d - a * f) / det, (a * e - b * d) / det
    )
}

fun transpose(m: Matrix4) = Matrix4(
        Vector4(m.x.x, m.y.x, m.z.x, m.w.x),
        Vector4(m.x.y, m.y.y, m.z.y, m.w.y),
        Vector4(m.x.z, m.y.z, m.z.z, m.w.z),
        Vector4(m.x.w, m.y.w, m.z.w, m.w.w)
)
fun inverse(m: Matrix4): Matrix4 {
    val result = Matrix4()

    var pair0 = m.z.z * m.w.w
    var pair1 = m.w.z * m.z.w
    var pair2 = m.y.z * m.w.w
    var pair3 = m.w.z * m.y.w
    var pair4 = m.y.z * m.z.w
    var pair5 = m.z.z * m.y.w
    var pair6 = m.x.z * m.w.w
    var pair7 = m.w.z * m.x.w
    var pair8 = m.x.z * m.z.w
    var pair9 = m.z.z * m.x.w
    var pair10 = m.x.z * m.y.w
    var pair11 = m.y.z * m.x.w

    result.x.x = pair0 * m.y.y + pair3 * m.z.y + pair4 * m.w.y
    result.x.x -= pair1 * m.y.y + pair2 * m.z.y + pair5 * m.w.y
    result.x.y = pair1 * m.x.y + pair6 * m.z.y + pair9 * m.w.y
    result.x.y -= pair0 * m.x.y + pair7 * m.z.y + pair8 * m.w.y
    result.x.z = pair2 * m.x.y + pair7 * m.y.y + pair10 * m.w.y
    result.x.z -= pair3 * m.x.y + pair6 * m.y.y + pair11 * m.w.y
    result.x.w = pair5 * m.x.y + pair8 * m.y.y + pair11 * m.z.y
    result.x.w -= pair4 * m.x.y + pair9 * m.y.y + pair10 * m.z.y
    result.y.x = pair1 * m.y.x + pair2 * m.z.x + pair5 * m.w.x
    result.y.x -= pair0 * m.y.x + pair3 * m.z.x + pair4 * m.w.x
    result.y.y = pair0 * m.x.x + pair7 * m.z.x + pair8 * m.w.x
    result.y.y -= pair1 * m.x.x + pair6 * m.z.x + pair9 * m.w.x
    result.y.z = pair3 * m.x.x + pair6 * m.y.x + pair11 * m.w.x
    result.y.z -= pair2 * m.x.x + pair7 * m.y.x + pair10 * m.w.x
    result.y.w = pair4 * m.x.x + pair9 * m.y.x + pair10 * m.z.x
    result.y.w -= pair5 * m.x.x + pair8 * m.y.x + pair11 * m.z.x

    pair0 = m.z.x * m.w.y
    pair1 = m.w.x * m.z.y
    pair2 = m.y.x * m.w.y
    pair3 = m.w.x * m.y.y
    pair4 = m.y.x * m.z.y
    pair5 = m.z.x * m.y.y
    pair6 = m.x.x * m.w.y
    pair7 = m.w.x * m.x.y
    pair8 = m.x.x * m.z.y
    pair9 = m.z.x * m.x.y
    pair10 = m.x.x * m.y.y
    pair11 = m.y.x * m.x.y

    result.z.x = pair0 * m.y.w + pair3 * m.z.w + pair4 * m.w.w
    result.z.x -= pair1 * m.y.w + pair2 * m.z.w + pair5 * m.w.w
    result.z.y = pair1 * m.x.w + pair6 * m.z.w + pair9 * m.w.w
    result.z.y -= pair0 * m.x.w + pair7 * m.z.w + pair8 * m.w.w
    result.z.z = pair2 * m.x.w + pair7 * m.y.w + pair10 * m.w.w
    result.z.z -= pair3 * m.x.w + pair6 * m.y.w + pair11 * m.w.w
    result.z.w = pair5 * m.x.w + pair8 * m.y.w + pair11 * m.z.w
    result.z.w -= pair4 * m.x.w + pair9 * m.y.w + pair10 * m.z.w
    result.w.x = pair2 * m.z.z + pair5 * m.w.z + pair1 * m.y.z
    result.w.x -= pair4 * m.w.z + pair0 * m.y.z + pair3 * m.z.z
    result.w.y = pair8 * m.w.z + pair0 * m.x.z + pair7 * m.z.z
    result.w.y -= pair6 * m.z.z + pair9 * m.w.z + pair1 * m.x.z
    result.w.z = pair6 * m.y.z + pair11 * m.w.z + pair3 * m.x.z
    result.w.z -= pair10 * m.w.z + pair2 * m.x.z + pair7 * m.y.z
    result.w.w = pair10 * m.z.z + pair4 * m.x.z + pair9 * m.y.z
    result.w.w -= pair8 * m.y.z + pair11 * m.z.z + pair5 * m.x.z

    val determinant = m.x.x * result.x.x + m.y.x * result.x.y +
            m.z.x * result.x.z + m.w.x * result.x.w

    return result / determinant
}

fun scale(s: Vector3) = Matrix4(Vector4(x = s.x), Vector4(y = s.y), Vector4(z = s.z))
fun scale(m: Matrix4) = scale(m.scale)

fun translation(t: Vector3) = Matrix4(w = Vector4(t, 1.0f))
fun translation(m: Matrix4) = translation(m.translation)

fun rotation(m: Matrix4) = Matrix4(normalize(m.right), normalize(m.up), normalize(m.forward))
fun rotation(d: Vector3): Matrix4 {
    val r = transform(d, ::radians)
    val c = transform(r, { x -> cos(x) })
    val s = transform(r, { x -> sin(x) })

    return Matrix4.of(
            c.y * c.z, -c.x * s.z + s.x * s.y * c.z, s.x * s.z + c.x * s.y * c.z, 0.0f,
            c.y * s.z, c.x * c.z + s.x * s.y * s.z, -s.x * c.z + c.x * s.y * s.z, 0.0f,
            -s.y, s.x * c.y, c.x * c.y, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    )
}
fun rotation(axis: Vector3, angle: Float): Matrix4 {
    val x = axis.x
    val y = axis.y
    val z = axis.z

    val r = radians(angle)
    val c = cos(r)
    val s = sin(r)
    val d = 1.0f - c

    return Matrix4.of(
            x * x * d + c, x * y * d - z * s, x * y * d + y * s, 0.0f,
            y * x * d + z * s, y * y * d + c, y * z * d - x * s, 0.0f,
            z * x * d - y * s, z * y * d + x * s, z * z * d + c, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    )
}

fun normal(m: Matrix4) = scale(1.0f / Vector3(length2(m.right),
        length2(m.up), length2(m.forward))) * m

fun lookAt(eye: Vector3, target: Vector3, up: Vector3 = Vector3(z = 1.0f)): Matrix4 {
    return lookTowards(eye, target - eye, up)
}

fun lookTowards(eye: Vector3, forward: Vector3, up: Vector3 = Vector3(z = 1.0f)): Matrix4 {
    val f = normalize(forward)
    val r = normalize(f x up)
    val u = normalize(r x f)
    return Matrix4(Vector4(r), Vector4(u), Vector4(f), Vector4(eye, 1.0f))
}

fun perspective(fov: Float, ratio: Float, near: Float, far: Float): Matrix4 {
    val t = 1.0f / tan(radians(fov) * 0.5f)
    val a = (far + near) / (far - near)
    val b = (2.0f * far * near) / (far - near)
    val c = t / ratio
    return Matrix4(Vector4(x = c), Vector4(y = t), Vector4(z = a, w = 1.0f), Vector4(z = -b))
}

fun ortho(l: Float, r: Float, b: Float, t: Float, n: Float, f: Float) = Matrix4(
        Vector4(x = 2.0f / (r - 1.0f)),
        Vector4(y = 2.0f / (t - b)),
        Vector4(z = -2.0f / (f - n)),
        Vector4(-(r + l) / (r - l), -(t + b) / (t - b), -(f + n) / (f - n), 1.0f)
)
