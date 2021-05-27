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

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.ModifiedFocusNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.NoInspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A [Modifier.Element] that wraps makes the modifiers on the right into a Focusable. Use a
 * different instance of [FocusModifier] for each focusable component.
 */
internal class FocusModifier(
    initialFocus: FocusStateImpl,
    // TODO(b/172265016): Make this a required parameter and remove the default value.
    //  Set this value in AndroidComposeView, and other places where we create a focus modifier
    //  using this internal constructor.
    inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo
) : Modifier.Element, InspectorValueInfo(inspectorInfo) {

    // TODO(b/188684110): Move focusState and focusedChild to ModiedFocusNode and make this
    //  modifier stateless.

    var focusState: FocusStateImpl = initialFocus

    var focusedChild: ModifiedFocusNode? = null

    lateinit var focusNode: ModifiedFocusNode
}

/**
 * Add this modifier to a component to make it focusable.
 */
fun Modifier.focusTarget(): Modifier = composed(debugInspectorInfo { name = "focusTarget" }) {
    remember { FocusModifier(Inactive) }
}

/**
 * Add this modifier to a component to make it focusable.
 */
@Deprecated(
    "Replaced by focusTarget",
    ReplaceWith("focusTarget()", "androidx.compose.ui.focus.focusTarget")
)
fun Modifier.focusModifier(): Modifier = composed(debugInspectorInfo { name = "focusModifier" }) {
    remember { FocusModifier(Inactive) }
}
