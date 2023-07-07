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
@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.node

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.areObjectsOfSameType
import androidx.compose.ui.layout.ModifierInfo

private val SentinelHead = object : Modifier.Node() {
    override fun toString() = "<Head>"
}.apply {
    aggregateChildKindSet = 0.inv()
}

internal class NodeChain(val layoutNode: LayoutNode) {
    internal val innerCoordinator = InnerNodeCoordinator(layoutNode)
    internal var outerCoordinator: NodeCoordinator = innerCoordinator
        private set
    internal val tail: Modifier.Node = innerCoordinator.tail
    internal var head: Modifier.Node = tail
        private set
    private val isUpdating: Boolean get() = head === SentinelHead
    private val aggregateChildKindSet: Int get() = head.aggregateChildKindSet
    private var current: MutableVector<Modifier.Element>? = null
    private var buffer: MutableVector<Modifier.Element>? = null
    private var cachedDiffer: Differ? = null
    private var logger: Logger? = null

    internal fun useLogger(logger: Logger?) {
        this.logger = logger
    }

    private fun padChain() {
        check(head !== SentinelHead)
        val currentHead = head
        currentHead.parent = SentinelHead
        SentinelHead.child = currentHead
        head = SentinelHead
    }

    private fun trimChain() {
        check(head === SentinelHead)
        head = SentinelHead.child ?: tail
        head.parent = null
        SentinelHead.child = null
        check(head !== SentinelHead)
    }

    /**
     * This method will update the node chain based on the provided modifier chain. This method is
     * responsible for calling all appropriate lifecycles for nodes that are
     * created/disposed/updated during this call.
     *
     * This method will attempt to optimize for the common scenario of the modifier chain being of
     * equal size and each element being able to be reused from the prior one. In most cases this
     * is what recomposition will result in, provided modifiers weren't conditionally provided. In
     * the cases where the modifier is not of equal length to the prior value, or modifiers of
     * different reuse types ended up in the same position, this method will deopt into a slower
     * path which will perform a diff on the modifier chain and execute a minimal number of
     * insertions and deletions.
     */
    internal fun updateFrom(m: Modifier) {
        // If we run the diff and there are no new nodes created, then we don't need to loop through
        // and run the attach cycle on them. We simply keep track of this during the diff to avoid
        // this overhead at the end if we can, since it should be fairly common.
        var attachNeeded = false
        // If we run the diff and there are no structural changes, we can avoid looping through the
        // list and updating the coordinators. We simply keep track of this during the diff to avoid
        // this overhead at the end if we can, since it should be fairly common. Note that this is
        // slightly different from [attachNeeded] since a node can be updated and return null or a
        // new instance which is perfectly valid and would require a new attach cycle, however the
        // coordinator would be identical and so [attachNeeded] would be true but this false
        var coordinatorSyncNeeded = false
        // Use the node chain itself as a head/tail temporarily to prevent pruning the linkedlist
        // to the point where we don't have reference to it. We need to undo this at the end of
        // this method.
        padChain()
        // to avoid allocating vectors every time modifier is set, we have two vectors that we
        // reuse over time. Since the common case is the modifier chains will be of equal length,
        // these vectors should be sized appropriately
        val before = current ?: MutableVector(capacity = 0)
        val after = m.fillVector(buffer ?: mutableVectorOf())
        if (after.size == before.size) {
            // assume if the sizes are the same, that we are in a common case of no structural
            // changes we will attempt an O(n) fast-path diff and exit if a diff is detected, and
            // do the O(N^2) diff beyond that point
            val size = before.size
            // for the linear diff we want to start with the "unpadded" tail
            var node: Modifier.Node? = tail.parent
            var i = size - 1
            var aggregateChildKindSet = 0
            while (node != null && i >= 0) {
                val prev = before[i]
                val next = after[i]
                when (actionForModifiers(prev, next)) {
                    ActionReplace -> {
                        // TODO(lmr): we could avoid running the diff if i = 0, since that would
                        //  always be simple remove + insert
                        // structural change!
                        // back up one for the structural diff algorithm. This should be safe since
                        // our chain is padded with the EmptyHead/EmptyTail nodes
                        logger?.linearDiffAborted(i, prev, next, node)
                        i++
                        node = node.child
                        break
                    }
                    ActionUpdate -> {
                        // this is "the same" modifier, but some things have changed so we want to
                        // reuse the node but also update it
                        val beforeUpdate = node
                        node = updateNodeAndReplaceIfNeeded(prev, next, beforeUpdate)
                        logger?.nodeUpdated(i, i, prev, next, beforeUpdate, node)
                    }
                    ActionReuse -> {
                        logger?.nodeReused(i, i, prev, next, node)
                        // no need to do anything, this is "the same" modifier
                    }
                }
                // if the node is new, we need to run attach on it
                if (!node.isAttached) attachNeeded = true

                aggregateChildKindSet = aggregateChildKindSet or node.kindSet
                node.aggregateChildKindSet = aggregateChildKindSet

                node = node.parent
                i--
            }

            if (i > 0) {
                check(node != null)
                attachNeeded = true
                coordinatorSyncNeeded = true
                // there must have been a structural change
                // we only need to diff what is left of the list, so we use `i` as the "beforeSize"
                // and "afterSize"
                structuralUpdate(
                    before,
                    i,
                    after,
                    i,
                    // its important that the node we pass in here has an accurate
                    // "aggregateChildMask"
                    node,
                )
            }
        } else if (before.size == 0) {
            // common case where we are initializing the chain and the previous size is zero. In
            // this case we just do all inserts. Since this is so common, we add a fast path here
            // for this condition.
            attachNeeded = true
            coordinatorSyncNeeded = true
            var i = after.size - 1
            var aggregateChildKindSet = 0
            var node = tail
            while (i >= 0) {
                val next = after[i]
                val child = node
                node = createAndInsertNodeAsParent(next, child)
                logger?.nodeInserted(0, i, next, child, node)
                aggregateChildKindSet = aggregateChildKindSet or node.kindSet
                node.aggregateChildKindSet = aggregateChildKindSet
                i--
            }
        } else if (after.size == 0) {
            // common case where we we are removing all the modifiers.
            coordinatorSyncNeeded = true
            var i = before.size - 1
            // for the linear traversal we want to start with the "unpadded" tail
            var node: Modifier.Node? = tail.parent
            while (node != null && i >= 0) {
                logger?.nodeRemoved(i, before[i], node)
                val parent = node.parent
                detachAndRemoveNode(node)
                node = parent
                i--
            }
        } else {
            attachNeeded = true
            coordinatorSyncNeeded = true
            structuralUpdate(
                before,
                before.size,
                after,
                after.size,
                tail,
            )
        }
        current = after
        // clear the before vector to allow old modifiers to be Garbage Collected
        buffer = before.also { it.clear() }
        trimChain()

        if (coordinatorSyncNeeded) {
            syncCoordinators()
        }
        if (attachNeeded && layoutNode.isAttached) {
            attach(performInvalidations = true)
        }
    }

