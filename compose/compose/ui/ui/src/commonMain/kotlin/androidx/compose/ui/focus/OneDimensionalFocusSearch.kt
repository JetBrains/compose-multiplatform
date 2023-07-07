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

package androidx.compose.ui.focus

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.nearestAncestor
import androidx.compose.ui.node.visitChildren
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

private const val InvalidFocusDirection = "This function should only be used for 1-D focus search"
private const val NoActiveChild = "ActiveParent must have a focusedChild"

@ExperimentalComposeUiApi
internal fun FocusTargetModifierNode.oneDimensionalFocusSearch(
    direction: FocusDirection,
    onFound: (FocusTargetModifierNode) -> Boolean
): Boolean = when (direction) {
    Next -> forwardFocusSearch(onFound)
    Previous -> backwardFocusSearch(onFound)
    else -> error(InvalidFocusDirection)
}

@ExperimentalComposeUiApi
private fun FocusTargetModifierNode.forwardFocusSearch(
    onFound: (FocusTargetModifierNode) -> Boolean
): Boolean = when (focusStateImpl) {
    ActiveParent -> {
        val focusedChild = activeChild ?: error(NoActiveChild)
        focusedChild.forwardFocusSearch(onFound) ||
            generateAndSearchChildren(focusedChild, Next, onFound)
    }
    Active, Captured -> pickChildForForwardSearch(onFound)
    Inactive -> if (fetchFocusProperties().canFocus) {
        onFound.invoke(this)
    } else {
        pickChildForForwardSearch(onFound)
    }
}

@ExperimentalComposeUiApi
private fun FocusTargetModifierNode.backwardFocusSearch(
    onFound: (FocusTargetModifierNode) -> Boolean
): Boolean = when (focusStateImpl) {
    ActiveParent -> {
        val focusedChild = activeChild ?: error(NoActiveChild)

        // Unlike forwardFocusSearch, backwardFocusSearch visits the children before the parent.
        when (focusedChild.focusStateImpl) {
            ActiveParent -> focusedChild.backwardFocusSearch(onFound) ||
                generateAndSearchChildren(focusedChild, Previous, onFound) ||
                (focusedChild.fetchFocusProperties().canFocus && onFound.invoke(focusedChild))

            // Since this item "is focused", it means we already visited all its children.
            // So just search among its siblings.
            Active, Captured -> generateAndSearchChildren(focusedChild, Previous, onFound)

            Inactive -> error(NoActiveChild)
        }
    }
    // BackwardFocusSearch is invoked at the root, and so it searches among siblings of the
    // ActiveParent for a child that is focused. If we encounter an active node (instead of an
    // ActiveParent) or a deactivated node (instead of a deactivated parent), it indicates
    // that the hierarchy does not have focus. ie. this is the initial focus state.
    // So we pick one of the children as the result.
    Active, Captured -> pickChildForBackwardSearch(onFound)

    // If we encounter an inactive node, we attempt to pick one of its children before picking
    // this node (backward search visits the children before the parent).
    Inactive -> pickChildForBackwardSearch(onFound) ||
        if (fetchFocusProperties().canFocus) onFound.invoke(this) else false
}

// Search among your children for the next child.
// If the next child is not found, generate more children by requesting a beyondBoundsLayout.
@ExperimentalComposeUiApi
private fun FocusTargetModifierNode.generateAndSearchChildren(
    focusedItem: FocusTargetModifierNode,
    direction: FocusDirection,
    onFound: (FocusTargetModifierNode) -> Boolean
): Boolean {
    // Search among the currently available children.
    if (searchChildren(focusedItem, direction, onFound)) {
        return true
    }

    // Generate more items until searchChildren() finds a result.
    return searchBeyondBounds(direction) {
        // Search among the added children. (The search continues as long as we return null).
        searchChildren(focusedItem, direction, onFound).takeIf { found ->
            // Stop searching when we find a result or if we don't have any more content.
            found || !hasMoreContent
        }
    } ?: false
}

// Search for the next sibling that should be granted focus.
@ExperimentalComposeUiApi
private fun FocusTargetModifierNode.searchChildren(
    focusedItem: FocusTargetModifierNode,
    direction: FocusDirection,
    onFound: (FocusTargetModifierNode) -> Boolean
): Boolean {
    check(focusStateImpl == ActiveParent) {
        "This function should only be used within a parent that has focus."
    }
    val children = MutableVector<FocusTargetModifierNode>().apply {
        visitChildren(Nodes.FocusTarget) { add(it) }
    }
    children.sortWith(FocusableChildrenComparator)
    when (direction) {
        Next -> children.forEachItemAfter(focusedItem) { child ->
            if (child.isEligibleForFocusSearch && child.forwardFocusSearch(onFound)) return true
        }
        Previous -> children.forEachItemBefore(focusedItem) { child ->
            if (child.isEligibleForFocusSearch && child.backwardFocusSearch(onFound)) return true
        }
        else -> error(InvalidFocusDirection)
    }

    // If all the children have been visited, return null if this is a forward search. If it is a
    // backward search, we want to move focus to the parent unless the parent is deactivated.
    // We also don't want to move focus to the root because from the user's perspective this would
    // look like nothing is focused.
    if (direction == Next || !fetchFocusProperties().canFocus || isRoot()) return false

    return onFound.invoke(this)
}

