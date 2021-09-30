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

package androidx.compose.ui.node

// This is not a sorted tree at all,
// but expect class only requires us to provide first()
// above the standard Set interface
internal actual class TreeSet<E> actual constructor(private val comparator: Comparator<in E>) {
    private val backing = hashSetOf<E>()

    actual fun first(): E = backing.minOfWith(comparator) { it }

    actual fun add(element: E): Boolean = backing.add(element)
    actual fun remove(element: E): Boolean = backing.remove(element)
    actual fun contains(element: E): Boolean = backing.contains(element)
    actual fun isEmpty(): Boolean = backing.isEmpty()
}
