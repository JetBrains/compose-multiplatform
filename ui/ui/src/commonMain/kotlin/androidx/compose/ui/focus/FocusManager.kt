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
import androidx.compose.ui.focus.FocusStateImpl.Disabled
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.positionChangeConsumed
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

interface FocusManager {
    /**
     * Call this function to clear focus from the currently focused component, and set the focus to
     * the root focus modifier.
     *
     *  @param force: Whether we should forcefully clear focus regardless of whether we have
     *  any components that have Captured focus.
     */
    fun clearFocus(force: Boolean = false)

    /**
     * Moves focus in the specified [direction][FocusDirection].
     *
     * If you are not satisfied with the default focus order, consider setting a custom order using
     * [Modifier.focusOrder()][focusOrder].
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     */
    fun moveFocus(focusDirection: FocusDirection): Boolean

    /**
     * Moves focus to one of the children of the currently focused item.
     *
     * This function is deprecated. Use FocusManager.moveFocus(FocusDirection.In) instead.
     *
     * @return true if focus was moved successfully.
     */
    @ExperimentalComposeUiApi
    @Deprecated(
        message = "Use FocusManager.moveFocus(FocusDirection.In) instead",
        ReplaceWith(
            "moveFocus(In)",
            "androidx.compose.ui.focus.FocusDirection.Companion.In"
        )
    )
    fun moveFocusIn(): Boolean = false

    /**
     * Moves focus to the nearest focusable parent of the currently focused item.
     *
     *  This function is deprecated. Use FocusManager.moveFocus(FocusDirection.Out) instead.
     *
     * @return true if focus was moved successfully.
     */
    @ExperimentalComposeUiApi
    @Deprecated(
        message = "Use FocusManager.moveFocus(FocusDirection.Out) instead",
        ReplaceWith(
            "moveFocus(Out)",
            "androidx.compose.ui.focus.FocusDirection.Companion.Out"
        )
    )
    fun moveFocusOut(): Boolean = false
}

/**
 * The focus manager is used by different [Owner][androidx.compose.ui.node.Owner] implementations
 * to control focus.
 *
 * @param focusModifier The modifier that will be used as the root focus modifier.
 */
internal class FocusManagerImpl(
    private val focusModifier: FocusModifier = FocusModifier(Inactive)
) : FocusManager {

    /**
     * This gesture is fired when the user clicks on a non-clickable / non-focusable part of the
     * screen. Since no other gesture handled this click, we handle it here.
     */
    private val passThroughClickModifier = PointerInputModifierImpl(
        FocusTapGestureFilter().apply {
            onTap = {
                // The user clicked on a non-clickable part of the screen when something was
                // focused. This is an indication that the user wants to clear focus.
                clearFocus()
            }
            consumeChanges = false
        }
    )

    /**
     * A [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier
     * list that contains the modifiers required by the focus system. (Eg, a root focus modifier).
     */
    val modifier: Modifier
        // TODO(b/168831247): return an empty Modifier when there are no focusable children.
        get() = passThroughClickModifier
            .then(focusModifier)

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it gains focus. This
     * informs the [focus manager][FocusManagerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] gained focus, and that it should propagate this
     * focus to one of the focus modifiers in the component hierarchy.
     */
    fun takeFocus() {
        // If the focus state is not Inactive, it indicates that the focus state is already
        // set (possibly by dispatchWindowFocusChanged). So we don't update the state.
        if (focusModifier.focusState == Inactive) {
            focusModifier.focusState = Active
            // TODO(b/152535715): propagate focus to children based on child focusability.
        }
    }

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it loses focus. This
     * informs the [focus manager][FocusManagerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] lost focus, and that it should clear focus from
     * all the focus modifiers in the component hierarchy.
     */
    fun releaseFocus() {
        focusModifier.focusNode.clearFocus(forcedClear = true)
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
        // If this hierarchy had focus before clearing it, it indicates that the host view has
        // focus. So after clearing focus within the compose hierarchy, we should reset the root
        // focus modifier to "Active" to maintain consistency with the host view.
        val rootWasFocused = when (focusModifier.focusState) {
            Active, ActiveParent, Captured -> true
            Disabled, Inactive -> false
        }

        if (focusModifier.focusNode.clearFocus(force) && rootWasFocused) {
            focusModifier.focusState = Active
        }
    }

    /**
     * Moves focus in the specified direction.
     *
     * Focus moving is still being implemented. Right now, focus will move only if the user
     * specified a custom focus traversal order for the item that is currently focused. (Using the
     * [Modifier.focusOrder()][focusOrder] API).
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     */
    override fun moveFocus(focusDirection: FocusDirection): Boolean {
        return focusModifier.focusNode.moveFocus(focusDirection)
    }
}

