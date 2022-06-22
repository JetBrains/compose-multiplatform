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

package androidx.compose.ui.node

import androidx.compose.runtime.collection.mutableVectorOf

/**
 * Tracks the nodes being positioned and dispatches OnPositioned callbacks when we finished
 * the measure/layout pass.
 */
internal class OnPositionedDispatcher {
    private val layoutNodes = mutableVectorOf<LayoutNode>()

    fun onNodePositioned(node: LayoutNode) {
        layoutNodes += node
        node.needsOnPositionedDispatch = true
    }

    fun onRootNodePositioned(rootNode: LayoutNode) {
        layoutNodes.clear()
        layoutNodes += rootNode
        rootNode.needsOnPositionedDispatch = true
    }

    fun dispatch() {
        // sort layoutNodes so that the root is at the end and leaves are at the front
        layoutNodes.sortWith(DepthComparator)
        layoutNodes.forEachReversed { layoutNode ->
            if (layoutNode.needsOnPositionedDispatch) {
                dispatchHierarchy(layoutNode)
            }
        }
        layoutNodes.clear()
    }

    private fun dispatchHierarchy(layoutNode: LayoutNode) {
        layoutNode.dispatchOnPositionedCallbacks()
        layoutNode.needsOnPositionedDispatch = false

        layoutNode.forEachChild { child ->
            dispatchHierarchy(child)
        }
    }

    internal companion object {
        private object DepthComparator : Comparator<LayoutNode> {
            override fun compare(a: LayoutNode, b: LayoutNode): Int {
                val depthDiff = b.depth.compareTo(a.depth)
                if (depthDiff != 0) {
                    return depthDiff
                }
                return a.hashCode().compareTo(b.hashCode())
            }
        }
    }
}
