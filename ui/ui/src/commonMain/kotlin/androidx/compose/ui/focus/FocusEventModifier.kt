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
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * A [modifier][Modifier.Element] that can be used to observe focus state events.
 */
@Deprecated("Use FocusEventModifierNode instead")
@JvmDefaultWithCompatibility
interface FocusEventModifier : Modifier.Element {
    /**
     * A callback that is called whenever the focus system raises events.
     */
    fun onFocusEvent(focusState: FocusState)
}

/**
 * Add this modifier to a component to observe focus state events.
 */
fun Modifier.onFocusEvent(
    onFocusEvent: (FocusState) -> Unit
): Modifier = this then FocusEventElement(onFocusEvent)

@OptIn(ExperimentalComposeUiApi::class)
private data class FocusEventElement(
    val onFocusEvent: (FocusState) -> Unit
) : ModifierNodeElement<FocusEventModifierNodeImpl>() {
    override fun create() = FocusEventModifierNodeImpl(onFocusEvent)

    override fun update(node: FocusEventModifierNodeImpl) = node.apply {
        onFocusEvent = this@FocusEventElement.onFocusEvent
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onFocusEvent"
        properties["onFocusEvent"] = onFocusEvent
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class FocusEventModifierNodeImpl(
    var onFocusEvent: (FocusState) -> Unit
) : FocusEventModifierNode, Modifier.Node() {

    override fun onFocusEvent(focusState: FocusState) {
        this.onFocusEvent.invoke(focusState)
    }
}
