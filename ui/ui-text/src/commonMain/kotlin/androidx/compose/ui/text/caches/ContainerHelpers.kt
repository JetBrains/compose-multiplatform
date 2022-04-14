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

package androidx.compose.ui.text.caches

/**
 * Copied from ContainerHelpers.binarySearch until collection2 can be linked
 */
@kotlin.jvm.JvmField
internal val EMPTY_INTS = IntArray(0)

/**
 * Copied from ContainerHelpers.binarySearch until collection2 can be linked
 */
@kotlin.jvm.JvmField
internal val EMPTY_OBJECTS = arrayOfNulls<Any>(0)

/**
 * Copied from ContainerHelpers.binarySearch until collection2 can be linked
 */
internal fun IntArray.binarySearchInternal(size: Int, value: Int): Int {
    var lo = 0
    var hi = size - 1
    while (lo <= hi) {
        val mid = lo + hi ushr 1
        val midVal = this[mid]
        if (midVal < value) {
            lo = mid + 1
        } else if (midVal > value) {
            hi = mid - 1
        } else {
            return mid // value found
        }
    }
    return lo.inv() // value not present
}