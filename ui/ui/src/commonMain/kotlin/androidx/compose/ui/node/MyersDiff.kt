/*
 * Copyright 2022 The Android Open Source Project
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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.node

import kotlin.math.abs
import kotlin.math.min

internal interface DiffCallback {
    fun areItemsTheSame(oldIndex: Int, newIndex: Int): Boolean
    fun insert(atIndex: Int, newIndex: Int)
    fun remove(oldIndex: Int)
    fun same(oldIndex: Int, newIndex: Int)
}

// Myers' algorithm uses two lists as axis labels. In DiffUtil's implementation, `x` axis is
// used for old list and `y` axis is used for new list.
/**
 * Calculates the list of update operations that can covert one list into the other one.
 *
 *
 * If your old and new lists are sorted by the same constraint and items never move (swap
 * positions), you can disable move detection which takes `O(N^2)` time where
 * N is the number of added, moved, removed items.
 *
 * @param cb The callback that acts as a gateway to the backing list data
 *
 * @return A LongStack that contains the diagonals which are used by [applyDiff] to update the list
 */
private fun calculateDiff(
    oldSize: Int,
    newSize: Int,
    cb: DiffCallback,
): IntStack {
    val max = (oldSize + newSize + 1) / 2
    val diagonals = IntStack(max * 3)
    // instead of a recursive implementation, we keep our own stack to avoid potential stack
    // overflow exceptions
    val stack = IntStack(max * 4)
    stack.pushRange(0, oldSize, 0, newSize)
    // allocate forward and backward k-lines. K lines are diagonal lines in the matrix.
    // (see the paper for details) These arrays lines keep the max reachable position for
    // each k-line.
    val forward = CenteredArray(IntArray(max * 2 + 1))
    val backward = CenteredArray(IntArray(max * 2 + 1))
    val snake = Snake(IntArray(5))

    while (stack.isNotEmpty()) {
        val newEnd = stack.pop()
        val newStart = stack.pop()
        val oldEnd = stack.pop()
        val oldStart = stack.pop()

        val found = midPoint(
            oldStart,
            oldEnd,
            newStart,
            newEnd,
            cb, forward, backward, snake.data)

        if (found) {
            // if it has a diagonal, save it
            if (snake.diagonalSize > 0) {
                snake.addDiagonalToStack(diagonals)
            }
            // add new ranges for left and right
            // left
            stack.pushRange(
                oldStart = oldStart,
                oldEnd = snake.startX,
                newStart = newStart,
                newEnd = snake.startY,
            )

            // right
            stack.pushRange(
                oldStart = snake.endX,
                oldEnd = oldEnd,
                newStart = snake.endY,
                newEnd = newEnd,
            )
        }
    }
    // sort snakes
    diagonals.sortDiagonals()
    // always add one last
    diagonals.pushDiagonal(oldSize, newSize, 0)

    return diagonals
}

private fun applyDiff(
    oldSize: Int,
    newSize: Int,
    diagonals: IntStack,
    callback: DiffCallback,
) {
    var posX = oldSize
    var posY = newSize
    while (diagonals.isNotEmpty()) {
        var i = diagonals.pop() // diagonal size
        val endY = diagonals.pop()
        val endX = diagonals.pop()
        while (posX > endX) {
            posX--
            callback.remove(posX)
        }
        while (posY > endY) {
            posY--
            callback.insert(posX, posY)
        }
        while (i-- > 0) {
            posX--
            posY--
            callback.same(posX, posY)
        }
    }
    // the last remaining diagonals are just remove/insert until we hit zero
    while (posX > 0) {
        posX--
        callback.remove(posX)
    }
    while (posY > 0) {
        posY--
        callback.insert(posX, posY)
    }
}

internal fun executeDiff(oldSize: Int, newSize: Int, callback: DiffCallback) {
    val diagonals = calculateDiff(oldSize, newSize, callback)
    applyDiff(oldSize, newSize, diagonals, callback)
}

/**
 * Finds a middle snake in the given range.
 */
private fun midPoint(
    oldStart: Int,
    oldEnd: Int,
    newStart: Int,
    newEnd: Int,
    cb: DiffCallback,
    forward: CenteredArray,
    backward: CenteredArray,
    snake: IntArray,
): Boolean {
    val oldSize = oldEnd - oldStart
    val newSize = newEnd - newStart
    if (oldSize < 1 || newSize < 1) {
        return false
    }
    val max = (oldSize + newSize + 1) / 2
    forward[1] = oldStart
    backward[1] = oldEnd
    for (d in 0 until max) {
        val found = forward(
            oldStart,
            oldEnd,
            newStart,
            newEnd, cb, forward, backward, d, snake)
        if (found) {
            return true
        }
        val found2 = backward(
            oldStart,
            oldEnd,
            newStart,
            newEnd, cb, forward, backward, d, snake)
        if (found2) {
            return true
        }
    }
    return false
}

