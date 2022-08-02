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
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Deactivated
import androidx.compose.ui.focus.FocusStateImpl.DeactivatedParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeWrapper
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

private const val InvalidFocusDirection = "This function should only be used for 1-D focus search"
private const val NoActiveChild = "ActiveParent must have a focusedChild"

internal fun FocusModifier.oneDimensionalFocusSearch(
    direction: FocusDirection,
    onFound: (FocusModifier) -> Boolean
): Boolean = when (direction) {
    Next -> forwardFocusSearch(onFound)
    Previous -> backwardFocusSearch(onFound)
    else -> error(InvalidFocusDirection)
}

private fun FocusModifier.forwardFocusSearch(
    onFound: (FocusModifier) -> Boolean
): Boolean = when (focusState) {
    ActiveParent, DeactivatedParent -> {
        val focusedChild = focusedChild ?: error(NoActiveChild)
        focusedChild.forwardFocusSearch(onFound) ||
            generateAndSearchChildren(focusedChild, Next, onFound)
    }
    Active, Captured, Deactivated -> pickChildForForwardSearch(onFound)
    Inactive -> onFound.invoke(this)
}

private fun FocusModifier.backwardFocusSearch(
    onFound: (FocusModifier) -> Boolean
): Boolean = when (focusState) {
    ActiveParent, DeactivatedParent -> {
        val focusedChild = focusedChild ?: error(NoActiveChild)

        // Unlike forwardFocusSearch, backwardFocusSearch visits the children before the parent.
        when (focusedChild.focusState) {
            ActiveParent -> focusedChild.backwardFocusSearch(onFound) ||
                // Don't forget to visit this item after visiting all its children.
                onFound.invoke(focusedChild)

            DeactivatedParent -> focusedChild.backwardFocusSearch(onFound) ||
                // Since this item is deactivated, just skip it and search among its siblings.
                generateAndSearchChildren(focusedChild, Previous, onFound)

            // Since this item "is focused", it means we already visited all its children.
            // So just search among its siblings.
            Active, Captured -> generateAndSearchChildren(focusedChild, Previous, onFound)

            Deactivated, Inactive -> error(NoActiveChild)
        }
    }
    // BackwardFocusSearch is invoked at the root, and so it searches among siblings of the
    // ActiveParent for a child that is focused. If we encounter an active node (instead of an
    // ActiveParent) or a deactivated node (instead of a deactivated parent), it indicates
    // that the hierarchy does not have focus. ie. this is the initial focus state.
    // So we pick one of the children as the result.
    Active, Captured, Deactivated -> pickChildForBackwardSearch(onFound)

    // If we encounter an inactive node, we attempt to pick one of its children before picking
    // this node (backward search visits the children before the parent).
    Inactive -> pickChildForBackwardSearch(onFound) || onFound.invoke(this)
}

// Search among your children for the next child.
// If the next child is not found, generate more children by requesting a beyondBoundsLayout.
private fun FocusModifier.generateAndSearchChildren(
    focusedItem: FocusModifier,
    direction: FocusDirection,
    onFound: (FocusModifier) -> Boolean
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
private fun FocusModifier.searchChildren(
    focusedItem: FocusModifier,
    direction: FocusDirection,
    onFound: (FocusModifier) -> Boolean
): Boolean {
    check(focusState == ActiveParent || focusState == DeactivatedParent) {
        "This function should only be used within a parent that has focus."
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
    if (direction == Next || focusState == DeactivatedParent || isRoot()) return false

    return onFound.invoke(this)
}

private fun FocusModifier.pickChildForForwardSearch(
    onFound: (FocusModifier) -> Boolean
): Boolean {
    children.sortWith(FocusableChildrenComparator)
    return children.any { it.isEligibleForFocusSearch && it.forwardFocusSearch(onFound) }
}

private fun FocusModifier.pickChildForBackwardSearch(
    onFound: (FocusModifier) -> Boolean
): Boolean {
    children.sortWith(FocusableChildrenComparator)
    children.forEachReversed {
        if (it.isEligibleForFocusSearch && it.backwardFocusSearch(onFound)) {
            return true
        }
    }
    return false
}

private fun FocusModifier.isRoot() = parent == null

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
private object FocusableChildrenComparator : Comparator<FocusModifier> {
    override fun compare(focusModifier1: FocusModifier?, focusModifier2: FocusModifier?): Int {
        requireNotNull(focusModifier1)
        requireNotNull(focusModifier2)
        if (focusModifier1 === focusModifier2) return 0

        // Ignore non-attached focus modifiers as they won't be considered during focus search.
        val wrapper1 = focusModifier1.layoutNodeWrapper ?: return 0
        val wrapper2 = focusModifier2.layoutNodeWrapper ?: return 0

        // Compare the place order of the children of the least common ancestor.
        val pathFromRoot1 = pathFromRoot(wrapper1)
        val pathFromRoot2 = pathFromRoot(wrapper2)
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

    private fun pathFromRoot(layoutNodeWrapper: LayoutNodeWrapper): MutableVector<LayoutNode> {
        val path = mutableVectorOf<LayoutNode>()
        var current: LayoutNode? = layoutNodeWrapper.layoutNode
        while (current != null) {
            path.add(0, current)
            current = current.parent
        }
        return path
    }
}
