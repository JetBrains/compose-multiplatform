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
     * The alignment lines of this layout, inherited + intrinsic
     */
    private val alignmentLines: MutableMap<AlignmentLine, Int> = hashMapOf()

    private val previousAlignmentLines = mutableMapOf<AlignmentLine, Int>()

    fun getLastCalculation(): Map<AlignmentLine, Int> = alignmentLines

    fun recalculate() {
        previousAlignmentLines.clear()
        previousAlignmentLines.putAll(alignmentLines)
        alignmentLines.clear()
        layoutNode._children.forEach { child ->
            val childAlignments = child.alignmentLines
            if (!child.isPlaced || childAlignments == null) return@forEach
            childAlignments.alignmentLines.keys.forEach { childLine ->
                val linePositionInContainer = childAlignments.getAlignmentLine(childLine)!!
                // If the line was already provided by a previous child, merge the values.
                alignmentLines[childLine] = if (childLine in alignmentLines) {
                    childLine.merge(
                        alignmentLines.getValue(childLine),
                        linePositionInContainer
                    )
                } else {
                    linePositionInContainer
                }
            }
        }
        alignmentLines += layoutNode.providedAlignmentLines
        if (previousAlignmentLines != alignmentLines) {
            layoutNode.onAlignmentsChanged()
        }
    }

    /**
     * Returns the alignment line value for a given alignment line without affecting whether
     * the flag for whether the alignment line was read.
     */
    private fun getAlignmentLine(alignmentLine: AlignmentLine): Int? {
        val linePos = alignmentLines[alignmentLine] ?: return null
        var pos = Offset(linePos.toFloat(), linePos.toFloat())
        var wrapper = layoutNode.innerLayoutNodeWrapper
        while (wrapper != layoutNode.outerLayoutNodeWrapper) {
            pos = wrapper.toParentPosition(pos)
            wrapper = wrapper.wrappedBy!!
        }
        pos = wrapper.toParentPosition(pos)
        return if (alignmentLine is HorizontalAlignmentLine) {
            pos.y.roundToInt()
        } else {
            pos.x.roundToInt()
        }
    }
}
