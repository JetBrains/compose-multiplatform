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

private const val MaxListSize = Short.MAX_VALUE.toInt() - 1

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
): LongStack {
    val max = (oldSize + newSize + 1) / 2
    require(oldSize < MaxListSize && newSize < MaxListSize) {
        "This diff algorithm encodes various values as Shorts, and thus the before and after " +
            "lists must have size less than ${Short.MAX_VALUE} in order to be valid."
    }
    val diagonals = LongStack(max)
    // instead of a recursive implementation, we keep our own stack to avoid potential stack
    // overflow exceptions
    val stack = LongStack(10)
    stack.pushRange(Range(0, oldSize, 0, newSize))
    // allocate androidx.compose.ui.node.forward and androidx.compose.ui.node.backward k-lines. K
    // lines are diagonal lines in the matrix. (see the paper for details)
    // These arrays lines keep the max reachable position for each k-line.
    val forward = CenteredArray(IntArray(max * 2 + 1))
    val backward = CenteredArray(IntArray(max * 2 + 1))

    while (stack.isNotEmpty()) {
        val range = stack.popRange()
        val snake = midPoint(range, cb, forward, backward)
        if (snake != NullSnake) {
            // if it has a diagonal, save it
            if (snake.diagonalSize > 0) {
                diagonals.pushDiagonal(snake.toDiagonal())
            }
            // add new ranges for left and right
            // left
            stack.pushRange(
                Range(
                    oldStart = range.oldStart,
                    oldEnd = snake.startX,
                    newStart = range.newStart,
                    newEnd = snake.startY,
                )
            )

            // right
            stack.pushRange(
                Range(
                    oldStart = snake.endX,
                    oldEnd = range.oldEnd,
                    newStart = snake.endY,
                    newEnd = range.newEnd,
                )
            )
        }
    }
    // sort snakes
    diagonals.sort()
    // always add one last
    diagonals.pushDiagonal(Diagonal(oldSize, newSize, 0))

    return diagonals
}

private fun applyDiff(
    oldSize: Int,
    newSize: Int,
    diagonals: LongStack,
    callback: DiffCallback,
) {
    var posX = oldSize
    var posY = newSize
    for (diagonalIndex in diagonals.size - 1 downTo 0) {
        val diagonal = Diagonal(diagonals[diagonalIndex])
        val endX = diagonal.endX
        val endY = diagonal.endY
        while (posX > endX) {
            posX--
            callback.remove(posX)
        }
        while (posY > endY) {
            posY--
            callback.insert(posX, posY)
        }
        var i = diagonal.size
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
    range: Range,
    cb: DiffCallback,
    forward: CenteredArray,
    backward: CenteredArray
): Snake {
    if (range.oldSize < 1 || range.newSize < 1) {
        return NullSnake
    }
    val max = (range.oldSize + range.newSize + 1) / 2
    forward[1] = range.oldStart
    backward[1] = range.oldEnd
    for (d in 0 until max) {
        var snake = forward(range, cb, forward, backward, d)
        if (snake != NullSnake) {
            return snake
        }
        snake = backward(range, cb, forward, backward, d)
        if (snake != NullSnake) {
            return snake
        }
    }
    return NullSnake
}

private fun forward(
    range: Range,
    cb: DiffCallback,
    forward: CenteredArray,
    backward: CenteredArray,
    d: Int
): Snake {
    val checkForSnake = abs(range.oldSize - range.newSize) % 2 == 1
    val delta = range.oldSize - range.newSize
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
        var y: Int = range.newStart + (x - range.oldStart) - k
        startY = if (d == 0 || x != startX) y else y - 1
        // now find snake size
        while ((x < range.oldEnd) && y < range.newEnd && cb.areItemsTheSame(x, y)) {
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
                return Snake(
                    startX,
                    startY,
                    x,
                    y,
                    false
                )
            }
        }
        k += 2
    }
    return NullSnake
}

private fun backward(
    range: Range,
    cb: DiffCallback,
    forward: CenteredArray,
    backward: CenteredArray,
    d: Int
): Snake {
    val checkForSnake = (range.oldSize - range.newSize) % 2 == 0
    val delta = range.oldSize - range.newSize
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
        var y = range.newEnd - (range.oldEnd - x - k)
        val startY = if (d == 0 || x != startX) y else y + 1
        // now find snake size
        while ((x > range.oldStart) && y > range.newStart && cb.areItemsTheSame(x - 1, y - 1)) {
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
                return Snake(
                    x,
                    y,
                    startX,
                    startY,
                    true
                )
            }
        }
        k += 2
    }
    return NullSnake
}

/**
 * A diagonal is a match in the graph.
 * Rather than snakes, we only record the diagonals in the path.
 */
@JvmInline
internal value class Diagonal(val packedValue: ULong) {
    val x: Int get() = endX - size
    val y: Int get() = endY - size

    // NOTE: we choose to store endX/endY/size instead of x/y/size since only
    // the endX/endY/size are used when applying the diff.
    val endX: Int get() = unpackShort1(packedValue)
    val endY: Int get() = unpackShort2(packedValue)
    val size: Int get() = unpackShort3(packedValue)
    override fun toString() = "Diagonal($endX,$endY,$size)"
}

