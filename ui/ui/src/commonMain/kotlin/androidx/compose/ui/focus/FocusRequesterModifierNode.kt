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

package androidx.compose.ui.focus

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.visitChildren

/**
 * Implement this interface to create a modifier node that can be used to request changes in
 * the focus state of a [FocusTargetModifierNode] down the hierarchy.
 */
@ExperimentalComposeUiApi
interface FocusRequesterModifierNode : DelegatableNode

/**
 * Use this function to request focus. If the system grants focus to a component associated
 * with this [FocusRequester], its [onFocusChanged] modifiers will receive a [FocusState] object
 * where [FocusState.isFocused] is true.
 *
 * @sample androidx.compose.ui.samples.RequestFocusSample
 */
@ExperimentalComposeUiApi
fun FocusRequesterModifierNode.requestFocus(): Boolean {
    visitChildren(Nodes.FocusTarget) {
        if (it.requestFocus()) return true
    }
    return false
}

/**
 * Deny requests to clear focus.
 *
 * Use this function to send a request to capture focus. If a component captures focus,
 * it will send a [FocusState] object to its associated [onFocusChanged]
 * modifiers where [FocusState.isCaptured]() == true.
 *
 * When a component is in a Captured state, all focus requests from other components are
 * declined.
 *
 * @return true if the focus was successfully captured by one of the
 * [focus][focusTarget] modifiers associated with this [FocusRequester]. False otherwise.
 *
 * @sample androidx.compose.ui.samples.CaptureFocusSample
 */
@ExperimentalComposeUiApi
fun FocusRequesterModifierNode.captureFocus(): Boolean {
    visitChildren(Nodes.FocusTarget) {
        if (it.captureFocus()) {
            return true
        }
    }
    return false
}

/**
 * Use this function to send a request to free focus when one of the components associated
 * with this [FocusRequester] is in a Captured state. If a component frees focus,
 * it will send a [FocusState] object to its associated [onFocusChanged]
 * modifiers where [FocusState.isCaptured]() == false.
 *
 * When a component is in a Captured state, all focus requests from other components are
 * declined.
 *.
 * @return true if the captured focus was successfully released. i.e. At the end of this
 * operation, one of the components associated with this [focusRequester] freed focus.
 *
 * @sample androidx.compose.ui.samples.CaptureFocusSample
 */
@ExperimentalComposeUiApi
fun FocusRequesterModifierNode.freeFocus(): Boolean {
    visitChildren(Nodes.FocusTarget) {
        if (it.freeFocus()) return true
    }
    return false
}
