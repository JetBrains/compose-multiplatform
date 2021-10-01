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

package androidx.compose.ui.node

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequesterModifier
import androidx.compose.ui.focus.searchChildrenForFocusNode

internal class ModifiedFocusRequesterNode(
    wrapped: LayoutNodeWrapper,
    modifier: FocusRequesterModifier
) : DelegatingLayoutNodeWrapper<FocusRequesterModifier>(wrapped, modifier) {

    private var focusRequester: FocusRequester? = null
        set(value) {
            // Check if this focus requester node is associated with another focusRequester.
            field?.focusRequesterNodes?.remove(this)
            field = value
            field?.focusRequesterNodes?.add(this)
        }

    // Searches for the focus node associated with this focus requester node.
    internal fun findFocusNode(): ModifiedFocusNode? {
        return findNextFocusWrapper(excludeDeactivated = false)
            ?: layoutNode.searchChildrenForFocusNode(excludeDeactivated = false)
    }

    override fun onModifierChanged() {
        super.onModifierChanged()
        focusRequester = modifier.focusRequester
    }

    override fun attach() {
        super.attach()
        focusRequester = modifier.focusRequester
    }

    override fun detach() {
        focusRequester = null
        super.detach()
    }
}