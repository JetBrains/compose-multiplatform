/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.semantics

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.util.fastForEach

/**
 * Owns [SemanticsNode] objects and notifies listeners of changes to the
 * semantics tree
 */
@OptIn(ExperimentalComposeUiApi::class)
class SemanticsOwner internal constructor(private val rootNode: LayoutNode) {
    /**
     * The root node of the semantics tree.  Does not contain any unmerged data.
     * May contain merged data.
     */
    val rootSemanticsNode: SemanticsNode
        get() {
            return SemanticsNode(rootNode.outerSemantics!!, mergingEnabled = true)
        }

    val unmergedRootSemanticsNode: SemanticsNode
        get() {
            return SemanticsNode(rootNode.outerSemantics!!, mergingEnabled = false)
        }
}

/**
 * Finds all [SemanticsNode]s in the tree owned by this [SemanticsOwner]. Return the results in a
 * list.
 */
fun SemanticsOwner.getAllSemanticsNodes(mergingEnabled: Boolean): List<SemanticsNode> {
    return getAllSemanticsNodesToMap(useUnmergedTree = !mergingEnabled).values.toList()
}

/**
 * Finds all [SemanticsNode]s in the tree owned by this [SemanticsOwner]. Return the results in a
 * map.
 */
internal fun SemanticsOwner.getAllSemanticsNodesToMap(
    useUnmergedTree: Boolean = false
): Map<Int, SemanticsNode> {
    val nodes = mutableMapOf<Int, SemanticsNode>()

    fun findAllSemanticNodesRecursive(currentNode: SemanticsNode) {
        nodes[currentNode.id] = currentNode
        currentNode.children.fastForEach { child ->
            findAllSemanticNodesRecursive(child)
        }
    }

    val root = if (useUnmergedTree) unmergedRootSemanticsNode else rootSemanticsNode
    findAllSemanticNodesRecursive(root)
    return nodes
}
