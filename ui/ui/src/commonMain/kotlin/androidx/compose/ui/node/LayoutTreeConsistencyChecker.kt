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

import androidx.compose.ui.util.fastForEach

/**
 * There are some contracts between the tree of LayoutNodes and the state of AndroidComposeView
 * which is hard to enforce but important to maintain. This method is intended to do the
 * work only during our tests and will iterate through the tree to validate the states consistency.
 */
internal class LayoutTreeConsistencyChecker(
    private val root: LayoutNode,
    private val relayoutNodes: DepthSortedSet,
    private val postponedMeasureRequests: List<LayoutNode>
) {
    fun assertConsistent() {
        val inconsistencyFound = !isTreeConsistent(root)
        if (inconsistencyFound) {
            println(logTree())
            throw IllegalStateException("Inconsistency found!")
        }
    }

    private fun isTreeConsistent(node: LayoutNode): Boolean {
        if (!node.consistentLayoutState()) {
            return false
        }
        node.children.fastForEach {
            if (!isTreeConsistent(it)) {
                return@isTreeConsistent false
            }
        }
        return true
    }

    private fun LayoutNode.consistentLayoutState(): Boolean {
        val parent = this.parent
        if (isPlaced ||
            placeOrder != LayoutNode.NotPlacedPlaceOrder && parent?.isPlaced == true
        ) {
            if (measurePending &&
                postponedMeasureRequests.contains(this)
            ) {
                // this node is waiting to be measured by parent or if this will not happen
                // `onRequestMeasure` will be called for all items in `postponedMeasureRequests`
                return true
            }
            // remeasure or relayout is scheduled
            val parentLayoutState = parent?.layoutState
            if (measurePending) {
                return relayoutNodes.contains(this) ||
                    parent?.measurePending == true ||
                    parentLayoutState == LayoutNode.LayoutState.Measuring
            }
            if (layoutPending) {
                return relayoutNodes.contains(this) ||
                    parent?.measurePending == true ||
                    parent?.layoutPending == true ||
                    parentLayoutState == LayoutNode.LayoutState.Measuring ||
                    parentLayoutState == LayoutNode.LayoutState.LayingOut
            }
        }
        return true
    }

    private fun nodeToString(node: LayoutNode): String {
        return with(StringBuilder()) {
            append(node)
            append("[${node.layoutState}]")
            if (!node.isPlaced) append("[!isPlaced]")
            append("[measuredByParent=${node.measuredByParent}]")
            if (!node.consistentLayoutState()) {
                append("[INCONSISTENT]")
            }
            toString()
        }
    }

    /** Prints the nodes tree into the logs. */
    private fun logTree(): String {
        val stringBuilder = StringBuilder()
        fun printSubTree(node: LayoutNode, depth: Int) {
            var childrenDepth = depth
            val nodeRepresentation = nodeToString(node)
            if (nodeRepresentation.isNotEmpty()) {
                for (i in 0 until depth) {
                    stringBuilder.append("..")
                }
                stringBuilder.appendLine(nodeRepresentation)
                childrenDepth += 1
            }
            node.children.fastForEach { printSubTree(it, childrenDepth) }
        }
        stringBuilder.appendLine("Tree state:")
        printSubTree(root, 0)
        return stringBuilder.toString()
    }
}
