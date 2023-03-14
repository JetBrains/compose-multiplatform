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
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Add this modifier to a component to observe focus state events. [onFocusChanged] is invoked
 * when the focus state changes. The [onFocusChanged] modifier listens to the state of the first
 * [focusTarget] following this modifier.
 *
 * @sample androidx.compose.ui.samples.FocusableSample
 *
 * Note: If you want to be notified every time the internal focus state is written to (even if it
 * hasn't changed), use [onFocusEvent] instead.
 */
fun Modifier.onFocusChanged(
    onFocusChanged: (FocusState) -> Unit
): Modifier = this then FocusChangedElement(onFocusChanged)

@OptIn(ExperimentalComposeUiApi::class)
private data class FocusChangedElement(
    val onFocusChanged: (FocusState) -> Unit
) : ModifierNodeElement<FocusChangedModifierNode>() {
    override fun create() = FocusChangedModifierNode(onFocusChanged)

    override fun update(node: FocusChangedModifierNode) = node.apply {
        onFocusChanged = this@FocusChangedElement.onFocusChanged
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onFocusChanged"
        properties["onFocusChanged"] = onFocusChanged
    }
}

@ExperimentalComposeUiApi
private class FocusChangedModifierNode(
    var onFocusChanged: (FocusState) -> Unit
) : FocusEventModifierNode, Modifier.Node() {
    private var focusState: FocusState? = null
    override fun onFocusEvent(focusState: FocusState) {
        if (this.focusState != focusState) {
            this.focusState = focusState
            this.onFocusChanged.invoke(focusState)
        }
    }
}
