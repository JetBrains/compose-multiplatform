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

package androidx.compose.ui.platform.accessibility

import androidx.compose.ui.fastReduce
import androidx.compose.ui.fastZipWithNext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import kotlin.math.abs

internal fun setCollectionInfo(node: SemanticsNode, info: AccessibilityNodeInfoCompat) {
    // prioritise collection info provided by developer
    val collectionInfo = node.config.getOrNull(SemanticsProperties.CollectionInfo)
    if (collectionInfo != null) {
        info.setCollectionInfo(collectionInfo.toAccessibilityCollectionInfo())
        return
    }

    // if no collection info is provided, we'll check the 'SelectableGroup'
    val groupedChildren = mutableListOf<SemanticsNode>()

    if (node.config.getOrNull(SemanticsProperties.SelectableGroup) != null) {
        node.replacedChildren.fastForEach { childNode ->
            // we assume that Tabs and RadioButtons are not mixed under a single group
            if (childNode.config.contains(SemanticsProperties.Selected)) {
                groupedChildren.add(childNode)
            }
        }
    }

    if (groupedChildren.isNotEmpty()) {
        val isHorizontal = calculateIfHorizontallyStacked(groupedChildren)
        info.setCollectionInfo(
            AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
                if (isHorizontal) 1 else groupedChildren.count(),
                if (isHorizontal) groupedChildren.count() else 1,
                false,
                AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_NONE
            )
        )
    }
}

internal fun setCollectionItemInfo(node: SemanticsNode, info: AccessibilityNodeInfoCompat) {
    // prioritise collection item info provided by developer
    val collectionItemInfo = node.config.getOrNull(SemanticsProperties.CollectionItemInfo)
    if (collectionItemInfo != null) {
        info.setCollectionItemInfo(collectionItemInfo.toAccessibilityCollectionItemInfo(node))
    }

    // if no collection item info is provided, we'll check the 'SelectableGroup'
    val parentNode = node.parent ?: return
    if (parentNode.config.getOrNull(SemanticsProperties.SelectableGroup) != null) {
        // first check if parent has a CollectionInfo. If it does and any of the counters is
        // unknown, then we assume that it is a lazy collection so we won't provide
        // collectionItemInfo using `SelectableGroup`
        val collectionInfo = parentNode.config.getOrNull(SemanticsProperties.CollectionInfo)
        if (collectionInfo != null && collectionInfo.isLazyCollection) return

        // `SelectableGroup` designed for selectable elements
        if (!node.config.contains(SemanticsProperties.Selected)) return

        val groupedChildren = mutableListOf<SemanticsNode>()

        // find all siblings to calculate the index
        parentNode.replacedChildren.fastForEach { childNode ->
            if (childNode.config.contains(SemanticsProperties.Selected)) {
                groupedChildren.add(childNode)
            }
        }

        if (groupedChildren.isNotEmpty()) {
            val isHorizontal = calculateIfHorizontallyStacked(groupedChildren)

            groupedChildren.fastForEachIndexed { index, tabNode ->
                if (tabNode.id == node.id) {
                    val itemInfo = AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                        if (isHorizontal) 0 else index,
                        1,
                        if (isHorizontal) index else 0,
                        1,
                        false,
                        tabNode.config.getOrElse(SemanticsProperties.Selected) { false }
                    )
                    if (itemInfo != null) {
                        info.setCollectionItemInfo(itemInfo)
                    }
                }
            }
        }
    }
}

/** A naïve algorithm to determine if elements are stacked vertically or horizontally */
private fun calculateIfHorizontallyStacked(items: List<SemanticsNode>): Boolean {
    if (items.count() < 2) return true

    val deltas = items.fastZipWithNext { el1, el2 ->
        Offset(
            abs(el1.boundsInRoot.center.x - el2.boundsInRoot.center.x),
            abs(el1.boundsInRoot.center.y - el2.boundsInRoot.center.y)
        )
    }
    val (deltaX, deltaY) = when (deltas.count()) {
        1 -> deltas.first()
        else -> deltas.fastReduce { result, element -> result + element }
    }
    return deltaY < deltaX
}

private val CollectionInfo.isLazyCollection get() = rowCount < 0 || columnCount < 0

private fun CollectionInfo.toAccessibilityCollectionInfo() =
    AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
        rowCount,
        columnCount,
        false,
        AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_NONE
    )

private fun CollectionItemInfo.toAccessibilityCollectionItemInfo(itemNode: SemanticsNode) =
    AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
        rowIndex,
        rowSpan,
        columnIndex,
        columnSpan,
        false,
        itemNode.config.getOrElse(SemanticsProperties.Selected) { false }
    )