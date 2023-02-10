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
 * A [modifier][Modifier.Element] that is used to pass in a [FocusRequester] that can be used to
 * request focus state changes.
 *
 * @sample androidx.compose.ui.samples.RequestFocusSample
 *
 * @see FocusRequester
 * @see Modifier.focusRequester
 */
@Deprecated("Use FocusRequesterModifierNode instead")
@JvmDefaultWithCompatibility
interface FocusRequesterModifier : Modifier.Element {
    /**
     * An instance of [FocusRequester], that can be used to request focus state changes.
     *
     * @sample androidx.compose.ui.samples.RequestFocusSample
     */
    val focusRequester: FocusRequester
}

/**
 * Add this modifier to a component to request changes to focus.
 *
 * @sample androidx.compose.ui.samples.RequestFocusSample
 */
fun Modifier.focusRequester(focusRequester: FocusRequester): Modifier =
    this then FocusRequesterElement(focusRequester)

@OptIn(ExperimentalComposeUiApi::class)
private data class FocusRequesterElement(
    val focusRequester: FocusRequester
) : ModifierNodeElement<FocusRequesterModifierNodeImpl>() {
    override fun create() = FocusRequesterModifierNodeImpl(focusRequester)

    override fun update(node: FocusRequesterModifierNodeImpl) = node.apply {
        focusRequester.focusRequesterNodes -= this
        focusRequester = this@FocusRequesterElement.focusRequester
        focusRequester.focusRequesterNodes += this
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "focusRequester"
        properties["focusRequester"] = focusRequester
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class FocusRequesterModifierNodeImpl(
    var focusRequester: FocusRequester
) : FocusRequesterModifierNode, Modifier.Node() {
    override fun onAttach() {
        super.onAttach()
        focusRequester.focusRequesterNodes += this
    }

    override fun onDetach() {
        focusRequester.focusRequesterNodes -= this
        super.onDetach()
    }
}
