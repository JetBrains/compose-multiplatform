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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset

internal class ModifiedLayoutNode(
    override var wrapped: LayoutNodeWrapper,
    var modifier: LayoutModifier
) : LayoutNodeWrapper(wrapped.layoutNode) {
    override val measureScope: MeasureScope
        get() = wrapped.measureScope

    // This is used by LayoutNode to mark LayoutNodeWrappers that are going to be reused
    // because they match the modifier instance.
    var toBeReusedForSameModifier = false

    override fun onInitialize() {
        super.onInitialize()
        wrapped.wrappedBy = this
    }

    override fun measure(constraints: Constraints): Placeable {
        val placeable = performingMeasure(constraints) {
            with(modifier) {
                measureResult = measureScope.measure(wrapped, constraints)
                this@ModifiedLayoutNode
            }
        }
        onMeasured()
        return placeable
    }

    override fun minIntrinsicWidth(height: Int): Int =
        with(modifierFromState()) {
            measureScope.minIntrinsicWidth(wrapped, height)
        }

    override fun maxIntrinsicWidth(height: Int): Int =
        with(modifierFromState()) {
            measureScope.maxIntrinsicWidth(wrapped, height)
        }

    override fun minIntrinsicHeight(width: Int): Int =
        with(modifierFromState()) {
            measureScope.minIntrinsicHeight(wrapped, width)
        }

    override fun maxIntrinsicHeight(width: Int): Int =
        with(modifierFromState()) {
            measureScope.maxIntrinsicHeight(wrapped, width)
        }

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

        onPlaced()

        PlacementScope.executeWithRtlMirroringValues(
            measuredSize.width,
            measureScope.layoutDirection
        ) {
            measureResult.placeChildren()
        }
    }

    private var modifierState: MutableState<LayoutModifier>? = null

    @Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
    private fun modifierFromState(): LayoutModifier {
        val currentModifierState = modifierState ?: mutableStateOf(modifier)
        modifierState = currentModifierState
        return currentModifierState.value
    }

    override fun onModifierChanged() {
        super.onModifierChanged()
        modifierState?.value = modifier
    }

    override fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int {
        if (measureResult.alignmentLines.containsKey(alignmentLine)) {
            return measureResult.alignmentLines[alignmentLine] ?: AlignmentLine.Unspecified
        }
        val positionInWrapped = wrapped[alignmentLine]
        if (positionInWrapped == AlignmentLine.Unspecified) {
            return AlignmentLine.Unspecified
        }
        // Place our wrapped to obtain their position inside ourselves.
        isShallowPlacing = true
        placeAt(this.position, this.zIndex, this.layerBlock)
        isShallowPlacing = false
        return if (alignmentLine is HorizontalAlignmentLine) {
            positionInWrapped + wrapped.position.y
        } else {
            positionInWrapped + wrapped.position.x
        }
    }

    override fun performDraw(canvas: Canvas) {
        wrapped.draw(canvas)
        if (layoutNode.requireOwner().showLayoutBounds) {
            drawBorder(canvas, modifierBoundsPaint)
        }
    }

    internal companion object {
        val modifierBoundsPaint = Paint().also { paint ->
            paint.color = Color.Blue
            paint.strokeWidth = 1f
            paint.style = PaintingStyle.Stroke
        }
    }
}
