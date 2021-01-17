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

package androidx.compose.ui.inspection.proto
import androidx.compose.ui.tooling.inspector.InspectorNode
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Bounds
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Quad
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Rect

fun InspectorNode.toComposableNode(stringTable: StringTable): ComposableNode {
    return toComposableNodeImpl(stringTable).build()
}

private fun InspectorNode.toComposableNodeImpl(stringTable: StringTable): ComposableNode.Builder {
    val inspectorNode = this
    return ComposableNode.newBuilder().apply {
        id = inspectorNode.id

        packageHash = inspectorNode.packageHash
        filename = stringTable.put(inspectorNode.fileName)
        lineNumber = inspectorNode.lineNumber
        offset = inspectorNode.offset

        name = stringTable.put(inspectorNode.name)

        bounds = Bounds.newBuilder().apply {
            layout = Rect.newBuilder().apply {
                x = inspectorNode.left
                y = inspectorNode.top
                w = inspectorNode.width
                h = inspectorNode.height
            }.build()
            if (inspectorNode.bounds.size == 8) {
                // Note: Inspector bounds are clockwise order (TL, TR, BR, BL) but Studio expects
                // (TL, TR, BL, BR)
                render = Quad.newBuilder().apply {
                    x0 = inspectorNode.bounds[0]
                    y0 = inspectorNode.bounds[1]
                    x1 = inspectorNode.bounds[2]
                    y1 = inspectorNode.bounds[3]
                    x2 = inspectorNode.bounds[6]
                    y2 = inspectorNode.bounds[7]
                    x3 = inspectorNode.bounds[4]
                    y3 = inspectorNode.bounds[5]
                }.build()
            }
        }.build()

        children.forEach { child -> addChildren(child.toComposableNodeImpl(stringTable)) }
    }
}

fun Iterable<InspectorNode>.toComposableNodes(stringTable: StringTable): List<ComposableNode> {
    return this.map { it.toComposableNode(stringTable) }
}
