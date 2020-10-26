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

import androidx.compose.ui.DrawLayerModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.layout.globalPosition
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset

internal class LayerWrapper(
    wrapped: LayoutNodeWrapper,
    modifier: DrawLayerModifier
) : DelegatingLayoutNodeWrapper<DrawLayerModifier>(wrapped, modifier), (Canvas) -> Unit {
    private var _layer: OwnedLayer? = null

    // Do not invalidate itself on position change.
    override val invalidateLayerOnBoundsChange get() = false

    override var modifier: DrawLayerModifier
        get() = super.modifier
        set(value) {
            super.modifier = value
            _layer?.modifier = value
        }

    private val invalidateParentLayer: () -> Unit = {
        wrappedBy?.invalidateLayer()
    }

    /**
     * True when the last drawing of this layer didn't draw the real content as the LayoutNode
     * containing this layer was not placed by the parent.
     */
    internal var lastDrawingWasSkipped = false
        private set

    val layer: OwnedLayer
        get() = _layer!!

    // TODO (njawad): This cache matrix is not thread safe
    private var _matrixCache: Matrix? = null
    private val matrixCache: Matrix
        get() = _matrixCache ?: Matrix().also { _matrixCache = it }

    override fun performMeasure(constraints: Constraints): Placeable {
        val placeable = super.performMeasure(constraints)
        layer.resize(measuredSize)
        return placeable
    }

    override fun placeAt(position: IntOffset) {
        super.placeAt(position)
        layer.move(position)
    }

    override fun draw(canvas: Canvas) {
        layer.drawLayer(canvas)
    }

    override fun attach() {
        super.attach()
        _layer = layoutNode.requireOwner().createLayer(
            modifier,
            this,
            invalidateParentLayer
        )
        invalidateParentLayer()
    }

    override fun detach() {
        super.detach()
        // The layer has been removed and we need to invalidate the containing layer. We've lost
        // which layer contained this one, but all layers in this modifier chain will be invalidated
        // in onModifierChanged(). Therefore the only possible layer that won't automatically be
        // invalidated is the parent's layer. We'll invalidate it here:
        @OptIn(ExperimentalLayoutNodeApi::class)
        layoutNode.parent?.invalidateLayer()
        _layer?.destroy()
        _layer = null
    }

    override fun invalidateLayer() {
        _layer?.invalidate()
    }

    override fun fromParentPosition(position: Offset): Offset {
        val inverse = matrixCache
        layer.getMatrix(inverse)
        inverse.invert()
        val targetPosition = inverse.map(position)
        return super.fromParentPosition(targetPosition)
    }

    override fun toParentPosition(position: Offset): Offset {
        val matrix = matrixCache
        val targetPosition = matrix.map(position)
        return super.toParentPosition(targetPosition)
    }

    override fun rectInParent(bounds: MutableRect) {
        if (modifier.clip) {
            bounds.intersect(0f, 0f, size.width.toFloat(), size.height.toFloat())
            if (bounds.isEmpty) {
                return
            }
        }
        val matrix = matrixCache
        layer.getMatrix(matrix)
        matrix.map(bounds)
        return super.rectInParent(bounds)
    }

    override fun hitTest(
        pointerPositionRelativeToScreen: Offset,
        hitPointerInputFilters: MutableList<PointerInputFilter>
    ) {
        if (modifier.clip) {
            val l = globalPosition.x
            val t = globalPosition.y
            val r = l + width
            val b = t + height

            val localBoundsRelativeToScreen = Rect(l, t, r, b)
            if (!localBoundsRelativeToScreen.contains(pointerPositionRelativeToScreen)) {
                // If we should clip pointer input hit testing to our bounds, and the pointer is
                // not in our bounds, then return false now.
                return
            }
        }

        // If we are here, either we aren't clipping to bounds or we are and the pointer was in
        // bounds.
        super.hitTest(
            pointerPositionRelativeToScreen,
            hitPointerInputFilters
        )
    }

    override fun onModifierChanged() {
        _layer?.invalidate()
    }

    @ExperimentalLayoutNodeApi
    override fun invoke(canvas: Canvas) {
        if (layoutNode.isPlaced) {
            require(layoutNode.layoutState == LayoutNode.LayoutState.Ready) {
                "Layer is redrawn for LayoutNode in state ${layoutNode.layoutState} [$layoutNode]"
            }
            wrapped.draw(canvas)
            lastDrawingWasSkipped = false
        } else {
            // The invalidation is requested even for nodes which are not placed. As we are not
            // going to display them we skip the drawing. It is safe to just draw nothing as the
            // layer will be invalidated again when the node will be finally placed.
            lastDrawingWasSkipped = true
        }
    }
}
