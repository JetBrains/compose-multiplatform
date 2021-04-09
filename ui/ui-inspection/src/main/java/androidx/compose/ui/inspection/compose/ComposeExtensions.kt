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

package androidx.compose.ui.inspection.compose

import androidx.compose.ui.inspection.inspector.InspectorNode
import androidx.compose.ui.inspection.inspector.LayoutInspectorTree
import androidx.compose.ui.inspection.inspector.NodeParameter
import androidx.compose.ui.inspection.inspector.ParameterKind
import androidx.compose.ui.inspection.inspector.ParameterKind.MergedSemantics
import androidx.compose.ui.inspection.inspector.ParameterKind.Normal
import androidx.compose.ui.inspection.inspector.ParameterKind.UnmergedSemantics
import androidx.compose.ui.inspection.proto.StringTable
import androidx.compose.ui.inspection.proto.convertAll
import androidx.compose.ui.inspection.util.ThreadUtils
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ParameterGroup

/**
 * Convert parameters and semantics from [InspectorNode] into a [ParameterGroup].
 */
fun InspectorNode.convertToParameterGroup(
    layoutInspectorTree: LayoutInspectorTree,
    rootId: Long,
    maxRecursions: Int,
    maxInitialIterableSize: Int,
    stringTable: StringTable
): ParameterGroup = ParameterGroup.newBuilder().apply {
    composableId = id
    addAllParameter(
        convertParameters(
            layoutInspectorTree, Normal, rootId, maxRecursions, maxInitialIterableSize
        ).convertAll(stringTable)
    )
    addAllMergedSemantics(
        convertParameters(
            layoutInspectorTree, MergedSemantics, rootId, maxRecursions, maxInitialIterableSize
        ).convertAll(stringTable)
    )
    addAllUnmergedSemantics(
        convertParameters(
            layoutInspectorTree, UnmergedSemantics, rootId, maxRecursions, maxInitialIterableSize
        ).convertAll(stringTable)
    )
}.build()

/**
 * Convert [InspectorNode] into [NodeParameter]s.
 *
 * This method can take a long time, especially the first time, and should be called off the main
 * thread.
 */
fun InspectorNode.convertParameters(
    layoutInspectorTree: LayoutInspectorTree,
    kind: ParameterKind,
    rootId: Long,
    maxRecursions: Int,
    maxInitialIterableSize: Int
): List<NodeParameter> {
    ThreadUtils.assertOffMainThread()
    return layoutInspectorTree.convertParameters(
        rootId,
        this,
        kind,
        maxRecursions,
        maxInitialIterableSize
    )
}

/**
 * Flatten an inspector node into a list containing itself and all its children.
 */
fun InspectorNode.flatten(): Sequence<InspectorNode> {
    val remaining = mutableListOf(this)
    return generateSequence {
        val next = remaining.removeLastOrNull()
        if (next != null) {
            remaining.addAll(next.children)
        }
        next
    }
}