    internal fun resetState() {
        val current = current
        if (current == null) {
            // We have no modifiers set so there is nothing to reset.
            return
        }
        val size = current.size
        var node: Modifier.Node? = tail.parent
        var i = size - 1
        while (node != null && i >= 0) {
            if (node.isAttached) {
                node.reset()
                node.detach()
            }
            node = node.parent
            i--
        }
    }

    private fun syncCoordinators() {
        var coordinator: NodeCoordinator = innerCoordinator
        var node: Modifier.Node? = tail.parent
        while (node != null) {
            if (node.isKind(Nodes.Layout) && node is LayoutModifierNode) {
                val next = if (node.coordinator != null) {
                    val c = node.coordinator as LayoutModifierNodeCoordinator
                    val prevNode = c.layoutModifierNode
                    c.layoutModifierNode = node
                    if (prevNode !== node) c.onLayoutModifierNodeChanged()
                    c
                } else {
                    val c = LayoutModifierNodeCoordinator(layoutNode, node)
                    node.updateCoordinator(c)
                    c
                }
                coordinator.wrappedBy = next
                next.wrapped = coordinator
                coordinator = next
            } else {
                node.updateCoordinator(coordinator)
            }
            node = node.parent
        }
        coordinator.wrappedBy = layoutNode.parent?.innerCoordinator
        outerCoordinator = coordinator
    }

    fun attach(performInvalidations: Boolean) {
        headToTail {
            if (!it.isAttached) {
                it.attach()
                if (performInvalidations) {
                    if (it.insertedNodeAwaitingAttachForInvalidation) {
                        autoInvalidateInsertedNode(it)
                    }
                    if (it.updatedNodeAwaitingAttachForInvalidation) {
                        autoInvalidateUpdatedNode(it)
                    }
                }
                // when we attach with performInvalidations == false no separate
                // invalidations needed as the whole LayoutNode is attached to the tree.
                // it will cause all the needed invalidations.
                it.insertedNodeAwaitingAttachForInvalidation = false
                it.updatedNodeAwaitingAttachForInvalidation = false
            }
        }
    }

