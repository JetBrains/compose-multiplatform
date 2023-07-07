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

import android.graphics.Matrix as AndroidMatrix
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.setFrom

/**
 * Helper class to cache a [Matrix] and inverse [Matrix], allowing the instance to be reused until
 * the Layer's properties have changed, causing it to call [invalidate].
 *
 * This allows us to avoid repeated calls to [AndroidMatrix.getValues], which calls
 * an expensive native method (nGetValues). If we know the matrix hasn't changed, we can just
 * re-use it without needing to read and update values.
 */
internal class LayerMatrixCache<T>(
    private val getMatrix: (target: T, matrix: AndroidMatrix) -> Unit
) {
    private var androidMatrixCache: AndroidMatrix? = null
    private var previousAndroidMatrix: AndroidMatrix? = null
    private var matrixCache: Matrix? = null
    private var inverseMatrixCache: Matrix? = null

    private var isDirty = true
    private var isInverseDirty = true
    private var isInverseValid = true

    /**
     * Ensures that the internal matrix will be updated next time [calculateMatrix] or
     * [calculateInverseMatrix] is called - this should be called when something that will
     * change the matrix calculation has happened.
     */
    fun invalidate() {
        isDirty = true
        isInverseDirty = true
    }

    /**
     * Returns the cached [Matrix], updating it if required (if [invalidate] was previously called).
     */
    fun calculateMatrix(target: T): Matrix {
        val matrix = matrixCache ?: Matrix().also {
            matrixCache = it
        }
        if (!isDirty) {
            return matrix
        }

        val cachedMatrix = androidMatrixCache ?: AndroidMatrix().also {
            androidMatrixCache = it
        }

        getMatrix(target, cachedMatrix)

        val prevMatrix = previousAndroidMatrix
        if (prevMatrix == null || cachedMatrix != prevMatrix) {
            matrix.setFrom(cachedMatrix)
            androidMatrixCache = prevMatrix
            previousAndroidMatrix = cachedMatrix
        }

        isDirty = false
        return matrix
    }

    /**
     * Returns the cached inverse [Matrix], updating it if required (if [invalidate] was previously
     * called). This returns `null` if the inverse matrix isn't valid. This can happen, for example,
     * when scaling is 0.
     */
    fun calculateInverseMatrix(target: T): Matrix? {
        val matrix = inverseMatrixCache ?: Matrix().also {
            inverseMatrixCache = it
        }
        if (isInverseDirty) {
            val normalMatrix = calculateMatrix(target)
            isInverseValid = normalMatrix.invertTo(matrix)
            isInverseDirty = false
        }
        return if (isInverseValid) matrix else null
    }
}
