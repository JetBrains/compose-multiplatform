/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.ui.input.pointer.PointerInputModifier

internal class PointerInputEntity(
    layoutNodeWrapper: LayoutNodeWrapper,
    modifier: PointerInputModifier
) : LayoutNodeEntity<PointerInputEntity, PointerInputModifier>(layoutNodeWrapper, modifier) {

    override fun onAttach() {
        super.onAttach()
        modifier.pointerInputFilter.layoutCoordinates = layoutNodeWrapper
        modifier.pointerInputFilter.isAttached = true
    }

    override fun onDetach() {
        super.onDetach()
        modifier.pointerInputFilter.isAttached = false
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun shouldSharePointerInputWithSiblings(): Boolean =
        modifier.pointerInputFilter.shareWithSiblings ||
            next?.shouldSharePointerInputWithSiblings() ?: false
}
