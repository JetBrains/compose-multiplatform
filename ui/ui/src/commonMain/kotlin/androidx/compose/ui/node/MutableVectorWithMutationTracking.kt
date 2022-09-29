/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.runtime.collection.MutableVector

/**
 * This class tracks the mutation to the provided [vector] through the provided methods.
 * On mutation, the [onVectorMutated] lambda will be invoked.
 */
internal class MutableVectorWithMutationTracking<T>(
    val vector: MutableVector<T>,
    val onVectorMutated: () -> Unit,
) {
    val size: Int
        get() = vector.size

    fun clear() {
        vector.clear()
        onVectorMutated()
    }

    fun add(index: Int, element: T) {
        vector.add(index, element)
        onVectorMutated()
    }

    fun removeAt(index: Int): T {
        return vector.removeAt(index).also {
            onVectorMutated()
        }
    }

    inline fun forEach(block: (T) -> Unit) = vector.forEach(block)

    fun asList(): List<T> = vector.asMutableList()

    operator fun get(index: Int): T = vector[index]
}