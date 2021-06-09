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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints

internal class ModifiedLayoutNode(
    wrapped: LayoutNodeWrapper,
    modifier: LayoutModifier
) : DelegatingLayoutNodeWrapper<LayoutModifier>(wrapped, modifier) {

    override fun measure(constraints: Constraints): Placeable = performingMeasure(constraints) {
        with(modifier) {
            measureResult = measureScope.measure(wrapped, constraints)
            this@ModifiedLayoutNode
        }
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

    override fun getWrappedByCoordinates(): LayoutCoordinates {
        return this
    }

    internal companion object {
        val modifierBoundsPaint = Paint().also { paint ->
            paint.color = Color.Blue
            paint.strokeWidth = 1f
            paint.style = PaintingStyle.Stroke
        }
    }
}