    /**
     * This returns a new List of Modifiers and the coordinates and any extra information
     * that may be useful. This is used for tooling to retrieve layout modifier and layer
     * information.
     */
    fun getModifierInfo(): List<ModifierInfo> {
        val current = current ?: return listOf()
        val infoList = MutableVector<ModifierInfo>(current.size)
        var i = 0
        headToTailExclusive { node ->
            val coordinator = requireNotNull(node.coordinator)
            infoList += ModifierInfo(current[i++], coordinator, coordinator.layer)
        }
        return infoList.asMutableList()
    }

    internal fun detach() {
        // NOTE(lmr): Currently this implementation allows for nodes to be
        // attached/detached/attached. We need to decide if that's what we want. If we
        // don't, the commented out implementation below it might be better.
        tailToHead {
            if (it.isAttached) it.detach()
        }
//        tailToHead {
//            if (it.isAttached) it.detach()
//            it.child?.parent = null
//            it.child = null
//        }
//        current?.clear()
    }

    private fun getDiffer(
        tail: Modifier.Node,
        before: MutableVector<Modifier.Element>,
        after: MutableVector<Modifier.Element>,
    ): Differ {
        val current = cachedDiffer
        @Suppress("IfThenToElvis")
        return if (current == null) {
            Differ(
                tail,
                tail.aggregateChildKindSet,
                before,
                after,
            ).also { cachedDiffer = it }
        } else {
            current.also {
                it.node = tail
                it.aggregateChildKindSet = tail.aggregateChildKindSet
                it.before = before
                it.after = after
            }
        }
    }

    private inner class Differ(
        var node: Modifier.Node,
        var aggregateChildKindSet: Int,
        var before: MutableVector<Modifier.Element>,
        var after: MutableVector<Modifier.Element>,
    ) : DiffCallback {
        override fun areItemsTheSame(oldIndex: Int, newIndex: Int): Boolean {
            return actionForModifiers(before[oldIndex], after[newIndex]) != ActionReplace
        }

        override fun insert(atIndex: Int, newIndex: Int) {
            val child = node
            node = createAndInsertNodeAsParent(after[newIndex], child)
            check(!node.isAttached)
            node.insertedNodeAwaitingAttachForInvalidation = true
            logger?.nodeInserted(atIndex, newIndex, after[newIndex], child, node)
            aggregateChildKindSet = aggregateChildKindSet or node.kindSet
            node.aggregateChildKindSet = aggregateChildKindSet
        }

        override fun remove(oldIndex: Int) {
            node = node.parent!!
            logger?.nodeRemoved(oldIndex, before[oldIndex], node)
            node = detachAndRemoveNode(node)
        }

        override fun same(oldIndex: Int, newIndex: Int) {
            node = node.parent!!
            val prev = before[oldIndex]
            val next = after[newIndex]
            if (prev != next) {
                val beforeUpdate = node
                node = updateNodeAndReplaceIfNeeded(prev, next, beforeUpdate)
                logger?.nodeUpdated(oldIndex, newIndex, prev, next, beforeUpdate, node)
            } else {
                logger?.nodeReused(oldIndex, newIndex, prev, next, node)
            }
            aggregateChildKindSet = aggregateChildKindSet or node.kindSet
            node.aggregateChildKindSet = aggregateChildKindSet
        }
    }

    internal interface Logger {
        fun linearDiffAborted(
            index: Int,
            prev: Modifier.Element,
            next: Modifier.Element,
            node: Modifier.Node
        )

        fun nodeUpdated(
            oldIndex: Int,
            newIndex: Int,
            prev: Modifier.Element,
            next: Modifier.Element,
            before: Modifier.Node,
            after: Modifier.Node
        )

        fun nodeReused(
            oldIndex: Int,
            newIndex: Int,
            prev: Modifier.Element,
            next: Modifier.Element,
            node: Modifier.Node
        )

        fun nodeInserted(
            atIndex: Int,
            newIndex: Int,
            element: Modifier.Element,
            child: Modifier.Node,
            inserted: Modifier.Node
        )

        fun nodeRemoved(
            oldIndex: Int,
            element: Modifier.Element,
            node: Modifier.Node
        )
    }

