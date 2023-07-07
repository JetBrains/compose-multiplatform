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

package androidx.compose.ui.focus

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.visitChildren

/**
 * The [FocusInvalidationManager] allows us to schedule focus related nodes for invalidation.
 * These nodes are invalidated after onApplyChanges. It does this by registering an
 * onApplyChangesListener when nodes are scheduled for invalidation.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class FocusInvalidationManager(
    private val onRequestApplyChangesListener: (() -> Unit) -> Unit
) {
    private var focusTargetNodes = mutableSetOf<FocusTargetModifierNode>()
    private var focusEventNodes = mutableSetOf<FocusEventModifierNode>()
    private var focusPropertiesNodes = mutableSetOf<FocusPropertiesModifierNode>()

    fun scheduleInvalidation(node: FocusTargetModifierNode) {
        focusTargetNodes.scheduleInvalidation(node)
    }

    fun scheduleInvalidation(node: FocusEventModifierNode) {
        focusEventNodes.scheduleInvalidation(node)
    }

    fun scheduleInvalidation(node: FocusPropertiesModifierNode) {
        focusPropertiesNodes.scheduleInvalidation(node)
    }

    private fun <T> MutableSet<T>.scheduleInvalidation(node: T) {
        // We don't schedule a node if it is already scheduled during this composition.
        if (contains(node)) return

        add(node)

        // If this is the first node scheduled for invalidation,
        // we set up a listener that runs after onApplyChanges.
        if (focusTargetNodes.size + focusEventNodes.size + focusPropertiesNodes.size == 1) {
            onRequestApplyChangesListener.invoke(invalidateNodes)
        }
    }

    private val invalidateNodes: () -> Unit = {
        // Process all the invalidated FocusProperties nodes.
        focusPropertiesNodes.forEach {
            it.visitChildren(Nodes.FocusTarget) { focusTarget ->
                focusTargetNodes.add(focusTarget)
            }
        }
        focusPropertiesNodes.clear()

        // Process all the focus events nodes.
        val focusTargetsWithInvalidatedFocusEvents = mutableSetOf<FocusTargetModifierNode>()
        focusEventNodes.forEach { focusEventNode ->
            // When focus nodes are removed, the corresponding focus events are scheduled for
            // invalidation. If the focus event was also removed, we don't need to invalidate it.
            // We call onFocusEvent with the default value, just to make it easier for the user,
            // so that they don't have to keep track of whether they caused a focused item to be
            // removed (Which would cause it to lose focus).
            if (!focusEventNode.node.isAttached) {
                focusEventNode.onFocusEvent(Inactive)
                return@forEach
            }

            var requiresUpdate = true
            var aggregatedNode = false
            var focusTarget: FocusTargetModifierNode? = null
            focusEventNode.visitChildren(Nodes.FocusTarget) {

                // If there are multiple focus targets associated with this focus event node,
                // we need to calculate the aggregated state.
                if (focusTarget != null) {
                    aggregatedNode = true
                }

                focusTarget = it

                // If the associated focus node is already scheduled for invalidation, it will
                // send an onFocusEvent if the invalidation causes a focus state change.
                // However this onFocusEvent was invalidated, so we have to ensure that we call
                // onFocusEvent even if the focus state didn't change.
                if (focusTargetNodes.contains(it)) {
                    requiresUpdate = false
                    focusTargetsWithInvalidatedFocusEvents.add(it)
                    return@visitChildren
                }
            }

            if (requiresUpdate) {
                focusEventNode.onFocusEvent(
                    if (aggregatedNode) {
                        focusEventNode.getFocusState()
                    } else {
                        focusTarget?.focusState ?: Inactive
                    }
                )
            }
        }
        focusEventNodes.clear()

        // Process all the focus target nodes.
        focusTargetNodes.forEach {
            // We don't need to invalidate the focus target if it was scheduled for invalidation
            // earlier in the composition but was then removed.
            if (!it.isAttached) return@forEach

            val preInvalidationState = it.focusState
            it.invalidateFocus()
            if (preInvalidationState != it.focusState ||
                focusTargetsWithInvalidatedFocusEvents.contains(it)) {
                it.refreshFocusEventNodes()
            }
        }
        focusTargetNodes.clear()
        focusTargetsWithInvalidatedFocusEvents.clear()

        check(focusPropertiesNodes.isEmpty())
        check(focusEventNodes.isEmpty())
        check(focusTargetNodes.isEmpty())
    }
}
