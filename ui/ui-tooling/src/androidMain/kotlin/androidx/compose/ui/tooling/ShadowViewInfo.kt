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

package androidx.compose.ui.tooling

import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.tooling.data.UiToolingDataApi

/**
 * Version of [ViewInfo] that allows mutations. This is used to be able to re-attach different
 * composition roots to the right tree.
 */
@OptIn(UiToolingDataApi::class)
private class ShadowViewInfo private constructor(
    var parent: ShadowViewInfo?,
    private val viewInfo: ViewInfo
) {

    /** Constructor for root ShadowViewInfo nodes */
    constructor(viewInfo: ViewInfo) : this(null, viewInfo)

    private val _children: MutableList<ShadowViewInfo> =
        viewInfo.children.map { ShadowViewInfo(this, it) }.toMutableList()

    val children: List<ShadowViewInfo>
        get() = _children

    val layoutInfo: LayoutInfo?
        get() = viewInfo.layoutInfo as? LayoutInfo

    val allNodes: Sequence<ShadowViewInfo> = sequence {
        yield(this@ShadowViewInfo)
        children.flatMap { it.allNodes }.forEach { yield(it) }
    }

    fun setNewParent(parent: ShadowViewInfo) {
        this.parent?._children?.remove(this)
        parent._children.add(this)
        this.parent = parent
    }

    fun findRoot(): ShadowViewInfo =
        if (this.parent == null)
            this
        else
            this.parent!!.findRoot()

    fun toViewInfo(): ViewInfo = ViewInfo(
        viewInfo.fileName,
        viewInfo.lineNumber,
        viewInfo.bounds,
        viewInfo.location,
        _children.map { it.toViewInfo() },
        viewInfo.layoutInfo
    )
}

/**
 * Takes a number of composition roots and stitches them using the [LayoutInfo] information
 * available. Ideally, if all composition roots are related, this method will return a list
 * containing a list with a single element that will have all the input roots attached.
 */
internal fun stitchTrees(allViewInfoRoots: List<ViewInfo>): List<ViewInfo> {
    if (allViewInfoRoots.size < 2) return allViewInfoRoots

    // Convert trees info shadow mutable trees
    val shadowTreeRoots = allViewInfoRoots.map { ShadowViewInfo(it) }

    // Create an index of all the nodes indexed by their layoutInfo so we can quickly lookup
    // the ShadowNode based on its LayoutInfo
    val shadowNodesWithLayoutInfo = shadowTreeRoots
        .flatMap { it.allNodes }
        .map { it.layoutInfo to it }
        .filter { it.first != null }
        .groupBy { it.first }

    val currentRoots = LinkedHashSet(shadowTreeRoots)

    // Now, for each root, see if it can be attached to any other tree.
    shadowTreeRoots
        .forEach { rootToAttach ->
            rootToAttach
                // For the root we are trying to find, if it belongs somewhere else, get all
                // nodes and see if any has a LayoutInfo parent information that matches the
                // LayoutInfo in a separate tree.
                .allNodes
                .flatMap { candidate ->
                    shadowNodesWithLayoutInfo[candidate.layoutInfo?.parentInfo] ?: emptyList()
                }
                .filter {
                    // Ensure that the node we have found is in a different root
                    it.second.findRoot() != rootToAttach
                }
                .map { (_, candidateNode) -> candidateNode }
                .firstOrNull()?.let { nodeToAttachTo ->
                    // We found it, re-attach to the candidate node
                    rootToAttach.setNewParent(nodeToAttachTo)
                    currentRoots.remove(rootToAttach)
                }
        }

    val newTree = currentRoots.map {
        it.toViewInfo()
    }

    return newTree
}