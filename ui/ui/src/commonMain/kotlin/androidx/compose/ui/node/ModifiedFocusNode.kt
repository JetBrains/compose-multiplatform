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

package androidx.compose.ui.node

import androidx.compose.ui.focus.FocusModifier
import androidx.compose.ui.focus.FocusOrder
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.FocusStateImpl
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Disabled
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.focus.findFocusableChildren
import androidx.compose.ui.focus.searchChildrenForFocusNode
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.util.fastForEach

internal class ModifiedFocusNode(
    wrapped: LayoutNodeWrapper,
    modifier: FocusModifier
) : DelegatingLayoutNodeWrapper<FocusModifier>(wrapped, modifier) {

    init {
        modifier.focusNode = this
    }

    var focusState: FocusStateImpl
        get() = modifier.focusState
        set(value) {
            modifier.focusState = value
            sendOnFocusEvent(value)
        }

    var focusedChild: ModifiedFocusNode?
        get() = modifier.focusedChild
        set(value) {
            modifier.focusedChild = value
        }

    // TODO(b/175900268): Add API to allow a parent to extends the bounds of the focus Modifier.
    //  For now we just use the bounds of this node.
    fun focusRect(): Rect = boundsInRoot()

    // TODO(b/152051577): Measure the performance of focusableChildren.
    //  Consider caching the children.
    fun focusableChildren(): List<ModifiedFocusNode> {
        // Check the modifier chain that this focus node is part of. If it has a focus modifier,
        // that means you have found the only focusable child for this node.
        val focusableChild = wrapped.findNextFocusWrapper()
        // findChildFocusNodeInWrapperChain()
        if (focusableChild != null) {
            return listOf(focusableChild)
        }

        // Go through all your children and find the first focusable node from each child.
        val focusableChildren = mutableListOf<ModifiedFocusNode>()
        layoutNode.children.fastForEach { it.findFocusableChildren(focusableChildren) }
        return focusableChildren
    }

    fun sendOnFocusEvent(focusState: FocusState) {
        wrappedBy?.propagateFocusEvent(focusState)
    }

    override fun onModifierChanged() {
        super.onModifierChanged()
        sendOnFocusEvent(focusState)
    }

    override fun attach() {
        super.attach()
        sendOnFocusEvent(focusState)
    }

    override fun detach() {
        when (focusState) {
            // If this node is focused, set the focus on the root layoutNode before removing it.
            Active, Captured -> {
                layoutNode.owner?.focusManager?.clearFocus(force = true)
            }
            // Propagate the state of the next focus node to any focus observers in the hierarchy.
            ActiveParent -> {
                // Find the next focus node.
                val nextFocusNode = wrapped.findNextFocusWrapper()
                    ?: layoutNode.searchChildrenForFocusNode()
                if (nextFocusNode != null) {
                    findParentFocusNode()?.modifier?.focusedChild = nextFocusNode
                    sendOnFocusEvent(nextFocusNode.focusState)
                } else {
                    sendOnFocusEvent(Inactive)
                }
            }
            // TODO(b/155212782): Implement this after adding support for disabling focus modifiers.
            Disabled -> {}
            // Do nothing, as the nextFocusNode is also Inactive.
            Inactive -> {}
        }

        super.detach()
    }

    override fun findPreviousFocusWrapper() = this

    override fun findNextFocusWrapper() = this

    override fun propagateFocusEvent(focusState: FocusState) {
        // Do nothing. Stop propagating the focus change (since we hit another focus node).
    }

    override fun populateFocusOrder(focusOrder: FocusOrder) {
        // Do nothing. Stop propagating the fetchFocusOrder (since we hit another focus node).
    }
}
