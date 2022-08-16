/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.isAttached
import androidx.compose.ui.node.layoutCoordinates
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach

/**
 * Organizes pointers and the [PointerInputFilter]s that they hit into a hierarchy such that
 * [PointerInputChange]s can be dispatched to the [PointerInputFilter]s in a hierarchical fashion.
 *
 * @property rootCoordinates the root [LayoutCoordinates] that [PointerInputChange]s will be
 * relative to.
 */
@OptIn(InternalCoreApi::class, ExperimentalComposeUiApi::class)
internal class HitPathTracker(private val rootCoordinates: LayoutCoordinates) {

    /*@VisibleForTesting*/
    internal val root: NodeParent = NodeParent()

    /**
     * Associates a [pointerId] to a list of hit [pointerInputNodes] and keeps track of them.
     *
     * This enables future calls to [dispatchChanges] to dispatch the correct [PointerInputChange]s
     * to the right [PointerInputFilter]s at the right time.
     *
     * If [pointerInputNodes] is empty, nothing will be added.
     *
     * @param pointerId The id of the pointer that was hit tested against [PointerInputFilter]s
     * @param pointerInputNodes The [PointerInputFilter]s that were hit by [pointerId].  Must be
     * ordered from ancestor to descendant.
     */
    fun addHitPath(pointerId: PointerId, pointerInputNodes: List<PointerInputModifierNode>) {
        var parent: NodeParent = root
        var merging = true
        eachPin@ for (i in pointerInputNodes.indices) {
            val pointerInputNode = pointerInputNodes[i]
            if (merging) {
                val node = parent.children.firstOrNull {
                    it.pointerInputNode == pointerInputNode
                }
                if (node != null) {
                    node.markIsIn()
                    if (pointerId !in node.pointerIds) node.pointerIds.add(pointerId)
                    parent = node
                    continue@eachPin
                } else {
                    merging = false
                }
            }
            // TODO(lmr): i wonder if Node here and PointerInputNode ought to be the same thing?
            val node = Node(pointerInputNode).apply {
                pointerIds.add(pointerId)
            }
            parent.children.add(node)
            parent = node
        }
    }

    /**
     * Dispatches [internalPointerEvent] through the hierarchy.
     *
     * @param internalPointerEvent The change to dispatch.
     *
     * @return whether this event was dispatched to a [PointerInputFilter]
     */
    fun dispatchChanges(
        internalPointerEvent: InternalPointerEvent,
        isInBounds: Boolean = true
    ): Boolean {
        val changed = root.buildCache(
            internalPointerEvent.changes,
            rootCoordinates,
            internalPointerEvent,
            isInBounds
        )
        if (!changed) {
            return false
        }
        var dispatchHit = root.dispatchMainEventPass(
            internalPointerEvent.changes,
            rootCoordinates,
            internalPointerEvent,
            isInBounds
        )
        dispatchHit = root.dispatchFinalEventPass(internalPointerEvent) || dispatchHit

        return dispatchHit
    }

    /**
     * Dispatches cancel events to all tracked [PointerInputFilter]s to notify them that
     * [PointerInputFilter.onPointerEvent] will not be called again until all pointers have been
     * removed from the application and then at least one is added again, and removes all tracked
     * data.
     */
    fun processCancel() {
        root.dispatchCancel()
        root.clear()
    }

    /**
     * Removes [PointerInputFilter]s that have been removed from the component tree.
     */
    // TODO(shepshapard): Ideally, we can process the detaching of PointerInputFilters at the time
    //  that either their associated LayoutNode is removed from the three, or their
    //  associated PointerInputModifier is removed from a LayoutNode.
    fun removeDetachedPointerInputFilters() {
        root.removeDetachedPointerInputFilters()
    }
}

/**
 * Represents a parent node in the [HitPathTracker]'s tree.  This primarily exists because the tree
 * necessarily has a root that is very similar to all other nodes, except that it does not track any
 * pointer or [PointerInputFilter] information.
 */
/*@VisibleForTesting*/
@OptIn(InternalCoreApi::class, ExperimentalComposeUiApi::class)
internal open class NodeParent {
    val children: MutableVector<Node> = mutableVectorOf()

