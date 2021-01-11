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

import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.plus

/**
 * Organizes pointers and the [PointerInputFilter]s that they hit into a hierarchy such that
 * [PointerInputChange]s can be dispatched to the [PointerInputFilter]s in a hierarchical fashion.
 */
@OptIn(InternalCoreApi::class)
internal class HitPathTracker {

    /*@VisibleForTesting*/
    internal val root: NodeParent = NodeParent()

    private val hitPathsToRetain: MutableMap<PointerId, Int> = mutableMapOf()
    private val retainedHitPaths: MutableSet<PointerId> = mutableSetOf()

    internal interface DispatchChangesRetVal {
        operator fun component1(): InternalPointerEvent
        operator fun component2(): Boolean
    }

    private class DispatchChangesRetValImpl : DispatchChangesRetVal {
        lateinit var internalPointerEvent: InternalPointerEvent
        var wasDispatchedToSomething: Boolean = false
        override operator fun component1() = internalPointerEvent
        override operator fun component2() = wasDispatchedToSomething
    }

    // See https://youtrack.jetbrains.com/issue/KT-39905.
    private val dispatchChangesRetVal = DispatchChangesRetValImpl()

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
        eachPin@ for (pointerInputFilter in pointerInputFilters) {
            if (merging) {
                val node = parent.children.find { it.pointerInputFilter == pointerInputFilter }
                if (node != null) {
                    node.pointerIds.add(pointerId)
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

            // TODO(shepshapard): Would be nice to not create CustomEventDispatcherImpl if the
            //  pointerInputFilter isn't going to use it.
            pointerInputFilter.onInit(
                CustomEventDispatcherImpl(
                    node,
                    this
                )
            )
        }
    }

    /**
     * Stops tracking the [pointerId] and stops tracking any [PointerInputFilter]s that are
     * therefore no longer associated with any pointer ids.
     *
     * Note: if a [PointerInputFilter] retains a hit path by calling
     * [CustomEventDispatcher.retainHitPaths], the hit paths will not actually be removed when
     * this method is called, but instead the paths will be marked for removal when the hit path
     * is released via [CustomEventDispatcher.releaseHitPaths].
     */
    fun removeHitPath(pointerId: PointerId) {
        if (hitPathsToRetain.containsKey(pointerId)) {
            retainedHitPaths.add(pointerId)
        } else {
            removeHitPathInternal(pointerId)
            removeHitPathInternal(pointerId)
        }
    }

    /**
     * Dispatches [internalPointerEvent] through the hierarchy.
     *
     * Returns a [DispatchChangesRetVal] that should not be referenced directly, but instead
     * should be destrutured immediately.  Each instance of [HitPathTracker] reuses a single
     * [DispatchChangesRetVal] and mutates it for each return for performance reasons.
     *
     * [DispatchChangesRetVal.component1] references the resulting changes after dispatch.
     * [DispatchChangesRetVal.component2] is true if the dispatch reached at least one
     * [PointerInputModifier].
     *
     * @param internalPointerEvent The change to dispatch.
     *
     * @return The DispatchChangesRetVal that should be destructured immediately.
     */
    fun dispatchChanges(internalPointerEvent: InternalPointerEvent): DispatchChangesRetVal {
        var dispatchHit = false

        dispatchHit =
            root.dispatchChanges(
            internalPointerEvent,
            PointerEventPass.Initial,
            PointerEventPass.Main
        ) || dispatchHit
        dispatchHit =
            root.dispatchChanges(
            internalPointerEvent,
            PointerEventPass.Final,
            null
        ) || dispatchHit

        dispatchChangesRetVal.wasDispatchedToSomething = dispatchHit
        dispatchChangesRetVal.internalPointerEvent = internalPointerEvent
        return dispatchChangesRetVal
    }

    /**
     * Dispatches the [event] through the hierarchy in all 5 passes of [PointerEventPass].
     *
     * @param event The [Any] to dispatch.
     * @param dispatchingNode The pointer input node responsible for the dispatch.
     *
     * @return The resulting [PointerInputChange]s.
     */
    /*@VisibleForTesting*/
    internal fun dispatchCustomEvent(
        event: CustomEvent,
        dispatchingNode: Node
    ) {
        val associatedPointers = dispatchingNode.pointerIds

        // TODO(b/124523868): It may be more efficient for PointerInputFilters to be able to opt in
        //  or out of passes.
        root.dispatchCustomEvent(
            event,
            associatedPointers,
            PointerEventPass.Initial,
            PointerEventPass.Main,
            dispatchingNode
        )
        root.dispatchCustomEvent(
            event,
            associatedPointers,
            PointerEventPass.Final,
            null,
            dispatchingNode
        )
    }

    /**
     * Dispatches cancel events to all tracked [PointerInputFilter]s to notify them that
     * [PointerInputFilter.onPointerInput] will not be called again until all pointers have been
     * removed from the application and then at least one is added again, and removes all tracked
     * data.
     */
    fun processCancel() {
        hitPathsToRetain.clear()
        retainedHitPaths.clear()
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

    /**
     * Arranges to retain the hit paths associated with the provided [pointerIds] such that if
     * they are requested to be removed for any reason, they are retained.
     */
    private fun retainHitPaths(pointerIds: Set<PointerId>) {
        pointerIds.forEach { pointerId ->
            hitPathsToRetain.putOrUpdate(pointerId, 1) { value ->
                value + 1
            }
        }
    }

    /**
     * Arranges to release any hit paths associated with the provided [pointerIds] such that if
     * they will be requested to be removed in the future, they will be removed upon request.
     *
     * If they were already requested to be removed while they were retained, they will be
     * removed immediately upon release.
     */
    private fun releaseHitPaths(pointerIds: Set<PointerId>) {
        pointerIds.forEach {
            val removed = hitPathsToRetain.removeOrUpdate(
                it,
                { value -> value == 1 },
                { value -> value - 1 }
            )
            if (removed && retainedHitPaths.remove(it)) {
                removeHitPathInternal(it)
            }
        }
    }

    /**
     * Actually removes hit paths.
     */
    private fun removeHitPathInternal(pointerId: PointerId) {
        root.removePointerId(pointerId)
    }

    private class CustomEventDispatcherImpl(
        val dispatchingNode: Node,
        val hitPathTracker: HitPathTracker
    ) : CustomEventDispatcher {
        override fun dispatchCustomEvent(event: CustomEvent) {
            hitPathTracker.dispatchCustomEvent(event, dispatchingNode)
        }

        override fun retainHitPaths(pointerIds: Set<PointerId>) {
            hitPathTracker.retainHitPaths(pointerIds)
        }

        override fun releaseHitPaths(pointerIds: Set<PointerId>) {
            hitPathTracker.releaseHitPaths(pointerIds)
        }
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
    val children: MutableSet<Node> = mutableSetOf()

    /**
     * Dispatches the [InternalPointerEvent] down the tree.
     *
     * Note: [InternalPointerEvent] is expected to be mutated during dispatch.
     */
    open fun dispatchChanges(
        internalPointerEvent: InternalPointerEvent,
        downPass: PointerEventPass,
        upPass: PointerEventPass?
    ): Boolean {
        var dispatchedToSomething = false
        children.forEach {
            dispatchedToSomething =
                it.dispatchChanges(
                internalPointerEvent,
                downPass,
                upPass
            ) || dispatchedToSomething
        }
        return dispatchedToSomething
    }

    /**
     * Dispatches the [event] to all child [Node]s.
     */
    open fun dispatchCustomEvent(
        event: CustomEvent,
        relevantPointers: Set<PointerId>,
        downPass: PointerEventPass,
        upPass: PointerEventPass?,
        dispatchingNode: Node
    ) {
        children.forEach {
            it.dispatchCustomEvent(
                event,
                relevantPointers,
                downPass,
                upPass,
                dispatchingNode
            )
        }
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
        children.removeAndProcess(
            removeIf = {
                !it.pointerInputFilter.isAttached
            },
            ifRemoved = {
                it.dispatchCancel()
            },
            ifKept = {
                it.removeDetachedPointerInputFilters()
            }
        )
    }

    /**
     * Removes the tracking of [pointerId] and removes all child [Node]s that are no longer
     * tracking
     * any [PointerId]s.
     */
    fun removePointerId(pointerId: PointerId) {
        children.forEach {
            it.pointerIds.remove(pointerId)
        }
        children.removeAll {
            it.pointerIds.isEmpty()
        }
        children.forEach {
            it.removePointerId(pointerId)
        }
    }

    /**
     * With each item, if calling [removeIf] with it is true, removes the item from [this] and calls
     * [ifRemoved] with it, otherwise calls [ifKept] with it.
     */
    private fun <T> MutableIterable<T>.removeAndProcess(
        removeIf: (T) -> Boolean,
        ifRemoved: (T) -> Unit,
        ifKept: (T) -> Unit
    ) {
        with(iterator()) {
            while (hasNext()) {
                val next = next()
                if (removeIf(next)) {
                    remove()
                    ifRemoved(next)
                } else {
                    ifKept(next)
                }
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

    val pointerIds: MutableSet<PointerId> = mutableSetOf()

    override fun dispatchChanges(
        internalPointerEvent: InternalPointerEvent,
        downPass: PointerEventPass,
        upPass: PointerEventPass?
    ): Boolean {

        // TODO(shepshapard): Creating a new map everytime here, we could create a reusable one
        //  per node.
        // Filter for changes that are associated with pointer ids that are relevant to this node.
        val relevantChanges =
            internalPointerEvent.changes.filterTo(mutableMapOf()) { entry ->
                pointerIds.contains(entry.key)
            }

        if (relevantChanges.isEmpty()) {
            // If there are not relevant changes, there is nothing to process so return false.
            return false
        }

        // Store all of the changes locally and put the relevant changes back into our event.
        val allChanges = internalPointerEvent.changes
        internalPointerEvent.changes = relevantChanges

        // TODO(b/158243568): The below dispatching operations may cause the pointerInputFilter to
        //  become detached. Currently, they just no-op if it becomes detached and the detached
        //  pointerInputFilters are removed from being tracked with the next event. I currently
        //  believe they should be detached immediately. Though, it is possible they should be
        //  detached after the conclusion of dispatch (so onCancel isn't called during calls
        //  to onPointerEvent).

        // Dispatch on the tunneling pass.
        internalPointerEvent.dispatchToPointerInputFilter(pointerInputFilter, downPass)

        // Dispatch to children.
        if (pointerInputFilter.isAttached) {
            children.forEach { it.dispatchChanges(internalPointerEvent, downPass, upPass) }
        }

        // Dispatch on the bubbling pass.
        internalPointerEvent.dispatchToPointerInputFilter(pointerInputFilter, upPass)

        // Put all of the relevant changes that were in the internalPointerEvent back into all of
        // the changes, and then set all of the changes back onto the internalPointerEvent.
        allChanges.putAll(internalPointerEvent.changes)
        internalPointerEvent.changes = allChanges

        // We dispatched to at least one pointer input filter so return true.
        return true
    }

    /**
     * Dispatches the [event] to the pointer input node this [Node] is tracking and to all child
     * [Node]s.
     *
     * If this [Node] is tracking any [PointerId]s in [relevantPointers],
     * <ol>
     * <li> Dispatches the [event] to the pointer input node it is tracking with [downPass].
     * <li> Dispatches the [event] to all child [Node]s.
     * <li> Dispatches the [event] to the pointer input node it is tracking with [upPass] (if not
     * null).
     * </ol>
     */
    override fun dispatchCustomEvent(
        event: CustomEvent,
        relevantPointers: Set<PointerId>,
        downPass: PointerEventPass,
        upPass: PointerEventPass?,
        dispatchingNode: Node
    ) {
        // If we aren't tracking any of the relevant pointers, return.
        if (!relevantPointers.any { pointerIds.contains(it) }) {
            return
        }

        // TODO(b/158243568): For this attached check, and all of the following checks like this, we
        //  should ideally be dispatching cancel to the sub tree with this node as it's root, and
        //  we should remove the same sub tree from the tracker.  This will currently happen on
        //  the next dispatch of events, but we shouldn't have to wait for another event.
        if (pointerInputFilter.isAttached && this != dispatchingNode) {
            pointerInputFilter.onCustomEvent(event, downPass)
        }

        if (pointerInputFilter.isAttached) {
            // Call children recursively with the relevant changes.
            children.forEach {
                it.dispatchCustomEvent(event, relevantPointers, downPass, upPass, dispatchingNode)
            }
        }

        if (pointerInputFilter.isAttached && upPass != null && this != dispatchingNode) {
            pointerInputFilter.onCustomEvent(event, upPass)
        }
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

    /**
     * Dispatches [this] to [filter].
     *
     * This includes offsetting the pointer coordinates to be relative to [filter].  Also manages
     * cases where the [filter] is removed from the hierarchy during dispatch.
     *
     * Is a no-op if [filter] is not attached or [pass] is null.
     */
    private fun InternalPointerEvent.dispatchToPointerInputFilter(
        filter: PointerInputFilter,
        pass: PointerEventPass?
    ) {
        if (pass == null || !pointerInputFilter.isAttached) {
            return
        }

        // Get the position before dispatch as the PointerInputFilter may not be attached after
        // dispatch or could have moved in some synchronous way (an Android parent may have moved
        // for example) and we actually want to add back whatever position was previously
        // subtracted.
        val position = pointerInputFilter.position
        val size = pointerInputFilter.size

        // TODO(shepshapard): Subtracting offsets and adding offsets is currently expensive because
        //  PointerInputChanges are copied during the operation. Should be better when
        //  PointerInputChanges are privately mutable.
        subtractOffset(position)
        val pointerEvent = PointerEvent(this.changes.values.toList(), this)
        filter.onPointerEvent(pointerEvent, pass, size)
        addOffset(position)
    }

    private fun InternalPointerEvent.addOffset(position: IntOffset) {
        // TODO(shepshapard): Replace everything is costly, we should be able to simply change
        //  data in place here and prevent it from being changed when dispatched to
        //  PointerInputFilters.
        if (position != IntOffset.Zero) {
            changes.replaceEverything {
                it.copy(
                    currentPosition = it.position + position,
                    previousPosition = it.previousPosition + position
                )
            }
        }
    }

    private fun InternalPointerEvent.subtractOffset(position: IntOffset) {
        addOffset(-position)
    }

    private inline fun <K, V> MutableMap<K, V>.replaceEverything(f: (V) -> V) {
        for (entry in this) {
            entry.setValue(f(entry.value))
        }
    }
}

private inline fun <K, V> MutableMap<K, V>.putOrUpdate(
    key: K,
    putValue: V,
    updateBlock: (valueToUpdate: V) -> V
) {
    val value = get(key)
    if (value == null) {
        put(key, putValue)
    } else {
        put(key, updateBlock(value))
    }
}

/**
 * Removes the item at [key] if [removePredicate] returns true, otherwise updates the item with the
 * value returned by [updateBlock].
 *
 * @return True if value was removed, false if updated.
 */
private inline fun <K, V> MutableMap<K, V>.removeOrUpdate(
    key: K,
    removePredicate: (valueToRemove: V) -> Boolean,
    updateBlock: (valueToUpdate: V) -> V
): Boolean {
    val value = get(key)
    if (value != null) {
        if (removePredicate(value)) {
            remove(key)
            return true
        } else {
            put(key, updateBlock(value))
        }
    }
    return false
}
