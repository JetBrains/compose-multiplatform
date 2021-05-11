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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.InternalCoreApi

/**
 * Organizes pointers and the [PointerInputFilter]s that they hit into a hierarchy such that
 * [PointerInputChange]s can be dispatched to the [PointerInputFilter]s in a hierarchical fashion.
 *
 * @property rootCoordinates the root [LayoutCoordinates] that [PointerInputChange]s will be
 * relative to.
 */
@OptIn(InternalCoreApi::class)
internal class HitPathTracker(private val rootCoordinates: LayoutCoordinates) {

    /*@VisibleForTesting*/
    internal val root: NodeParent = NodeParent()

    /**
     * Associates a [pointerId] to a list of hit [pointerInputFilters] and keeps track of them.
     *
     * This enables future calls to [dispatchChanges] to dispatch the correct [PointerInputChange]s
     * to the right [PointerInputFilter]s at the right time.
     *
     * If [pointerInputFilters] is empty, nothing will be added.
     *
     * @param pointerId The id of the pointer that was hit tested against [PointerInputFilter]s
     * @param pointerInputFilters The [PointerInputFilter]s that were hit by [pointerId].  Must be
     * ordered from ancestor to descendant.
     */
    fun addHitPath(pointerId: PointerId, pointerInputFilters: List<PointerInputFilter>) {
        var parent: NodeParent = root
        var merging = true
        eachPin@ for (i in pointerInputFilters.indices) {
            val pointerInputFilter = pointerInputFilters[i]
            if (merging) {
                val node = parent.children.firstOrNull {
                    it.pointerInputFilter == pointerInputFilter
                }
                if (node != null) {
                    if (pointerId !in node.pointerIds) node.pointerIds.add(pointerId)
                    parent = node
                    continue@eachPin
                } else {
                    merging = false
                }
            }
            val node = Node(pointerInputFilter).apply {
                pointerIds.add(pointerId)
            }
            parent.children.add(node)
            parent = node
        }
    }

    /**
     * Stops tracking the [pointerId] and stops tracking any [PointerInputFilter]s that are
     * therefore no longer associated with any pointer ids.
     */
    fun removeHitPath(pointerId: PointerId) {
        root.recursivelyRemovePointerId(pointerId)
    }

    /**
     * Dispatches [internalPointerEvent] through the hierarchy.
     *
     * @param internalPointerEvent The change to dispatch.
     *
     * @return whether this event was dispatched to a [PointerInputFilter]
     */
    fun dispatchChanges(internalPointerEvent: InternalPointerEvent): Boolean {
        var dispatchHit = root.dispatchMainEventPass(
            internalPointerEvent.changes,
            rootCoordinates,
            internalPointerEvent
        )
        dispatchHit = root.dispatchFinalEventPass() || dispatchHit

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
@OptIn(InternalCoreApi::class)
internal open class NodeParent {
    val children: MutableVector<Node> = mutableVectorOf()

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
        internalPointerEvent: InternalPointerEvent
    ): Boolean {
        var dispatched = false
        children.forEach {
            dispatched = it.dispatchMainEventPass(
                changes,
                parentCoordinates,
                internalPointerEvent
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
    open fun dispatchFinalEventPass(): Boolean {
        var dispatched = false
        children.forEach {
            dispatched = it.dispatchFinalEventPass() || dispatched
        }
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
            if (!child.pointerInputFilter.isAttached) {
                children.removeAt(index)
                child.dispatchCancel()
            } else {
                index++
                child.removeDetachedPointerInputFilters()
            }
        }
    }

    /**
     * Removes the tracking of [pointerId] and removes all child [Node]s that are no longer
     * tracking any [PointerId]s.
     */
    fun recursivelyRemovePointerId(pointerId: PointerId) {
        var index = 0
        while (index < children.size) {
            val child = children[index]
            child.pointerIds.remove(pointerId)
            if (child.pointerIds.isEmpty()) {
                children.removeAt(index)
            } else {
                child.recursivelyRemovePointerId(pointerId)
                index++
            }
        }
    }
}

/**
 * Represents a single Node in the tree that also tracks a [PointerInputFilter] and which pointers
 * hit it (tracked as [PointerId]s).
 */
/*@VisibleForTesting*/
@OptIn(InternalCoreApi::class)
internal class Node(val pointerInputFilter: PointerInputFilter) : NodeParent() {

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

    override fun dispatchMainEventPass(
        changes: Map<PointerId, PointerInputChange>,
        parentCoordinates: LayoutCoordinates,
        internalPointerEvent: InternalPointerEvent
    ): Boolean {
        // Build the cache that will be used for both the main and final pass
        buildCache(changes, parentCoordinates, internalPointerEvent)

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
            pointerInputFilter.onPointerEvent(event, PointerEventPass.Initial, size)

            // Dispatch to children.
            if (pointerInputFilter.isAttached) {
                children.forEach {
                    it.dispatchMainEventPass(
                        // Pass only the already-filtered and position-translated changes down to
                        // children
                        relevantChanges,
                        coordinates!!,
                        internalPointerEvent
                    )
                }
            }

            if (pointerInputFilter.isAttached) {
                // Dispatch on the bubbling pass.
                pointerInputFilter.onPointerEvent(event, PointerEventPass.Main, size)
            }
        }
    }

    override fun dispatchFinalEventPass(): Boolean {
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
            pointerInputFilter.onPointerEvent(event, PointerEventPass.Final, size)

            // Dispatch to children.
            if (pointerInputFilter.isAttached) {
                children.forEach { it.dispatchFinalEventPass() }
            }
        }
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
    private fun buildCache(
        changes: Map<PointerId, PointerInputChange>,
        parentCoordinates: LayoutCoordinates,
        internalPointerEvent: InternalPointerEvent
    ) {
        // Avoid future work if we know this node will no-op
        if (!pointerInputFilter.isAttached) return

        coordinates = pointerInputFilter.layoutCoordinates

        for ((key, change) in changes) {
            // Filter for changes that are associated with pointer ids that are relevant to this
            // node
            if (key in pointerIds) {
                // And translate their position relative to the parent coordinates, to give us a
                // change local to the PointerInputFilter's coordinates
                relevantChanges[key] = change.copy(
                    previousPosition = coordinates!!.localPositionOf(
                        parentCoordinates,
                        change.previousPosition
                    ),
                    currentPosition = coordinates!!.localPositionOf(
                        parentCoordinates,
                        change.position
                    )
                )
            }
        }

        if (relevantChanges.isEmpty()) return

        pointerEvent = PointerEvent(relevantChanges.values.toList(), internalPointerEvent)
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
        pointerEvent = null
    }

    /**
     * Calls [block] if there are relevant changes, and if [pointerInputFilter] is attached
     *
     * @return whether [block] was called
     */
    private inline fun dispatchIfNeeded(
        block: () -> Unit
    ): Boolean {
        // If there are no relevant changes, there is nothing to process so return false.
        if (relevantChanges.isEmpty()) return false
        // If the input filter is not attached, avoid dispatching
        if (!pointerInputFilter.isAttached) return false

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
        pointerInputFilter.onCancel()
    }

    override fun toString(): String {
        return "Node(pointerInputFilter=$pointerInputFilter, children=$children, " +
            "pointerIds=$pointerIds)"
    }
}
