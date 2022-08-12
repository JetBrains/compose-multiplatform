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
import androidx.compose.runtime.collection.mutableVectorOf

internal class NestedVectorStack<T> {
    private var current: Int = -1
    private var lastIndex = 0
    private var indexes = IntArray(16)
    private val vectors = mutableVectorOf<MutableVector<T>>()
    private fun pushIndex(value: Int) {
        if (lastIndex >= indexes.size) {
            indexes = indexes.copyOf(indexes.size * 2)
        }
        indexes[lastIndex++] = value
    }

    fun isNotEmpty(): Boolean {
        return current >= 0 && indexes[current] >= 0
    }

    fun pop(): T {
        val i = current
        val index = indexes[i]
        val vector = vectors[i]
        if (index > 0) indexes[i]--
        else if (index == 0) {
            vectors.removeAt(i)
            current--
        }
        return vector[index]
    }

    fun push(vector: MutableVector<T>) {
        if (vector.isNotEmpty()) {
            vectors.add(vector)
            pushIndex(vector.size - 1)
            current++
        }
    }
}