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

import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.OnGloballyPositionedModifier

/**
 * Wrapper around the [OnGloballyPositionedModifier].
 */
internal class OnGloballyPositionedModifierWrapper(
    wrapped: LayoutNodeWrapper,
    modifier: OnGloballyPositionedModifier
) : DelegatingLayoutNodeWrapper<OnGloballyPositionedModifier>(wrapped, modifier) {
    override val providedAlignmentLines: Set<AlignmentLine>
        get() {
            val result = mutableSetOf<AlignmentLine>()
            layoutNode
            var wrapper = wrapped as LayoutNodeWrapper?
            while (wrapper != null) {
                result += wrapper.providedAlignmentLines
                if (wrapper == layoutNode.innerLayoutNodeWrapper) break
                wrapper = wrapper.wrapped
            }
            return result
        }
}
