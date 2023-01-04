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

package androidx.compose.ui.focus

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.modifierElementOf
import androidx.compose.ui.node.visitAncestors
import androidx.compose.ui.node.visitChildren

/**
 * Implement this interface create a modifier node that can be used to observe focus state changes
 * to a [FocusTargetModifierNode] down the hierarchy.
 */
@ExperimentalComposeUiApi
interface FocusEventModifierNode : DelegatableNode {

    /**
     * A parent FocusEventNode is notified of [FocusState] changes to the [FocusTargetModifierNode]
     * associated with this [FocusEventModifierNode].
     */
    fun onFocusEvent(focusState: FocusState)
}

/**
 * A [modifier][Modifier.Element] that can be used to observe focus state events.
 */
@Deprecated("Use FocusEventModifierNode instead")
@JvmDefaultWithCompatibility
interface FocusEventModifier : Modifier.Element {
    /**
     * A callback that is called whenever the focus system raises events.
     */
    fun onFocusEvent(focusState: FocusState)
}

@OptIn(ExperimentalComposeUiApi::class)
internal class FocusEventModifierNodeImpl(
    var onFocusEvent: (FocusState) -> Unit
) : FocusEventModifierNode, Modifier.Node() {

    override fun onFocusEvent(focusState: FocusState) {
        this.onFocusEvent.invoke(focusState)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun FocusEventModifierNode.getFocusState(): FocusState {
    visitChildren(Nodes.FocusTarget) {
        when (val focusState = it.focusStateImpl) {
            // If we find a focused child, we use that child's state as the aggregated state.
            Active, ActiveParent, Captured -> return focusState
            // We use the Inactive state only if we don't have a focused child.
            // ie. we ignore this child if another child provides aggregated state.
            Inactive -> return@visitChildren
        }
    }
    return Inactive
}

/**
 * Add this modifier to a component to observe focus state events.
 */
@Suppress("ModifierInspectorInfo") // b/251831790.
fun Modifier.onFocusEvent(onFocusEvent: (FocusState) -> Unit): Modifier = this.then(
    @OptIn(ExperimentalComposeUiApi::class)
    modifierElementOf(
        key = onFocusEvent,
        create = { FocusEventModifierNodeImpl(onFocusEvent) },
        update = { it.onFocusEvent = onFocusEvent },
        definitions = {
            name = "onFocusEvent"
            properties["onFocusEvent"] = onFocusEvent
        }
    )
)

/**
 * Sends a "Focus Event" up the hierarchy that asks all [FocusEventModifierNode]s to recompute their
 * observed focus state.
 *
 * Make this public after [FocusTargetModifierNode] is made public.
 */
@ExperimentalComposeUiApi
internal fun FocusTargetModifierNode.refreshFocusEventNodes() {
    visitAncestors(Nodes.FocusEvent or Nodes.FocusTarget) {
        // If we reach the previous focus target node, we have gone too far, as
        //  this is applies to the another focus event.
        if (it.isKind(Nodes.FocusTarget)) return

        // TODO(251833873): Consider caching it.getFocusState().
        check(it is FocusEventModifierNode)
        it.onFocusEvent(it.getFocusState())
    }
}
