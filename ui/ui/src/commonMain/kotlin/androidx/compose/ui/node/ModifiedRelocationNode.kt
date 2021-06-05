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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.RelocationModifier
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalComposeUiApi
internal class ModifiedRelocationNode(
    wrapped: LayoutNodeWrapper,
    modifier: RelocationModifier
) : DelegatingLayoutNodeWrapper<RelocationModifier>(wrapped, modifier) {

    override suspend fun propagateRelocationRequest(rect: Rect) {
        // Compute the position of the item after it is brought within this node's bounding box.
        val destinationRect = modifier.computeDestination(rect, this)

        // Perform the relocation for this node, and send a relocation request to its parents.
        //
        // Note: For now we run both of these in parallel, but in the future we could make this
        // configurable. (The child relocation could be executed before the parent, or parent before
        // the child).
        withContext(currentCoroutineContext()) {
            launch { modifier.performRelocation(rect, destinationRect) }
            launch { super.propagateRelocationRequest(destinationRect) }
        }
    }
}
