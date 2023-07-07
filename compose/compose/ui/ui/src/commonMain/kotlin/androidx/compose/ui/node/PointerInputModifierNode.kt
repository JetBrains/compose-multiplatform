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
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize

/**
 * A [androidx.compose.ui.Modifier.Node] that receives [PointerInputChange]s,
 * interprets them, and consumes the aspects of the changes that it is react to such that other
 * [PointerInputModifierNode]s don't also react to them.
 *
 * This is the [androidx.compose.ui.Modifier.Node] equivalent of
 * [androidx.compose.ui.input.pointer.PointerInputModifier]
 *
 * @sample androidx.compose.ui.samples.PointerInputModifierNodeSample
 */
@ExperimentalComposeUiApi
interface PointerInputModifierNode : DelegatableNode {
    /**
     * Invoked when pointers that previously hit this [PointerInputModifierNode] have changed. It is
     * expected that any [PointerInputChange]s that are used during this event and should not be
     * considered valid to be used in other nodes should be marked as consumed by calling
     * [PointerInputChange.consume].
     *
     * @param pointerEvent The list of [PointerInputChange]s with positions relative to this
     * [PointerInputModifierNode].
     * @param pass The [PointerEventPass] in which this function is being called.
     * @param bounds The width and height associated with this [PointerInputModifierNode].
     *
     * @see PointerInputChange
     * @see PointerEventPass
     */
    fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    )

    /**
     * Invoked to notify the handler that no more calls to [PointerInputModifierNode] will be made,
     * until at least new pointers exist.  This can occur for a few reasons:
     * 1. Android dispatches ACTION_CANCEL to Compose.
     * 2. This [PointerInputModifierNode] is no longer associated with a LayoutNode.
     * 3. This [PointerInputModifierNode]'s associated LayoutNode is no longer in the composition
     * tree.
     */
    fun onCancelPointerInput()

    /**
     * Intercept pointer input that children receive even if the pointer is out of bounds.
     *
     * If `true`, and a child has been moved out of this layout and receives an event, this
     * will receive that event. If `false`, a child receiving pointer input outside of the
     * bounds of this layout will not trigger any events in this.
     */
    fun interceptOutOfBoundsChildEvents(): Boolean = false

    /**
     * If `false`, then this [PointerInputModifierNode] will not allow siblings under it to respond
     * to events. If `true`, this will have the first chance to respond and the next sibling
     * under will then get a chance to respond as well. This trigger acts at the Layout
     * level, so if any [PointerInputModifierNode]s on a Layout has
     * [sharePointerInputWithSiblings] set to `true` then the Layout will share with siblings.
     */
    fun sharePointerInputWithSiblings(): Boolean = false
}

@OptIn(ExperimentalComposeUiApi::class)
internal val PointerInputModifierNode.isAttached: Boolean
    get() = node.isAttached

@OptIn(ExperimentalComposeUiApi::class)
internal val PointerInputModifierNode.layoutCoordinates: LayoutCoordinates
    get() = requireCoordinator(Nodes.PointerInput)
