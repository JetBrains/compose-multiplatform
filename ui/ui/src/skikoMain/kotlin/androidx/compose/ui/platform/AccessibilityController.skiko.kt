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

package androidx.compose.ui.platform

import androidx.compose.ui.node.LayoutNode

internal interface AccessibilityController {
    /**
     * Action to trigger when the semantic tree was changed.
     *
     * @see androidx.compose.ui.node.Owner.onSemanticsChange
     */
    fun onSemanticsChange()

    /**
     * Action to trigger when the position and/or size of the
     * [layoutNode] was changed to update semantic tree.
     *
     * @see androidx.compose.ui.node.Owner.onLayoutChange
     */
    fun onLayoutChange(layoutNode: LayoutNode)

    /**
     * Start a background job to sync semantic nodes with their
     * platform-specific counterparts.
     */
    suspend fun syncLoop()
}