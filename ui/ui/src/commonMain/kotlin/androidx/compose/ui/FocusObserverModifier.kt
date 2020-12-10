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

import androidx.compose.ui.focus.FocusState

// TODO(b/174728671): Remove this deprecated API after Alpha 09
/**
 * A [modifier][Modifier.Element] that can be used to observe focus state changes.
 */
@Deprecated(
    "Please use FocusEventModifier",
    replaceWith = ReplaceWith(
        "FocusEventModifier",
        "androidx.compose.ui.focus.FocusEventModifier"
    )
)
interface FocusObserverModifier : Modifier.Element {
    /**
     * A callback that is called whenever focus state changes.
     */
    val onFocusChange: (FocusState) -> Unit
}

// TODO(b/174728671): Remove this deprecated API after Alpha 09
/**
 * Add this modifier to a component to observe focus state changes.
 */
@Suppress("unused", "UNUSED_PARAMETER")
@Deprecated(
    message = "Please use either onFocusChanged or onFocusEvent",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith(
        "onFocusChanged(onFocusChange)",
        "androidx.compose.ui.focus.onFocusChanged"
    )
)
fun Modifier.focusObserver(onFocusChange: (FocusState) -> Unit): Modifier {
    return Modifier
}
