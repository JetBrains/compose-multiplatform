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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.unit.LayoutDirection

/**
 * The focus owner provides some internal APIs that are not exposed by focus manager.
 */
internal interface FocusOwner : FocusManager {

    /**
     * A [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier
     * list that contains the modifiers required by the focus system. (Eg, a root focus modifier).
     */
    val modifier: Modifier

    /**
     * The owner sets the layoutDirection that is then used during focus search.
     */
    var layoutDirection: LayoutDirection

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it gains focus. This
     * informs the [focus manager][FocusOwnerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] gained focus, and that it should propagate this
     * focus to one of the focus modifiers in the component hierarchy.
     */
    fun takeFocus()

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it loses focus. This
     * informs the [focus manager][FocusOwnerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] lost focus, and that it should clear focus from
     * all the focus modifiers in the component hierarchy.
     */
    fun releaseFocus()

    /**
     * Call this function to set the focus to the root focus modifier.
     *
     * @param force: Whether we should forcefully clear focus regardless of whether we have
     * any components that have captured focus.
     *
     * @param refreshFocusEvents: Whether we should send an event up the hierarchy to update
     * the associated onFocusEvent nodes.
     *
     * This could be used to clear focus when a user clicks on empty space outside a focusable
     * component.
     */
    fun clearFocus(force: Boolean, refreshFocusEvents: Boolean)

    /**
     * Searches for the currently focused item, and returns its coordinates as a rect.
     */
    fun getFocusRect(): Rect?

    /**
     * Dispatches a key event through the compose hierarchy.
     */
    fun dispatchKeyEvent(keyEvent: KeyEvent): Boolean

    /**
     * Dispatches a rotary scroll event through the compose hierarchy.
     */
    fun dispatchRotaryEvent(event: RotaryScrollEvent): Boolean

    /**
     * Schedule a FocusTarget node to be invalidated after onApplyChanges.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun scheduleInvalidation(node: FocusTargetModifierNode)

    /**
     * Schedule a FocusEvent node to be invalidated after onApplyChanges.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun scheduleInvalidation(node: FocusEventModifierNode)

    /**
     * Schedule a FocusProperties node to be invalidated after onApplyChanges.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun scheduleInvalidation(node: FocusPropertiesModifierNode)
}
