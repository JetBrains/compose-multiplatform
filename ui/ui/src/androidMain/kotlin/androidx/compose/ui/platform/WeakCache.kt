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

import androidx.compose.runtime.collection.mutableVectorOf
import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

/**
 * A simple collection that keeps values as [WeakReference]s.
 * Elements are added with [push] and removed with [pop].
 */
internal class WeakCache<T> {
    private val values = mutableVectorOf<Reference<T>>()
    private val referenceQueue = ReferenceQueue<T>()

    /**
     * Add [element] to the collection as a [WeakReference]. It will be removed when
     * garbage collected or from [pop].
     */
    fun push(element: T) {
        clearWeakReferences()
        values += WeakReference(element, referenceQueue)
    }

    /**
     * Remove an element from the collection and return it. If no element is
     * available, `null` is returned.
     */
    fun pop(): T? {
        clearWeakReferences()

        while (values.isNotEmpty()) {
            val item = values.removeAt(values.lastIndex).get()
            if (item != null) {
                return item
            }
        }
        return null
    }

    /**
     * The number of elements currently in the collection. This may change between
     * calls if the references have been garbage collected.
     */
    val size: Int
        get() {
            clearWeakReferences()
            return values.size
        }

    private fun clearWeakReferences() {
        do {
            val item: Reference<out T>? = referenceQueue.poll()
            if (item != null) {
                @Suppress("UNCHECKED_CAST")
                values.remove(item as Reference<T>)
            }
        } while (item != null)
    }
}