private fun forward(
    oldStart: Int,
    oldEnd: Int,
    newStart: Int,
    newEnd: Int,
    cb: DiffCallback,
    forward: CenteredArray,
    backward: CenteredArray,
    d: Int,
    snake: IntArray,
): Boolean {
    val oldSize = oldEnd - oldStart
    val newSize = newEnd - newStart
    val checkForSnake = abs(oldSize - newSize) % 2 == 1
    val delta = oldSize - newSize
    var k = -d
    while (k <= d) {
        // we either come from d-1, k-1 OR d-1. k+1
        // as we move in steps of 2, array always holds both current and previous d values
        // k = x - y and each array value holds the max X, y = x - k
        val startX: Int
        val startY: Int
        var x: Int
        if ((k == -d) || (k != d) && (forward[k + 1] > forward[k - 1])) {
            // picking k + 1, incrementing Y (by simply not incrementing X)
            startX = forward[k + 1]
            x = startX
        } else {
            // picking k - 1, incrementing X
            startX = forward[k - 1]
            x = startX + 1
        }
        var y: Int = newStart + (x - oldStart) - k
        startY = if (d == 0 || x != startX) y else y - 1
        // now find snake size
        while ((x < oldEnd) && y < newEnd && cb.areItemsTheSame(x, y)) {
            x++
            y++
        }
        // now we have furthest reaching x, record it
        forward[k] = x
        if (checkForSnake) {
            // see if we did pass over a backwards array
            // mapping function: delta - k
            val backwardsK = delta - k
            // if backwards K is calculated and it passed me, found match
            if ((backwardsK >= -d + 1 && backwardsK <= d - 1) && backward[backwardsK] <= x) {
                // match
                fillSnake(
                    startX,
                    startY,
                    x,
                    y,
                    false,
                    snake,
                )
                return true
            }
        }
        k += 2
    }
    return false
}

private fun backward(
    oldStart: Int,
    oldEnd: Int,
    newStart: Int,
    newEnd: Int,
    cb: DiffCallback,
    forward: CenteredArray,
    backward: CenteredArray,
    d: Int,
    snake: IntArray,
): Boolean {
    val oldSize = oldEnd - oldStart
    val newSize = newEnd - newStart
    val checkForSnake = (oldSize - newSize) % 2 == 0
    val delta = oldSize - newSize
    // same as androidx.compose.ui.node.forward but we go backwards from end of the lists to be
    // beginning this also means we'll try to optimize for minimizing x instead of maximizing it
    var k = -d
    while (k <= d) {

        // we either come from d-1, k-1 OR d-1, k+1
        // as we move in steps of 2, array always holds both current and previous d values
        // k = x - y and each array value holds the MIN X, y = x - k
        // when x's are equal, we prioritize deletion over insertion
        val startX: Int
        var x: Int
        if (k == -d || k != d && (backward[k + 1] < backward[k - 1])) {
            // picking k + 1, decrementing Y (by simply not decrementing X)
            startX = backward[k + 1]
            x = startX
        } else {
            // picking k - 1, decrementing X
            startX = backward[k - 1]
            x = startX - 1
        }
        var y = newEnd - (oldEnd - x - k)
        val startY = if (d == 0 || x != startX) y else y + 1
        // now find snake size
        while ((x > oldStart) && y > newStart && cb.areItemsTheSame(x - 1, y - 1)) {
            x--
            y--
        }
        // now we have furthest point, record it (min X)
        backward[k] = x
        if (checkForSnake) {
            // see if we did pass over a backwards array
            // mapping function: delta - k
            val forwardsK = delta - k
            // if forwards K is calculated and it passed me, found match
            if (((forwardsK >= -d) && forwardsK <= d) && forward[forwardsK] >= x) {
                // match
                // assignment are reverse since we are a reverse snake
                fillSnake(
                    x,
                    y,
                    startX,
                    startY,
                    true,
                    snake,
                )
                return true
            }
        }
        k += 2
    }
    return false
}

/**
 * Snakes represent a match between two lists. It is optionally prefixed or postfixed with an
 * add androidx.compose.ui.node.or remove operation. See the Myers' paper for details.
 */
