/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableSet

import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.assert
import kotlin.js.JsName

internal open class PersistentHashSetIterator<E>(node: TrieNode<E>) : Iterator<E> {
    protected val path = mutableListOf(TrieNodeIterator<E>())
    protected var pathLastIndex = 0
    @JsName("_hasNext")
    private var hasNext = true

    init {
        path[0].reset(node.buffer)
        pathLastIndex = 0
        ensureNextElementIsReady()
    }

    private fun moveToNextNodeWithData(pathIndex: Int): Int {
        if (path[pathIndex].hasNextElement()) {
            return pathIndex
        }
        if (path[pathIndex].hasNextNode()) {
            val node = path[pathIndex].currentNode()

            if (pathIndex + 1 == path.size) {
                path.add(TrieNodeIterator())
            }
            path[pathIndex + 1].reset(node.buffer)
            return moveToNextNodeWithData(pathIndex + 1)
        }
        return -1
    }

    private fun ensureNextElementIsReady() {
        if (path[pathLastIndex].hasNextElement()) {
            return
        }
        for(i in pathLastIndex downTo 0) {
            var result = moveToNextNodeWithData(i)

            if (result == -1 && path[i].hasNextCell()) {
                path[i].moveToNextCell()
                result = moveToNextNodeWithData(i)
            }
            if (result != -1) {
                pathLastIndex = result
                return
            }
            if (i > 0) {
                path[i - 1].moveToNextCell()
            }
            path[i].reset(TrieNode.EMPTY.buffer, 0)
        }
        hasNext = false
    }

    override fun hasNext(): Boolean {
        return hasNext
    }

    override fun next(): E {
        if (!hasNext)
            throw NoSuchElementException()

        val result = path[pathLastIndex].nextElement()
        ensureNextElementIsReady()
        return result
    }

    protected fun currentElement(): E {
        assert(hasNext())
        return path[pathLastIndex].currentElement()
    }
}

internal class TrieNodeIterator<out E> {
    private var buffer = TrieNode.EMPTY.buffer
    private var index = 0

    fun reset(buffer: Array<Any?>, index: Int = 0) {
        this.buffer = buffer
        this.index = index
    }

    fun hasNextCell(): Boolean {
        return index < buffer.size
    }

    fun moveToNextCell() {
        assert(hasNextCell())
        index++
    }

    fun hasNextElement(): Boolean {
        return hasNextCell() && buffer[index] !is TrieNode<*>
    }

    fun currentElement(): E {
        assert(hasNextElement())
        @Suppress("UNCHECKED_CAST")
        return buffer[index] as E
    }

    fun nextElement(): E {
        assert(hasNextElement())
        @Suppress("UNCHECKED_CAST")
        return buffer[index++] as E
    }

    fun hasNextNode(): Boolean {
        return hasNextCell() && buffer[index] is TrieNode<*>
    }

    fun currentNode(): TrieNode<out E> {
        assert(hasNextNode())
        @Suppress("UNCHECKED_CAST")
        return buffer[index] as TrieNode<E>
    }
}
