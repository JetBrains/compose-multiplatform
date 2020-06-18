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

package androidx.compose

/**
 * An Applier is responsible for applying the tree-based operations that get emitted during a
 * composition. Every [Composer] has an [Applier] which it uses to [emit].
 *
 * A custom [Applier] implementation will be needed in order to utilize Compose to build and
 * maintain a tree of a novel type.
 *
 * @sample androidx.compose.samples.CustomTreeComposition
 *
 * @see AbstractApplier
 * @see compositionFor
 * @see Composer
 * @see emit
 */
@ExperimentalComposeApi
interface Applier<N> {
    /**
     * The node that operations will be applied on at any given time. It is expected that the
     * value of this property will change as [down] and [up] are called.
     */
    val current: N

    /**
     * Indicates that the applier is getting traversed "down" the tree. When this gets called,
     * [node] is expected to be a child of [current], and after this operation, [node] is
     * expected to be the new [current].
     */
    fun down(node: N)

    /**
     * Indicates that the applier is getting traversed "up" the tree. After this operation
     * completes, the [current] should return the "parent" of the [current] node at the beginning
     * of this operation.
     */
    fun up()

    /**
     * Indicates that [instance] should be inserted as a child to [current] at [index]
     */
    fun insert(index: Int, instance: N)

    /**
     * Indicates that the children of [current] from [index] to [index] + [count] should be removed.
     */
    fun remove(index: Int, count: Int)

    /**
     * Indicates that the children of [current] from [from] to [from] + [count] should be moved
     * to [to] + [count].
     *
     * The [to] index is related to the position before the change, so, for example, to move an
     * element at position 1 to after the element at position 2, [from] should be `1` and [to]
     * should be `3`. If the elements were A B C D E, calling `move(1, 3, 1)` would result in the
     * elements being reordered to A C B D E.
     */
    fun move(from: Int, to: Int, count: Int)

    /**
     * Reset the applier's state
     */
    fun reset()
}

/**
 * An abstract [Applier] implementation that builds the tree "top down".
 *
 * @sample androidx.compose.samples.CustomTreeComposition
 *
 * @see Applier
 * @see compositionFor
 * @see Composer
 * @see emit
 */
@ExperimentalComposeApi
abstract class AbstractApplier<T>(val root: T) : Applier<T> {
    private val stack = mutableListOf<T>()
    override var current: T = root

    override fun down(node: T) {
        stack.add(current)
        current = node
    }

    override fun up() {
        current = stack.removeAt(stack.size - 1)
    }

    override fun reset() {
        stack.clear()
        current = root
    }

    protected fun MutableList<T>.remove(index: Int, count: Int) {
        if (count == 1) {
            removeAt(index)
        } else {
            subList(index, index + count).clear()
        }
    }

    protected fun MutableList<T>.move(from: Int, to: Int, count: Int) {
        if (count == 1) {
            val fromEl = get(from)
            val toEl = set(to, fromEl)
            set(from, toEl)
        } else {
            val subView = subList(from, from + count)
            val subCopy = subView.toMutableList()
            val dest = if (from > to) to else (to - count)
            subView.clear()
            addAll(dest, subCopy)
        }
    }
}