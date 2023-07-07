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

package androidx.compose.ui.util

/**
 * Helper for implementing a wrapper around some collections
 * that has only `add` and `removeAt` methods.
 */
internal abstract class AddRemoveMutableList<T> : AbstractMutableList<T>() {
    override fun set(index: Int, element: T): T {
        val old = removeAt(index)
        add(index, element)
        return old
    }

    override fun add(index: Int, element: T) {
        if (index == size) {
            performAdd(element)
        } else {
            val removed = slice(index until size)
            removeRange(index, size)
            performAdd(element)
            addAll(removed)
        }
    }

    override fun removeAt(index: Int): T {
        val old = this[index]
        performRemove(index)
        return old
    }

    protected abstract fun performAdd(element: T)

    protected abstract fun performRemove(index: Int)
}
