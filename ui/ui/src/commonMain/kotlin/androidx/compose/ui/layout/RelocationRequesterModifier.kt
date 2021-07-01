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

package androidx.compose.ui.layout

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.ModifiedRelocationRequesterNode
import androidx.compose.ui.platform.debugInspectorInfo

// This modifier keeps track of the coordinates when the item is positioned, and when
// bringIntoParentBounds() is called, it asks parents to scroll to bring this item into view.
internal class RelocationRequesterModifier : Modifier.Element {

    lateinit var relocationRequesterNode: ModifiedRelocationRequesterNode

    // TODO(b/191393349): Consider adding public API to RelocationRequester to let users specify
    //  the rectangle that they want to bring inView.
    suspend fun bringRectIntoView(rect: Rect) {
        // Ask parents to scroll (or perform some other operation) so that the item is visible.
        relocationRequesterNode.propagateRelocationRequest(rect)
    }
}

/**
 * This is a modifier that can be used to send relocation requests.
 *
 * Here is an example where the a [relocationRequester] can be used to bring an item into parent
 * bounds. It demonstrates how a composable can ask its parents to scroll so that the component
 * using this modifier is brought into the bounds of all its parents.
 * @sample androidx.compose.ui.samples.BringIntoViewSample
 *
 * @param relocationRequester an instance of [RelocationRequester]. This hoisted object can be
 * used to send relocation requests to parents of the current composable.
 */
@ExperimentalComposeUiApi
fun Modifier.relocationRequester(relocationRequester: RelocationRequester): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "relocationRequester"
        properties["relocationRequester"] = relocationRequester
    }
) {
    val modifier = remember { RelocationRequesterModifier() }
    DisposableEffect(relocationRequester) {
        relocationRequester.modifiers += modifier
        onDispose { relocationRequester.modifiers -= modifier }
    }
    modifier
}
