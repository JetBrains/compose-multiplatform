/*
 * Copyright 2023 The Android Open Source Project
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

/**
 * Implement this interface create a modifier node that can be used to modify the focus properties
 * of the associated [FocusTargetModifierNode].
 */
@ExperimentalComposeUiApi
interface FocusPropertiesModifierNode : DelegatableNode {
    /**
     * A parent can modify the focus properties associated with the nearest
     * [FocusTargetModifierNode] child node. If a [FocusTargetModifierNode] has multiple parent
     * [FocusPropertiesModifierNode]s, properties set by a parent higher up in the hierarchy
     * overwrite properties set by those that are lower in the hierarchy.
     */
    fun modifyFocusProperties(focusProperties: FocusProperties)
}
