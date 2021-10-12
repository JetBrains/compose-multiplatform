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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusModifier
import androidx.compose.ui.focus.FocusOrder
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.FocusStateImpl
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Deactivated
import androidx.compose.ui.focus.FocusStateImpl.DeactivatedParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.focus.searchChildrenForFocusNode
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.findRoot

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
    fun focusRect(): Rect = findRoot().localBoundingBoxOf(this, clipBounds = false)

    fun sendOnFocusEvent(focusState: FocusState) {
        if (isAttached) {
            wrappedBy?.propagateFocusEvent(focusState)
        }
    }

    override fun onModifierChanged() {
        super.onModifierChanged()
        sendOnFocusEvent(focusState)
    }

    // TODO(b/202621526) Handle cases where a focus modifier is attached to a node that is focused.
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
            ActiveParent, DeactivatedParent -> {
                val nextFocusNode = wrapped.findNextFocusWrapper(excludeDeactivated = false)
                    ?: layoutNode.searchChildrenForFocusNode(excludeDeactivated = false)
                val parentFocusNode = findParentFocusNode()
                if (parentFocusNode != null) {
                    parentFocusNode.modifier.focusedChild = nextFocusNode
                    if (nextFocusNode != null) {
                        sendOnFocusEvent(nextFocusNode.focusState)
                    } else {
                        parentFocusNode.focusState = when (parentFocusNode.focusState) {
                            ActiveParent -> Inactive
                            DeactivatedParent -> Deactivated
                            else -> parentFocusNode.focusState
                        }
                    }
                }
            }
            Deactivated -> {
                val nextFocusNode = wrapped.findNextFocusWrapper(excludeDeactivated = false)
                    ?: layoutNode.searchChildrenForFocusNode(excludeDeactivated = false)
                sendOnFocusEvent(nextFocusNode?.focusState ?: Inactive)
            }
            // Do nothing, as the nextFocusNode is also Inactive.
            Inactive -> {}
        }
        super.detach()
    }

    override fun findPreviousFocusWrapper() = this

    @OptIn(ExperimentalComposeUiApi::class)
    override fun findNextFocusWrapper(excludeDeactivated: Boolean): ModifiedFocusNode? {
        return if (modifier.focusState.isDeactivated && excludeDeactivated) {
            super.findNextFocusWrapper(excludeDeactivated)
        } else {
            this
        }
    }

    override fun propagateFocusEvent(focusState: FocusState) {
        // Do nothing. Stop propagating the focus change (since we hit another focus node).
    }

    override fun populateFocusOrder(focusOrder: FocusOrder) {
        // Do nothing. Stop propagating the fetchFocusOrder (since we hit another focus node).
    }
}
