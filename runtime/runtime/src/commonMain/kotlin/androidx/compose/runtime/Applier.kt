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

import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * An Applier is responsible for applying the tree-based operations that get emitted during a
 * composition. Every [Composer] has an [Applier] which it uses to emit a [ComposeNode].
 *
 * A custom [Applier] implementation will be needed in order to utilize Compose to build and
 * maintain a tree of a novel type.
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @see AbstractApplier
 * @see Composition
 * @see Composer
 * @see ComposeNode
 */
@JvmDefaultWithCompatibility
interface Applier<N> {
    /**
     * The node that operations will be applied on at any given time. It is expected that the
     * value of this property will change as [down] and [up] are called.
     */
    val current: N

    /**
     * Called when the [Composer] is about to begin applying changes using this applier.
     * [onEndChanges] will be called when changes are complete.
     */
    fun onBeginChanges() {}

    /**
     * Called when the [Composer] is finished applying changes using this applier.
     * A call to [onBeginChanges] will always precede a call to [onEndChanges].
     */
    fun onEndChanges() {}

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
     * Indicates that [instance] should be inserted as a child to [current] at [index]. An applier
     * should insert the node into the tree either in [insertTopDown] or [insertBottomUp], not both.
     *
     * The [insertTopDown] method is called before the children of [instance] have been created and
     * inserted into it. [insertBottomUp] is called after all children have been created and
     * inserted.
     *
     * Some trees are faster to build top-down, in which case the [insertTopDown] method should
     * be used to insert the [instance]. Other tress are faster to build bottom-up in which case
     * [insertBottomUp] should be used.
     *
     * To give example of building a tree top-down vs. bottom-up consider the following tree,
     *
     * ```
     *      R
     *      |
     *      B
     *     / \
     *    A   C
     *  ```
     *
     *  where the node `B` is being inserted into the tree at `R`. Top-down building of the tree
     *  first inserts `B` into `R`, then inserts `A` into `B` followed by inserting `C` into B`.
     *  For example,
     *
     *  ```
     *      1           2           3
     *      R           R           R
     *      |           |           |
     *      B           B           B
     *                 /           / \
     *                A           A   C
     * ```
     *
     * A bottom-up building of the tree starts with inserting `A` and `C` into `B` then inserts
     * `B` tree into `R`.
     *
     * ```
     *    1           2           3
     *    B           B           R
     *    |          / \          |
     *    A         A   C         B
     *                           / \
     *                          A   C
     * ```
     *
     * To see how building top-down vs. bottom-up can differ significantly in performance
     * consider a tree where whenever a child is added to the tree all parent nodes, up to the root,
     * are notified of the new child entering the tree. If the tree is built top-down,
     *
     *  1. `R` is notified of `B` entering.
     *  2. `B` is notified of `A` entering, `R` is notified of `A` entering.
     *  3. `B` is notified of `C` entering, `R` is notified of `C` entering.
     *
     *  for a total of 5 notifications. The number of notifications grows exponentially with the
     *  number of inserts.
     *
     *  For bottom-up, the notifications are,
     *
     *  1. `B` is notified `A` entering.
     *  2. `B` is notified `C` entering.
     *  3. `R` is notified `B` entering.
     *
     *  The notifications are linear to the number of nodes inserted.
     *
     *  If, on the other hand, all children are notified when the parent enters a tree, then the
     *  notifications are, for top-down,
     *
     *  1. `B` is notified it is entering `R`.
     *  2. `A` is notified it is entering `B`.
     *  3. `C` is notified it is entering `B`.
     *
     *  which is linear to the number of nodes inserted.
     *
     *  For bottom-up, the notifications look like,
     *
     *  1. `A` is notified it is entering `B`.
     *  2. `C` is notified it is entering `B`.
     *  3. `B` is notified it is entering `R`, `A` is notified it is entering `R`,
     *     `C` is notified it is entering `R`.
     *
     *  which exponential to the number of nodes inserted.
     */
    fun insertTopDown(index: Int, instance: N)

    /**
     * Indicates that [instance] should be inserted as a child of [current] at [index]. An applier
     * should insert the node into the tree either in [insertTopDown] or [insertBottomUp], not
     * both. See the description of [insertTopDown] to which describes when to implement
     * [insertTopDown] and when to use [insertBottomUp].
     */
    fun insertBottomUp(index: Int, instance: N)

    /**
     * Indicates that the children of [current] from [index] to [index] + [count] should be removed.
     */
    fun remove(index: Int, count: Int)

    /**
     * Indicates that [count] children of [current] should be moved from index [from] to index [to].
     *
     * The [to] index is relative to the position before the change, so, for example, to move an
     * element at position 1 to after the element at position 2, [from] should be `1` and [to]
     * should be `3`. If the elements were A B C D E, calling `move(1, 3, 1)` would result in the
     * elements being reordered to A C B D E.
     */
    fun move(from: Int, to: Int, count: Int)

    /**
     * Move to the root and remove all nodes from the root, preparing both this [Applier]
     * and its root to be used as the target of a new composition in the future.
     */
    fun clear()
}

/**
 * An abstract [Applier] implementation.
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @see Applier
 * @see Composition
 * @see Composer
 * @see ComposeNode
 */
abstract class AbstractApplier<T>(val root: T) : Applier<T> {
    private val stack = mutableListOf<T>()
    override var current: T = root
        protected set

    override fun down(node: T) {
        stack.add(current)
        current = node
    }

    override fun up() {
        check(stack.isNotEmpty())
        current = stack.removeAt(stack.size - 1)
    }

    final override fun clear() {
        stack.clear()
        current = root
        onClear()
    }

    /**
     * Called to perform clearing of the [root] when [clear] is called.
     */
    protected abstract fun onClear()

    protected fun MutableList<T>.remove(index: Int, count: Int) {
        if (count == 1) {
            removeAt(index)
        } else {
            subList(index, index + count).clear()
        }
    }

    protected fun MutableList<T>.move(from: Int, to: Int, count: Int) {
        val dest = if (from > to) to else to - count
        if (count == 1) {
            if (from == to + 1 || from == to - 1) {
                // Adjacent elements, perform swap to avoid backing array manipulations.
                val fromEl = get(from)
                val toEl = set(to, fromEl)
                set(from, toEl)
            } else {
                val fromEl = removeAt(from)
                add(dest, fromEl)
            }
        } else {
            val subView = subList(from, from + count)
            val subCopy = subView.toMutableList()
            subView.clear()
            addAll(dest, subCopy)
        }
    }
}

internal class OffsetApplier<N>(
    private val applier: Applier<N>,
    private val offset: Int
) : Applier<N> {
    private var nesting = 0
    override val current: N get() = applier.current

    override fun down(node: N) {
        nesting++
        applier.down(node)
    }

    override fun up() {
        runtimeCheck(nesting > 0) { "OffsetApplier up called with no corresponding down" }
        nesting--
        applier.up()
    }

    override fun insertTopDown(index: Int, instance: N) {
        applier.insertTopDown(index + if (nesting == 0) offset else 0, instance)
    }

    override fun insertBottomUp(index: Int, instance: N) {
        applier.insertBottomUp(index + if (nesting == 0) offset else 0, instance)
    }

    override fun remove(index: Int, count: Int) {
        applier.remove(index + if (nesting == 0) offset else 0, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        val effectiveOffset = if (nesting == 0) offset else 0
        applier.move(from + effectiveOffset, to + effectiveOffset, count)
    }

    override fun clear() {
        runtimeCheck(false) { "Clear is not valid on OffsetApplier" }
    }
}