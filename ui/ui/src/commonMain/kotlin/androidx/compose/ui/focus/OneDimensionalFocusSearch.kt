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
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Deactivated
import androidx.compose.ui.focus.FocusStateImpl.DeactivatedParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
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
        focusedChild.forwardFocusSearch(onFound) || searchChildren(focusedChild, Next, onFound)
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
                searchChildren(focusedChild, Previous, onFound)

            // Since this item "is focused", it means we already visited all its children.
            // So just search among its siblings.
            Active, Captured -> searchChildren(focusedChild, Previous, onFound)

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

// Search for the next sibling that should be granted focus.
private fun FocusModifier.searchChildren(
    focusedItem: FocusModifier,
    direction: FocusDirection,
    onFound: (FocusModifier) -> Boolean
): Boolean {
    check(focusState == ActiveParent || focusState == DeactivatedParent) {
        "This function should only be used within a parent that has focus."
    }

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
): Boolean = children.any { it.forwardFocusSearch(onFound) }

private fun FocusModifier.pickChildForBackwardSearch(
    onFound: (FocusModifier) -> Boolean
): Boolean {
    children.forEachReversed {
        if (it.backwardFocusSearch(onFound)) {
            return true
        }
    }
    return false
}

private fun FocusModifier.isRoot() = parent == null

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
