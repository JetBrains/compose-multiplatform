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

package androidx.compose.ui.input.key

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusModifier
import androidx.compose.ui.focus.ModifierLocalParentFocusModifier
import androidx.compose.ui.focus.findActiveFocusNode
import androidx.compose.ui.focus.findLastKeyInputModifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnPlacedModifier
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeWrapper
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept hardware key events when it (or one of its children) is focused.
 *
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware keyboard.
 * While implementing this callback, return true to stop propagation of this event. If you return
 * false, the key event will be sent to this [onKeyEvent]'s parent.
 *
 * @sample androidx.compose.ui.samples.KeyEventSample
 */
fun Modifier.onKeyEvent(onKeyEvent: (KeyEvent) -> Boolean): Modifier = inspectable(
    inspectorInfo = debugInspectorInfo {
        name = "onKeyEvent"
        properties["onKeyEvent"] = onKeyEvent
    }
) {
    KeyInputModifier(onKeyEvent = onKeyEvent, onPreviewKeyEvent = null)
}

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept hardware key events when it (or one of its children) is focused.
 *
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be sent
 * to this [onPreviewKeyEvent]'s child. If none of the children consume the event, it will be
 * sent back up to the root [KeyInputModifier] using the onKeyEvent callback.
 *
 * @sample androidx.compose.ui.samples.KeyEventSample
 */
fun Modifier.onPreviewKeyEvent(onPreviewKeyEvent: (KeyEvent) -> Boolean): Modifier = inspectable(
    inspectorInfo = debugInspectorInfo {
        name = "onPreviewKeyEvent"
        properties["onPreviewKeyEvent"] = onPreviewKeyEvent
    }
) {
    KeyInputModifier(onKeyEvent = null, onPreviewKeyEvent = onPreviewKeyEvent)
}

/**
 * Used to build a tree of [KeyInputModifier]s. This contains the [KeyInputModifier] that is
 * higher in the layout tree.
 */
internal val ModifierLocalKeyInput = modifierLocalOf<KeyInputModifier?> { null }

internal class KeyInputModifier(
    val onKeyEvent: ((KeyEvent) -> Boolean)?,
    val onPreviewKeyEvent: ((KeyEvent) -> Boolean)?
) : ModifierLocalConsumer, ModifierLocalProvider<KeyInputModifier?>, OnPlacedModifier {
    private var focusModifier: FocusModifier? = null
    var parent: KeyInputModifier? = null
        private set
    var layoutNode: LayoutNode? = null
        private set

    override val key: ProvidableModifierLocal<KeyInputModifier?>
        get() = ModifierLocalKeyInput
    override val value: KeyInputModifier
        get() = this

    fun processKeyInput(keyEvent: KeyEvent): Boolean {
        val activeKeyInputModifier = focusModifier
            ?.findActiveFocusNode()
            ?.findLastKeyInputModifier()
            ?: error("KeyEvent can't be processed because this key input node is not active.")
        val consumed = activeKeyInputModifier.propagatePreviewKeyEvent(keyEvent)
        return if (consumed) true else activeKeyInputModifier.propagateKeyEvent(keyEvent)
    }

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) = with(scope) {
        focusModifier?.keyInputChildren?.remove(this@KeyInputModifier)
        focusModifier = ModifierLocalParentFocusModifier.current
        focusModifier?.keyInputChildren?.add(this@KeyInputModifier)
        parent = ModifierLocalKeyInput.current
    }

    fun propagatePreviewKeyEvent(keyEvent: KeyEvent): Boolean {
        // We first propagate the preview key event to the parent.
        val consumed = parent?.propagatePreviewKeyEvent(keyEvent)
        if (consumed == true) return consumed

        // If none of the parents consumed the event, we attempt to consume it.
        return onPreviewKeyEvent?.invoke(keyEvent) ?: false
    }

    fun propagateKeyEvent(keyEvent: KeyEvent): Boolean {
        // We attempt to consume the key event first.
        val consumed = onKeyEvent?.invoke(keyEvent)
        if (consumed == true) return consumed

        // If the event is not consumed, we propagate it to the parent.
        return parent?.propagateKeyEvent(keyEvent) ?: false
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        layoutNode = (coordinates as LayoutNodeWrapper).layoutNode
    }
}
