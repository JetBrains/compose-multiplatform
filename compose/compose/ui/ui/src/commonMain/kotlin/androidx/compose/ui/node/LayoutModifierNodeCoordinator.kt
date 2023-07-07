/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.IntermediateLayoutModifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

@OptIn(ExperimentalComposeUiApi::class)
internal class LayoutModifierNodeCoordinator(
    layoutNode: LayoutNode,
    measureNode: LayoutModifierNode,
) : NodeCoordinator(layoutNode) {
    var layoutModifierNode: LayoutModifierNode = measureNode
        internal set

    override val tail: Modifier.Node
        get() = layoutModifierNode.node

    val wrappedNonNull: NodeCoordinator get() = wrapped!!

    private var lookAheadTransientMeasureNode: IntermediateLayoutModifierNode? = measureNode.run {
        if (node.isKind(Nodes.IntermediateMeasure) && this is IntermediateLayoutModifierNode) this
        else null
    }

    /**
     * LookaheadDelegate impl for when the modifier is any [LayoutModifier] except
     * [IntermediateLayoutModifier]. This impl will invoke [LayoutModifier.measure] for
     * the lookahead measurement.
     */
    private inner class LookaheadDelegateForLayoutModifierNode(
        scope: LookaheadScope
    ) : LookaheadDelegate(this, scope) {
        // LookaheadMeasure
        override fun measure(constraints: Constraints): Placeable =
            performingMeasure(constraints) {
                with(layoutModifierNode) {
                    measure(
                        // This allows `measure` calls in the modifier to be redirected to
                        // calling lookaheadMeasure in wrapped.
                        wrappedNonNull.lookaheadDelegate!!, constraints
                    )
                }
            }

        override fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int {
            return calculateAlignmentAndPlaceChildAsNeeded(alignmentLine).also {
                cachedAlignmentLinesMap[alignmentLine] = it
            }
        }

        override fun minIntrinsicWidth(height: Int): Int =
            with(layoutModifierNode) {
                minIntrinsicWidth(wrappedNonNull.lookaheadDelegate!!, height)
            }

        override fun maxIntrinsicWidth(height: Int): Int =
            with(layoutModifierNode) {
                maxIntrinsicWidth(wrappedNonNull.lookaheadDelegate!!, height)
            }

        override fun minIntrinsicHeight(width: Int): Int =
            with(layoutModifierNode) {
                minIntrinsicHeight(wrappedNonNull.lookaheadDelegate!!, width)
            }

        override fun maxIntrinsicHeight(width: Int): Int =
            with(layoutModifierNode) {
                maxIntrinsicHeight(wrappedNonNull.lookaheadDelegate!!, width)
            }
    }

    /**
     * LookaheadDelegate impl for when the [layoutModifierNode] is an
     * [IntermediateLayoutModifierNode]. This impl will redirect the measure call to the next
     * lookahead delegate in the chain, without invoking the measure lambda defined in the modifier.
     * This is necessary because [IntermediateLayoutModifierNode] does not participate in lookahead.
     */
    private inner class LookaheadDelegateForIntermediateLayoutModifier(
        scope: LookaheadScope,
        val intermediateMeasureNode: IntermediateLayoutModifierNode
    ) : LookaheadDelegate(this, scope) {
        private inner class PassThroughMeasureResult : MeasureResult {
            override val width: Int
                get() = wrappedNonNull.lookaheadDelegate!!.measureResult.width
            override val height: Int
                get() = wrappedNonNull.lookaheadDelegate!!.measureResult.height
            override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()

            override fun placeChildren() {
                with(PlacementScope) {
                    wrappedNonNull.lookaheadDelegate!!.place(0, 0)
                }
            }
        }
        private val passThroughMeasureResult = PassThroughMeasureResult()

        // LookaheadMeasure
        override fun measure(constraints: Constraints): Placeable =
            with(intermediateMeasureNode) {
                performingMeasure(constraints) {
                    wrappedNonNull.lookaheadDelegate!!.run {
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
        return lookAheadTransientMeasureNode?.let {
            LookaheadDelegateForIntermediateLayoutModifier(scope, it)
        } ?: LookaheadDelegateForLayoutModifierNode(scope)
    }

    override fun measure(constraints: Constraints): Placeable {
        performingMeasure(constraints) {
            with(layoutModifierNode) {
                measureResult = measure(wrappedNonNull, constraints)
                this@LayoutModifierNodeCoordinator
            }
        }
        onMeasured()
        return this
    }

    override fun minIntrinsicWidth(height: Int): Int {
        return with(layoutModifierNode) {
            minIntrinsicWidth(wrappedNonNull, height)
        }
    }

    override fun maxIntrinsicWidth(height: Int): Int =
        with(layoutModifierNode) {
            maxIntrinsicWidth(wrappedNonNull, height)
        }

    override fun minIntrinsicHeight(width: Int): Int =
        with(layoutModifierNode) {
            minIntrinsicHeight(wrappedNonNull, width)
        }

    override fun maxIntrinsicHeight(width: Int): Int =
        with(layoutModifierNode) {
            maxIntrinsicHeight(wrappedNonNull, width)
        }

    override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        super.placeAt(position, zIndex, layerBlock)
        // The coordinator only runs their placement block to obtain our position, which allows them
        // to calculate the offset of an alignment line we have already provided a position for.
        // No need to place our wrapped as well (we might have actually done this already in
        // get(line), to obtain the position of the alignment line the coordinator currently needs
        // our position in order ot know how to offset the value we provided).
        if (isShallowPlacing) return
        onPlaced()
        PlacementScope.executeWithRtlMirroringValues(
            measuredSize.width,
            layoutDirection,
            this
        ) {
            measureResult.placeChildren()
        }
    }

    override fun onLayoutModifierNodeChanged() {
        super.onLayoutModifierNodeChanged()
        layoutModifierNode.let { node ->
            // Creates different [LookaheadDelegate]s based on the type of the modifier.
            if (node.node.isKind(Nodes.IntermediateMeasure) &&
                node is IntermediateLayoutModifierNode
            ) {
                lookAheadTransientMeasureNode = node
                lookaheadDelegate?.let {
                    updateLookaheadDelegate(
                        LookaheadDelegateForIntermediateLayoutModifier(it.lookaheadScope, node)
                    )
                }
            } else {
                lookAheadTransientMeasureNode = null
                lookaheadDelegate?.let {
                    updateLookaheadDelegate(
                        LookaheadDelegateForLayoutModifierNode(it.lookaheadScope)
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
        wrappedNonNull.draw(canvas)
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
    isPlacingForAlignment = true
    replace()
    child.isShallowPlacing = false
    isPlacingForAlignment = false
    return if (alignmentLine is HorizontalAlignmentLine) {
        positionInWrapped + child.position.y
    } else {
        positionInWrapped + child.position.x
    }
}
