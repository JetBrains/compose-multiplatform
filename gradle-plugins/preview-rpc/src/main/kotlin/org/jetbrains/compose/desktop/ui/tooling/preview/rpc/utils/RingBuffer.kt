/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc.utils

internal class RingBuffer<T : Any>(internal val maxSize: Int) : Iterable<T> {
    private var start = 0
    private var size = 0
    private val values = arrayOfNulls<Any?>(maxSize)

    init {
        check(maxSize > 0) { "Max size should be a positive number: $maxSize" }
    }

    fun add(element: T) {
        if (size < maxSize) {
            size++
        } else {
            start = (start + 1) % maxSize
        }
        values[(start + size - 1) % maxSize] = element
    }

    fun addAll(elements: Iterable<T>) {
        elements.forEach { add(it) }
    }

    fun clear() {
        start = 0
        size = 0
        for (i in values.indices) {
            values[i] = null
        }
    }

    override fun iterator(): Iterator<T> =
        object : Iterator<T> {
            private var i = 0

            override fun hasNext(): Boolean = i < size

            override fun next(): T {
                if (!hasNext()) throw NoSuchElementException()

                @Suppress("UNCHECKED_CAST")
                return values[(start + i++) % maxSize] as T
            }
        }
}