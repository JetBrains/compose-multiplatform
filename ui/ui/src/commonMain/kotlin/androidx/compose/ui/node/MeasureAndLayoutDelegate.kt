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

import androidx.compose.ui.node.LayoutNode.LayoutState.LayingOut
import androidx.compose.ui.node.LayoutNode.LayoutState.Measuring
import androidx.compose.ui.node.LayoutNode.LayoutState.NeedsRelayout
import androidx.compose.ui.node.LayoutNode.LayoutState.NeedsRemeasure
import androidx.compose.ui.node.LayoutNode.LayoutState.Ready
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
 * dispatch [OnPositionedModifier] callbacks for the nodes affected by the previous
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
            root.layoutState = NeedsRemeasure
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
     * @return returns true if the [measureAndLayout] execution should be scheduled as a result
     * of the request.
     */
    fun requestRemeasure(layoutNode: LayoutNode): Boolean = when (layoutNode.layoutState) {
        Measuring, NeedsRemeasure -> {
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
        NeedsRelayout, Ready -> {
            if (duringMeasureLayout && layoutNode.wasMeasuredDuringThisIteration) {
                postponedMeasureRequests.add(layoutNode)
            } else {
                layoutNode.layoutState = NeedsRemeasure
                if (layoutNode.isPlaced || layoutNode.canAffectParent) {
                    val parentLayoutState = layoutNode.parent?.layoutState
                    if (parentLayoutState != NeedsRemeasure) {
                        relayoutNodes.add(layoutNode)
                    }
                }
            }
            !duringMeasureLayout
        }
    }

    /**
     * Requests relayout for this [layoutNode] and nodes affected by its position.
     *
     * @return returns true if the [measureAndLayout] execution should be scheduled as a result
     * of the request.
     */
    fun requestRelayout(layoutNode: LayoutNode): Boolean = when (layoutNode.layoutState) {
        Measuring, NeedsRemeasure, NeedsRelayout, LayingOut -> {
            // don't need to do anything else since the parent is already scheduled
            // for a relayout (measure will trigger relayout), or is laying out right now
            consistencyChecker?.assertConsistent()
            false
        }
        Ready -> {
            layoutNode.layoutState = NeedsRelayout
            if (layoutNode.isPlaced) {
                val parentLayoutState = layoutNode.parent?.layoutState
                if (parentLayoutState != NeedsRemeasure && parentLayoutState != NeedsRelayout) {
                    relayoutNodes.add(layoutNode)
                }
            }
            !duringMeasureLayout
        }
    }

    private fun doRemeasure(layoutNode: LayoutNode, rootConstraints: Constraints): Boolean {
        val sizeChanged = if (layoutNode === root) {
            layoutNode.remeasure(rootConstraints)
        } else {
            layoutNode.remeasure()
        }
        val parent = layoutNode.parent
        if (sizeChanged) {
            if (parent == null) {
                return true
            } else if (layoutNode.measuredByParent == InMeasureBlock) {
                requestRemeasure(parent)
            } else {
                require(layoutNode.measuredByParent == InLayoutBlock)
                requestRelayout(parent)
            }
        }
        return false
    }

    /**
     * Iterates through all LayoutNodes that have requested layout and measures and lays them out
     */
    fun measureAndLayout(onLayout: (() -> Unit)? = null): Boolean {
        require(root.isAttached)
        require(root.isPlaced)
        require(!duringMeasureLayout)
        // we don't need to measure any children unless we have the correct root constraints
        val rootConstraints = rootConstraints ?: return false

        var rootNodeResized = false
        if (relayoutNodes.isNotEmpty()) {
            duringMeasureLayout = true
            try {
                relayoutNodes.popEach { layoutNode ->
                    if (layoutNode.isPlaced ||
                        layoutNode.canAffectParent ||
                        layoutNode.alignmentLines.required
                    ) {
                        if (layoutNode.layoutState == NeedsRemeasure) {
                            if (doRemeasure(layoutNode, rootConstraints)) {
                                rootNodeResized = true
                            }
                        }
                        if (layoutNode.layoutState == NeedsRelayout && layoutNode.isPlaced) {
                            if (layoutNode === root) {
                                layoutNode.place(0, 0)
                            } else {
                                layoutNode.replace()
                            }
                            onPositionedDispatcher.onNodePositioned(layoutNode)
                            consistencyChecker?.assertConsistent()
                        }
                        measureIteration++
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
                }
            } finally {
                duringMeasureLayout = false
            }
            consistencyChecker?.assertConsistent()
            onLayout?.invoke()
        }
        return rootNodeResized
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
        get() = layoutState == NeedsRemeasure &&
            (measuredByParent == InMeasureBlock || alignmentLines.required)
}