    open fun buildCache(
        changes: Map<PointerId, PointerInputChange>,
        parentCoordinates: LayoutCoordinates,
        internalPointerEvent: InternalPointerEvent,
        isInBounds: Boolean
    ): Boolean {
        var changed = false
        children.forEach {
            changed = it.buildCache(
                changes,
                parentCoordinates,
                internalPointerEvent,
                isInBounds
            ) || changed
        }
        return changed
    }

    /**
     * Dispatches [changes] down the tree, for the initial and main pass.
     *
     * [changes] and other properties needed in all passes should be cached inside this method so
     * they can be reused in [dispatchFinalEventPass], since the passes happen consecutively.
     *
     * @param changes the map containing [PointerInputChange]s that will be dispatched to
     * relevant [PointerInputFilter]s
     * @param parentCoordinates the [LayoutCoordinates] the positional information in [changes]
     * is relative to
     * @param internalPointerEvent the [InternalPointerEvent] needed to construct [PointerEvent]s
     */
    open fun dispatchMainEventPass(
        changes: Map<PointerId, PointerInputChange>,
        parentCoordinates: LayoutCoordinates,
        internalPointerEvent: InternalPointerEvent,
        isInBounds: Boolean
    ): Boolean {
        var dispatched = false
        children.forEach {
            dispatched = it.dispatchMainEventPass(
                changes,
                parentCoordinates,
                internalPointerEvent,
                isInBounds
            ) || dispatched
        }
        return dispatched
    }

    /**
     * Dispatches the final event pass down the tree.
     *
     * Properties cached in [dispatchMainEventPass] should be reset after this method, to ensure
     * clean state for a future pass where pointer IDs / positions might be different.
     */
    open fun dispatchFinalEventPass(internalPointerEvent: InternalPointerEvent): Boolean {
        var dispatched = false
        children.forEach {
            dispatched = it.dispatchFinalEventPass(internalPointerEvent) || dispatched
        }
        cleanUpHits(internalPointerEvent)
        return dispatched
    }

    /**
     * Dispatches the cancel event to all child [Node]s.
     */
    open fun dispatchCancel() {
        children.forEach { it.dispatchCancel() }
    }

    /**
     * Removes all child nodes.
     */
    fun clear() {
        children.clear()
    }

    /**
     * Removes all child [Node]s that are no longer attached to the compose tree.
     */
    fun removeDetachedPointerInputFilters() {
        var index = 0
        while (index < children.size) {
            val child = children[index]
            if (!child.pointerInputNode.isAttached) {
                children.removeAt(index)
                child.dispatchCancel()
            } else {
                index++
                child.removeDetachedPointerInputFilters()
            }
        }
    }

    open fun cleanUpHits(internalPointerEvent: InternalPointerEvent) {
        for (i in children.lastIndex downTo 0) {
            val child = children[i]
            if (child.pointerIds.isEmpty()) {
                children.removeAt(i)
            }
        }
    }
}

/**
 * Represents a single Node in the tree that also tracks a [PointerInputFilter] and which pointers
 * hit it (tracked as [PointerId]s).
 */
/*@VisibleForTesting*/
@OptIn(InternalCoreApi::class, ExperimentalComposeUiApi::class)
internal class Node(val pointerInputNode: PointerInputModifierNode) : NodeParent() {

    // Note: this is essentially a set, and writes should be guarded accordingly. We use a
    // MutableVector here instead since a set ends up being quite heavy, and calls to
    // set.contains() show up noticeably (~1%) in traces. Since the maximum size of this vector
    // is small (due to the limited amount of concurrent PointerIds there _could_ be), iterating
    // through the small vector in most cases should have a lower performance impact than using a
    // set.
    val pointerIds: MutableVector<PointerId> = mutableVectorOf()

    /**
     * Cached properties that will be set before the main event pass, and reset after the final
     * pass. Since we know that these won't change within the entire pass, we don't need to
     * calculate / create these for each pass / multiple times during a pass.
     *
     * @see buildCache
     * @see clearCache
     */
    private val relevantChanges: MutableMap<PointerId, PointerInputChange> = mutableMapOf()
    private var coordinates: LayoutCoordinates? = null
    private var pointerEvent: PointerEvent? = null
    private var wasIn = false
    private var isIn = true
    private var hasExited = true

