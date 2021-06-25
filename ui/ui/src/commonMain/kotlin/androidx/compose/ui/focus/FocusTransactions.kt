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
import androidx.compose.ui.focus.FocusStateImpl.Disabled
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.ModifiedFocusNode

/**
 * Request focus for this node.
 *
 * @param propagateFocus Whether the focus should be propagated to the node's children.
 *
 * In Compose, the parent [FocusNode][ModifiedFocusNode] controls focus for its focusable
 * children. Calling this function will send a focus request to this
 * [FocusNode][ModifiedFocusNode]'s parent [FocusNode][ModifiedFocusNode].
 */
internal fun ModifiedFocusNode.requestFocus(propagateFocus: Boolean = true) {
    when (focusState) {
        Active, Captured, Disabled -> {
            // There is no change in focus state, but we send a focus event to notify the user
            // that the focus request is completed.
            sendOnFocusEvent(focusState)
        }
        ActiveParent -> {
            val currentFocusedChild = focusedChild
            requireNotNull(currentFocusedChild)

            // We don't need to do anything if [propagateFocus] is true,
            // since this subtree already has focus.
            if (propagateFocus) {
                sendOnFocusEvent(focusState)
                return
            }

            if (currentFocusedChild.clearFocus()) {
                grantFocus(propagateFocus)
                focusedChild = null
            }
        }
        Inactive -> {
            val focusParent = findParentFocusNode()
            if (focusParent == null) {
                if (requestFocusForOwner()) {
                    grantFocus(propagateFocus)
                }
            } else {
                focusParent.requestFocusForChild(this, propagateFocus)
            }
        }
    }
}

/**
 * Deny requests to clear focus.
 *
 * This is used when a component wants to hold onto focus (eg. A phone number field with an
 * invalid number.
 *
 * @return true if the focus was successfully captured. False otherwise.
 */
internal fun ModifiedFocusNode.captureFocus() = when (focusState) {
    Active -> {
        focusState = Captured
        true
    }
    Captured -> true
    else -> false
}

/**
 * When the node is in the [Captured] state, it rejects all requests to clear focus. Calling
 * [freeFocus] puts the node in the [Active] state, where it is no longer preventing other
 * nodes from requesting focus.
 *
 * @return true if the captured focus was released. False Otherwise.
 */
internal fun ModifiedFocusNode.freeFocus() = when (focusState) {
    Captured -> {
        focusState = Active
        true
    }
    Active -> true
    else -> false
}

/**
 * This function clears focus from this node.
 *
 * Note: This function should only be called by a parent [focus node][ModifiedFocusNode] to
 * clear focus from one of its child [focus node][ModifiedFocusNode]s. It does not change the
 * state of the parent.
 */
internal fun ModifiedFocusNode.clearFocus(forcedClear: Boolean = false): Boolean {
    return when (focusState) {
        Active -> {
            focusState = Inactive
            true
        }
        /**
         * If the node is [ActiveParent], we need to clear focus from the [Active] descendant
         * first, before clearing focus of this node.
         */
        ActiveParent -> {
            val currentFocusedChild = focusedChild
            requireNotNull(currentFocusedChild)
            currentFocusedChild.clearFocus(forcedClear).also { success ->
                if (success) {
                    focusState = Inactive
                    focusedChild = null
                }
            }
        }
        /**
         * If the node is [Captured], deny requests to clear focus, except for a forced clear.
         */
        Captured -> {
            if (forcedClear) {
                focusState = Inactive
            }
            forcedClear
        }
        /**
         * Nothing to do if the node is not focused.
         */
        Inactive, Disabled -> true
    }
}

/**
 * This function grants focus to this node.
 *
 * @param propagateFocus Whether the focus should be propagated to the node's children.
 *
 * Note: This function is private, and should only be called by a parent [ModifiedFocusNode] to
 * grant focus to one of its child [ModifiedFocusNode]s.
 */
private fun ModifiedFocusNode.grantFocus(propagateFocus: Boolean) {

    // TODO (b/144126570) use ChildFocusability.
    //  For now we assume children get focus before parent).

    // TODO (b/144126759): Design a system to decide which child gets focus.
    //  for now we grant focus to the first child.
    val focusedCandidate = focusableChildren().firstOrNull()

    if (focusedCandidate == null || !propagateFocus) {
        // No Focused Children, or we don't want to propagate focus to children.
        focusState = Active
    } else {
        focusState = ActiveParent
        focusedChild = focusedCandidate
        focusedCandidate.grantFocus(propagateFocus)
    }
}

/**
 * Focusable children of this [focus node][ModifiedFocusNode] can use this function to request
 * focus.
 *
 * @param childNode: The node that is requesting focus.
 * @param propagateFocus Whether the focus should be propagated to the node's children.
 * @return true if focus was granted, false otherwise.
 */
private fun ModifiedFocusNode.requestFocusForChild(
    childNode: ModifiedFocusNode,
    propagateFocus: Boolean
): Boolean {

    // Only this node's children can ask for focus.
    if (!focusableChildren().contains(childNode)) {
        error("Non child node cannot request focus.")
    }

    return when (focusState) {
        /**
         * If this node is [Active], it can give focus to the requesting child.
         */
        Active -> {
            focusState = ActiveParent
            focusedChild = childNode
            childNode.grantFocus(propagateFocus)
            true
        }
        /**
         * If this node is [ActiveParent] ie, one of the parent's descendants is [Active],
         * remove focus from the currently focused child and grant it to the requesting child.
         */
        ActiveParent -> {
            val previouslyFocusedNode = focusedChild
            requireNotNull(previouslyFocusedNode)
            if (previouslyFocusedNode.clearFocus()) {
                focusedChild = childNode
                childNode.grantFocus(propagateFocus)
                true
            } else {
                // Currently focused component does not want to give up focus.
                false
            }
        }
        /**
         * If this node is not [Active], we must gain focus first before granting it
         * to the requesting child.
         */
        Inactive -> {
            val focusParent = findParentFocusNode()
            if (focusParent == null) {
                // If the owner successfully gains focus, proceed otherwise return false.
                if (requestFocusForOwner()) {
                    focusState = Active
                    requestFocusForChild(childNode, propagateFocus)
                } else {
                    false
                }
            } else if (focusParent.requestFocusForChild(this, propagateFocus = false)) {
                requestFocusForChild(childNode, propagateFocus)
            } else {
                // Could not gain focus, so have no focus to give.
                false
            }
        }
        /**
         * If this node is [Captured], decline requests from the children.
         */
        Captured -> false
        /**
         * Children of a [Disabled] parent should also be [Disabled].
         */
        Disabled -> error("non root FocusNode needs a focusable parent")
    }
}

private fun ModifiedFocusNode.requestFocusForOwner(): Boolean {
    val owner = layoutNode.owner
    requireNotNull(owner, { "Owner not initialized." })
    return owner.requestFocus()
}
