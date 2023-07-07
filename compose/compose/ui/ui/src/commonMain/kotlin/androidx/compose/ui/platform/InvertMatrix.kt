/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.ui.graphics.Matrix

/**
 * Sets [other] to be the inverse of this. Returns `true` if the inverse worked or `false`
 * if it failed.
 */
internal fun Matrix.invertTo(other: Matrix): Boolean {
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
        return false
    }
    val invDet = 1.0f / det
    other[0, 0] = ((a11 * b11 - a12 * b10 + a13 * b09) * invDet)
    other[0, 1] = ((-a01 * b11 + a02 * b10 - a03 * b09) * invDet)
    other[0, 2] = ((a31 * b05 - a32 * b04 + a33 * b03) * invDet)
    other[0, 3] = ((-a21 * b05 + a22 * b04 - a23 * b03) * invDet)
    other[1, 0] = ((-a10 * b11 + a12 * b08 - a13 * b07) * invDet)
    other[1, 1] = ((a00 * b11 - a02 * b08 + a03 * b07) * invDet)
    other[1, 2] = ((-a30 * b05 + a32 * b02 - a33 * b01) * invDet)
    other[1, 3] = ((a20 * b05 - a22 * b02 + a23 * b01) * invDet)
    other[2, 0] = ((a10 * b10 - a11 * b08 + a13 * b06) * invDet)
    other[2, 1] = ((-a00 * b10 + a01 * b08 - a03 * b06) * invDet)
    other[2, 2] = ((a30 * b04 - a31 * b02 + a33 * b00) * invDet)
    other[2, 3] = ((-a20 * b04 + a21 * b02 - a23 * b00) * invDet)
    other[3, 0] = ((-a10 * b09 + a11 * b07 - a12 * b06) * invDet)
    other[3, 1] = ((a00 * b09 - a01 * b07 + a02 * b06) * invDet)
    other[3, 2] = ((-a30 * b03 + a31 * b01 - a32 * b00) * invDet)
    other[3, 3] = ((a20 * b03 - a21 * b01 + a22 * b00) * invDet)
    return true
}