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
import androidx.compose.ui.focus.FocusStateImpl.Disabled
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.ModifiedFocusNode
import androidx.compose.ui.util.fastForEach

private const val InvalidFocusDirection = "This function should only be used for 1-D focus search"
private const val NoActiveChild = "ActiveParent must have a focusedChild"
private const val NotYetAvailable = "Implement this after adding API to disable a node"

internal fun ModifiedFocusNode.oneDimensionalFocusSearch(
    direction: FocusDirection
): ModifiedFocusNode = when (direction) {
    Next -> forwardFocusSearch() ?: this
    Previous -> backwardFocusSearch()
    else -> error(InvalidFocusDirection)
}

private fun ModifiedFocusNode.forwardFocusSearch(): ModifiedFocusNode? = when (focusState) {
    ActiveParent -> {
        val focusedChild = focusedChild ?: error(NoActiveChild)
        focusedChild.forwardFocusSearch()?.let { return it }

        var currentItemIsAfterFocusedItem = false
        // TODO(b/192681045): Instead of fetching the children and then iterating on them, add a
        //  forEachFocusableChild function that does not allocate a list.
        focusableChildren().fastForEach {
            if (currentItemIsAfterFocusedItem) {
                return it
            }
            if (it == focusedChild) {
                currentItemIsAfterFocusedItem = true
            }
        }
        null // Couldn't find a focusable child after the current focused child.
    }
    Active, Captured -> focusableChildren().firstOrNull()
    Inactive -> this
    Disabled -> TODO(NotYetAvailable)
}

private fun ModifiedFocusNode.backwardFocusSearch(): ModifiedFocusNode = when (focusState) {
    ActiveParent -> {
        val focusedChild = focusedChild ?: error(NoActiveChild)
        when (focusedChild.focusState) {
            ActiveParent -> focusedChild.backwardFocusSearch()
            Active, Captured -> {
                var previousFocusedItem: ModifiedFocusNode? = null
                // TODO(b/192681045): Instead of fetching the children and then iterating on them, add a
                //  forEachFocusableChild() function that does not allocate a list.
                focusableChildren().fastForEach {
                    if (it == focusedChild) {
                        return previousFocusedItem?.backwardFocusSearch() ?: this
                    }
                    previousFocusedItem = it
                }
                error(NoActiveChild)
            }
            else -> error(NoActiveChild)
        }
    }
    // The BackwardFocusSearch Searches among siblings of the ActiveParent for a child that is
    // focused. So this function should never be called when this node is focused. If we reached
    // here, it indicates an initial focus state, so we run the same logic as if this node was
    // Inactive.
    Active, Captured, Inactive -> focusableChildren().lastOrNull()?.backwardFocusSearch() ?: this
    Disabled -> TODO(NotYetAvailable)
}
