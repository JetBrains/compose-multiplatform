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

package androidx.compose.ui.inspection.inspector

import androidx.compose.ui.layout.LayoutInfo

internal const val UNDEFINED_ID = 0L

/**
 * Node representing a Composable for the Layout Inspector.
 */
class InspectorNode internal constructor(
    /**
     * The associated render node id or 0.
     */
    val id: Long,

    /**
     * The name of the Composable.
     */
    val name: String,

    /**
     * The fileName where the Composable was called.
     */
    val fileName: String,

    /**
     * A hash of the package name to help disambiguate duplicate [fileName] values.
     *
     * This hash is calculated by,
     *
     *   `packageName.fold(0) { hash, current -> hash * 31 + current.toInt() }?.absoluteValue`
     *
     * where the package name is the dotted name of the package. This can be used to disambiguate
     * which file is referenced by [fileName]. This number is -1 if there was no package hash
     * information generated such as when the file does not contain a package declaration.
     */
    val packageHash: Int,

    /**
     * The line number where the Composable was called.
     */
    val lineNumber: Int,

    /**
     * The UTF-16 offset in the file where the Composable was called
     */
    val offset: Int,

    /**
     * The number of UTF-16 code point comprise the Composable call
     */
    val length: Int,

    /**
     * Left side of the Composable in pixels.
     */
    val left: Int,

    /**
     * Top of the Composable in pixels.
     */
    val top: Int,

    /**
     * Width of the Composable in pixels.
     */
    val width: Int,

    /**
     * Width of the Composable in pixels.
     */
    val height: Int,

    /**
     * The 4 corners of the polygon after transformations of the original rectangle.
     */
    val bounds: QuadBounds? = null,

    /**
     * The parameters of this Composable.
     */
    val parameters: List<RawParameter>,

    /**
     * The id of a android View embedded under this node.
     */
    val viewId: Long,

    /**
     * The merged semantics information of this Composable.
     */
    val mergedSemantics: List<RawParameter>,

    /**
     * The un-merged semantics information of this Composable.
     */
    val unmergedSemantics: List<RawParameter>,

    /**
     * The children nodes of this Composable.
     */
    val children: List<InspectorNode>
) {
    fun parametersByKind(kind: ParameterKind): List<RawParameter> = when (kind) {
        ParameterKind.Normal -> parameters
        ParameterKind.MergedSemantics -> mergedSemantics
        ParameterKind.UnmergedSemantics -> unmergedSemantics
    }
}

data class QuadBounds(
    val x0: Int,
    val y0: Int,
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val x3: Int,
    val y3: Int,
)

/**
 * Parameter definition with a raw value reference.
 */
class RawParameter(val name: String, val value: Any?)

/**
 * Mutable version of [InspectorNode].
 */
internal class MutableInspectorNode {
    var id = UNDEFINED_ID
    val layoutNodes = mutableListOf<LayoutInfo>()
    val mergedSemantics = mutableListOf<RawParameter>()
    val unmergedSemantics = mutableListOf<RawParameter>()
    var name = ""
    var fileName = ""
    var packageHash = -1
    var lineNumber = 0
    var offset = 0
    var length = 0
    var left = 0
    var top = 0
    var width = 0
    var height = 0
    var bounds: QuadBounds? = null
    val parameters = mutableListOf<RawParameter>()
    var viewId = UNDEFINED_ID
    val children = mutableListOf<InspectorNode>()

    fun reset() {
        markUnwanted()
        id = UNDEFINED_ID
        viewId = UNDEFINED_ID
        left = 0
        top = 0
        width = 0
        height = 0
        layoutNodes.clear()
        mergedSemantics.clear()
        unmergedSemantics.clear()
        bounds = null
        children.clear()
    }

    fun markUnwanted() {
        name = ""
        fileName = ""
        packageHash = -1
        lineNumber = 0
        offset = 0
        length = 0
        parameters.clear()
    }

    fun shallowCopy(node: InspectorNode): MutableInspectorNode = apply {
        id = node.id
        viewId = node.viewId
        name = node.name
        fileName = node.fileName
        packageHash = node.packageHash
        lineNumber = node.lineNumber
        offset = node.offset
        length = node.length
        left = node.left
        top = node.top
        width = node.width
        height = node.height
        bounds = node.bounds
        mergedSemantics.addAll(node.mergedSemantics)
        unmergedSemantics.addAll(node.unmergedSemantics)
        parameters.addAll(node.parameters)
        children.addAll(node.children)
    }

    fun build(withSemantics: Boolean = true): InspectorNode =
        InspectorNode(
            id, name, fileName, packageHash, lineNumber, offset, length, left, top, width, height,
            bounds, parameters.toList(), viewId,
            if (withSemantics) mergedSemantics.toList() else emptyList(),
            if (withSemantics) unmergedSemantics.toList() else emptyList(),
            children.toList()
        )
}