@JvmInline
private value class Snake(val data: IntArray) {
    /**
     * Position in the old list
     */
    val startX: Int get() = data[0]

    /**
     * Position in the new list
     */
    val startY: Int get() = data[1]

    /**
     * End position in the old list, exclusive
     */
    val endX: Int get() = data[2]

    /**
     * End position in the new list, exclusive
     */
    val endY: Int get() = data[3]

    /**
     * True if this snake was created in the reverse search, false otherwise.
     */
    val reverse: Boolean get() = data[4] != 0
    val diagonalSize: Int
        get() = min(endX - startX, endY - startY)

    private val hasAdditionOrRemoval: Boolean
        get() = endY - startY != endX - startX

    private val isAddition: Boolean
        get() = endY - startY > endX - startX

    /**
     * Extract the diagonal of the snake to make reasoning easier for the rest of the
     * algorithm where we try to produce a path and also find moves.
     */
    fun addDiagonalToStack(diagonals: IntStack) {
        if (hasAdditionOrRemoval) {
            if (reverse) {
                // snake edge it at the end
                diagonals.pushDiagonal(startX, startY, diagonalSize)
            } else {
                // snake edge it at the beginning
                if (isAddition) {
                    diagonals.pushDiagonal(startX, startY + 1, diagonalSize)
                } else {
                    diagonals.pushDiagonal(startX + 1, startY, diagonalSize)
                }
            }
        } else {
            // we are a pure diagonal
            diagonals.pushDiagonal(startX, startY, endX - startX)
        }
    }

    override fun toString() = "Snake($startX,$startY,$endX,$endY,$reverse)"
}

internal fun fillSnake(
    startX: Int,
    startY: Int,
    endX: Int,
    endY: Int,
    reverse: Boolean,
    data: IntArray,
) {
    data[0] = startX
    data[1] = startY
    data[2] = endX
    data[3] = endY
    data[4] = if (reverse) 1 else 0
}

/**
 * Array wrapper w/ negative index support.
 * We use this array instead of a regular array so that algorithm is easier to read without
 * too many offsets when accessing the "k" array in the algorithm.
 */
@JvmInline
private value class CenteredArray(private val data: IntArray) {

    private val mid: Int get() = data.size / 2

    operator fun get(index: Int): Int = data[index + mid]

    operator fun set(index: Int, value: Int) {
        data[index + mid] = value
    }
}

private class IntStack(initialCapacity: Int) {
    private var stack = IntArray(initialCapacity)
    private var lastIndex = 0

    fun pushRange(
        oldStart: Int,
        oldEnd: Int,
        newStart: Int,
        newEnd: Int,
    ) {
        val i = lastIndex
        if (i + 4 >= stack.size) {
            stack = stack.copyOf(stack.size * 2)
        }
        val stack = stack
        stack[i + 0] = oldStart
        stack[i + 1] = oldEnd
        stack[i + 2] = newStart
        stack[i + 3] = newEnd
        lastIndex = i + 4
    }

    fun pushDiagonal(
        x: Int,
        y: Int,
        size: Int,
    ) {
        val i = lastIndex
        if (i + 3 >= stack.size) {
            stack = stack.copyOf(stack.size * 2)
        }
        val stack = stack
        stack[i + 0] = x + size
        stack[i + 1] = y + size
        stack[i + 2] = size
        lastIndex = i + 3
    }

    fun pop(): Int = stack[--lastIndex]

    fun isNotEmpty() = lastIndex != 0

    fun sortDiagonals() {
        // diagonals are made up of 3 elements, so we must ensure that the array size is some
        // multiple of three, or else it is malformed. If the size is 3, then there is no need to
        // sort. If it is greater than 3, we pass in the index of the "start" element of the last
        // diagonal
        val i = lastIndex
        check(i % 3 == 0)
        if (i > 3) {
            quickSort(0, i - 3, 3)
        }
    }

    private fun quickSort(start: Int, end: Int, elSize: Int) {
        if (start < end) {
            val i = partition(start, end, elSize)
            quickSort(start, i - elSize, elSize)
            quickSort(i + elSize, end, elSize)
        }
    }

    private fun partition(start: Int, end: Int, elSize: Int): Int {
        var i = start - elSize
        var j = start
        while (j < end) {
            if (compareDiagonal(j, end)) {
                i += elSize
                swapDiagonal(i, j)
            }
            j += elSize
        }
        swapDiagonal(i + elSize, end)
        return i + elSize
    }

    private fun swapDiagonal(i: Int, j: Int) {
        val stack = stack
        stack.swap(i, j)
        stack.swap(i + 1, j + 1)
        stack.swap(i + 2, j + 2)
    }

    private fun compareDiagonal(a: Int, b: Int): Boolean {
        val stack = stack
        val a0 = stack[a]
        val b0 = stack[b]
        return a0 < b0 || (a0 == b0 && stack[a + 1] <= stack[b + 1])
    }
}

private fun IntArray.swap(i: Int, j: Int) {
    val tmp = this[i]
    this[i] = this[j]
    this[j] = tmp
}