internal fun Diagonal(x: Int, y: Int, size: Int) = Diagonal(
    packShorts((x + size).toShort(), (y + size).toShort(), size.toShort(), 0)
)

/**
 * Snakes represent a match between two lists. It is optionally prefixed or postfixed with an
 * add androidx.compose.ui.node.or remove operation. See the Myers' paper for details.
 */
@JvmInline
internal value class Snake(private val packedValue: ULong) {
    /**
     * Position in the old list
     */
    val startX: Int get() = unpackShort1(packedValue)

    /**
     * Position in the new list
     */
    val startY: Int get() = unpackShort2(packedValue)

    /**
     * End position in the old list, exclusive
     */
    val endX: Int get() = unpackShort3(packedValue)

    /**
     * End position in the new list, exclusive
     */
    val endY: Int get() = unpackShort4(packedValue)

    /**
     * True if this snake was created in the reverse search, false otherwise.
     */
    val reverse: Boolean get() = unpackHighestBit(packedValue) == 1
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
    fun toDiagonal(): Diagonal {
        if (hasAdditionOrRemoval) {
            return if (reverse) {
                // snake edge it at the end
                Diagonal(startX, startY, diagonalSize)
            } else {
                // snake edge it at the beginning
                if (isAddition) {
                    Diagonal(startX, startY + 1, diagonalSize)
                } else {
                    Diagonal(startX + 1, startY, diagonalSize)
                }
            }
        } else {
            // we are a pure diagonal
            return Diagonal(startX, startY, endX - startX)
        }
    }

    override fun toString() = "Snake($startX,$startY,$endX,$endY,$reverse)"
}

private val NullSnake = Snake(ULong.MAX_VALUE)

internal fun Snake(
    startX: Int,
    startY: Int,
    endX: Int,
    endY: Int,
    reverse: Boolean,
) = Snake(
    packShortsAndBool(
        startX.toShort(),
        startY.toShort(),
        endX.toShort(),
        endY.toShort(),
        reverse,
    )
)

/**
 * Represents a range in two lists that needs to be solved.
 *
 *
 * This internal class is used when running Myers' algorithm without recursion.
 *
 *
 * Ends are exclusive
 */
@JvmInline
internal value class Range(val packedValue: ULong) {
    val oldStart: Int get() = unpackShort1(packedValue)
    val oldEnd: Int get() = unpackShort2(packedValue)
    val newStart: Int get() = unpackShort3(packedValue)
    val newEnd: Int get() = unpackShort4(packedValue)
    val oldSize: Int get() = oldEnd - oldStart
    val newSize: Int get() = newEnd - newStart
    override fun toString() = "Range($oldStart,$oldEnd,$newStart,$newEnd,$oldSize,$newSize)"
}

internal fun Range(
    oldStart: Int,
    oldEnd: Int,
    newStart: Int,
    newEnd: Int,
) = Range(
    packShorts(
        oldStart.toShort(),
        oldEnd.toShort(),
        newStart.toShort(),
        newEnd.toShort(),
    )
)

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

@OptIn(ExperimentalUnsignedTypes::class)
private class LongStack(initialCapacity: Int) {
    private var stack = ULongArray(initialCapacity)
    private var lastIndex = 0

    val size: Int get() = lastIndex

    operator fun get(index: Int) = stack[index]
    fun push(value: ULong) {
        if (lastIndex >= stack.size) {
            stack = stack.copyOf(stack.size * 2)
        }
        stack[lastIndex++] = value
    }

    fun pop(): ULong = stack[--lastIndex]
    fun sort() = stack.sort(fromIndex = 0, toIndex = lastIndex)
    fun isNotEmpty() = lastIndex != 0
}

private fun LongStack.pushRange(range: Range) = push(range.packedValue)
private fun LongStack.popRange() = Range(pop())
private fun LongStack.pushDiagonal(diagonal: Diagonal) = push(diagonal.packedValue)

internal inline fun packShorts(
    val1: Short,
    val2: Short,
    val3: Short,
    val4: Short
): ULong {
    return val1.toULong().shl(48) or
        val2.toULong().shl(32) or
        val3.toULong().shl(16) or
        val4.toULong()
}

internal inline fun unpackHighestBit(value: ULong): Int {
    // leaving the sign bit off
    return value.shr(63).and(0b1u).toInt()
}

internal inline fun unpackShort1(value: ULong): Int {
    // leaving the sign bit off
    return value.shr(48).and(0b0111_1111_1111_1111u).toInt()
}

internal inline fun unpackShort2(value: ULong): Int {
    return value.shr(32).and(0xFFFFu).toInt()
}

internal inline fun unpackShort3(value: ULong): Int {
    return value.shr(16).and(0xFFFFu).toInt()
}

internal inline fun unpackShort4(value: ULong): Int {
    return value.and(0xFFFFu).toInt()
}

internal inline fun packShortsAndBool(
    val1: Short,
    val2: Short,
    val3: Short,
    val4: Short,
    bool: Boolean,
): ULong {
    return (if (bool) 1 else 0).toULong().shl(63) or
        val1.toULong().shl(48) or
        val2.toULong().shl(32) or
        val3.toULong().shl(16) or
        val4.toULong()
}
