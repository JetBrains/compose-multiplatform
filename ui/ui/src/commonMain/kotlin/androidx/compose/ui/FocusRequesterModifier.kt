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

package androidx.compose.ui

import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A [modifier][Modifier.Element] that can be used to pass in a [FocusRequester] that can be used
 * to request focus state changes.
 *
 * @see FocusRequester
 */
@ExperimentalFocus
interface FocusRequesterModifier : Modifier.Element {
    /**
     * An instance of [FocusRequester], that can be used to request focus state changes.
     */
    val focusRequester: FocusRequester
}

@OptIn(ExperimentalFocus::class)
internal class FocusRequesterModifierImpl(
    override val focusRequester: FocusRequester,
    inspectorInfo: InspectorInfo.() -> Unit
) : FocusRequesterModifier, InspectorValueInfo(inspectorInfo)

/**
 * Add this modifier to a component to observe changes to focus state.
 */
@ExperimentalFocus
fun Modifier.focusRequester(focusRequester: FocusRequester): Modifier {
    return this.then(
        FocusRequesterModifierImpl(
            focusRequester = focusRequester,
            inspectorInfo = debugInspectorInfo {
                name = "focusRequester"
                properties["focusRequester"] = focusRequester
            }
        )
    )
}
