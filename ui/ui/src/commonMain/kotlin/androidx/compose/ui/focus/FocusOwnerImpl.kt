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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.NodeKind
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.ancestors
import androidx.compose.ui.node.nearestAncestor
import androidx.compose.ui.node.visitLocalChildren
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed

/**
 * The focus manager is used by different [Owner][androidx.compose.ui.node.Owner] implementations
 * to control focus.
 */
internal class FocusOwnerImpl(onRequestApplyChangesListener: (() -> Unit) -> Unit) : FocusOwner {

    @OptIn(ExperimentalComposeUiApi::class)
    internal var rootFocusNode = FocusTargetModifierNode()

    private val focusInvalidationManager = FocusInvalidationManager(onRequestApplyChangesListener)

    /**
     * A [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier
     * list that contains the modifiers required by the focus system. (Eg, a root focus modifier).
     */
    // TODO(b/168831247): return an empty Modifier when there are no focusable children.
    @OptIn(ExperimentalComposeUiApi::class)
    override val modifier: Modifier = object : ModifierNodeElement<FocusTargetModifierNode>() {
        override fun create() = rootFocusNode

        override fun update(node: FocusTargetModifierNode) = node

        override fun InspectorInfo.inspectableProperties() {
            name = "RootFocusTarget"
        }

        override fun hashCode(): Int = rootFocusNode.hashCode()

        override fun equals(other: Any?) = other === this
    }

    override lateinit var layoutDirection: LayoutDirection

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it gains focus. This
     * informs the [focus manager][FocusOwnerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] gained focus, and that it should propagate this
     * focus to one of the focus modifiers in the component hierarchy.
     */
    override fun takeFocus() {
        // If the focus state is not Inactive, it indicates that the focus state is already
        // set (possibly by dispatchWindowFocusChanged). So we don't update the state.
        @OptIn(ExperimentalComposeUiApi::class)
        if (rootFocusNode.focusStateImpl == Inactive) {
            rootFocusNode.focusStateImpl = Active
            // TODO(b/152535715): propagate focus to children based on child focusability.
            //  moveFocus(FocusDirection.Enter)
        }
    }

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it loses focus. This
     * informs the [focus manager][FocusOwnerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] lost focus, and that it should clear focus from
     * all the focus modifiers in the component hierarchy.
     */
    override fun releaseFocus() {
        @OptIn(ExperimentalComposeUiApi::class)
        rootFocusNode.clearFocus(forced = true, refreshFocusEvents = true)
    }

