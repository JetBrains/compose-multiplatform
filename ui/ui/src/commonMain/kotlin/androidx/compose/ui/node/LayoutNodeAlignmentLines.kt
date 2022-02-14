/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.merge
import kotlin.math.roundToInt

internal class LayoutNodeAlignmentLines(
    private val layoutNode: LayoutNode
) {
    /**
     * `true` when the alignment lines needs to be recalculated because they might have changed.
     */
    internal var dirty = true

    /**
     * `true` when the alignment lines were used by the parent during measurement.
     */
    internal var usedDuringParentMeasurement = false

    /**
     * `true` when the alignment lines have been used by the parent during the current layout (or
     * previous layout if there is no layout in progress).
     */
    internal var usedDuringParentLayout = false
    /**
     * `true` when the alignment lines were used by the parent during the last completed layout.
     */
    internal var previousUsedDuringParentLayout = false

    /**
     * `true` when the alignment lines were used by the modifier of the node during measurement.
     */
    internal var usedByModifierMeasurement = false

    /**
     * `true` when the alignment lines were used by the modifier of the node during measurement.
     */
    internal var usedByModifierLayout = false

    /**
     * `true` when the direct parent or our modifier relies on our alignment lines.
     */
    internal val queried get() = usedDuringParentMeasurement ||
        previousUsedDuringParentLayout || usedByModifierMeasurement ||
        usedByModifierLayout

    /**
     * The closest layout node ancestor who was asked for alignment lines, either by the parent or
     * their own modifier. If the owner stops being queried for alignment lines, we have to
     * [recalculateQueryOwner] to find the new owner if one exists.
     */
    private var queryOwner: LayoutNode? = null

    /**
     * Whether the alignment lines of this node are relevant (whether an ancestor depends on them).
     */
    internal val required: Boolean get() {
        recalculateQueryOwner()
        return queryOwner != null
    }

    /**
     * Updates the alignment lines query owner according to the current values of the
     * alignmentUsedBy* of the layout nodes in the hierarchy.
     */
    internal fun recalculateQueryOwner() {
        queryOwner = if (queried) {
            layoutNode
        } else {
            val parent = layoutNode.parent ?: return
            val parentQueryOwner = parent.alignmentLines.queryOwner
            if (parentQueryOwner != null && parentQueryOwner.alignmentLines.queried) {
                parentQueryOwner
            } else {
                val owner = queryOwner
                if (owner == null || owner.alignmentLines.queried) return
                owner.parent?.alignmentLines?.recalculateQueryOwner()
                owner.parent?.alignmentLines?.queryOwner
            }
        }
    }

    /**
     * The alignment lines of this layout, inherited + intrinsic
     */
    private val alignmentLines: MutableMap<AlignmentLine, Int> = hashMapOf()

    fun getLastCalculation(): Map<AlignmentLine, Int> = alignmentLines

    fun recalculate() {
        alignmentLines.clear()
        /**
         * Returns the alignment line value for a given alignment line without affecting whether
         * the flag for whether the alignment line was read.
         */
        fun addAlignmentLine(
            alignmentLine: AlignmentLine,
            initialPosition: Int,
            initialWrapper: LayoutNodeWrapper
        ) {
            var position = Offset(initialPosition.toFloat(), initialPosition.toFloat())
            var wrapper = initialWrapper
            while (true) {
                position = wrapper.toParentPosition(position)
                wrapper = wrapper.wrappedBy!!
                if (wrapper == layoutNode.innerLayoutNodeWrapper) break
                if (alignmentLine in wrapper.measureResult.alignmentLines) {
                    val newPosition = wrapper[alignmentLine]
                    position = Offset(newPosition.toFloat(), newPosition.toFloat())
                }
            }
            val positionInContainer = if (alignmentLine is HorizontalAlignmentLine) {
                position.y.roundToInt()
            } else {
                position.x.roundToInt()
            }
            // If the line was already provided by a previous child, merge the values.
            alignmentLines[alignmentLine] = if (alignmentLine in alignmentLines) {
                alignmentLine.merge(
                    alignmentLines.getValue(alignmentLine),
                    positionInContainer
                )
            } else {
                positionInContainer
            }
        }
        layoutNode._children.forEach { child ->
            if (!child.isPlaced) return@forEach
            if (child.alignmentLines.dirty) {
                // It did not need relayout, but we still call layout to recalculate
                // alignment lines.
                child.layoutChildren()
            }
            // Add alignment lines on the child node.
            child.alignmentLines.alignmentLines.forEach { (childLine, linePosition) ->
                addAlignmentLine(childLine, linePosition, child.innerLayoutNodeWrapper)
            }

            // Add alignment lines on the modifier of the child.
            var wrapper = child.innerLayoutNodeWrapper.wrappedBy!!
            while (wrapper != layoutNode.innerLayoutNodeWrapper) {
                wrapper.measureResult.alignmentLines.keys.forEach { childLine ->
                    addAlignmentLine(childLine, wrapper[childLine], wrapper)
                }
                wrapper = wrapper.wrappedBy!!
            }
        }
        alignmentLines += layoutNode.innerLayoutNodeWrapper.measureResult.alignmentLines
        dirty = false
    }

    internal fun reset() {
        dirty = true
        usedDuringParentMeasurement = false
        previousUsedDuringParentLayout = false
        usedDuringParentLayout = false
        usedByModifierMeasurement = false
        usedByModifierLayout = false
        queryOwner = null
    }
}
