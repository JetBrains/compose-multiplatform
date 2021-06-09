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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.input.nestedscroll.NestedScrollDelegatingWrapper
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset

internal class InnerPlaceable(
    layoutNode: LayoutNode
) : LayoutNodeWrapper(layoutNode), Density by layoutNode.measureScope {

    override val measureScope get() = layoutNode.measureScope

    override fun measure(constraints: Constraints): Placeable = performingMeasure(constraints) {
        val measureResult = with(layoutNode.measurePolicy) {
            layoutNode.measureScope.measure(layoutNode.children, constraints)
        }
        layoutNode.handleMeasureResult(measureResult)
        return this
    }

    override val parentData: Any?
        get() = null

    override fun findPreviousFocusWrapper() = wrappedBy?.findPreviousFocusWrapper()

    override fun findNextFocusWrapper(): ModifiedFocusNode? = null

    override fun findLastFocusWrapper(): ModifiedFocusNode? = findPreviousFocusWrapper()

    override fun findPreviousKeyInputWrapper() = wrappedBy?.findPreviousKeyInputWrapper()

    override fun findPreviousNestedScrollWrapper() = wrappedBy?.findPreviousNestedScrollWrapper()

    override fun findNextNestedScrollWrapper(): NestedScrollDelegatingWrapper? = null

    override fun findNextKeyInputWrapper(): ModifiedKeyInputNode? = null

    override fun findLastKeyInputWrapper(): ModifiedKeyInputNode? = findPreviousKeyInputWrapper()

    override fun minIntrinsicWidth(height: Int) =
        layoutNode.intrinsicsPolicy.minIntrinsicWidth(height)

    override fun minIntrinsicHeight(width: Int) =
        layoutNode.intrinsicsPolicy.minIntrinsicHeight(width)

    override fun maxIntrinsicWidth(height: Int) =
        layoutNode.intrinsicsPolicy.maxIntrinsicWidth(height)

    override fun maxIntrinsicHeight(width: Int) =
        layoutNode.intrinsicsPolicy.maxIntrinsicHeight(width)

    override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        super.placeAt(position, zIndex, layerBlock)

        // The wrapper only runs their placement block to obtain our position, which allows them
        // to calculate the offset of an alignment line we have already provided a position for.
        // No need to place our wrapped as well (we might have actually done this already in
        // get(line), to obtain the position of the alignment line the wrapper currently needs
        // our position in order ot know how to offset the value we provided).
        if (wrappedBy?.isShallowPlacing == true) return

        layoutNode.onNodePlaced()
    }

    override fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int {
        return layoutNode.calculateAlignmentLines()[alignmentLine] ?: AlignmentLine.Unspecified
    }

    override fun performDraw(canvas: Canvas) {
        val owner = layoutNode.requireOwner()
        layoutNode.zSortedChildren.forEach { child ->
            if (child.isPlaced) {
                child.draw(canvas)
            }
        }
        if (owner.showLayoutBounds) {
            drawBorder(canvas, innerBoundsPaint)
        }
    }

    override fun hitTest(
        pointerPosition: Offset,
        hitPointerInputFilters: MutableList<PointerInputFilter>
    ) {
        if (withinLayerBounds(pointerPosition)) {
            // Any because as soon as true is returned, we know we have found a hit path and we must
            // not add PointerInputFilters on different paths so we should not even go looking.
            val originalSize = hitPointerInputFilters.size
            layoutNode.zSortedChildren.reversedAny { child ->
                if (child.isPlaced) {
                    callHitTest(child, pointerPosition, hitPointerInputFilters)
                    hitPointerInputFilters.size > originalSize
                } else {
                    false
                }
            }
        }
    }

    override fun getWrappedByCoordinates(): LayoutCoordinates {
        return this
    }

    internal companion object {
        val innerBoundsPaint = Paint().also { paint ->
            paint.color = Color.Red
            paint.strokeWidth = 1f
            paint.style = PaintingStyle.Stroke
        }

        private fun callHitTest(
            node: LayoutNode,
            pointerPosition: Offset,
            hitPointerInputFilters: MutableList<PointerInputFilter>
        ) {
            node.hitTest(pointerPosition, hitPointerInputFilters)
        }
    }
}