    override fun dispatchMainEventPass(
        changes: Map<PointerId, PointerInputChange>,
        parentCoordinates: LayoutCoordinates,
        internalPointerEvent: InternalPointerEvent,
        isInBounds: Boolean
    ): Boolean {
        // TODO(b/158243568): The below dispatching operations may cause the pointerInputFilter to
        //  become detached. Currently, they just no-op if it becomes detached and the detached
        //  pointerInputFilters are removed from being tracked with the next event. I currently
        //  believe they should be detached immediately. Though, it is possible they should be
        //  detached after the conclusion of dispatch (so onCancel isn't called during calls
        //  to onPointerEvent). As a result we guard each successive dispatch with the same check.
        return dispatchIfNeeded {
            val event = pointerEvent!!
            val size = coordinates!!.size
            // Dispatch on the tunneling pass.
            pointerInputNode.onPointerEvent(event, PointerEventPass.Initial, size)

            // Dispatch to children.
            if (pointerInputNode.isAttached) {
                children.forEach {
                    it.dispatchMainEventPass(
                        // Pass only the already-filtered and position-translated changes down to
                        // children
                        relevantChanges,
                        coordinates!!,
                        internalPointerEvent,
                        isInBounds
                    )
                }
            }

            if (pointerInputNode.isAttached) {
                // Dispatch on the bubbling pass.
                pointerInputNode.onPointerEvent(event, PointerEventPass.Main, size)
            }
        }
    }

    override fun dispatchFinalEventPass(internalPointerEvent: InternalPointerEvent): Boolean {
        // TODO(b/158243568): The below dispatching operations may cause the pointerInputFilter to
        //  become detached. Currently, they just no-op if it becomes detached and the detached
        //  pointerInputFilters are removed from being tracked with the next event. I currently
        //  believe they should be detached immediately. Though, it is possible they should be
        //  detached after the conclusion of dispatch (so onCancel isn't called during calls
        //  to onPointerEvent). As a result we guard each successive dispatch with the same check.
        val result = dispatchIfNeeded {
            val event = pointerEvent!!
            val size = coordinates!!.size
            // Dispatch on the tunneling pass.
            pointerInputNode.onPointerEvent(event, PointerEventPass.Final, size)

            // Dispatch to children.
            if (pointerInputNode.isAttached) {
                children.forEach { it.dispatchFinalEventPass(internalPointerEvent) }
            }
        }
        cleanUpHits(internalPointerEvent)
        clearCache()
        return result
    }

    /**
     * Calculates cached properties that will be stored in this [Node] for the duration of both
     * [dispatchMainEventPass] and [dispatchFinalEventPass]. This allows us to avoid repeated
     * work between passes, and within passes, as these properties won't change during the
     * overall dispatch.
     *
     * @see clearCache
     */
    override fun buildCache(
        changes: Map<PointerId, PointerInputChange>,
        parentCoordinates: LayoutCoordinates,
        internalPointerEvent: InternalPointerEvent,
        isInBounds: Boolean
    ): Boolean {
        val childChanged =
            super.buildCache(
                changes,
                parentCoordinates,
                internalPointerEvent,
                isInBounds
            )

        // Avoid future work if we know this node will no-op
        if (!pointerInputNode.isAttached) return true

        coordinates = pointerInputNode.layoutCoordinates

        @OptIn(ExperimentalComposeUiApi::class)
        for ((key, change) in changes) {
            // Filter for changes that are associated with pointer ids that are relevant to this
            // node
            if (key in pointerIds) {
                // And translate their position relative to the parent coordinates, to give us a
                // change local to the PointerInputFilter's coordinates
                val historical = mutableListOf<HistoricalChange>()
                change.historical.fastForEach {
                    historical.add(
                        HistoricalChange(
                            it.uptimeMillis,
                            coordinates!!.localPositionOf(parentCoordinates, it.position)
                        )
                    )
                }

                relevantChanges[key] = change.copy(
                    previousPosition = coordinates!!.localPositionOf(
                        parentCoordinates,
                        change.previousPosition
                    ),
                    currentPosition = coordinates!!.localPositionOf(
                        parentCoordinates,
                        change.position
                    ),
                    historical = historical
                )
            }
        }

        if (relevantChanges.isEmpty()) {
            pointerIds.clear()
            children.clear()
            return true // not hit
        }

        // Clean up any pointerIds that weren't dispatched
        for (i in pointerIds.lastIndex downTo 0) {
            val pointerId = pointerIds[i]
            if (!changes.containsKey(pointerId)) {
                pointerIds.removeAt(i)
            }
        }

        val event = PointerEvent(relevantChanges.values.toList(), internalPointerEvent)
        val enterExitChange = event.changes.fastFirstOrNull {
            internalPointerEvent.issuesEnterExitEvent(it.id)
        }
        if (enterExitChange != null) {
            if (!isInBounds) {
                isIn = false
            } else if (!isIn && (enterExitChange.pressed || enterExitChange.previousPressed)) {
                // We have to recalculate isIn because we didn't redo hit testing
                val size = coordinates!!.size
                @Suppress("DEPRECATION")
                isIn = !enterExitChange.isOutOfBounds(size)
            }
            if (isIn != wasIn &&
                (
                    event.type == PointerEventType.Move ||
                        event.type == PointerEventType.Enter ||
                        event.type == PointerEventType.Exit
                    )
            ) {
                event.type = if (isIn) {
                    PointerEventType.Enter
                } else {
                    PointerEventType.Exit
                }
            } else if (event.type == PointerEventType.Enter && wasIn && !hasExited) {
                event.type = PointerEventType.Move // We already knew that it was in.
            } else if (event.type == PointerEventType.Exit && isIn && enterExitChange.pressed) {
                event.type = PointerEventType.Move // We are still in.
            }
        }

        val changed = childChanged || event.type != PointerEventType.Move ||
            hasPositionChanged(pointerEvent, event)
        pointerEvent = event
        return changed
    }

