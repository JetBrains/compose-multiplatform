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

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.LayoutNode.LayoutState
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach

/**
 * This class works as a layout delegate for [LayoutNode]. It delegates all the measure/layout
 * requests to its [measurePassDelegate] and [lookaheadPassDelegate] depending on whether the
 * request is specific to lookahead.
 */
internal class LayoutNodeLayoutDelegate(
    private val layoutNode: LayoutNode,
    var outerWrapper: LayoutNodeWrapper
) {
    val lastConstraints: Constraints?
        get() = measurePassDelegate.lastConstraints
    val lastLookaheadConstraints: Constraints?
        get() = lookaheadPassDelegate?.lastConstraints
    internal val height: Int
        get() = measurePassDelegate.height
    internal val width: Int
        get() = measurePassDelegate.width

    /**
     * The layout state the node is currently in.
     *
     * The mutation of [layoutState] is confined to [LayoutNodeLayoutDelegate], and is therefore
     * read-only outside this class. This makes the state machine easier to reason about.
     */
    internal var layoutState = LayoutState.Idle
        private set

    /**
     * Tracks whether another measure pass is needed for the LayoutNodeLayoutDelegate.
     * Mutation to [measurePending] is confined to LayoutNodeLayoutDelegate. It can only be set true
     * from outside of this class via [markMeasurePending]. It is cleared (i.e. set false) during
     * the measure pass (i.e. in [performMeasure]).
     */
    internal var measurePending: Boolean = false
        private set

    /**
     * Tracks whether another layout pass is needed for the LayoutNodeLayoutDelegate.
     * Mutation to [layoutPending] is confined to this class. It can only be set true from outside
     * of this class via [markLayoutPending]. It is cleared (i.e. set false) during the layout pass
     * (i.e. in [MeasurePassDelegate.layoutChildren]).
     */
    internal var layoutPending: Boolean = false
        private set

    /**
     * Tracks whether another lookahead measure pass is needed for the LayoutNodeLayoutDelegate.
     * Mutation to [lookaheadMeasurePending] is confined to LayoutNodeLayoutDelegate. It can only
     * be set true from outside of this class via [markLookaheadMeasurePending]. It is cleared
     * (i.e. set false) during the lookahead measure pass (i.e. in [performLookaheadMeasure]).
     */
    internal var lookaheadMeasurePending: Boolean = false
        private set

    /**
     * Tracks whether another lookahead layout pass is needed for the LayoutNodeLayoutDelegate.
     * Mutation to [lookaheadLayoutPending] is confined to this class. It can only be set true
     * from outside of this class via [markLookaheadLayoutPending]. It is cleared (i.e. set false)
     * during the layout pass (i.e. in [LookaheadPassDelegate.layoutChildren]).
     */
    internal var lookaheadLayoutPending: Boolean = false
        private set

    /**
     * Marks the layoutNode dirty for another layout pass.
     */
    internal fun markLayoutPending() {
        layoutPending = true
    }

    /**
     * Marks the layoutNode dirty for another measure pass.
     */
    internal fun markMeasurePending() {
        measurePending = true
    }

    /**
     * Marks the layoutNode dirty for another lookahead layout pass.
     */
    internal fun markLookaheadLayoutPending() {
        lookaheadLayoutPending = true
    }

    /**
     * Marks the layoutNode dirty for another lookahead measure pass.
     */
    internal fun markLookaheadMeasurePending() {
        lookaheadMeasurePending = true
    }

    internal val alignmentLinesOwner: AlignmentLinesOwner
        get() = measurePassDelegate
    internal val lookaheadAlignmentLinesOwner: AlignmentLinesOwner?
        get() = lookaheadPassDelegate

    /**
     * measurePassDelegate manages the measure/layout and alignmentLine related queries for the
     * actual measure/layout pass.
     */
    internal val measurePassDelegate = MeasurePassDelegate()

    /**
     * lookaheadPassDelegate manages the measure/layout and alignmentLine related queries for the
     * lookahead pass.
     */
    internal var lookaheadPassDelegate: LookaheadPassDelegate? = null
        private set

    /**
     * [MeasurePassDelegate] manages the measure/layout and alignmentLine related queries for the
     * actual measure/layout pass.
     */
    inner class MeasurePassDelegate : Measurable, Placeable(), AlignmentLinesOwner {
        private var measuredOnce = false
        private var placedOnce = false
        val lastConstraints: Constraints?
            get() = if (measuredOnce) {
                measurementConstraints
            } else {
                null
            }
        internal var duringAlignmentLinesQuery = false

        private var lastPosition: IntOffset = IntOffset.Zero
        private var lastLayerBlock: (GraphicsLayerScope.() -> Unit)? = null
        private var lastZIndex: Float = 0f

        override var parentData: Any? = null
            private set
        override val isPlaced: Boolean
            get() = layoutNode.isPlaced
        override val innerLayoutNodeWrapper: LayoutNodeWrapper
            get() = layoutNode.innerLayoutNodeWrapper
        override val alignmentLines: AlignmentLines = LayoutNodeAlignmentLines(this)

        private val _childMeasurables = MutableVector<Measurable>()

        internal var childMeasurablesDirty: Boolean = true
        internal val childMeasurables: List<Measurable>
            get() {
                // Update the children list first so we know whether the cached list is
                // reusable.
                layoutNode.updateChildrenIfDirty()

                if (!childMeasurablesDirty) return _childMeasurables.asMutableList()
                layoutNode.updateChildMeasurables(_childMeasurables) {
                    it.layoutDelegate.measurePassDelegate
                }
                childMeasurablesDirty = false
                return _childMeasurables.asMutableList()
            }

        override fun layoutChildren() {
            alignmentLines.recalculateQueryOwner()

            if (layoutPending) {
                onBeforeLayoutChildren()
            }
            // as a result of the previous operation we can figure out a child has been resized
            // and we need to be remeasured, not relaid out
            if (layoutPending) {
                layoutPending = false
                layoutState = LayoutState.LayingOut
                with(layoutNode) {
                    val owner = requireOwner()
                    owner.snapshotObserver.observeLayoutSnapshotReads(
                        this,
                        affectsLookahead = false
                    ) {
                        layoutNode.clearPlaceOrder()
                        forEachChildAlignmentLinesOwner {
                            it.alignmentLines.usedDuringParentLayout
                        }
                        innerLayoutNodeWrapper.measureResult.placeChildren()

                        layoutNode.checkChildrenPlaceOrderForUpdates()
                        forEachChildAlignmentLinesOwner {
                            it.alignmentLines.previousUsedDuringParentLayout =
                                it.alignmentLines.usedDuringParentLayout
                        }
                    }
                }
                layoutState = LayoutState.Idle
            }

            if (alignmentLines.usedDuringParentLayout) {
                alignmentLines.previousUsedDuringParentLayout = true
            }
            if (alignmentLines.dirty && alignmentLines.required) alignmentLines.recalculate()
        }

        /**
         * The function to be executed when the parent layout measures its children.
         */
        override fun measure(constraints: Constraints): Placeable {
            if (layoutNode.intrinsicsUsageByParent == LayoutNode.UsageByParent.NotUsed) {
                // This LayoutNode may have asked children for intrinsics. If so, we should
                // clear the intrinsics usage for everything that was requested previously.
                layoutNode.clearSubtreeIntrinsicsUsage()
            }
            // If we are at the lookahead root of the tree, do both the lookahead measure and
            // regular measure. Otherwise, we'll be consistent with parent's lookahead measure
            // and regular measure stages. This avoids producing exponential amount of
            // lookahead when LookaheadLayouts are nested.
            if (layoutNode.isOutMostLookaheadRoot()) {
                measuredOnce = true
                measurementConstraints = constraints
                layoutNode.measuredByParentInLookahead = LayoutNode.UsageByParent.NotUsed
                lookaheadPassDelegate!!.measure(constraints)
            }
            layoutNode.trackMeasurementByParent()
            remeasure(constraints)
            return this
        }

        /**
         * Return true if the measured size has been changed
         */
        fun remeasure(constraints: Constraints): Boolean {
            val owner = layoutNode.requireOwner()
            val parent = layoutNode.parent
            @Suppress("Deprecation")
            layoutNode.canMultiMeasure = layoutNode.canMultiMeasure ||
                (parent != null && parent.canMultiMeasure)
            if (layoutNode.measurePending || measurementConstraints != constraints) {
                alignmentLines.usedByModifierMeasurement = false
                forEachChildAlignmentLinesOwner {
                    it.alignmentLines.usedDuringParentMeasurement = false
                }
                measuredOnce = true
                val outerWrapperPreviousMeasuredSize = outerWrapper.size
                measurementConstraints = constraints
                performMeasure(constraints)
                val sizeChanged = outerWrapper.size != outerWrapperPreviousMeasuredSize ||
                    outerWrapper.width != width ||
                    outerWrapper.height != height
                // We are using the coerced wrapper size here to avoid double offset in layout coop.
                measuredSize = IntSize(outerWrapper.width, outerWrapper.height)
                return sizeChanged
            } else {
                // this node doesn't require being remeasured. however in order to make sure we have
                // the final size we need to also make sure the whole subtree is remeasured as it can
                // trigger extra remeasure request on our node. we do it now in order to report the
                // final measured size to our parent without doing extra pass later.
                owner.forceMeasureTheSubtree(layoutNode)

                // Restore the intrinsics usage for the sub-tree
                layoutNode.resetSubtreeIntrinsicsUsage()
            }
            return false
        }

        private fun LayoutNode.trackMeasurementByParent() {
            val parent = parent
            if (parent != null) {
                check(
                    measuredByParent == LayoutNode.UsageByParent.NotUsed ||
                        @Suppress("DEPRECATION") canMultiMeasure
                ) {
                    "measure() may not be called multiple times on the same Measurable. Current " +
                        "state $measuredByParent. Parent state ${parent.layoutState}."
                }
                measuredByParent = when (parent.layoutState) {
                    LayoutState.Measuring ->
                        LayoutNode.UsageByParent.InMeasureBlock
                    LayoutState.LayingOut ->
                        LayoutNode.UsageByParent.InLayoutBlock
                    else -> throw IllegalStateException(
                        "Measurable could be only measured from the parent's measure or layout" +
                            " block. Parents state is ${parent.layoutState}"
                    )
                }
            } else {
                // when we measure the root it is like the virtual parent is currently laying out
                measuredByParent = LayoutNode.UsageByParent.NotUsed
            }
        }

        // We are setting our measuredSize to match the coerced outerWrapper size, to prevent
        // double offseting for layout cooperation. However, this means that here we need
        // to override these getters to make the measured values correct in Measured.
        // TODO(popam): clean this up
        override val measuredWidth: Int get() = outerWrapper.measuredWidth
        override val measuredHeight: Int get() = outerWrapper.measuredHeight

        override fun get(alignmentLine: AlignmentLine): Int {
            if (layoutNode.parent?.layoutState == LayoutState.Measuring) {
                alignmentLines.usedDuringParentMeasurement = true
            } else if (layoutNode.parent?.layoutState == LayoutState.LayingOut) {
                alignmentLines.usedDuringParentLayout = true
            }
            duringAlignmentLinesQuery = true
            val result = outerWrapper[alignmentLine]
            duringAlignmentLinesQuery = false
            return result
        }

        override fun placeAt(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            // This can actually be called as soon as LookaheadMeasure is done, but devs may expect
            // certain placement results (e.g. LayoutCoordinates) to be valid when lookahead placement
            // takes place. If that's not the case, it will make sense to move this right after
            // lookahead measure, before place.
            if (layoutNode.isOutMostLookaheadRoot()) {
                // Lookahead placement first
                with(PlacementScope) {
                    lookaheadPassDelegate!!.place(position.x, position.y)
                }
            }

            // Post-lookahead (if any) placement
            placeOuterWrapper(position, zIndex, layerBlock)
        }

        private fun placeOuterWrapper(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            lastPosition = position
            lastZIndex = zIndex
            lastLayerBlock = layerBlock

            placedOnce = true
            alignmentLines.usedByModifierLayout = false
            val owner = layoutNode.requireOwner()
            owner.snapshotObserver.observeLayoutModifierSnapshotReads(
                layoutNode,
                affectsLookahead = false
            ) {
                with(PlacementScope) {
                    if (layerBlock == null) {
                        outerWrapper.place(position, zIndex)
                    } else {
                        outerWrapper.placeWithLayer(position, zIndex, layerBlock)
                    }
                }
            }
        }

        /**
         * Calls [placeOuterWrapper] with the same position used during the last
         * [placeOuterWrapper] call. [placeOuterWrapper] only does the placement for
         * post-lookahead pass.
         */
        fun replace() {
            check(placedOnce)
            placeOuterWrapper(lastPosition, lastZIndex, lastLayerBlock)
        }

        override fun minIntrinsicWidth(height: Int): Int {
            onIntrinsicsQueried()
            return outerWrapper.minIntrinsicWidth(height)
        }

        override fun maxIntrinsicWidth(height: Int): Int {
            onIntrinsicsQueried()
            return outerWrapper.maxIntrinsicWidth(height)
        }

        override fun minIntrinsicHeight(width: Int): Int {
            onIntrinsicsQueried()
            return outerWrapper.minIntrinsicHeight(width)
        }

        override fun maxIntrinsicHeight(width: Int): Int {
            onIntrinsicsQueried()
            return outerWrapper.maxIntrinsicHeight(width)
        }

        private fun onIntrinsicsQueried() {
            // How intrinsics work when specific / custom intrinsics are not provided to the custom
            // layout is we essentially run the measure block of a child with not-final constraints
            // and fake measurables. It is possible that some measure blocks are not pure and have
            // side effects, like save some state calculated during the measurement.
            // In order to make it possible we always have to rerun the measure block with the real
            // final constraints after the intrinsics run. Sometimes it will cause unnecessary
            // remeasurements, but it makes sure such component states are using the correct final
            // constraints/sizes.
            layoutNode.requestRemeasure()

            // Mark the intrinsics size has been used by the parent if it hasn't already been marked.
            val parent = layoutNode.parent
            if (parent != null &&
                layoutNode.intrinsicsUsageByParent == LayoutNode.UsageByParent.NotUsed
            ) {
                layoutNode.intrinsicsUsageByParent = when (parent.layoutState) {
                    LayoutState.Measuring -> LayoutNode.UsageByParent.InMeasureBlock
                    LayoutState.LayingOut -> LayoutNode.UsageByParent.InLayoutBlock
                    // Called from parent's intrinsic measurement
                    else -> parent.intrinsicsUsageByParent
                }
            }
        }

        fun updateParentData(): Boolean {
            val changed = parentData != outerWrapper.parentData
            parentData = outerWrapper.parentData
            return changed
        }

        override fun calculateAlignmentLines(): Map<AlignmentLine, Int> {
            if (!duringAlignmentLinesQuery) {
                // Mark alignments used by modifier
                if (layoutState == LayoutState.Measuring) {
                    alignmentLines.usedByModifierMeasurement = true
                    // We quickly transition to layoutPending as we need the alignment lines now.
                    // Later we will see that we also laid out as part of measurement and will skip layout.
                    if (alignmentLines.dirty) markLayoutPending()
                } else {
                    // Note this can also happen for onGloballyPositioned queries.
                    alignmentLines.usedByModifierLayout = true
                }
            }
            layoutChildren()
            return alignmentLines.getLastCalculation()
        }

        override val parentAlignmentLinesOwner: AlignmentLinesOwner?
            get() = layoutNode.parent?.layoutDelegate?.alignmentLinesOwner

        override fun forEachChildAlignmentLinesOwner(block: (AlignmentLinesOwner) -> Unit) {
            layoutNode.children.fastForEach {
                block(it.layoutDelegate.alignmentLinesOwner)
            }
        }

        override fun requestLayout() {
            layoutNode.requestRelayout()
        }

        override fun requestMeasure() {
            layoutNode.requestRemeasure()
        }

        /**
         * The callback to be executed before running layoutChildren.
         *
         * There are possible cases when we run layoutChildren() on the parent node, but some of its
         * children are not yet measured even if they are supposed to be measured in the measure
         * block of our parent.
         *
         * Example:
         * val child = Layout(...)
         * Layout(child) { measurable, constraints ->
         *    val placeable = measurable.first().measure(constraints)
         *    layout(placeable.width, placeable.height) {
         *       placeable.place(0, 0)
         *    }
         * }
         * And now some set of changes scheduled remeasure for child and relayout for parent.
         *
         * During the [MeasureAndLayoutDelegate.measureAndLayout] we will start with the parent as it
         * has lower depth. Inside the layout block we will call placeable.width which is currently
         * dirty as the child was scheduled to remeasure. This callback will ensure it never happens
         * and pre-remeasure everything required for this layoutChildren().
         */
        private fun onBeforeLayoutChildren() {
            layoutNode.forEachChild {
                if (it.measurePending &&
                    it.measuredByParent == LayoutNode.UsageByParent.InMeasureBlock
                ) {
                    if (it.remeasure()) {
                        layoutNode.requestRemeasure()
                    }
                }
            }
        }

        /**
         * If this was used in an intrinsics measurement, find the parent that used it and
         * invalidate either the measure block or layout block.
         */
        fun invalidateIntrinsicsParent(forceRequest: Boolean) {
            val parent = layoutNode.parent
            val intrinsicsUsageByParent = layoutNode.intrinsicsUsageByParent
            if (parent != null && intrinsicsUsageByParent != LayoutNode.UsageByParent.NotUsed) {
                // find measuring parent
                var intrinsicsUsingParent: LayoutNode = parent
                while (intrinsicsUsingParent.intrinsicsUsageByParent == intrinsicsUsageByParent) {
                    intrinsicsUsingParent = intrinsicsUsingParent.parent ?: break
                }
                when (intrinsicsUsageByParent) {
                    LayoutNode.UsageByParent.InMeasureBlock ->
                        intrinsicsUsingParent.requestRemeasure(forceRequest)
                    LayoutNode.UsageByParent.InLayoutBlock ->
                        intrinsicsUsingParent.requestRelayout(forceRequest)
                    else -> error("Intrinsics isn't used by the parent")
                }
            }
        }
    }

    /**
     * [LookaheadPassDelegate] manages the measure/layout and alignmentLine related queries for
     * the lookahead pass.
     */
    inner class LookaheadPassDelegate(
        private val lookaheadScope: LookaheadScope,
    ) : Placeable(), Measurable, AlignmentLinesOwner {

        internal var duringAlignmentLinesQuery: Boolean = false
        private var placedOnce: Boolean = false
        private var measuredOnce: Boolean = false
        val lastConstraints: Constraints?
            get() = lookaheadConstraints
        private var lookaheadConstraints: Constraints? = null
        private var lastPosition: IntOffset = IntOffset.Zero

        // isPlaced is set to true when created because the construction of LookaheadPassDelegate
        // is triggered by [LayoutNode.attach]
        override var isPlaced: Boolean = true
        private var isPreviouslyPlaced: Boolean = false
        override val innerLayoutNodeWrapper: LayoutNodeWrapper
            get() = layoutNode.innerLayoutNodeWrapper
        override val alignmentLines: AlignmentLines = LookaheadAlignmentLines(this)

        private val _childMeasurables = MutableVector<Measurable>()

        internal var childMeasurablesDirty: Boolean = true
        internal val childMeasurables: List<Measurable>
            get() {
                layoutNode.children.let {
                    // Invoke children to get children updated before checking dirty
                    if (!childMeasurablesDirty) return _childMeasurables.asMutableList()
                }
                layoutNode.updateChildMeasurables(_childMeasurables) {
                    it.layoutDelegate.lookaheadPassDelegate!!
                }
                childMeasurablesDirty = false
                return _childMeasurables.asMutableList()
            }

        private inline fun forEachChildDelegate(block: (LookaheadPassDelegate) -> Unit) =
            layoutNode.forEachChild {
                block(it.layoutDelegate.lookaheadPassDelegate!!)
            }

        override fun layoutChildren() {
            alignmentLines.recalculateQueryOwner()

            if (lookaheadLayoutPending) {
                onBeforeLayoutChildren()
            }
            // as a result of the previous operation we can figure out a child has been resized
            // and we need to be remeasured, not relaid out
            if (lookaheadLayoutPending) {
                lookaheadLayoutPending = false
                layoutState = LayoutState.LookaheadLayingOut
                val owner = layoutNode.requireOwner()
                owner.snapshotObserver.observeLayoutSnapshotReads(layoutNode) {
                    forEachChildDelegate {
                        it.isPreviouslyPlaced = it.isPlaced
                        it.isPlaced = false
                    }
                    layoutNode.forEachChild {
                        // Before rerunning the user's layout block reset previous
                        // lookaheadlyMeasuredByParent for children which we measured in the
                        // layout block during the last run.
                        if (it.measuredByParentInLookahead ==
                            LayoutNode.UsageByParent.InLayoutBlock
                        ) {
                            it.measuredByParentInLookahead = LayoutNode.UsageByParent.NotUsed
                        }
                    }
                    forEachChildAlignmentLinesOwner { child ->
                        child.alignmentLines.usedDuringParentLayout = false
                    }
                    layoutNode.innerLayoutNodeWrapper.lookaheadDelegate!!.measureResult
                        .placeChildren()
                    forEachChildAlignmentLinesOwner { child ->
                        child.alignmentLines.previousUsedDuringParentLayout =
                            child.alignmentLines.usedDuringParentLayout
                    }
                    forEachChildDelegate {
                        if (!it.isPlaced) {
                            it.markSubtreeNotPlaced()
                        }
                    }
                }
                layoutState = LayoutState.Idle
            }
            if (alignmentLines.usedDuringParentLayout) {
                alignmentLines.previousUsedDuringParentLayout = true
            }
            if (alignmentLines.dirty && alignmentLines.required) alignmentLines.recalculate()
        }

        private fun markSubtreeNotPlaced() {
            isPlaced = false
            forEachChildDelegate { it.markSubtreeNotPlaced() }
        }

        override fun calculateAlignmentLines(): Map<AlignmentLine, Int> {
            if (!duringAlignmentLinesQuery) {
                if (layoutState == LayoutState.LookaheadMeasuring) {
                    // Mark alignments used by modifier
                    alignmentLines.usedByModifierMeasurement = true
                    // We quickly transition to layoutPending as we need the alignment lines now.
                    // Later we will see that we also laid out as part of measurement and will skip layout.
                    if (alignmentLines.dirty) markLookaheadLayoutPending()
                } else {
                    // Note this can also happen for onGloballyPositioned queries.
                    alignmentLines.usedByModifierLayout = true
                }
            }
            layoutChildren()
            return alignmentLines.getLastCalculation()
        }

        override val parentAlignmentLinesOwner: AlignmentLinesOwner?
            get() = layoutNode.parent?.layoutDelegate?.lookaheadAlignmentLinesOwner

        override fun forEachChildAlignmentLinesOwner(block: (AlignmentLinesOwner) -> Unit) {
            layoutNode.children.fastForEach {
                block(it.layoutDelegate.lookaheadAlignmentLinesOwner!!)
            }
        }

        override fun requestLayout() {
            layoutNode.requestLookaheadRelayout()
        }

        override fun requestMeasure() {
            layoutNode.requestLookaheadRemeasure()
        }

        override fun measure(constraints: Constraints): Placeable {
            layoutNode.trackLookaheadMeasurementByParent()
            if (layoutNode.intrinsicsUsageByParent == LayoutNode.UsageByParent.NotUsed) {
                // This LayoutNode may have asked children for intrinsics. If so, we should
                // clear the intrinsics usage for everything that was requested previously.
                layoutNode.clearSubtreeIntrinsicsUsage()
            }
            // Since this a measure request coming from the parent. We'd be starting lookahead
            // only if the current layoutNode is the top-level lookahead root.
            // This is an optimization to avoid redundant Snapshot.enter when creating new snapshots
            // for lookahead, in order to reduce the size of the call stack.
            remeasure(constraints)
            return this
        }

        // Track lookahead measurement
        private fun LayoutNode.trackLookaheadMeasurementByParent() {
            // when we measure the root it is like the virtual parent is currently laying out
            val parent = parent
            if (parent != null) {
                check(
                    measuredByParentInLookahead == LayoutNode.UsageByParent.NotUsed ||
                        @Suppress("DEPRECATION") canMultiMeasure
                ) {
                    "measure() may not be called multiple times on the same Measurable. Current " +
                        "state $measuredByParentInLookahead. Parent state ${parent.layoutState}."
                }
                measuredByParentInLookahead = when (parent.layoutState) {
                    LayoutState.LookaheadMeasuring, LayoutState.Measuring ->
                        LayoutNode.UsageByParent.InMeasureBlock
                    LayoutState.LayingOut, LayoutState.LookaheadLayingOut ->
                        LayoutNode.UsageByParent.InLayoutBlock
                    else -> throw IllegalStateException(
                        "Measurable could be only measured from the parent's measure or layout" +
                            " block. Parents state is ${parent.layoutState}"
                    )
                }
            } else {
                measuredByParentInLookahead = LayoutNode.UsageByParent.NotUsed
            }
        }

        override var parentData: Any? = measurePassDelegate.parentData
            private set

        // Lookahead remeasurement with the given constraints.
        fun remeasure(constraints: Constraints): Boolean {
            if (layoutNode.lookaheadMeasurePending || lookaheadConstraints != constraints) {
                lookaheadConstraints = constraints
                alignmentLines.usedByModifierMeasurement = false
                forEachChildAlignmentLinesOwner {
                    it.alignmentLines.usedDuringParentMeasurement = false
                }
                measuredOnce = true
                val lookaheadDelegate = outerWrapper.lookaheadDelegate
                check(lookaheadDelegate != null) {
                    "Lookahead result from lookaheadRemeasure cannot be null"
                }

                // Copy out the previous size before perform lookahead measure
                val lastLookaheadSize = IntSize(lookaheadDelegate.width, lookaheadDelegate.height)
                performLookaheadMeasure(constraints)
                measuredSize = IntSize(lookaheadDelegate.width, lookaheadDelegate.height)
                val sizeChanged = lastLookaheadSize.width != lookaheadDelegate.width ||
                    lastLookaheadSize.height != lookaheadDelegate.height
                return sizeChanged
            }
            return false
        }

        override fun placeAt(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            placedOnce = true
            alignmentLines.usedByModifierLayout = false
            val owner = layoutNode.requireOwner()
            owner.snapshotObserver.observeLayoutModifierSnapshotReads(layoutNode) {
                with(PlacementScope) {
                    outerWrapper.lookaheadDelegate!!.place(position)
                }
            }
            lastPosition = position
        }

        // We are setting our measuredSize to match the coerced outerWrapper size, to prevent
        // double offseting for layout cooperation. However, this means that here we need
        // to override these getters to make the measured values correct in Measured.
        // TODO(popam): clean this up
        override val measuredWidth: Int get() = outerWrapper.lookaheadDelegate!!.measuredWidth
        override val measuredHeight: Int get() = outerWrapper.lookaheadDelegate!!.measuredHeight

        override fun get(alignmentLine: AlignmentLine): Int {
            if (layoutNode.parent?.layoutState == LayoutState.LookaheadMeasuring) {
                alignmentLines.usedDuringParentMeasurement = true
            } else if (layoutNode.parent?.layoutState == LayoutState.LookaheadLayingOut) {
                alignmentLines.usedDuringParentLayout = true
            }
            duringAlignmentLinesQuery = true
            val result = outerWrapper.lookaheadDelegate!![alignmentLine]
            duringAlignmentLinesQuery = false
            return result
        }

        override fun minIntrinsicWidth(height: Int): Int {
            onIntrinsicsQueried()
            return outerWrapper.lookaheadDelegate!!.minIntrinsicWidth(height)
        }

        override fun maxIntrinsicWidth(height: Int): Int {
            onIntrinsicsQueried()
            return outerWrapper.lookaheadDelegate!!.maxIntrinsicWidth(height)
        }

        override fun minIntrinsicHeight(width: Int): Int {
            onIntrinsicsQueried()
            return outerWrapper.lookaheadDelegate!!.minIntrinsicHeight(width)
        }

        override fun maxIntrinsicHeight(width: Int): Int {
            onIntrinsicsQueried()
            return outerWrapper.lookaheadDelegate!!.maxIntrinsicHeight(width)
        }

        private fun onIntrinsicsQueried() {
            // How intrinsics work when specific / custom intrinsics are not provided to the custom
            // layout is we essentially run the measure block of a child with not-final constraints
            // and fake measurables. It is possible that some measure blocks are not pure and have
            // side effects, like save some state calculated during the measurement.
            // In order to make it possible we always have to rerun the measure block with the real
            // final constraints after the intrinsics run. Sometimes it will cause unnecessary
            // remeasurements, but it makes sure such component states are using the correct final
            // constraints/sizes.
            layoutNode.requestLookaheadRemeasure()

            // Mark the intrinsics size has been used by the parent if it hasn't already been marked.
            val parent = layoutNode.parent
            if (parent != null &&
                layoutNode.intrinsicsUsageByParent == LayoutNode.UsageByParent.NotUsed
            ) {
                layoutNode.intrinsicsUsageByParent = when (parent.layoutState) {
                    LayoutState.Measuring -> LayoutNode.UsageByParent.InMeasureBlock
                    LayoutState.LayingOut -> LayoutNode.UsageByParent.InLayoutBlock
                    // Called from parent's intrinsic measurement
                    else -> parent.intrinsicsUsageByParent
                }
            }
        }

        /**
         * If this was used in an intrinsics measurement, find the parent that used it and
         * invalidate either the measure block or layout block.
         */
        fun invalidateIntrinsicsParent(forceRequest: Boolean) {
            val parent = layoutNode.parent
            val intrinsicsUsageByParent = layoutNode.intrinsicsUsageByParent
            if (parent != null && intrinsicsUsageByParent != LayoutNode.UsageByParent.NotUsed) {
                // find measuring parent
                var intrinsicsUsingParent: LayoutNode = parent
                while (intrinsicsUsingParent.intrinsicsUsageByParent == intrinsicsUsageByParent) {
                    intrinsicsUsingParent = intrinsicsUsingParent.parent ?: break
                }
                when (intrinsicsUsageByParent) {
                    LayoutNode.UsageByParent.InMeasureBlock ->
                        intrinsicsUsingParent.requestLookaheadRemeasure(forceRequest)
                    LayoutNode.UsageByParent.InLayoutBlock ->
                        intrinsicsUsingParent.requestLookaheadRelayout(forceRequest)
                    else -> error("Intrinsics isn't used by the parent")
                }
            }
        }

        fun updateParentData(): Boolean {
            val changed = parentData != outerWrapper.lookaheadDelegate!!.parentData
            parentData = outerWrapper.lookaheadDelegate!!.parentData
            return changed
        }

        fun onPlaced() {
            if (!isPlaced) {
                isPlaced = true
                if (!isPreviouslyPlaced) {
                    requestSubtreeForLookahead()
                }
            }
        }

        private fun requestSubtreeForLookahead() {
            layoutNode.forEachChild {
                it.rescheduleRemeasureOrRelayout(it)
                it.layoutDelegate.lookaheadPassDelegate!!.requestSubtreeForLookahead()
            }
        }

        /**
         * The callback to be executed before running layoutChildren.
         *
         * There are possible cases when we run layoutChildren() on the parent node, but some of its
         * children are not yet measured even if they are supposed to be measured in the measure
         * block of our parent.
         *
         * Example:
         * val child = Layout(...)
         * Layout(child) { measurable, constraints ->
         *    val placeable = measurable.first().measure(constraints)
         *    layout(placeable.width, placeable.height) {
         *       placeable.place(0, 0)
         *    }
         * }
         * And now some set of changes scheduled remeasure for child and relayout for parent.
         *
         * During the [MeasureAndLayoutDelegate.measureAndLayout] we will start with the parent as it
         * has lower depth. Inside the layout block we will call placeable.width which is currently
         * dirty as the child was scheduled to remeasure. This callback will ensure it never happens
         * and pre-remeasure everything required for this layoutChildren().
         */
        private fun onBeforeLayoutChildren() {
            layoutNode.forEachChild {
                if (it.lookaheadMeasurePending &&
                    it.measuredByParentInLookahead == LayoutNode.UsageByParent.InMeasureBlock
                ) {
                    if (it.layoutDelegate.lookaheadPassDelegate!!.remeasure(
                            lastConstraints!!
                        )
                    ) {
                        layoutNode.requestLookaheadRemeasure()
                    }
                }
            }
        }

        fun replace() {
            check(placedOnce)
            placeAt(lastPosition, 0f, null)
        }
    }

    /**
     * Returns if the we are at the lookahead root of the tree, by checking if the parent is
     * has a lookahead root.
     */
    private fun LayoutNode.isOutMostLookaheadRoot(): Boolean =
        mLookaheadScope?.root == this

    /**
     * Performs measure with the given constraints and perform necessary state mutations before
     * and after the measurement.
     */
    private fun performMeasure(constraints: Constraints) {
        check(layoutState == LayoutState.Idle) {
            "layout state is not idle before measure starts"
        }
        layoutState = LayoutState.Measuring
        measurePending = false
        layoutNode.requireOwner().snapshotObserver.observeMeasureSnapshotReads(
            layoutNode,
            affectsLookahead = false
        ) {
            outerWrapper.measure(constraints)
        }
        // The resulting layout state might be Ready. This can happen when the layout node's
        // own modifier is querying an alignment line during measurement, therefore we
        // need to also layout the layout node.
        if (layoutState == LayoutState.Measuring) {
            markLayoutPending()
            layoutState = LayoutState.Idle
        }
    }

    private fun performLookaheadMeasure(
        constraints: Constraints
    ) {
        layoutState = LayoutState.LookaheadMeasuring
        lookaheadMeasurePending = false
        layoutNode.requireOwner().snapshotObserver.observeMeasureSnapshotReads(layoutNode) {
            outerWrapper.lookaheadDelegate!!.measure(constraints)
        }
        markLookaheadLayoutPending()
        if (layoutNode.isOutMostLookaheadRoot()) {
            // If layoutNode is the root of the lookahead, measure is redirected to lookahead
            // measure, and layout pass will begin lookahead placement, measure & layout.
            markLayoutPending()
        } else {
            // If layoutNode is not the root of the lookahead, measure needs to follow the
            // lookahead measure.
            markMeasurePending()
        }
        layoutState = LayoutState.Idle
    }

    internal fun onLookaheadScopeChanged(newScope: LookaheadScope?) {
        lookaheadPassDelegate = newScope?.let {
            LookaheadPassDelegate(it)
        }
    }

    fun updateParentData() {
        if (measurePassDelegate.updateParentData()) {
            layoutNode.parent?.requestRemeasure()
        }
        if (lookaheadPassDelegate?.updateParentData() == true) {
            if (layoutNode.isOutMostLookaheadRoot()) {
                layoutNode.parent?.requestRemeasure()
            } else {
                layoutNode.parent?.requestLookaheadRemeasure()
            }
        }
    }

    fun resetAlignmentLines() {
        measurePassDelegate.alignmentLines.reset()
        lookaheadPassDelegate?.alignmentLines?.reset()
    }

    fun markChildrenDirty() {
        measurePassDelegate.childMeasurablesDirty = true
        lookaheadPassDelegate?.let { it.childMeasurablesDirty = true }
    }
}