    /**
     * This method utilizes a modified Myers Diff Algorithm which will diff the two modifier chains
     * and execute a minimal number of insertions/deletions. We make no attempt to execute "moves"
     * as part of this diff. If a modifier moves that is no different than it being inserted in
     * the new location and removed in the old location.
     *
     * @param tail - The Node that corresponds to the _end_ of the [before] list. This Node is
     * expected to have an up to date [aggregateChildKindSet].
     */
    private fun structuralUpdate(
        before: MutableVector<Modifier.Element>,
        beforeSize: Int,
        after: MutableVector<Modifier.Element>,
        afterSize: Int,
        tail: Modifier.Node,
    ) {
        executeDiff(beforeSize, afterSize, getDiffer(tail, before, after))
    }

    /**
     * This method takes [prev] in the current linked list, and swaps it with [next], ensuring that
     * all the parent/child relationships are maintained.
     *
     * For example:
     *
     *      Head... -> parent -> prev -> child -> ...Tail
     *
     *  gets transformed into a list of the following shape:
     *
     *      Head... -> parent -> next -> child -> ...Tail
     *
     * @return This method returns the updated [next] node, for convenience
     */
    private fun replaceNode(prev: Modifier.Node, next: Modifier.Node): Modifier.Node {
        val parent = prev.parent
        if (parent != null) {
            next.parent = parent
            parent.child = next
            prev.parent = null
        }
        val child = prev.child
        if (child != null) {
            next.child = child
            child.parent = next
            prev.child = null
        }
        // NOTE: it is important that during a "replace", we keep the same coordinator as before
        //  as there is a chance that at the end of the diff we won't iterate through the chain and
        //  update all of the coordinators assuming there were no structural changes detected
        next.updateCoordinator(prev.coordinator)
        return next
    }

    private fun detachAndRemoveNode(node: Modifier.Node): Modifier.Node {
        if (node.isAttached) {
            // for removing nodes, we always do the autoInvalidateNode call,
            // regardless of whether or not it was a ModifierNodeElement with autoInvalidate
            // true, or a BackwardsCompatNode, etc.
            autoInvalidateRemovedNode(node)
            node.detach()
        }
        return removeNode(node)
    }

    /**
     * This removes [node] from the current linked list.
     * For example:
     *
     *      Head... -> parent -> node -> child -> ...Tail
     *
     *  gets transformed into a list of the following shape:
     *
     *      Head... -> parent -> child -> ...Tail
     *
     *  @return The child of the removed [node]
     */
    private fun removeNode(node: Modifier.Node): Modifier.Node {
        val child = node.child
        val parent = node.parent
        if (child != null) {
            child.parent = parent
            node.child = null
        }
        if (parent != null) {
            parent.child = child
            node.parent = null
        }
        return child!!
    }

    private fun createAndInsertNodeAsParent(
        element: Modifier.Element,
        child: Modifier.Node,
    ): Modifier.Node {
        val node = when (element) {
            is ModifierNodeElement<*> -> element.create().also {
                it.kindSet = calculateNodeKindSetFrom(it)
            }
            else -> BackwardsCompatNode(element)
        }
        check(!node.isAttached)
        node.insertedNodeAwaitingAttachForInvalidation = true
        return insertParent(node, child)
    }

    /**
     * This inserts [node] as the parent of [child] in the current linked list.
     * For example:
     *
     *      Head... -> child -> ...Tail
     *
     *  gets transformed into a list of the following shape:
     *
     *      Head... -> node -> child -> ...Tail
     *
     *  @return The inserted [node]
     */
    private fun insertParent(node: Modifier.Node, child: Modifier.Node): Modifier.Node {
        val theParent = child.parent
        if (theParent != null) {
            theParent.child = node
            node.parent = theParent
        }
        child.parent = node
        node.child = child
        return node
    }

    private fun updateNodeAndReplaceIfNeeded(
        prev: Modifier.Element,
        next: Modifier.Element,
        node: Modifier.Node
    ): Modifier.Node {
        when {
            prev is ModifierNodeElement<*> && next is ModifierNodeElement<*> -> {
                val updated = next.updateUnsafe(node)
                if (updated !== node) {
                    check(!updated.isAttached)
                    updated.insertedNodeAwaitingAttachForInvalidation = true
                    // if a new instance is returned, we want to detach the old one
                    if (node.isAttached) {
                        autoInvalidateRemovedNode(node)
                        node.detach()
                    }
                    return replaceNode(node, updated)
                } else {
                    // the node was updated. we are done.
                    if (next.autoInvalidate) {
                        if (updated.isAttached) {
                            // the modifier element is labeled as "auto invalidate", which means
                            // that since the node was updated, we need to invalidate everything
                            // relevant to it.
                            autoInvalidateUpdatedNode(updated)
                        } else {
                            updated.updatedNodeAwaitingAttachForInvalidation = true
                        }
                    }
                    return updated
                }
            }
            node is BackwardsCompatNode -> {
                node.element = next
                // We always autoInvalidate BackwardsCompatNode.
                if (node.isAttached) {
                    autoInvalidateUpdatedNode(node)
                } else {
                    node.updatedNodeAwaitingAttachForInvalidation = true
                }
                return node
            }
            else -> error("Unknown Modifier.Node type")
        }
    }

