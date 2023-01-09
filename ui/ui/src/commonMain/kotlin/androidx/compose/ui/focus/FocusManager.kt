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

import androidx.compose.ui.internal.JvmDefaultWithCompatibility

@JvmDefaultWithCompatibility
interface FocusManager {
    /**
     * Call this function to clear focus from the currently focused component, and set the focus to
     * the root focus modifier.
     *
     *  @param force: Whether we should forcefully clear focus regardless of whether we have
     *  any components that have Captured focus.
     *
     *  @sample androidx.compose.ui.samples.ClearFocusSample
     */
    fun clearFocus(force: Boolean = false)

    /**
     * Moves focus in the specified [direction][FocusDirection].
     *
     * If you are not satisfied with the default focus order, consider setting a custom order using
     * [Modifier.focusProperties()][focusProperties].
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     *
     * @sample androidx.compose.ui.samples.MoveFocusSample
     */
    fun moveFocus(focusDirection: FocusDirection): Boolean
}
