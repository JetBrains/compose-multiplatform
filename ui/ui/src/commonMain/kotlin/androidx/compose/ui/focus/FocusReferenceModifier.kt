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

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A [modifier][Modifier.Element] that can be used to pass in a [FocusReference] that can be used
 * to request focus state changes.
 *
 * @see FocusReference
 */
interface FocusReferenceModifier : Modifier.Element {
    /**
     * An instance of [FocusReference], that can be used to request focus state changes.
     */
    val focusReference: FocusReference
}

internal class FocusReferenceModifierImpl(
    override val focusReference: FocusReference,
    inspectorInfo: InspectorInfo.() -> Unit
) : FocusReferenceModifier, InspectorValueInfo(inspectorInfo)

/**
 * Add this modifier to a component to observe changes to focus state.
 */
fun Modifier.focusReference(focusReference: FocusReference): Modifier {
    return this.then(
        FocusReferenceModifierImpl(
            focusReference = focusReference,
            inspectorInfo = debugInspectorInfo {
                name = "focusReference"
                properties["focusReference"] = focusReference
            }
        )
    )
}
