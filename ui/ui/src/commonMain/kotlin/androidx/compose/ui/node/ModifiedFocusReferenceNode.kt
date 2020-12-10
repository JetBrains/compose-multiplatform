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

import androidx.compose.ui.focus.FocusReference
import androidx.compose.ui.focus.FocusReferenceModifier
import androidx.compose.ui.focus.searchChildrenForFocusNode

internal class ModifiedFocusReferenceNode(
    wrapped: LayoutNodeWrapper,
    modifier: FocusReferenceModifier
) : DelegatingLayoutNodeWrapper<FocusReferenceModifier>(wrapped, modifier) {

    private var focusReference: FocusReference? = null
        set(value) {
            // Check if this focus requester node is associated with another focusReference.
            field?.focusReferenceNodes?.remove(this)
            field = value
            field?.focusReferenceNodes?.add(this)
        }

    // Searches for the focus node associated with this focus requester node.
    internal fun findFocusNode(): ModifiedFocusNode? {
        return findNextFocusWrapper() ?: layoutNode.searchChildrenForFocusNode()
    }

    override fun onModifierChanged() {
        super.onModifierChanged()
        focusReference = modifier.focusReference
    }

    override fun attach() {
        super.attach()
        focusReference = modifier.focusReference
    }

    override fun detach() {
        focusReference = null
        super.detach()
    }
}