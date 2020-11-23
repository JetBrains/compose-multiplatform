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

@OptIn(ExperimentalLayoutNodeApi::class)
internal class OuterMeasurablePlaceable(
    private val layoutNode: LayoutNode,
    var outerWrapper: LayoutNodeWrapper
) : Measurable, Placeable() {

    private var measuredOnce = false
    val lastConstraints: Constraints? get() = if (measuredOnce) measurementConstraints else null
    var lastPosition: IntOffset? = null
        private set
    private var lastLayerBlock: (GraphicsLayerScope.() -> Unit)? = null
    private val lastProvidedAlignmentLines = mutableMapOf<AlignmentLine, Int>()
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
            measuredOnce = true
            layoutNode.layoutState = LayoutState.Measuring
            measurementConstraints = constraints
            lastProvidedAlignmentLines.clear()
            lastProvidedAlignmentLines.putAll(layoutNode.providedAlignmentLines)
            owner.snapshotObserver.observeMeasureSnapshotReads(layoutNode) {
                outerWrapper.measure(constraints)
            }
            layoutNode.layoutState = LayoutState.NeedsRelayout
            if (layoutNode.providedAlignmentLines != lastProvidedAlignmentLines) {
                layoutNode.onAlignmentsChanged()
            }
            val previousSize = measuredSize
            val newWidth = outerWrapper.width
            val newHeight = outerWrapper.height
            if (newWidth != previousSize.width ||
                newHeight != previousSize.height
            ) {
                measuredSize = IntSize(newWidth, newHeight)
                return true
            }
        }
        return false
    }

    override fun get(line: AlignmentLine): Int = outerWrapper[line]

    override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        lastPosition = position
        lastZIndex = zIndex
        lastLayerBlock = layerBlock
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
        placeAt(checkNotNull(lastPosition), lastZIndex, lastLayerBlock)
    }

    override fun minIntrinsicWidth(height: Int): Int = outerWrapper.minIntrinsicWidth(height)

    override fun maxIntrinsicWidth(height: Int): Int = outerWrapper.maxIntrinsicWidth(height)

    override fun minIntrinsicHeight(width: Int): Int = outerWrapper.minIntrinsicHeight(width)

    override fun maxIntrinsicHeight(width: Int): Int = outerWrapper.maxIntrinsicHeight(width)

    /**
     * Recalculates the parent data.
     */
    fun recalculateParentData() {
        parentData = outerWrapper.parentData
    }
}
