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

package androidx.compose.ui.node

import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.node.LayoutNode.LayoutState.LayingOut
import androidx.compose.ui.node.LayoutNode.LayoutState.Measuring
import androidx.compose.ui.node.LayoutNode.LayoutState.Idle
import androidx.compose.ui.node.LayoutNode.UsageByParent.InLayoutBlock
import androidx.compose.ui.node.LayoutNode.UsageByParent.InMeasureBlock
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach

/**
 * Keeps track of [LayoutNode]s which needs to be remeasured or relaid out.
 *
 * Use [requestRemeasure] to schedule remeasuring or [requestRelayout] to schedule relayout.
 *
 * Use [measureAndLayout] to perform scheduled actions and [dispatchOnPositionedCallbacks] to
 * dispatch [OnGloballyPositionedModifier] callbacks for the nodes affected by the previous
 * [measureAndLayout] execution.
 */
internal class MeasureAndLayoutDelegate(private val root: LayoutNode) {
    /**
     * LayoutNodes that need measure or layout.
     */
    private val relayoutNodes = DepthSortedSet(Owner.enableExtraAssertions)

    /**
     * Whether any LayoutNode needs measure or layout.
     */
    val hasPendingMeasureOrLayout get() = relayoutNodes.isNotEmpty()

    /**
     * Flag to indicate that we're currently measuring.
     */
    private var duringMeasureLayout = false

    /**
     * Dispatches on positioned callbacks.
     */
    private val onPositionedDispatcher = OnPositionedDispatcher()

    /**
     * List of listeners that must be called after layout has completed.
     */
    private val onLayoutCompletedListeners = mutableVectorOf<Owner.OnLayoutCompletedListener>()

    /**
     * The current measure iteration. The value is incremented during the [measureAndLayout]
     * execution. Some [measureAndLayout] executions will increment it more than once.
     */
    var measureIteration: Long = 1L
        get() {
            require(duringMeasureLayout) {
                "measureIteration should be only used during the measure/layout pass"
            }
            return field
        }
        private set

    /**
     * Stores the list of [LayoutNode]s scheduled to be remeasured in the next measure/layout pass.
     * We were unable to mark them as needsRemeasure=true previously as this request happened
     * during the previous measure/layout pass and they were already measured as part of it.
     * See [requestRemeasure] for more details.
     */
    private val postponedMeasureRequests = mutableListOf<LayoutNode>()

    private var rootConstraints: Constraints? = null

    /**
     * @param constraints The constraints to measure the root [LayoutNode] with
     */
    fun updateRootConstraints(constraints: Constraints) {
        if (rootConstraints != constraints) {
            require(!duringMeasureLayout)
            rootConstraints = constraints
            root.markMeasurePending()
            relayoutNodes.add(root)
        }
    }

    private val consistencyChecker: LayoutTreeConsistencyChecker? =
        if (Owner.enableExtraAssertions) {
            LayoutTreeConsistencyChecker(
                root,
                relayoutNodes,
                postponedMeasureRequests
            )
        } else {
            null
        }

    /**
     * Requests remeasure for this [layoutNode] and nodes affected by its measure result.
     *
     * @return true if the [measureAndLayout] execution should be scheduled as a result
     * of the request.
     */
    fun requestRemeasure(layoutNode: LayoutNode, forced: Boolean = false): Boolean =
        when (layoutNode.layoutState) {
            Measuring -> {
                // requestMeasure has already been called for this node or
                // we're currently measuring it, let's swallow. example when it happens: we compose
                // DataNode inside BoxWithConstraints, this calls onRequestMeasure on DataNode's
                // parent, but this parent is BoxWithConstraints which is currently measuring.
                false
            }
            LayingOut -> {
                // requestMeasure is currently laying out and it is incorrect to request remeasure
                // now, let's postpone it.
                postponedMeasureRequests.add(layoutNode)
                consistencyChecker?.assertConsistent()
                false
            }
            Idle -> {
                if (layoutNode.measurePending && !forced) {
                    false
                } else {
                    layoutNode.markMeasurePending()
                    if (layoutNode.isPlaced || layoutNode.canAffectParent) {
                        if (layoutNode.parent?.measurePending != true) {
                            relayoutNodes.add(layoutNode)
                        }
                    }
                    !duringMeasureLayout
                }
            }
        }

