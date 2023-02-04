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
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.visitAncestors
import androidx.compose.ui.node.visitChildren
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl

/**
 * Search up the component tree for any parent/parents that have specified a custom focus order.
 * Allowing parents higher up the hierarchy to overwrite the focus order specified by their
 * children.
 *
 * @param focusDirection the focus direction passed to [FocusManager.moveFocus] that triggered this
 * focus search.
 * @param layoutDirection the current system [LayoutDirection].
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun FocusTargetModifierNode.customFocusSearch(
    focusDirection: FocusDirection,
    layoutDirection: LayoutDirection
): FocusRequester {
    val focusProperties = fetchFocusProperties()
    return when (focusDirection) {
        Next -> focusProperties.next
        Previous -> focusProperties.previous
        Up -> focusProperties.up
        Down -> focusProperties.down
        Left -> when (layoutDirection) {
            Ltr -> focusProperties.start
            Rtl -> focusProperties.end
        }.takeUnless { it == Default } ?: focusProperties.left
        Right -> when (layoutDirection) {
            Ltr -> focusProperties.end
            Rtl -> focusProperties.start
        }.takeUnless { it == Default } ?: focusProperties.right
        // TODO(b/183746982): add focus order API for "In" and "Out".
        //  Developers can to specify a custom "In" to specify which child should be visited when
        //  the user presses dPad center. (They can also redirect the "In" to some other item).
        //  Developers can specify a custom "Out" to specify which composable should take focus
        //  when the user presses the back button.
        @OptIn(ExperimentalComposeUiApi::class)
        Enter -> {
            @OptIn(ExperimentalComposeUiApi::class)
            focusProperties.enter(focusDirection)
        }
        @OptIn(ExperimentalComposeUiApi::class)
        Exit -> {
            @OptIn(ExperimentalComposeUiApi::class)
            focusProperties.exit(focusDirection)
        }
        else -> error("invalid FocusDirection")
    }
}

/**
 * Moves focus based on the requested focus direction.
 *
 * @param focusDirection The requested direction to move focus.
 * @param layoutDirection Whether the layout is RTL or LTR.
 * @param onFound This lambda is invoked if focus search finds the next focus node.
 * @return if no focus node is found, we return false. otherwise we return the result of [onFound].
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun FocusTargetModifierNode.focusSearch(
    focusDirection: FocusDirection,
    layoutDirection: LayoutDirection,
    onFound: (FocusTargetModifierNode) -> Boolean
): Boolean {
    return when (focusDirection) {
        Next, Previous -> oneDimensionalFocusSearch(focusDirection, onFound)
        Left, Right, Up, Down -> twoDimensionalFocusSearch(focusDirection, onFound) ?: false
        @OptIn(ExperimentalComposeUiApi::class)
        Enter -> {
            // we search among the children of the active item.
            val direction = when (layoutDirection) { Rtl -> Left; Ltr -> Right }
            findActiveFocusNode()?.twoDimensionalFocusSearch(direction, onFound) ?: false
        }
        @OptIn(ExperimentalComposeUiApi::class)
        Exit -> findActiveFocusNode()?.findNonDeactivatedParent().let {
            if (it == null || it == this) false else onFound.invoke(it)
        }
        else -> error("Focus search invoked with invalid FocusDirection $focusDirection")
    }
}

/**
 * Returns the bounding box of the focus layout area in the root or [Rect.Zero] if the
 * FocusModifier has not had a layout.
 */
@ExperimentalComposeUiApi
internal fun FocusTargetModifierNode.focusRect(): Rect = coordinator?.let {
    it.findRootCoordinates().localBoundingBoxOf(it, clipBounds = false)
} ?: Rect.Zero

/**
 * Whether this node should be considered when searching for the next item during a traversal.
 */
@ExperimentalComposeUiApi
internal val FocusTargetModifierNode.isEligibleForFocusSearch: Boolean
    get() = coordinator?.layoutNode?.isPlaced == true &&
        coordinator?.layoutNode?.isAttached == true

@ExperimentalComposeUiApi
internal val FocusTargetModifierNode.activeChild: FocusTargetModifierNode?
    get() {
        if (!node.isAttached) return null

        visitChildren(Nodes.FocusTarget) {
            when (it.focusStateImpl) {
                Active, ActiveParent, Captured -> return it
                Inactive -> return@visitChildren
            }
        }
        return null
    }

@OptIn(ExperimentalComposeUiApi::class)
internal fun FocusTargetModifierNode.findActiveFocusNode(): FocusTargetModifierNode? {
    when (focusStateImpl) {
        Active, Captured -> return this
        ActiveParent -> {
            visitChildren(Nodes.FocusTarget) { node ->
                node.findActiveFocusNode()?.let { return it }
            }
            return null
        }
        Inactive -> return null
    }
}

@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
@OptIn(ExperimentalComposeUiApi::class)
private fun FocusTargetModifierNode.findNonDeactivatedParent(): FocusTargetModifierNode? {
    visitAncestors(Nodes.FocusTarget) {
        if (it.fetchFocusProperties().canFocus) return it
    }
    return null
}
