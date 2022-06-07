/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.runtime

// TODO(b/139762913): Consider changing to inline class of ArrayList<T> to avoid wrapper class
internal class Stack<T> {
    private val backing = ArrayList<T>()

    val size: Int get() = backing.size

    fun push(value: T) = backing.add(value)
    fun pop(): T = backing.removeAt(size - 1)
    fun peek(): T = backing.get(size - 1)
    fun peek(index: Int): T = backing.get(index)
    fun isEmpty() = backing.isEmpty()
    fun isNotEmpty() = !isEmpty()
    fun clear() = backing.clear()
    @Suppress("UNCHECKED_CAST")
    fun toArray(): Array<T> = Array<Any?>(backing.size) { backing[it] } as Array<T>
}

internal class IntStack {
    private var slots = IntArray(10)
    private var tos = 0

    val size: Int get() = tos

    fun push(value: Int) {
        if (tos >= slots.size) {
            slots = slots.copyOf(slots.size * 2)
        }
        slots[tos++] = value
    }

    fun pop(): Int = slots[--tos]
    fun peekOr(default: Int): Int = if (tos > 0) peek() else default
    fun peek() = slots[tos - 1]
    fun peek(index: Int) = slots[index]
    fun isEmpty() = tos == 0
    fun isNotEmpty() = tos != 0
    fun clear() { tos = 0 }
    fun indexOf(value: Int): Int {
        for (i in 0 until tos)
            if (slots[i] == value) return i
        return -1
    }
}
