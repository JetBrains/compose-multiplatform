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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.IntermediateLayoutModifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

@OptIn(ExperimentalComposeUiApi::class)
internal class ModifiedLayoutNode(
    override var wrapped: LayoutNodeWrapper,
    var modifier: LayoutModifier
) : LayoutNodeWrapper(wrapped.layoutNode) {
    override val measureScope: MeasureScope
        get() = wrapped.measureScope

    private var lookAheadTransientLayoutModifier: IntermediateLayoutModifier? =
        modifier as? IntermediateLayoutModifier

    // This is used by LayoutNode to mark LayoutNodeWrappers that are going to be reused
    // because they match the modifier instance.
    var toBeReusedForSameModifier = false
    override fun onInitialize() {
        super.onInitialize()
        wrapped.wrappedBy = this
    }

    /**
     * LookaheadDelegate impl for when the [modifier] is any [LayoutModifier] except
     * [IntermediateLayoutModifier]. This impl will invoke [LayoutModifier.measure] for
     * the lookahead measurement.
     */
    private inner class LookaheadDelegateForLayoutModifier(
        scope: LookaheadScope
    ) : LookaheadDelegate(this, scope) {
        // LookaheadMeasure
        override fun measure(constraints: Constraints): Placeable =
            performingMeasure(constraints) {
                with(modifier) {
                    measureScope.measure(
                        // This allows `measure` calls in the modifier to be redirected to
                        // calling lookaheadMeasure in wrapped.
                        wrapped.lookaheadDelegate!!, constraints
                    )
                }
            }

        override fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int {
            return calculateAlignmentAndPlaceChildAsNeeded(alignmentLine).also {
                cachedAlignmentLinesMap[alignmentLine] = it
            }
        }

        override fun minIntrinsicWidth(height: Int): Int =
            with(modifierFromState()) {
                measureScope.minIntrinsicWidth(wrapped.lookaheadDelegate!!, height)
            }

        override fun maxIntrinsicWidth(height: Int): Int =
            with(modifierFromState()) {
                measureScope.maxIntrinsicWidth(wrapped.lookaheadDelegate!!, height)
            }

        override fun minIntrinsicHeight(width: Int): Int =
            with(modifierFromState()) {
                measureScope.minIntrinsicHeight(wrapped.lookaheadDelegate!!, width)
            }

        override fun maxIntrinsicHeight(width: Int): Int =
            with(modifierFromState()) {
                measureScope.maxIntrinsicHeight(wrapped.lookaheadDelegate!!, width)
            }
    }

    /**
     * LookaheadDelegate impl for when the [modifier] is an
     * [IntermediateLayoutModifier]. This impl will redirect the measure call to the next
     * lookahead delegate in the chain, without invoking the measure lambda defined in the modifier.
     * This is necessary because [IntermediateLayoutModifier] does not participate in lookahead.
     */
    private inner class LookaheadDelegateForIntermediateLayoutModifier(
        scope: LookaheadScope,
        val intermediateLayoutModifier: IntermediateLayoutModifier
    ) : LookaheadDelegate(this, scope) {
        private inner class PassThroughMeasureResult : MeasureResult {
            override val width: Int
                get() = wrapped.lookaheadDelegate!!.measureResult.width
            override val height: Int
                get() = wrapped.lookaheadDelegate!!.measureResult.height
            override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()

            override fun placeChildren() {
                with(PlacementScope) {
                    wrapped.lookaheadDelegate!!.place(0, 0)
                }
            }
        }
        private val passThroughMeasureResult = PassThroughMeasureResult()

        // LookaheadMeasure
        override fun measure(constraints: Constraints): Placeable =
            with(intermediateLayoutModifier) {
                performingMeasure(constraints) {
                    wrapped.lookaheadDelegate!!.run {
                        measure(constraints)
                        targetSize = IntSize(measureResult.width, measureResult.height)
                    }
                    passThroughMeasureResult
                }
            }

        override fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int {
            return calculateAlignmentAndPlaceChildAsNeeded(alignmentLine).also {
                cachedAlignmentLinesMap[alignmentLine] = it
            }
        }
    }

    override fun createLookaheadDelegate(scope: LookaheadScope): LookaheadDelegate {
        return lookAheadTransientLayoutModifier?.let {
            LookaheadDelegateForIntermediateLayoutModifier(scope, it)
        } ?: LookaheadDelegateForLayoutModifier(scope)
    }

    override fun measure(constraints: Constraints): Placeable {
        performingMeasure(constraints) {
            with(modifier) {
                measureResult = measureScope.measure(wrapped, constraints)
                this@ModifiedLayoutNode
            }
        }
        onMeasured()
        return this
    }

    override fun minIntrinsicWidth(height: Int): Int {
        return with(modifierFromState()) {
            measureScope.minIntrinsicWidth(wrapped, height)
        }
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
        if (isShallowPlacing) return
        onPlaced()
        PlacementScope.executeWithRtlMirroringValues(
            measuredSize.width,
            measureScope.layoutDirection
        ) {
            measureResult.placeChildren()
        }
    }

    private var modifierState: MutableState<LayoutModifier?> = mutableStateOf(null)

    @Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
    private fun modifierFromState(): LayoutModifier {
        val currentModifier = modifierState.value ?: modifier
        modifierState.value = currentModifier
        return currentModifier
    }

    override fun onModifierChanged() {
        super.onModifierChanged()
        modifierState.value = modifier
        modifier.let { modifier ->
            // Creates different [LookaheadDelegate]s based on the type of the modifier.
            if (modifier is IntermediateLayoutModifier) {
                lookAheadTransientLayoutModifier = modifier
                lookaheadDelegate?.let {
                    updateLookaheadDelegate(
                        LookaheadDelegateForIntermediateLayoutModifier(it.lookaheadScope, modifier)
                    )
                }
            } else {
                lookAheadTransientLayoutModifier = null
                lookaheadDelegate?.let {
                    updateLookaheadDelegate(
                        LookaheadDelegateForLayoutModifier(it.lookaheadScope)
                    )
                }
            }
        }
    }

    override fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int {
        return lookaheadDelegate?.getCachedAlignmentLine(alignmentLine)
            ?: calculateAlignmentAndPlaceChildAsNeeded(alignmentLine)
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

private fun LookaheadCapablePlaceable.calculateAlignmentAndPlaceChildAsNeeded(
    alignmentLine: AlignmentLine
): Int {
    val child = child
    check(child != null) {
        "Child of $this cannot be null when calculating alignment line"
    }
    if (measureResult.alignmentLines.containsKey(alignmentLine)) {
        return measureResult.alignmentLines[alignmentLine] ?: AlignmentLine.Unspecified
    }
    val positionInWrapped = child[alignmentLine]
    if (positionInWrapped == AlignmentLine.Unspecified) {
        return AlignmentLine.Unspecified
    }
    // Place our wrapped to obtain their position inside ourselves.
    child.isShallowPlacing = true
    replace()
    child.isShallowPlacing = false
    return if (alignmentLine is HorizontalAlignmentLine) {
        positionInWrapped + child.position.y
    } else {
        positionInWrapped + child.position.x
    }
}