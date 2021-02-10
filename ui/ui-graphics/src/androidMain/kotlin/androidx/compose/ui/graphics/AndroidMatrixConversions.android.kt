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
    val scaleX = v[android.graphics.Matrix.MSCALE_X]
    val skewX = v[android.graphics.Matrix.MSKEW_X]
    val translateX = v[android.graphics.Matrix.MTRANS_X]
    val skewY = v[android.graphics.Matrix.MSKEW_Y]
    val scaleY = v[android.graphics.Matrix.MSCALE_Y]
    val translateY = v[android.graphics.Matrix.MTRANS_Y]
    val persp0 = v[android.graphics.Matrix.MPERSP_0]
    val persp1 = v[android.graphics.Matrix.MPERSP_1]
    val persp2 = v[android.graphics.Matrix.MPERSP_2]

    v[Matrix.ScaleX] = scaleX // 0
    v[Matrix.SkewY] = skewY // 1
    v[2] = 0f // 2
    v[Matrix.Perspective0] = persp0 // 3
    v[Matrix.SkewX] = skewX // 4
    v[Matrix.ScaleY] = scaleY // 5
    v[6] = 0f // 6
    v[Matrix.Perspective1] = persp1 // 7
    v[8] = 0f // 8
    v[9] = 0f // 9
    v[Matrix.ScaleZ] = 1.0f // 10
    v[11] = 0f // 11
    v[Matrix.TranslateX] = translateX // 12
    v[Matrix.TranslateY] = translateY // 13
    v[14] = 0f // 14
    v[Matrix.Perspective2] = persp2 // 15
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

    // We'll reuse the array used in Matrix to avoid allocation by temporarily
    // setting it to the 3x3 matrix used by android.graphics.Matrix
    // Store the values of the 4 x 4 matrix into temporary variables
    // to be reset after the 3 x 3 matrix is configured
    val scaleX = matrix.values[Matrix.ScaleX] // 0
    val skewY = matrix.values[Matrix.SkewY] // 1
    val v2 = matrix.values[2] // 2
    val persp0 = matrix.values[Matrix.Perspective0] // 3
    val skewX = matrix.values[Matrix.SkewX] // 4
    val scaleY = matrix.values[Matrix.ScaleY] // 5
    val v6 = matrix.values[6] // 6
    val persp1 = matrix.values[Matrix.Perspective1] // 7
    val v8 = matrix.values[8] // 8

    val translateX = matrix.values[Matrix.TranslateX]
    val translateY = matrix.values[Matrix.TranslateY]
    val persp2 = matrix.values[Matrix.Perspective2]

    val v = matrix.values

    v[android.graphics.Matrix.MSCALE_X] = scaleX
    v[android.graphics.Matrix.MSKEW_X] = skewX
    v[android.graphics.Matrix.MTRANS_X] = translateX
    v[android.graphics.Matrix.MSKEW_Y] = skewY
    v[android.graphics.Matrix.MSCALE_Y] = scaleY
    v[android.graphics.Matrix.MTRANS_Y] = translateY
    v[android.graphics.Matrix.MPERSP_0] = persp0
    v[android.graphics.Matrix.MPERSP_1] = persp1
    v[android.graphics.Matrix.MPERSP_2] = persp2

    setValues(v)

    // Reset the values back after the android.graphics.Matrix is configured
    v[Matrix.ScaleX] = scaleX // 0
    v[Matrix.SkewY] = skewY // 1
    v[2] = v2 // 2
    v[Matrix.Perspective0] = persp0 // 3
    v[Matrix.SkewX] = skewX // 4
    v[Matrix.ScaleY] = scaleY // 5
    v[6] = v6 // 6
    v[Matrix.Perspective1] = persp1 // 7
    v[8] = v8 // 8
}