    private fun hasPositionChanged(oldEvent: PointerEvent?, newEvent: PointerEvent): Boolean {
        if (oldEvent == null || oldEvent.changes.size != newEvent.changes.size) {
            return true
        }
        for (i in 0 until newEvent.changes.size) {
            val old = oldEvent.changes[i]
            val current = newEvent.changes[i]
            if (old.position != current.position) {
                return true
            }
        }
        return false
    }

    /**
     * Resets cached properties in case this node will continue to track different [pointerIds]
     * than the ones we built the cache for, instead of being removed.
     *
     * @see buildCache
     */
    private fun clearCache() {
        relevantChanges.clear()
        coordinates = null
    }

    /**
     * Calls [block] if there are relevant changes, and if [pointerInputNode] is attached
     *
     * @return whether [block] was called
     */
    private inline fun dispatchIfNeeded(
        block: () -> Unit
    ): Boolean {
        // If there are no relevant changes, there is nothing to process so return false.
        if (relevantChanges.isEmpty()) return false
        // If the input filter is not attached, avoid dispatching
        if (!pointerInputNode.isAttached) return false

        block()

        // We dispatched to at least one pointer input filter so return true.
        return true
    }

    // TODO(shepshapard): Should some order of cancel dispatch be guaranteed? I think the answer is
    //  essentially "no", but given that an order can be consistent... maybe we might as well
    //  set an arbitrary standard and stick to it so user expectations are maintained.
    /**
     * Does a depth first traversal and invokes [PointerInputFilter.onCancel] during
     * backtracking.
     */
    override fun dispatchCancel() {
        children.forEach { it.dispatchCancel() }
        pointerInputNode.onCancelPointerInput()
    }

    fun markIsIn() {
        isIn = true
    }

    override fun cleanUpHits(internalPointerEvent: InternalPointerEvent) {
        super.cleanUpHits(internalPointerEvent)

        val event = pointerEvent ?: return

        wasIn = isIn

        event.changes.fastForEach { change ->
            // If the pointer is released and doesn't support hover OR
            // the pointer supports over and is released outside the area
            val remove = !change.pressed &&
                (!internalPointerEvent.issuesEnterExitEvent(change.id) || !isIn)
            if (remove) {
                pointerIds.remove(change.id)
            }
        }

        isIn = false
        hasExited = event.type == PointerEventType.Exit
    }

    override fun toString(): String {
        return "Node(pointerInputFilter=$pointerInputNode, children=$children, " +
            "pointerIds=$pointerIds)"
    }
}
