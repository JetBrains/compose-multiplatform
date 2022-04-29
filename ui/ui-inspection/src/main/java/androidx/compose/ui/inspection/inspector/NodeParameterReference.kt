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

package androidx.compose.ui.inspection.inspector

import androidx.compose.ui.inspection.util.asIntArray

/**
 * A reference to a parameter to a [NodeParameter]
 *
 * @param nodeId is the id of the node the parameter belongs to
 * @param anchorHash is the anchor hash of the node the parameter belongs to
 * @param kind is this a reference to a normal, merged, or unmerged semantic parameter.
 * @param parameterIndex index into [InspectorNode.parameters], [InspectorNode.mergedSemantics],
 *        or [InspectorNode.unMergedSemantics]
 * @param indices are indices into the composite parameter
 */
class NodeParameterReference(
    val nodeId: Long,
    val anchorHash: Int,
    val kind: ParameterKind,
    val parameterIndex: Int,
    val indices: IntArray
) {
    constructor (
        nodeId: Long,
        anchorHash: Int,
        kind: ParameterKind,
        parameterIndex: Int,
        indices: List<Int>
    ) : this(nodeId, anchorHash, kind, parameterIndex, indices.asIntArray())

    // For testing:
    override fun toString(): String {
        val suffix = if (indices.isNotEmpty()) ", ${indices.joinToString()}" else ""
        return "[$nodeId, $anchorHash, $kind, $parameterIndex$suffix]"
    }
}

/**
 * Identifies which kind of parameter the [NodeParameterReference] is a reference to.
 */
enum class ParameterKind {
    Normal,
    MergedSemantics,
    UnmergedSemantics
}
