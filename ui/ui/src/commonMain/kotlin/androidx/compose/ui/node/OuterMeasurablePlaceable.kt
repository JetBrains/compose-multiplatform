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

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.LayoutNode.LayoutState
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

internal class OuterMeasurablePlaceable(
    private val layoutNode: LayoutNode,
    var outerWrapper: LayoutNodeWrapper
) : Measurable, Placeable() {

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

    /**
     * A local version of [Owner.measureIteration] to ensure that [MeasureBlocks.measure]
     * is not called multiple times within a measure pass.
     */
    var measureIteration = -1L
        private set

    override var parentData: Any? = null
        private set

    /**
     * The function to be executed when the parent layout measures its children.
     */
    override fun measure(constraints: Constraints): Placeable {
        // when we measure the root it is like the virtual parent is currently laying out
        val parentState = layoutNode.parent?.layoutState ?: LayoutState.LayingOut
        layoutNode.measuredByParent = when (parentState) {
            LayoutState.Measuring -> LayoutNode.UsageByParent.InMeasureBlock
            LayoutState.LayingOut -> LayoutNode.UsageByParent.InLayoutBlock
            else -> throw IllegalStateException(
                "Measurable could be only measured from the parent's measure or layout block." +
                    "Parents state is $parentState"
            )
        }
        remeasure(constraints)
        return this
    }

    /**
     * Return true if the measured size has been changed
     */
    fun remeasure(constraints: Constraints): Boolean {
        val owner = layoutNode.requireOwner()
        val iteration = owner.measureIteration
        val parent = layoutNode.parent
        @Suppress("Deprecation")
        layoutNode.canMultiMeasure = layoutNode.canMultiMeasure ||
            (parent != null && parent.canMultiMeasure)
        @Suppress("Deprecation")
        check(measureIteration != iteration || layoutNode.canMultiMeasure) {
            "measure() may not be called multiple times on the same Measurable"
        }
        measureIteration = owner.measureIteration
        if (layoutNode.layoutState == LayoutState.NeedsRemeasure ||
            measurementConstraints != constraints
        ) {
            layoutNode.alignmentLines.usedByModifierMeasurement = false
            layoutNode._children.forEach { it.alignmentLines.usedDuringParentMeasurement = false }
            measuredOnce = true
            layoutNode.layoutState = LayoutState.Measuring
            measurementConstraints = constraints
            val outerWrapperPreviousMeasuredSize = outerWrapper.size
            owner.snapshotObserver.observeMeasureSnapshotReads(layoutNode) {
                outerWrapper.measure(constraints)
            }
            // The resulting layout state might be Ready. This can happen when the layout node's
            // own modifier is querying an alignment line during measurement, therefore we
            // need to also layout the layout node.
            if (layoutNode.layoutState == LayoutState.Measuring) {
                layoutNode.layoutState = LayoutState.NeedsRelayout
            }
            val sizeChanged = outerWrapper.size != outerWrapperPreviousMeasuredSize ||
                outerWrapper.width != width ||
                outerWrapper.height != height
            // We are using the coerced wrapper size here to avoid double offset in layout coop.
            measuredSize = IntSize(outerWrapper.width, outerWrapper.height)
            return sizeChanged
        }
        return false
    }

    // We are setting our measuredSize to match the coerced outerWrapper size, to prevent
    // double offseting for layout cooperation. However, this means that here we need
    // to override these getters to make the measured values correct in Measured.
    // TODO(popam): clean this up
    override val measuredWidth: Int get() = outerWrapper.measuredWidth
    override val measuredHeight: Int get() = outerWrapper.measuredHeight

    override fun get(alignmentLine: AlignmentLine): Int {
        if (layoutNode.parent?.layoutState == LayoutState.Measuring) {
            layoutNode.alignmentLines.usedDuringParentMeasurement = true
        } else if (layoutNode.parent?.layoutState == LayoutState.LayingOut) {
            layoutNode.alignmentLines.usedDuringParentLayout = true
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
        placedOnce = true
        lastPosition = position
        lastZIndex = zIndex
        lastLayerBlock = layerBlock
        layoutNode.alignmentLines.usedByModifierLayout = false
        with(PlacementScope) {
            if (layerBlock == null) {
                outerWrapper.place(position, lastZIndex)
            } else {
                outerWrapper.placeWithLayer(position, lastZIndex, layerBlock)
            }
        }
    }

    /**
     * Calls [placeAt] with the same position used during the last [placeAt] call
     */
    fun replace() {
        check(placedOnce)
        placeAt(lastPosition, lastZIndex, lastLayerBlock)
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
    }

    /**
     * Recalculates the parent data.
     */
    fun recalculateParentData() {
        parentData = outerWrapper.parentData
    }
}