    /**
     * Requests relayout for this [layoutNode] and nodes affected by its position.
     *
     * @return true if the [measureAndLayout] execution should be scheduled as a result
     * of the request.
     */
    fun requestRelayout(layoutNode: LayoutNode, forced: Boolean = false): Boolean =
        when (layoutNode.layoutState) {
            Measuring, LayingOut -> {
                // don't need to do anything else since the parent is already scheduled
                // for a relayout (measure will trigger relayout), or is laying out right now
                consistencyChecker?.assertConsistent()
                false
            }
            Idle -> {
                if ((layoutNode.measurePending || layoutNode.layoutPending) && !forced) {
                    // don't need to do anything else since the parent is already scheduled
                    // for a relayout (measure will trigger relayout), or is laying out right now
                    consistencyChecker?.assertConsistent()
                    false
                } else {
                    layoutNode.markLayoutPending()
                    if (layoutNode.isPlaced) {
                        val parent = layoutNode.parent
                        if (parent?.layoutPending != true && parent?.measurePending != true) {
                            relayoutNodes.add(layoutNode)
                        }
                    }
                    !duringMeasureLayout
                }
            }
        }

    /**
     * @return true if the [LayoutNode] size has been changed.
     */
    private fun doRemeasure(layoutNode: LayoutNode, constraints: Constraints?): Boolean {
        val sizeChanged = if (constraints != null) {
            layoutNode.remeasure(constraints)
        } else {
            layoutNode.remeasure()
        }
        val parent = layoutNode.parent
        if (sizeChanged && parent != null) {
            if (layoutNode.measuredByParent == InMeasureBlock) {
                requestRemeasure(parent)
            } else if (layoutNode.measuredByParent == InLayoutBlock) {
                requestRelayout(parent)
            }
        }
        return sizeChanged
    }

    /**
     * Iterates through all LayoutNodes that have requested layout and measures and lays them out
     */
    fun measureAndLayout(onLayout: (() -> Unit)? = null): Boolean {
        var rootNodeResized = false
        performMeasureAndLayout {
            if (relayoutNodes.isNotEmpty()) {
                relayoutNodes.popEach { layoutNode ->
                    val sizeChanged = remeasureAndRelayoutIfNeeded(layoutNode)
                    if (layoutNode === root && sizeChanged) {
                        rootNodeResized = true
                    }
                }
                onLayout?.invoke()
            }
        }
        callOnLayoutCompletedListeners()
        return rootNodeResized
    }

    fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
        require(layoutNode != root)
        performMeasureAndLayout {
            relayoutNodes.remove(layoutNode)
            // we don't check for the layoutState as even if the node doesn't need remeasure
            // it could be remeasured because the constraints changed.
            doRemeasure(layoutNode, constraints)
            if (layoutNode.layoutPending && layoutNode.isPlaced) {
                layoutNode.replace()
                onPositionedDispatcher.onNodePositioned(layoutNode)
            }
        }
        callOnLayoutCompletedListeners()
    }

    private inline fun performMeasureAndLayout(block: () -> Unit) {
        require(root.isAttached)
        require(root.isPlaced)
        require(!duringMeasureLayout)
        // we don't need to measure any children unless we have the correct root constraints
        if (rootConstraints != null) {
            duringMeasureLayout = true
            try {
                block()
            } finally {
                duringMeasureLayout = false
            }
            consistencyChecker?.assertConsistent()
        }
    }

    fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
        onLayoutCompletedListeners += listener
    }

    private fun callOnLayoutCompletedListeners() {
        onLayoutCompletedListeners.forEach { it.onLayoutComplete() }
        onLayoutCompletedListeners.clear()
    }

    /**
     * Does actual remeasure and relayout on the node if it is required.
     * The [layoutNode] should be already removed from [relayoutNodes] before running it.
     *
     * @return true if the [LayoutNode] size has been changed.
     */
    private fun remeasureAndRelayoutIfNeeded(layoutNode: LayoutNode): Boolean {
        var sizeChanged = false
        if (layoutNode.isPlaced ||
            layoutNode.canAffectParent ||
            layoutNode.alignmentLines.required
        ) {
            if (layoutNode.measurePending) {
                val constraints = if (layoutNode === root) rootConstraints!! else null
                sizeChanged = doRemeasure(layoutNode, constraints)
            }
            if (layoutNode.layoutPending && layoutNode.isPlaced) {
                if (layoutNode === root) {
                    layoutNode.place(0, 0)
                } else {
                    layoutNode.replace()
                }
                onPositionedDispatcher.onNodePositioned(layoutNode)
                consistencyChecker?.assertConsistent()
            }
            // execute postponed `onRequestMeasure`
            if (postponedMeasureRequests.isNotEmpty()) {
                postponedMeasureRequests.fastForEach {
                    if (it.isAttached) {
                        requestRemeasure(it)
                    }
                }
                postponedMeasureRequests.clear()
            }
        }
        return sizeChanged
    }

    /**
     * Makes sure the passed [layoutNode] and its subtree is remeasured and has the final sizes.
     *
     * The node or some of the nodes in its subtree can still be kept unmeasured if they are
     * not placed and don't affect the parent size. See [requestRemeasure] for details.
     */
    fun forceMeasureTheSubtree(layoutNode: LayoutNode) {
        // if there is nothing in `relayoutNodes` everything is remeasured.
        if (relayoutNodes.isEmpty()) {
            return
        }

        // assert that it is executed during the `measureAndLayout` pass.
        check(duringMeasureLayout)
        // if this node is not yet measured this invocation shouldn't be needed.
        require(!layoutNode.measurePending)

        layoutNode._children.forEach { child ->
            if (child.measurePending && relayoutNodes.remove(child)) {
                remeasureAndRelayoutIfNeeded(child)
            }

            // if the child is still in NeedsRemeasure state then this child remeasure wasn't
            // needed. it can happen for example when this child is not placed and can't affect
            // the parent size. we can skip the whole subtree.
            if (!child.measurePending) {
                // run recursively for the subtree.
                forceMeasureTheSubtree(child)
            }
        }

        // if the child was resized during the remeasurement it could request a remeasure on
        // the parent. we need to remeasure now as this function assumes the whole subtree is
        // fully measured as a result of the invocation.
        if (layoutNode.measurePending && relayoutNodes.remove(layoutNode)) {
            remeasureAndRelayoutIfNeeded(layoutNode)
        }
    }

    /**
     * Dispatch [OnPositionedModifier] callbacks for the nodes affected by the previous
     * [measureAndLayout] execution.
     *
     * @param forceDispatch true means the whole tree should dispatch the callback (for example
     * when the global position of the Owner has been changed)
     */
    fun dispatchOnPositionedCallbacks(forceDispatch: Boolean = false) {
        if (forceDispatch) {
            onPositionedDispatcher.onRootNodePositioned(root)
        }
        onPositionedDispatcher.dispatch()
    }

    /**
     * Removes [node] from the list of LayoutNodes being scheduled for the remeasure/relayout as
     * it was detached.
     */
    fun onNodeDetached(node: LayoutNode) {
        relayoutNodes.remove(node)
    }

    private val LayoutNode.canAffectParent
        get() = measurePending &&
            (measuredByParent == InMeasureBlock || alignmentLines.required)
}
