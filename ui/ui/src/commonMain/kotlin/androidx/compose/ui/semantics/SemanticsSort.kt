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

package androidx.compose.ui.semantics

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeWrapper
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

// This part is a copy from ViewGroup#addChildrenForAccessibility.
internal fun LayoutNode.findOneLayerOfSemanticsWrappersSortedByBounds(
    list: MutableList<SemanticsEntity> = mutableListOf()
): List<SemanticsEntity> {
    fun sortWithStrategy(holders: List<NodeLocationHolder>): List<NodeLocationHolder> {
        // This is gross but the least risky solution. The current comparison
        // strategy breaks transitivity but produces very good results. Coming
        // up with a new strategy requires time which we do not have, so ...
        return try {
            NodeLocationHolder.comparisonStrategy = NodeLocationHolder.ComparisonStrategy.Stripe
            holders.toMutableList().apply { sort() }
        } catch (iae: IllegalArgumentException) {
            // Note that in practice this occurs extremely rarely in a couple
            // of pathological cases.
            NodeLocationHolder.comparisonStrategy = NodeLocationHolder.ComparisonStrategy.Location
            holders.toMutableList().apply { sort() }
        }
    }

    if (!isAttached) {
        return list
    }
    val holders = ArrayList<NodeLocationHolder>()
    children.fastForEach {
        if (it.isAttached) holders.add(NodeLocationHolder(this, it))
    }
    val sortedChildren = sortWithStrategy(holders).fastMap { it.node }

    sortedChildren.fastForEach { child ->
        val outerSemantics = child.outerSemantics
        if (outerSemantics != null) {
            list.add(outerSemantics)
        } else {
            child.findOneLayerOfSemanticsWrappersSortedByBounds(list)
        }
    }
    return list
}

internal class NodeLocationHolder internal constructor(
    internal val subtreeRoot: LayoutNode,
    internal val node: LayoutNode
) : Comparable<NodeLocationHolder> {
    internal companion object {
        internal var comparisonStrategy = ComparisonStrategy.Stripe
    }

    internal enum class ComparisonStrategy { Stripe, Location }

    private val location: Rect?

    private val layoutDirection = subtreeRoot.layoutDirection

    init {
        val subtreeRootWrapper = subtreeRoot.innerLayoutNodeWrapper
        val nodeWrapper = node.findWrapperToGetBounds()
        location = if (subtreeRootWrapper.isAttached && nodeWrapper.isAttached) {
            subtreeRootWrapper.localBoundingBoxOf(nodeWrapper)
        } else {
            null
        }
    }

    override fun compareTo(other: NodeLocationHolder): Int {
        if (location == null) {
            // put the unattached nodes at last. This probably can save accessibility services time.
            return 1
        }
        if (other.location == null) {
            return -1
        }

        if (comparisonStrategy == ComparisonStrategy.Stripe) {
            // First is above second.
            if (location.bottom - other.location.top <= 0) {
                return -1
            }
            // First is below second.
            if (location.top - other.location.bottom >= 0) {
                return 1
            }
        }

        // We are ordering left-to-right, top-to-bottom.
        if (layoutDirection == LayoutDirection.Ltr) {
            val leftDifference = location.left - other.location.left
            if (leftDifference != 0f) {
                return if (leftDifference < 0) -1 else 1
            }
        } else { // RTL
            val rightDifference = location.right - other.location.right
            if (rightDifference != 0f) {
                return if (rightDifference < 0) 1 else -1
            }
        }
        // We are ordering left-to-right, top-to-bottom.
        val topDifference = location.top - other.location.top
        if (topDifference != 0f) {
            return if (topDifference < 0) -1 else 1
        }
        // Break tie by height.
        val heightDifference = location.height - other.location.height
        if (heightDifference != 0f) {
            return if (heightDifference < 0) 1 else -1
        }
        // Break tie by width.
        val widthDifference = location.width - other.location.width
        if (widthDifference != 0f) {
            return if (widthDifference < 0) 1 else -1
        }

        // Find a child of each view with different screen bounds. If we get here, node and
        // other.node must be attached.
        val view1Bounds = node.findWrapperToGetBounds().boundsInRoot()
        val view2Bounds = other.node.findWrapperToGetBounds().boundsInRoot()
        val child1 = node.findNodeByPredicateTraversal {
            val wrapper = it.findWrapperToGetBounds()
            wrapper.isAttached && view1Bounds != wrapper.boundsInRoot()
        }
        val child2 = other.node.findNodeByPredicateTraversal {
            val wrapper = it.findWrapperToGetBounds()
            wrapper.isAttached && view2Bounds != wrapper.boundsInRoot()
        }
        // Compare the children recursively
        if ((child1 != null) && (child2 != null)) {
            val childHolder1 = NodeLocationHolder(subtreeRoot, child1)
            val childHolder2 = NodeLocationHolder(other.subtreeRoot, child2)
            return childHolder1.compareTo(childHolder2)
        }

        // If only one has a child, use that one
        if (child1 != null) {
            return 1
        }

        if (child2 != null) {
            return -1
        }

        // Break tie somehow
        return -1
    }
}

internal fun LayoutNode.findNodeByPredicateTraversal(
    predicate: (LayoutNode) -> Boolean
): LayoutNode? {
    if (predicate(this)) {
        return this
    }

    children.fastForEach {
        val result = it.findNodeByPredicateTraversal(predicate)
        if (result != null) {
            return result
        }
    }

    return null
}

/**
 * If this node has semantics, we use the semantics wrapper to get bounds. Otherwise, we use
 * innerLayoutNodeWrapper because it seems the bounds after padding is the effective content.
 */
internal fun LayoutNode.findWrapperToGetBounds(): LayoutNodeWrapper {
    return (outerMergingSemantics ?: outerSemantics)?.layoutNodeWrapper ?: innerLayoutNodeWrapper
}