    // TRAVERSAL

    internal inline fun <reified T> firstFromHead(
        type: NodeKind<T>,
        block: (T) -> Boolean
    ): T? {
        headToTail(type) {
            if (block(it)) return it
        }
        return null
    }

    internal inline fun <reified T> headToTail(type: NodeKind<T>, block: (T) -> Unit) {
        headToTail(type.mask) {
            if (it is T) block(it)
        }
    }

    internal inline fun headToTail(mask: Int, block: (Modifier.Node) -> Unit) {
        if (aggregateChildKindSet and mask == 0) return
        headToTail {
            if (it.kindSet and mask != 0) {
                block(it)
            }
            if (it.aggregateChildKindSet and mask == 0) return
        }
    }

    /**
     * Traverses the linked list from head to tail, running [block] on each Node as it goes. If
     * [block] returns true, it will stop traversing and return true. If [block] returns false,
     * it will continue.
     *
     * @return Returns true if [block] ever returned true, false otherwise.
     */
    internal inline fun headToTail(block: (Modifier.Node) -> Unit) {
        var node: Modifier.Node? = head
        while (node != null) {
            block(node)
            node = node.child
        }
    }

    internal inline fun headToTailExclusive(block: (Modifier.Node) -> Unit) {
        var node: Modifier.Node? = head
        while (node != null && node !== tail) {
            block(node)
            node = node.child
        }
    }

    internal inline fun <reified T> tailToHead(type: NodeKind<T>, block: (T) -> Unit) {
        tailToHead(type.mask) {
            if (it is T) block(it)
        }
    }

    internal inline fun tailToHead(mask: Int, block: (Modifier.Node) -> Unit) {
        if (aggregateChildKindSet and mask == 0) return
        tailToHead {
            if (it.kindSet and mask != 0) {
                block(it)
            }
        }
    }

    internal inline fun tailToHead(block: (Modifier.Node) -> Unit) {
        var node: Modifier.Node? = tail
        while (node != null) {
            block(node)
            node = node.parent
        }
    }

    internal inline fun <reified T> tail(type: NodeKind<T>): T? {
        tailToHead(type) {
            return it
        }
        return null
    }

    internal inline fun <reified T> head(type: NodeKind<T>): T? {
        headToTail(type) {
            return it
        }
        return null
    }

    internal fun has(type: NodeKind<*>): Boolean = aggregateChildKindSet and type.mask != 0

    internal fun has(mask: Int): Boolean = aggregateChildKindSet and mask != 0

    override fun toString(): String = buildString {
        append("[")
        if (head === tail) {
            append("]")
            return@buildString
        }
        headToTailExclusive {
            append("$it")
            if (it.child === tail) {
                append("]")
                return@buildString
            }
            append(",")
        }
    }
}

private const val ActionReplace = 0
private const val ActionUpdate = 1
private const val ActionReuse = 2

/**
 * Here's the rules for reusing nodes for different modifiers:
 * 1. if modifiers are equals, we REUSE but NOT UPDATE
 * 2. if modifiers are same class, we REUSE and UPDATE
 * 3. else REPLACE (NO REUSE, NO UPDATE)
 */
internal fun actionForModifiers(prev: Modifier.Element, next: Modifier.Element): Int {
    return if (prev == next)
        ActionReuse
    else if (areObjectsOfSameType(prev, next))
        ActionUpdate
    else
        ActionReplace
}

private fun <T : Modifier.Node> ModifierNodeElement<T>.updateUnsafe(
    node: Modifier.Node
): Modifier.Node {
    @Suppress("UNCHECKED_CAST")
    return update(node as T)
}

private fun Modifier.fillVector(
    result: MutableVector<Modifier.Element>
): MutableVector<Modifier.Element> {
    val stack = MutableVector<Modifier>(result.size).also { it.add(this) }
    while (stack.isNotEmpty()) {
        when (val next = stack.removeAt(stack.size - 1)) {
            is CombinedModifier -> {
                stack.add(next.inner)
                stack.add(next.outer)
            }
            is Modifier.Element -> result.add(next)
            // some other androidx.compose.ui.node.Modifier implementation that we don't know about...
            else -> next.all {
                result.add(it)
                true
            }
        }
    }
    return result
}