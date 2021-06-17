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
import androidx.compose.ui.composed
import androidx.compose.ui.focus.findActiveFocusNode
import androidx.compose.ui.node.ModifiedKeyInputNode
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept hardware key events.
 *
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware keyboard.
 * While implementing this callback, return true to stop propagation of this event. If you return
 * false, the key event will be sent to this [onKeyEvent]'s parent.
 */
// TODO: b/191017532 remove Modifier.composed
@Suppress("UnnecessaryComposedModifier")
fun Modifier.onKeyEvent(onKeyEvent: (KeyEvent) -> Boolean): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "onKeyEvent"
        properties["onKeyEvent"] = onKeyEvent
    }
) {
    KeyInputModifier(onKeyEvent = onKeyEvent, onPreviewKeyEvent = null)
}

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept hardware key events.
 *
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be sent
 * to this [onPreviewKeyEvent]'s child. If none of the children consume the event, it will be
 * sent back up to the root [KeyInputModifier] using the onKeyEvent callback.
 */
// TODO: b/191017532 remove Modifier.composed
@Suppress("UnnecessaryComposedModifier")
fun Modifier.onPreviewKeyEvent(onPreviewKeyEvent: (KeyEvent) -> Boolean): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "onPreviewKeyEvent"
        properties["onPreviewKeyEvent"] = onPreviewKeyEvent
    }
) {
    KeyInputModifier(onKeyEvent = null, onPreviewKeyEvent = onPreviewKeyEvent)
}

internal class KeyInputModifier(
    val onKeyEvent: ((KeyEvent) -> Boolean)?,
    val onPreviewKeyEvent: ((KeyEvent) -> Boolean)?
) : Modifier.Element {
    lateinit var keyInputNode: ModifiedKeyInputNode

    fun processKeyInput(keyEvent: KeyEvent): Boolean {
        val activeKeyInputNode = keyInputNode.findPreviousFocusWrapper()
            ?.findActiveFocusNode()
            ?.findLastKeyInputWrapper()
            ?: error("KeyEvent can't be processed because this key input node is not active.")
        return with(activeKeyInputNode) {
            val consumed = propagatePreviewKeyEvent(keyEvent)
            if (consumed) true else propagateKeyEvent(keyEvent)
        }
    }
}
