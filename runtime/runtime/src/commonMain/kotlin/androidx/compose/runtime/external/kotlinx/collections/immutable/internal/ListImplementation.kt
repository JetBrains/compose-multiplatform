/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.internal

import kotlin.jvm.JvmStatic

internal object ListImplementation {

    @JvmStatic
    internal fun checkElementIndex(index: Int, size: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("index: $index, size: $size")
        }
    }

    @JvmStatic
    internal fun checkPositionIndex(index: Int, size: Int) {
        if (index < 0 || index > size) {
            throw IndexOutOfBoundsException("index: $index, size: $size")
        }
    }

    @JvmStatic
    internal fun checkRangeIndexes(fromIndex: Int, toIndex: Int, size: Int) {
        if (fromIndex < 0 || toIndex > size) {
            throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex, size: $size")
        }
        if (fromIndex > toIndex) {
            throw IllegalArgumentException("fromIndex: $fromIndex > toIndex: $toIndex")
        }
    }

    @JvmStatic
    internal fun orderedHashCode(c: Collection<*>): Int {
        var hashCode = 1
        for (e in c) {
            hashCode = 31 * hashCode + (e?.hashCode() ?: 0)
        }
        return hashCode
    }

    @JvmStatic
    internal fun orderedEquals(c: Collection<*>, other: Collection<*>): Boolean {
        if (c.size != other.size) return false

        val otherIterator = other.iterator()
        for (elem in c) {
            val elemOther = otherIterator.next()
            if (elem != elemOther) {
                return false
            }
        }
        return true
    }
}