    /**
     * Call this function to set the focus to the root focus modifier.
     *
     * @param force: Whether we should forcefully clear focus regardless of whether we have
     * any components that have captured focus.
     *
     * This could be used to clear focus when a user clicks on empty space outside a focusable
     * component.
     */
    override fun clearFocus(force: Boolean) {
        clearFocus(force, refreshFocusEvents = true)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun clearFocus(force: Boolean, refreshFocusEvents: Boolean) {
        // If this hierarchy had focus before clearing it, it indicates that the host view has
        // focus. So after clearing focus within the compose hierarchy, we should restore focus to
        // the root focus modifier to maintain consistency with the host view.
        val rootInitialState = rootFocusNode.focusStateImpl
        if (rootFocusNode.clearFocus(force, refreshFocusEvents)) {
            rootFocusNode.focusStateImpl = when (rootInitialState) {
                Active, ActiveParent, Captured -> Active
                Inactive -> Inactive
            }
        }
    }

    /**
     * Moves focus in the specified direction.
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    override fun moveFocus(focusDirection: FocusDirection): Boolean {

        // If there is no active node in this sub-hierarchy, we can't move focus.
        val source = rootFocusNode.findActiveFocusNode() ?: return false

        // Check if a custom focus traversal order is specified.
        when (val next = source.customFocusSearch(focusDirection, layoutDirection)) {
            @OptIn(ExperimentalComposeUiApi::class)
            Cancel -> return false
            Default -> {
                val foundNextItem =
                    rootFocusNode.focusSearch(focusDirection, layoutDirection) { destination ->
                        if (destination == source) return@focusSearch false
                        checkNotNull(destination.nearestAncestor(Nodes.FocusTarget)) {
                            "Focus search landed at the root."
                        }
                        // If we found a potential next item, move focus to it.
                        destination.requestFocus()
                    }
                // If we didn't find a potential next item, try to wrap around.
                return foundNextItem || wrapAroundFocus(focusDirection)
            }
            else -> return next.findFocusTarget { it.requestFocus() }
        }
    }

    /**
     * Dispatches a key event through the compose hierarchy.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    override fun dispatchKeyEvent(keyEvent: KeyEvent): Boolean {
        val activeFocusTarget = rootFocusNode.findActiveFocusNode()
        checkNotNull(activeFocusTarget) {
            "Event can't be processed because we do not have an active focus target."
        }
        val focusedKeyInputNode = activeFocusTarget.lastLocalKeyInputNode()
            ?: activeFocusTarget.nearestAncestor(Nodes.KeyInput)

        focusedKeyInputNode?.traverseAncestors(
            type = Nodes.KeyInput,
            onPreVisit = { if (it.onPreKeyEvent(keyEvent)) return true },
            onVisit = { if (it.onKeyEvent(keyEvent)) return true }
        )

        return false
    }

    /**
     * Dispatches a rotary scroll event through the compose hierarchy.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    override fun dispatchRotaryEvent(event: RotaryScrollEvent): Boolean {
        val focusedRotaryInputNode = rootFocusNode.findActiveFocusNode()
            ?.nearestAncestor(Nodes.RotaryInput)

        focusedRotaryInputNode?.traverseAncestors(
            type = Nodes.RotaryInput,
            onPreVisit = { if (it.onPreRotaryScrollEvent(event)) return true },
            onVisit = { if (it.onRotaryScrollEvent(event)) return true }
        )

        return false
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun scheduleInvalidation(node: FocusTargetModifierNode) {
        focusInvalidationManager.scheduleInvalidation(node)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun scheduleInvalidation(node: FocusEventModifierNode) {
        focusInvalidationManager.scheduleInvalidation(node)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun scheduleInvalidation(node: FocusPropertiesModifierNode) {
        focusInvalidationManager.scheduleInvalidation(node)
    }

    @ExperimentalComposeUiApi
    private inline fun <reified T : DelegatableNode> T.traverseAncestors(
        type: NodeKind<T>,
        onPreVisit: (T) -> Unit,
        onVisit: (T) -> Unit
    ) {
        val ancestors = ancestors(type)
        ancestors?.fastForEachReversed(onPreVisit)
        onPreVisit(this)
        onVisit(this)
        ancestors?.fastForEach(onVisit)
    }

    /**
     * Searches for the currently focused item, and returns its coordinates as a rect.
     */
    override fun getFocusRect(): Rect? {
        @OptIn(ExperimentalComposeUiApi::class)
        return rootFocusNode.findActiveFocusNode()?.focusRect()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun DelegatableNode.lastLocalKeyInputNode(): KeyInputModifierNode? {
        var focusedKeyInputNode: KeyInputModifierNode? = null
        visitLocalChildren(Nodes.FocusTarget or Nodes.KeyInput) { modifierNode ->
            if (modifierNode.isKind(Nodes.FocusTarget)) return focusedKeyInputNode

            check(modifierNode is KeyInputModifierNode)
            focusedKeyInputNode = modifierNode
        }
        return focusedKeyInputNode
    }

    // TODO(b/144116848): This is a hack to make Next/Previous wrap around. This must be
    //  replaced by code that sends the move request back to the view system. The view system
    //  will then pass focus to other views, and ultimately return back to this compose view.
    private fun wrapAroundFocus(focusDirection: FocusDirection): Boolean {
        // Wrap is not supported when this sub-hierarchy doesn't have focus.
        @OptIn(ExperimentalComposeUiApi::class)
        if (!rootFocusNode.focusState.hasFocus || rootFocusNode.focusState.isFocused) return false

        // Next and Previous wraps around.
        when (focusDirection) {
            Next, Previous -> {
                // Clear Focus to send focus the root node.
                clearFocus(force = false)
                @OptIn(ExperimentalComposeUiApi::class)
                if (!rootFocusNode.focusState.isFocused) return false

                // Wrap around by calling moveFocus after the root gains focus.
                return moveFocus(focusDirection)
            }
            // We only wrap-around for 1D Focus search.
            else -> return false
        }
    }
}
