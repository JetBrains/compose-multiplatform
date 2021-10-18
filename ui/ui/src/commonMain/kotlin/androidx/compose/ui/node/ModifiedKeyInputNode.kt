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

package androidx.compose.ui.node

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifier

internal class ModifiedKeyInputNode(
    wrapped: LayoutNodeWrapper,
    keyInputModifier: KeyInputModifier
) : DelegatingLayoutNodeWrapper<KeyInputModifier>(wrapped, keyInputModifier) {

    init {
        keyInputModifier.keyInputNode = this
    }

    override var modifier: KeyInputModifier
        get() = super.modifier
        set(value) {
            super.modifier = value
            value.keyInputNode = this
        }

    override fun findPreviousKeyInputWrapper() = this

    override fun findNextKeyInputWrapper() = this

    fun propagatePreviewKeyEvent(keyEvent: KeyEvent): Boolean {
        // We first propagate the preview key event to the parent.
        val consumed = findParentKeyInputNode()?.propagatePreviewKeyEvent(keyEvent)
        if (consumed == true) return consumed

        // If none of the parents consumed the event, we attempt to consume it.
        return modifier.onPreviewKeyEvent?.invoke(keyEvent) ?: false
    }

    fun propagateKeyEvent(keyEvent: KeyEvent): Boolean {
        // We attempt to consume the key event first.
        val consumed = modifier.onKeyEvent?.invoke(keyEvent)
        if (consumed == true) return consumed

        // If the event is not consumed, we propagate it to the parent.
        return findParentKeyInputNode()?.propagateKeyEvent(keyEvent) ?: false
    }
}
