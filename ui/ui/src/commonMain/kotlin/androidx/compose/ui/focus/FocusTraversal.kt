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
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.In
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Out
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Deactivated
import androidx.compose.ui.focus.FocusStateImpl.DeactivatedParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.KeyInputModifier
import androidx.compose.ui.layout.findRoot
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
            In -> "In"
            @OptIn(ExperimentalComposeUiApi::class)
            Out -> "Out"
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
        @ExperimentalComposeUiApi
        val In: FocusDirection = FocusDirection(7)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you want to move focus to
         *  the parent of the currently focused item.
         */
        @ExperimentalComposeUiApi
        val Out: FocusDirection = FocusDirection(8)
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
internal fun FocusModifier.focusSearch(
    focusDirection: FocusDirection,
    layoutDirection: LayoutDirection,
    onFound: (FocusModifier) -> Boolean
): Boolean {
    return when (focusDirection) {
        Next, Previous -> oneDimensionalFocusSearch(focusDirection, onFound)
        Left, Right, Up, Down -> twoDimensionalFocusSearch(focusDirection, onFound)
        @OptIn(ExperimentalComposeUiApi::class)
        In -> {
            // we search among the children of the active item.
            val direction = when (layoutDirection) { Rtl -> Left; Ltr -> Right }
            findActiveFocusNode()?.twoDimensionalFocusSearch(direction, onFound) ?: false
        }
        @OptIn(ExperimentalComposeUiApi::class)
        Out -> findActiveFocusNode()?.findActiveParent().let {
            if (it == this || it == null) false else onFound.invoke(it)
        }
        else -> error(invalidFocusDirection)
    }
}

@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
internal fun FocusModifier.findActiveFocusNode(): FocusModifier? {
    return when (focusState) {
        Active, Captured -> this
        ActiveParent, DeactivatedParent -> focusedChild?.findActiveFocusNode()
        Inactive, Deactivated -> null
    }
}

@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
internal fun FocusModifier.findActiveParent(): FocusModifier? = parent?.let {
        when (focusState) {
            Active, Captured, Deactivated, DeactivatedParent, Inactive -> it.findActiveParent()
            ActiveParent -> this
        }
    }

/**
 * Returns the bounding box of the focus layout area in the root or [Rect.Zero] if the
 * FocusModifier has not had a layout.
 */
internal fun FocusModifier.focusRect(): Rect = layoutNodeWrapper?.let {
    it.findRoot().localBoundingBoxOf(it, clipBounds = false)
} ?: Rect.Zero

/**
 * Returns all [FocusModifier] children that are not [FocusStateImpl.isDeactivated]. Any
 * child that is deactivated will add activated children instead.
 */
internal fun FocusModifier.activatedChildren(): MutableVector<FocusModifier> {
    if (!children.any { it.focusState.isDeactivated }) {
        return children
    }
    val activated = mutableVectorOf<FocusModifier>()
    children.forEach { child ->
        if (!child.focusState.isDeactivated) {
            activated += child
        } else {
            activated.addAll(child.activatedChildren())
        }
    }
    return activated
}

/**
 * Returns the inner-most KeyInputModifier on the same LayoutNode as this FocusModifier.
 */
@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
internal fun FocusModifier.findLastKeyInputModifier(): KeyInputModifier? {
    val layoutNode = layoutNodeWrapper?.layoutNode ?: return null
    var best: KeyInputModifier? = null
    keyInputChildren.forEach { keyInputModifier ->
        if (keyInputModifier.layoutNode == layoutNode) {
            best = lastOf(keyInputModifier, best)
        }
    }
    if (best != null) {
        return best
    }
    // There isn't a KeyInputModifier after this, but there may be one before this.
    return keyInputModifier
}

/**
 * Whether this node should be considered when searching for the next item during a traversal.
 */
internal val FocusModifier.isEligibleForFocusSearch: Boolean
    get() = layoutNodeWrapper?.layoutNode?.isPlaced == true &&
            layoutNodeWrapper?.layoutNode?.isAttached == true

/**
 * Returns [one] if it comes after [two] in the modifier chain or [two] if it comes after [one].
 */
@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
private fun lastOf(one: KeyInputModifier, two: KeyInputModifier?): KeyInputModifier {
    var mod = two ?: return one
    val layoutNode = one.layoutNode
    while (mod != one) {
        val parent = mod.parent
        if (parent == null || parent.layoutNode != layoutNode) {
            return one
        }
        mod = parent
    }
    return two
}