@ExperimentalComposeUiApi
private fun FocusTargetModifierNode.pickChildForForwardSearch(
    onFound: (FocusTargetModifierNode) -> Boolean
): Boolean {
    val children = MutableVector<FocusTargetModifierNode>().apply {
        visitChildren(Nodes.FocusTarget) { add(it) }
    }
    children.sortWith(FocusableChildrenComparator)
    return children.any { it.isEligibleForFocusSearch && it.forwardFocusSearch(onFound) }
}

@ExperimentalComposeUiApi
private fun FocusTargetModifierNode.pickChildForBackwardSearch(
    onFound: (FocusTargetModifierNode) -> Boolean
): Boolean {
    val children = MutableVector<FocusTargetModifierNode>().apply {
        visitChildren(Nodes.FocusTarget) { add(it) }
    }
    children.sortWith(FocusableChildrenComparator)
    children.forEachReversed {
        if (it.isEligibleForFocusSearch && it.backwardFocusSearch(onFound)) {
            return true
        }
    }
    return false
}

@OptIn(ExperimentalComposeUiApi::class)
private fun FocusTargetModifierNode.isRoot() = nearestAncestor(Nodes.FocusTarget) == null

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
private inline fun <T> MutableVector<T>.forEachItemAfter(item: T, action: (T) -> Unit) {
    contract { callsInPlace(action) }
    var itemFound = false
    for (index in indices) {
        if (itemFound) {
            action(get(index))
        }
        if (get(index) == item) {
            itemFound = true
        }
    }
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
private inline fun <T> MutableVector<T>.forEachItemBefore(item: T, action: (T) -> Unit) {
    contract { callsInPlace(action) }
    var itemFound = false
    for (index in indices.reversed()) {
        if (itemFound) {
            action(get(index))
        }
        if (get(index) == item) {
            itemFound = true
        }
    }
}

/**
 * We use this comparator to sort the focus modifiers in place order.
 *
 * We want to visit the nodes in placement order instead of composition order.
 * This is because components like LazyList reuse nodes without re-composing them, but it always
 * re-places nodes that are reused.
 *
 * Instead of sorting the items, we could just look for the next largest place order index in linear
 * time. However if the next item is deactivated, not eligible for focus search or none of its
 * children are focusable we would have to backtrack and find the item with the next largest place
 * order index. This would be more expensive than sorting the items. In addition to this, sorting
 * the items makes the next focus search more efficient.
 */
@OptIn(ExperimentalComposeUiApi::class)
private object FocusableChildrenComparator : Comparator<FocusTargetModifierNode> {
    override fun compare(
        focusTarget1: FocusTargetModifierNode,
        focusTarget2: FocusTargetModifierNode
    ): Int {
        requireNotNull(focusTarget1)
        requireNotNull(focusTarget2)

        // Ignore focus modifiers that won't be considered during focus search.
        if (!focusTarget1.isEligibleForFocusSearch || !focusTarget2.isEligibleForFocusSearch) {
            if (focusTarget1.isEligibleForFocusSearch) return -1
            if (focusTarget2.isEligibleForFocusSearch) return 1
            return 0
        }

        val layoutNode1 = checkNotNull(focusTarget1.coordinator?.layoutNode)
        val layoutNode2 = checkNotNull(focusTarget2.coordinator?.layoutNode)

        // Use natural order for focus modifiers within the same layout node.
        if (layoutNode1 == layoutNode2) return 0

        // Compare the place order of the children of the least common ancestor.
        val pathFromRoot1 = pathFromRoot(layoutNode1)
        val pathFromRoot2 = pathFromRoot(layoutNode2)
        for (depth in 0..minOf(pathFromRoot1.lastIndex, pathFromRoot2.lastIndex)) {
            // If the items from the two paths are not equal, we have
            // found the first two children after the least common ancestor.
            // We use the place order of these two parents to compare the focus modifiers.
            if (pathFromRoot1[depth] != pathFromRoot2[depth]) {
                return pathFromRoot1[depth].placeOrder.compareTo(pathFromRoot2[depth].placeOrder)
            }
        }
        error("Could not find a common ancestor between the two FocusModifiers.")
    }

    private fun pathFromRoot(layoutNode: LayoutNode): MutableVector<LayoutNode> {
        val path = mutableVectorOf<LayoutNode>()
        var current: LayoutNode? = layoutNode
        while (current != null) {
            path.add(0, current)
            current = current.parent
        }
        return path
    }
}
