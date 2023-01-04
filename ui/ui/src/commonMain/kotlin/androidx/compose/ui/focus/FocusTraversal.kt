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

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.visitAncestors
import androidx.compose.ui.node.visitChildren
import androidx.compose.ui.node.visitSubtreeIf
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl

private const val invalidFocusDirection = "Invalid FocusDirection"

/**
 * The [FocusDirection] is used to specify the direction for a [FocusManager.moveFocus]
 * request.
 *
 * @sample androidx.compose.ui.samples.MoveFocusSample
 */
@kotlin.jvm.JvmInline
value class FocusDirection internal constructor(@Suppress("unused") private val value: Int) {

    override fun toString(): String {
        return when (this) {
            Next -> "Next"
            Previous -> "Previous"
            Left -> "Left"
            Right -> "Right"
            Up -> "Up"
            Down -> "Down"
            @OptIn(ExperimentalComposeUiApi::class)
            Enter -> "Enter"
            @OptIn(ExperimentalComposeUiApi::class)
            Exit -> "Exit"
            else -> invalidFocusDirection
        }
    }

    companion object {
        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item.
         *
         *  @sample androidx.compose.ui.samples.MoveFocusSample
         */
        val Next: FocusDirection = FocusDirection(1)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  previous focusable item.
         *
         *  @sample androidx.compose.ui.samples.MoveFocusSample
         */
        val Previous: FocusDirection = FocusDirection(2)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item to the left of the currently focused item.
         *
         *  @sample androidx.compose.ui.samples.MoveFocusSample
         */
        val Left: FocusDirection = FocusDirection(3)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item to the right of the currently focused item.
         *
         *  @sample androidx.compose.ui.samples.MoveFocusSample
         */
        val Right: FocusDirection = FocusDirection(4)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item that is above the currently focused item.
         *
         *  @sample androidx.compose.ui.samples.MoveFocusSample
         */
        val Up: FocusDirection = FocusDirection(5)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item that is below the currently focused item.
         *
         *  @sample androidx.compose.ui.samples.MoveFocusSample
         */
        val Down: FocusDirection = FocusDirection(6)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item that is a child of the currently focused item.
         */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @get:ExperimentalComposeUiApi
        @ExperimentalComposeUiApi
        val Enter: FocusDirection = FocusDirection(7)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you want to move focus to
         *  the parent of the currently focused item.
         */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @get:ExperimentalComposeUiApi
        @ExperimentalComposeUiApi
        val Exit: FocusDirection = FocusDirection(8)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item that is a child of the currently focused item.
         */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET", "Unused")
        @get:ExperimentalComposeUiApi
        @ExperimentalComposeUiApi
        @Deprecated(
            "Use FocusDirection.Enter instead.",
            ReplaceWith("Enter", "androidx.compose.ui.focus.FocusDirection.Companion.Enter"),
            DeprecationLevel.WARNING
        )
        val In: FocusDirection = Enter

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you want to move focus to
         *  the parent of the currently focused item.
         */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET", "Unused")
        @get:ExperimentalComposeUiApi
        @ExperimentalComposeUiApi
        @Deprecated(
            "Use FocusDirection.Exit instead.",
            ReplaceWith("Exit", "androidx.compose.ui.focus.FocusDirection.Companion.Exit"),
            DeprecationLevel.WARNING
        )
        val Out: FocusDirection = Exit
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
        Left, Right, Up, Down -> twoDimensionalFocusSearch(focusDirection, onFound)
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
        else -> error(invalidFocusDirection)
    }
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
internal fun FocusTargetModifierNode.findNonDeactivatedParent(): FocusTargetModifierNode? {
    visitAncestors(Nodes.FocusTarget) {
        if (it.fetchFocusProperties().canFocus) return it
    }
    return null
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
 * Returns all [FocusTargetModifierNode] children that are not Deactivated. Any
 * child that is deactivated will add activated children instead, unless the deactivated
 * node has a custom Enter specified.
 */
@ExperimentalComposeUiApi
internal fun DelegatableNode.collectAccessibleChildren(
    accessibleChildren: MutableVector<FocusTargetModifierNode>
) {
    visitSubtreeIf(Nodes.FocusTarget) {

        if (it.fetchFocusProperties().canFocus) {
            accessibleChildren.add(it)
            return@visitSubtreeIf false
        }

        // If we encounter a deactivated child, we mimic a moveFocus(Enter).
        when (val customEnter = it.fetchFocusProperties().enter(Enter)) {
            // If the user declined a custom enter, omit this part of the tree.
            Cancel -> return@visitSubtreeIf false

            // If there is no custom enter, we consider all the children.
            Default -> return@visitSubtreeIf true

            else -> customEnter.focusRequesterNodes.forEach { node ->
                node.collectAccessibleChildren(accessibleChildren)
            }
        }
        false
    }
}

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
