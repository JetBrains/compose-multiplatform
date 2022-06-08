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

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LookaheadLayoutCoordinatesImpl
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * This is the base class for LayoutNodeWrapper and LookaheadDelegate. The common
 * functionalities between the two are extracted here.
 */
internal abstract class LookaheadCapablePlaceable : Placeable() {
    abstract val position: IntOffset
    abstract val child: LookaheadCapablePlaceable?
    abstract val hasMeasureResult: Boolean
    final override fun get(alignmentLine: AlignmentLine): Int {
        if (!hasMeasureResult) return AlignmentLine.Unspecified
        val measuredPosition = calculateAlignmentLine(alignmentLine)
        if (measuredPosition == AlignmentLine.Unspecified) return AlignmentLine.Unspecified
        return measuredPosition + if (alignmentLine is VerticalAlignmentLine) {
            apparentToRealOffset.x
        } else {
            apparentToRealOffset.y
        }
    }

    abstract fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int

    // True when the wrapper is running its own placing block to obtain the position
    // in parent, but is not interested in the position of children.
    internal var isShallowPlacing: Boolean = false
    internal abstract val measureResult: MeasureResult
    internal abstract fun replace()
    abstract val alignmentLinesOwner: AlignmentLinesOwner

    protected fun LayoutNodeWrapper.invalidateAlignmentLinesFromPositionChange() {
        if (wrapped?.layoutNode != layoutNode) {
            alignmentLinesOwner.alignmentLines.onAlignmentsChanged()
        } else {
            alignmentLinesOwner.parentAlignmentLinesOwner?.alignmentLines?.onAlignmentsChanged()
        }
    }
}

internal abstract class LookaheadDelegate(
    val wrapper: LayoutNodeWrapper,
    val lookaheadScope: LookaheadScope
) : Measurable, LookaheadCapablePlaceable() {
    override val child: LookaheadCapablePlaceable?
        get() = wrapper.wrapped?.lookaheadDelegate
    override val hasMeasureResult: Boolean
        get() = _measureResult != null
    override var position = IntOffset.Zero
    private var oldAlignmentLines: MutableMap<AlignmentLine, Int>? = null
    override val measureResult: MeasureResult
        get() = _measureResult ?: error(
            "LookaheadDelegate has not been measured yet when measureResult is requested."
        )

    val lookaheadLayoutCoordinates = LookaheadLayoutCoordinatesImpl(this)
    override val alignmentLinesOwner: AlignmentLinesOwner
        get() = wrapper.layoutNode.layoutDelegate.lookaheadAlignmentLinesOwner!!

    private var _measureResult: MeasureResult? = null
        set(result) {
            result?.let {
                measuredSize = IntSize(it.width, it.height)
            } ?: run { measuredSize = IntSize.Zero }
            if (field != result && result != null) {
                // We do not simply compare against old.alignmentLines in case this is a
                // MutableStateMap and the same instance might be passed.
                if ((!oldAlignmentLines.isNullOrEmpty() || result.alignmentLines.isNotEmpty()) &&
                    result.alignmentLines != oldAlignmentLines
                ) {
                    alignmentLinesOwner.alignmentLines.onAlignmentsChanged()

                    val oldLines = oldAlignmentLines
                        ?: (mutableMapOf<AlignmentLine, Int>().also { oldAlignmentLines = it })
                    oldLines.clear()
                    oldLines.putAll(result.alignmentLines)
                }
            }
            field = result
        }

    protected val cachedAlignmentLinesMap = mutableMapOf<AlignmentLine, Int>()

    internal fun getCachedAlignmentLine(alignmentLine: AlignmentLine): Int =
        cachedAlignmentLinesMap[alignmentLine] ?: AlignmentLine.Unspecified

    override fun replace() {
        placeAt(position, 0f, null)
    }

    final override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        if (this.position != position) {
            this.position = position
            wrapper.invalidateAlignmentLinesFromPositionChange()
        }
        if (isShallowPlacing) return
        placeChildren()
    }

    protected open fun placeChildren() {
        PlacementScope.executeWithRtlMirroringValues(
            measureResult.width,
            wrapper.measureScope.layoutDirection
        ) {
            measureResult.placeChildren()
        }
    }

    inline fun performingMeasure(
        constraints: Constraints,
        block: () -> MeasureResult
    ): Placeable {
        measurementConstraints = constraints
        _measureResult = block()
        return this
    }

    override val parentData: Any?
        get() = wrapper.parentData

    override fun minIntrinsicWidth(height: Int): Int {
        return wrapper.wrapped!!.lookaheadDelegate!!.minIntrinsicWidth(height)
    }

    override fun maxIntrinsicWidth(height: Int): Int {
        return wrapper.wrapped!!.lookaheadDelegate!!.maxIntrinsicWidth(height)
    }

    override fun minIntrinsicHeight(width: Int): Int {
        return wrapper.wrapped!!.lookaheadDelegate!!.minIntrinsicHeight(width)
    }

    override fun maxIntrinsicHeight(width: Int): Int {
        return wrapper.wrapped!!.lookaheadDelegate!!.maxIntrinsicHeight(width)
    }

    internal fun positionIn(ancestor: LookaheadDelegate): IntOffset {
        var aggregatedOffset = IntOffset.Zero
        var lookaheadDelegate = this
        while (lookaheadDelegate != ancestor) {
            aggregatedOffset += lookaheadDelegate.position
            lookaheadDelegate = lookaheadDelegate.wrapper.wrappedBy!!.lookaheadDelegate!!
        }
        return aggregatedOffset
    }
}