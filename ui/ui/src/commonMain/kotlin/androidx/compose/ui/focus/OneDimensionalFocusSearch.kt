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

import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusStateImpl.Deactivated
import androidx.compose.ui.focus.FocusStateImpl.DeactivatedParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.ModifiedFocusNode
import androidx.compose.ui.util.fastForEach
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

private const val InvalidFocusDirection = "This function should only be used for 1-D focus search"
private const val NoActiveChild = "ActiveParent must have a focusedChild"

internal fun ModifiedFocusNode.oneDimensionalFocusSearch(
    direction: FocusDirection
): ModifiedFocusNode? = when (direction) {
    Next -> forwardFocusSearch()
    Previous -> backwardFocusSearch()
    else -> error(InvalidFocusDirection)
}

private fun ModifiedFocusNode.forwardFocusSearch(): ModifiedFocusNode? {
    when (focusState) {
        ActiveParent, DeactivatedParent -> {
            val focusedChild = focusedChild ?: error(NoActiveChild)
            focusedChild.forwardFocusSearch()?.let { return it }

            // TODO(b/192681045): Instead of fetching the children and then iterating on them, add a
            //  forEachFocusableChild() function that does not allocate a list.
            focusableChildren(excludeDeactivated = false).forEachItemAfter(focusedChild) { child ->
                child.forwardFocusSearch()?.let { return it }
            }
            return null
        }
        Active, Captured, Deactivated -> {
            focusableChildren(excludeDeactivated = false).fastForEach { focusableChild ->
                focusableChild.forwardFocusSearch()?.let { return it }
            }
            return null
        }
        Inactive -> return this
    }
}

private fun ModifiedFocusNode.backwardFocusSearch(): ModifiedFocusNode? {
    when (focusState) {
        ActiveParent -> {
            val focusedChild = focusedChild ?: error(NoActiveChild)
            when (focusedChild.focusState) {
                ActiveParent -> return focusedChild.backwardFocusSearch() ?: focusedChild
                DeactivatedParent -> {
                    focusedChild.backwardFocusSearch()?.let { return it }
                    focusableChildren(excludeDeactivated = false).forEachItemBefore(focusedChild) {
                        it.backwardFocusSearch()?.let { return it }
                    }
                    // backward search returns the parent unless it is the root
                    // (We don't want to move focus to the root).
                    return if (isRoot()) null else this
                }
                Active, Captured -> {
                    focusableChildren(excludeDeactivated = false).forEachItemBefore(focusedChild) {
                        it.backwardFocusSearch()?.let { return it }
                    }
                    // backward search returns the parent unless it is the root
                    // (We don't want to move focus to the root).
                    return if (isRoot()) null else this
                }
                Deactivated, Inactive -> error(NoActiveChild)
            }
        }
        DeactivatedParent -> {
            val focusedChild = focusedChild ?: error(NoActiveChild)
            when (focusedChild.focusState) {
                ActiveParent -> return focusedChild.backwardFocusSearch() ?: focusedChild
                DeactivatedParent -> {
                    focusedChild.backwardFocusSearch()?.let { return it }
                    focusableChildren(excludeDeactivated = false).forEachItemBefore(focusedChild) {
                        it.backwardFocusSearch()?.let { return it }
                    }
                    return null
                }
                Active, Captured -> {
                    focusableChildren(excludeDeactivated = false).forEachItemBefore(focusedChild) {
                        it.backwardFocusSearch()?.let { return it }
                    }
                    return null
                }
                Deactivated, Inactive -> error(NoActiveChild)
            }
        }
        // BackwardFocusSearch Searches among siblings of the ActiveParent for a child that is
        // focused. So this function should never be called when this node is focused. If we
        // reached here, it indicates that this is an initial focus state, so we run the same logic
        // as if this node was Inactive. If we can't find an item for initial focus, we return this
        // root node as the result.
        Active, Captured, Inactive ->
            return focusableChildren(excludeDeactivated = true)
                .lastOrNull()?.backwardFocusSearch() ?: this
        // BackwardFocusSearch Searches among siblings of the ActiveParent for a child that is
        // focused. The search excludes deactivated items, so this function should never be called
        // on a node with a Deactivated state. If we reached here, it indicates that this is an
        // initial focus state, so we run the same logic as if this node was Inactive. If we can't
        // find an item for initial focus, we return null.
        Deactivated ->
            return focusableChildren(excludeDeactivated = true).lastOrNull()?.backwardFocusSearch()
    }
}

private fun ModifiedFocusNode.isRoot() = findParentFocusNode() == null

@OptIn(ExperimentalContracts::class)
private inline fun <T> List<T>.forEachItemAfter(item: T, action: (T) -> Unit) {
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
private inline fun <T> List<T>.forEachItemBefore(item: T, action: (T) -> Unit) {
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
