/*
 * Copyright 2023 The Android Open Source Project
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

internal actual class SortedSet<E> actual constructor(
    private val comparator: Comparator<in E>
) {
    private val list = mutableListOf<E>()

    actual fun first(): E = list.first()

    actual fun add(element: E): Boolean {
        var index = list.binarySearch(element, comparator)
        if (index < 0) {
            index = -index - 1
        }
        list.add(index, element)
        return true
    }

    actual fun remove(element: E): Boolean {
        val index = list.binarySearch(element, comparator)
        val found = index in list.indices
        if (found) {
            list.removeAt(index)
        }
        return found
    }

    actual fun contains(element: E): Boolean {
        val index = list.binarySearch(element, comparator)
        return index in list.indices && list[index] == element
    }

    actual fun isEmpty(): Boolean = list.isEmpty()
}
