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

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.nativeClass
import androidx.compose.ui.semantics.SemanticsWrapper
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset

/**
 * [LayoutNodeWrapper] with default implementations for methods.
 */
internal open class DelegatingLayoutNodeWrapper<T : Modifier.Element>(
    override var wrapped: LayoutNodeWrapper,
    open var modifier: T
) : LayoutNodeWrapper(wrapped.layoutNode) {
    override val measureScope: MeasureScope get() = wrapped.measureScope

    /**
     * Indicates that this modifier is used in [wrappedBy] also.
     */
    var isChained = false

    // This is used by LayoutNode to mark LayoutNodeWrappers that are going to be reused
    // because they match the modifier instance.
    var toBeReusedForSameModifier = false

    init {
        wrapped.wrappedBy = this
    }

    /**
     * Sets the modifier instance to the new modifier. [modifier] must be the
     * same type as the current modifier.
     */
    fun setModifierTo(modifier: Modifier.Element) {
        if (modifier !== this.modifier) {
            require(modifier.nativeClass() == this.modifier.nativeClass())
            @Suppress("UNCHECKED_CAST")
            this.modifier = modifier as T
        }
    }

    override fun performDraw(canvas: Canvas) {
        wrapped.draw(canvas)
    }

    override fun hitTest(
        pointerPosition: Offset,
        hitTestResult: HitTestResult<PointerInputFilter>,
        isTouchEvent: Boolean
    ) {
        if (withinLayerBounds(pointerPosition, isTouchEvent)) {
            val positionInWrapped = wrapped.fromParentPosition(pointerPosition)
            wrapped.hitTest(positionInWrapped, hitTestResult, isTouchEvent)
        }
    }

    override fun hitTestSemantics(
        pointerPosition: Offset,
        hitSemanticsWrappers: HitTestResult<SemanticsWrapper>
    ) {
        if (withinLayerBounds(pointerPosition, true)) {
            val positionInWrapped = wrapped.fromParentPosition(pointerPosition)
            wrapped.hitTestSemantics(positionInWrapped, hitSemanticsWrappers)
        }
    }

    override fun calculateAlignmentLine(alignmentLine: AlignmentLine) = wrapped[alignmentLine]

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

        PlacementScope.executeWithRtlMirroringValues(
            measuredSize.width,
            measureScope.layoutDirection
        ) {
            measureResult.placeChildren()
        }
    }

    override fun measure(constraints: Constraints): Placeable = performingMeasure(constraints) {
        val placeable = wrapped.measure(constraints)
        measureResult = object : MeasureResult {
            override val width: Int = wrapped.measureResult.width
            override val height: Int = wrapped.measureResult.height
            override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()
            override fun placeChildren() {
                with(PlacementScope) {
                    placeable.place(-apparentToRealOffset)
                }
            }
        }
        return this
    }

    override fun findPreviousFocusWrapper() = wrappedBy?.findPreviousFocusWrapper()

    override fun findNextFocusWrapper(excludeDeactivated: Boolean): ModifiedFocusNode? {
        return wrapped.findNextFocusWrapper(excludeDeactivated)
    }

    override fun findLastFocusWrapper(): ModifiedFocusNode? {
        var lastFocusWrapper: ModifiedFocusNode? = null

        // Find last focus wrapper for the current layout node.
        var next: ModifiedFocusNode? = findNextFocusWrapper(excludeDeactivated = false)
        while (next != null) {
            lastFocusWrapper = next
            next = next.wrapped.findNextFocusWrapper(excludeDeactivated = false)
        }
        return lastFocusWrapper
    }

    override fun findPreviousNestedScrollWrapper() = wrappedBy?.findPreviousNestedScrollWrapper()

    override fun findNextNestedScrollWrapper() = wrapped.findNextNestedScrollWrapper()

    override fun findPreviousKeyInputWrapper() = wrappedBy?.findPreviousKeyInputWrapper()

    override fun findNextKeyInputWrapper() = wrapped.findNextKeyInputWrapper()

    override fun findLastKeyInputWrapper(): ModifiedKeyInputNode? {
        val wrapper = layoutNode.innerLayoutNodeWrapper.findPreviousKeyInputWrapper()
        return if (wrapper !== this) wrapper else null
    }

    override fun minIntrinsicWidth(height: Int) = wrapped.minIntrinsicWidth(height)

    override fun maxIntrinsicWidth(height: Int) = wrapped.maxIntrinsicWidth(height)

    override fun minIntrinsicHeight(width: Int) = wrapped.minIntrinsicHeight(width)

    override fun maxIntrinsicHeight(width: Int) = wrapped.maxIntrinsicHeight(width)

    override val parentData: Any? get() = wrapped.parentData

    /**
     * Returns the additional amount on the horizontal and vertical dimensions that
     * this extends beyond [width] and [height] on all sides. This takes into account
     * [minimumTouchTargetSize] and [measuredSize] vs. [width] and [height].
     */
    protected fun calculateMinimumTouchTargetPadding(minimumTouchTargetSize: Size): Size {
        val widthDiff = minimumTouchTargetSize.width - measuredWidth.toFloat()
        val heightDiff = minimumTouchTargetSize.height - measuredHeight.toFloat()
        return Size(maxOf(0f, widthDiff / 2f), maxOf(0f, heightDiff / 2f))
    }

    /**
     * Does a hit test, adding [content] as a [HitTestResult.hit] or
     * [HitTestResult.hitInMinimumTouchTarget] depending on whether or not it hit
     * or hit in the minimum touch target size area. The newly-created [HitTestResult] is returned
     * if there was a hit or `null` is returned if it missed.
     */
    protected fun <T> hitTestInMinimumTouchTarget(
        pointerPosition: Offset,
        hitTestResult: HitTestResult<T>,
        forceParentIntercept: Boolean,
        useTouchSize: Boolean,
        content: T,
        block: () -> Unit
    ) {
        if (!withinLayerBounds(pointerPosition, useTouchSize)) {
            return
        }
        if (isPointerInBounds(pointerPosition)) {
            hitTestResult.hit(content, block)
        } else {
            val offsetFromEdge = offsetFromEdge(pointerPosition)
            val distanceFromEdge = maxOf(offsetFromEdge.x, offsetFromEdge.y)

            if (useTouchSize && isHitInMinimumTouchTarget(offsetFromEdge, minimumTouchTargetSize) &&
                hitTestResult.isHitInMinimumTouchTargetBetter(distanceFromEdge)
            ) {
                // Hit closer than existing handlers, so just record it
                hitTestResult.hitInMinimumTouchTarget(content, distanceFromEdge, block)
            }
            if (forceParentIntercept) {
                // We only want to replace the existing touch target if there are better
                // hits in the children
                hitTestResult.speculativeHit(content, distanceFromEdge, block)
            } else {
                // The parent wasn't hit, but the child may be.
                block()
            }
        }
    }

    private fun isHitInMinimumTouchTarget(
        offsetFromEdge: Offset,
        minimumTouchTargetSize: Size
    ): Boolean {
        val (width, height) = calculateMinimumTouchTargetPadding(minimumTouchTargetSize)
        return (width > 0f || height > 0f) &&
            offsetFromEdge.x <= width && offsetFromEdge.y <= height
    }
}