private data class PointerInputModifierImpl(override val pointerInputFilter: PointerInputFilter) :
    PointerInputModifier

// TODO: remove in b/179602539
private class FocusTapGestureFilter : PointerInputFilter() {
    /**
     * Called to indicate that a press gesture has successfully completed.
     *
     * This should be used to fire a state changing event as if a button was pressed.
     */
    lateinit var onTap: (Offset) -> Unit

    /**
     * Whether or not to consume changes.
     */
    var consumeChanges: Boolean = true

    /**
     * True when we are primed to call [onTap] and may be consuming all down changes.
     */
    private var primed = false

    private var downPointers: MutableSet<PointerId> = mutableSetOf()
    private var upBlockedPointers: MutableSet<PointerId> = mutableSetOf()
    private var lastPxPosition: Offset? = null

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        val changes = pointerEvent.changes

        if (pass == PointerEventPass.Main) {

            if (primed &&
                changes.fastAll { it.changedToUp() }
            ) {
                val pointerPxPosition: Offset = changes[0].previousPosition
                if (changes.fastAny { !upBlockedPointers.contains(it.id) }) {
                    // If we are primed, all pointers went up, and at least one of the pointers is
                    // not blocked, we can fire, reset, and consume all of the up events.
                    reset()
                    onTap.invoke(pointerPxPosition)
                    if (consumeChanges) {
                        changes.fastForEach {
                            it.consumeDownChange()
                        }
                    }
                    return
                } else {
                    lastPxPosition = pointerPxPosition
                }
            }

            if (changes.fastAll { it.changedToDown() }) {
                // Reset in case we were incorrectly left waiting on a delayUp message.
                reset()
                // If all of the changes are down, can become primed.
                primed = true
            }

            if (primed) {
                changes.fastForEach {
                    if (it.changedToDown()) {
                        downPointers.add(it.id)
                    }
                    if (it.changedToUpIgnoreConsumed()) {
                        downPointers.remove(it.id)
                    }
                }
            }
        }

        if (pass == PointerEventPass.Final && primed) {

            val anyPositionChangeConsumed = changes.fastAny { it.positionChangeConsumed() }

            val noPointersInBounds =
                upBlockedPointers.isEmpty() && !changes.anyPointersInBounds(bounds)

            if (anyPositionChangeConsumed || noPointersInBounds) {
                // If we are on the final pass, we are primed, and either we aren't blocked and
                // all pointers are out of bounds.
                reset()
            }
        }
    }

    // TODO(shepshapard): This continues to be very confusing to use.  Have to come up with a better
//  way of easily expressing this.
    /**
     * Utility method that determines if any pointers are currently in [bounds].
     *
     * A pointer is considered in bounds if it is currently down and it's current
     * position is within the provided [bounds]
     *
     * @return True if at least one pointer is in bounds.
     */
    private fun List<PointerInputChange>.anyPointersInBounds(bounds: IntSize) =
        fastAny {
            it.pressed &&
                it.position.x >= 0 &&
                it.position.x < bounds.width &&
                it.position.y >= 0 &&
                it.position.y < bounds.height
        }

    override fun onCancel() {
        reset()
    }

    private fun reset() {
        primed = false
        upBlockedPointers.clear()
        downPointers.clear()
        lastPxPosition = null
    }
}