private fun LayoutNode.updateChildMeasurables(
    destination: MutableVector<Measurable>,
    transform: (LayoutNode) -> Measurable
) {
    forEachChildIndexed { i, layoutNode ->
        if (destination.size <= i) {
            destination.add(transform(layoutNode))
        } else {
            destination[i] = transform(layoutNode)
        }
    }
    destination.removeRange(
        children.size,
        destination.size
    )
}

/**
 * AlignmentLinesOwner defines APIs that are needed to respond to alignment line changes, and to
 * query alignment line related info.
 *
 * [LayoutNodeLayoutDelegate.LookaheadPassDelegate] and
 * [LayoutNodeLayoutDelegate.MeasurePassDelegate] both implement this interface, and they
 * encapsulate the difference in alignment lines handling for lookahead pass vs. actual
 * measure/layout pass.
 */
internal interface AlignmentLinesOwner : Measurable {
    /**
     * Whether the AlignmentLinesOwner has been placed.
     */
    val isPlaced: Boolean

    /**
     * InnerPlaceable of the LayoutNode that the AlignmentLinesOwner operates on.
     */
    val innerLayoutNodeWrapper: LayoutNodeWrapper

    /**
     * Alignment lines for either lookahead pass or post-lookahead pass, depending on the
     * AlignmentLineOwner.
     */
    val alignmentLines: AlignmentLines

    /**
     * The implementation for laying out children. Different types of AlignmentLinesOwner will
     * layout children for either the lookahead pass, or the layout pass post-lookahead.
     */
    fun layoutChildren()

    /**
     * Recalculate the alignment lines if dirty, and layout children as needed.
     */
    fun calculateAlignmentLines(): Map<AlignmentLine, Int>

    /**
     * Parent [AlignmentLinesOwner]. This will be the AlignmentLinesOwner for the same pass but for
     * the parent [LayoutNode].
     */
    val parentAlignmentLinesOwner: AlignmentLinesOwner?

    /**
     * This allows iterating all the AlignmentOwners for the same pass for each of the child
     * LayoutNodes
     */
    fun forEachChildAlignmentLinesOwner(block: (AlignmentLinesOwner) -> Unit)

    /**
     * Depending on which pass the [AlignmentLinesOwner] is created for, this could mean
     * requestLookaheadLayout() for the lookahead pass, or requestLayout() for post-
     * lookahead pass.
     */
    fun requestLayout()

    /**
     * Depending on which pass the [AlignmentLinesOwner] is created for, this could mean
     * requestLookaheadMeasure() for the lookahead pass, or requestMeasure() for post-
     * lookahead pass.
     */
    fun requestMeasure()
}
