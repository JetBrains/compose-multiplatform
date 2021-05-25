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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Offset.Companion.Zero
import androidx.compose.ui.input.nestedscroll.NestedScrollDelegatingWrapper
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Drag
import androidx.compose.ui.node.LayoutNodeWrapper
import androidx.compose.ui.platform.debugInspectorInfo
import kotlin.math.abs

// This modifier keeps track of the coordinates when the item is positioned, and when
// bringIntoParentBounds() is called, it asks parents to scroll to bring this item into view.
internal class RelocationRequesterModifier : OnGloballyPositionedModifier {
    lateinit var coordinates: LayoutCoordinates

    fun bringIntoView() {
        val layoutNodeWrapper = coordinates
        check(layoutNodeWrapper is LayoutNodeWrapper)

        // Recursively scroll parents so that the item is visible.
        layoutNodeWrapper.findPreviousNestedScrollWrapper()?.bringIntoView(coordinates)

        // Ask the owner to send a request to its parents to make sure this item is visible.
        layoutNodeWrapper.layoutNode.owner?.requestRectangleOnScreen(coordinates.boundsInRoot())
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
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

// Scroll this nested scroll parent to bring the child into view. Then find the nested scroll parent
// of this nested scroll parent and ask them to do the same. This results in scrolls propagating
// up to all the nested scroll parents to bring the specified child into view.
private fun NestedScrollDelegatingWrapper.bringIntoView(child: LayoutCoordinates) {
    val childBounds = localBoundingBoxOf(child, false)
    val offset = Offset(
        calculateOffset(childBounds.left, childBounds.right, size.width.toFloat()),
        calculateOffset(childBounds.top, childBounds.bottom, size.height.toFloat())
    )
    // TODO(b/187432148): We ideally shouldn't be using internal connection functions. We need to
    //  build a better system with a more granular API that allows us to send scroll requests to
    //  specific parents.
    modifier.connection.onPostScroll(Zero, offset, Drag)

    wrappedBy?.findPreviousNestedScrollWrapper()?.bringIntoView(child)
}

// Calculate the offset needed to bring one of the edges into view. The leadingEdge is the side
// closest to the origin (For the x-axis this is 'left', for the y-axis this is 'top').
// The trailing edge is the other side (For the x-axis this is 'right', for the y-axis this is
// 'bottom').
private fun calculateOffset(leadingEdge: Float, trailingEdge: Float, parentSize: Float) = when {
    // If the item is already visible, no need to scroll.
    leadingEdge >= 0 && trailingEdge <= parentSize -> 0f

    // If the item is visible but larger than the parent, we don't scroll.
    leadingEdge < 0 && trailingEdge > parentSize -> 0f

    // Find the minimum scroll needed to make one of the edges coincide with the parent's edge.
    abs(leadingEdge) < abs(trailingEdge - parentSize) -> -leadingEdge
    else -> parentSize - trailingEdge
}
