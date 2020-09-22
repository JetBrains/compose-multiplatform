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

/**
 * Set the matrix values the native [android.graphics.Matrix].
 */
fun Matrix.setFrom(matrix: android.graphics.Matrix) {
    val v = values
    matrix.getValues(v)

    val v30 = v[2]
    val v01 = v[3]
    val v11 = v[4]
    val v31 = v[5]
    val v03 = v[6]
    val v13 = v[7]
    val v33 = v[8]

    // this[0, 0] and this[1, 0] are already set properly
    this[2, 0] = 0f
    this[3, 0] = v30
    this[0, 1] = v01
    this[1, 1] = v11
    this[2, 1] = 0f
    this[3, 1] = v31
    this[0, 2] = 0f
    this[1, 2] = 0f
    this[2, 2] = 1f
    this[3, 2] = 0f
    this[0, 3] = v03
    this[1, 3] = v13
    this[2, 3] = 0f
    this[3, 3] = v33
}

/**
 * Set the native [android.graphics.Matrix] from [matrix].
 */
fun android.graphics.Matrix.setFrom(matrix: Matrix) {
    require(
        matrix[0, 2] == 0f &&
            matrix[1, 2] == 0f &&
            matrix[2, 2] == 1f &&
            matrix[3, 2] == 0f &&
            matrix[2, 0] == 0f &&
            matrix[2, 1] == 0f &&
            matrix[2, 3] == 0f
    ) {
        "Android does not support arbitrary transforms"
    }
    val v01 = matrix[0, 1]
    val v03 = matrix[0, 3]
    val v11 = matrix[1, 1]
    val v13 = matrix[1, 3]
    val v30 = matrix[3, 0]
    val v31 = matrix[3, 1]
    val v33 = matrix[3, 3]

    // We'll reuse the array used in Matrix to avoid allocation by temporarily
    // setting it to the 3x3 matrix used by android.graphics.Matrix
    val v = matrix.values
    v[2] = v30
    v[3] = v01
    v[4] = v11
    v[5] = v31
    v[6] = v03
    v[7] = v13
    v[8] = v33
    setValues(v)

    // now reset the values we just set temporarily
    v[2] = 0f
    v[3] = v30
    v[4] = v01
    v[5] = v11
    v[6] = 0f
    v[7] = v31
    v[8] = 0f
}
