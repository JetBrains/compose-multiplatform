/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableList

internal class TrieIterator<out E>(root: Array<Any?>,
                                   index: Int,
                                   size: Int,
                                   private var height: Int) : AbstractListIterator<E>(index, size) {
    private var path: Array<Any?> = arrayOfNulls<Any?>(height)
    private var isInRightEdge = index == size

    init {
        path[0] = root
        fillPath(index - if (isInRightEdge) 1 else 0, 1)
    }

    internal fun reset(root: Array<Any?>, index: Int, size: Int, height: Int) {
        this.index = index
        this.size = size
        this.height = height
        if (path.size < height) path = arrayOfNulls(height)
        path[0] = root
        isInRightEdge = index == size

        fillPath(index - if (isInRightEdge) 1 else 0, 1)
    }

    private fun fillPath(index: Int, startLevel: Int) {
        var shift = (height - startLevel) * LOG_MAX_BUFFER_SIZE
        var i = startLevel
        while (i < height) {
            @Suppress("UNCHECKED_CAST")
            path[i] = (path[i - 1] as Array<Any?>)[indexSegment(index, shift)]
            shift -= LOG_MAX_BUFFER_SIZE
            i += 1
        }
    }

    // TODO: Document that it positions path to the first or the last element
    private fun fillPathIfNeeded(indexPredicate: Int) {
        var shift = 0
        while (indexSegment(index, shift) == indexPredicate) {
            shift += LOG_MAX_BUFFER_SIZE
        }

        if (shift > 0) {
            val level = height - 1 - shift / LOG_MAX_BUFFER_SIZE
            fillPath(index, level + 1)
        }
    }

    private fun elementAtCurrentIndex(): E {
        val leafBufferIndex = index and MAX_BUFFER_SIZE_MINUS_ONE
        @Suppress("UNCHECKED_CAST")
        return (path[height - 1] as Array<E>)[leafBufferIndex]
    }

    override fun next(): E {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        val result = elementAtCurrentIndex()
        index += 1

        if (index == size) {
            isInRightEdge = true
            return result
        }

        fillPathIfNeeded(0)

        return result
    }

    override fun previous(): E {
        if (!hasPrevious()) {
            throw NoSuchElementException()
        }

        index -= 1

        if (isInRightEdge) {
            isInRightEdge = false
            return elementAtCurrentIndex()
        }

        fillPathIfNeeded(MAX_BUFFER_SIZE_MINUS_ONE)

        return elementAtCurrentIndex()
    }
}