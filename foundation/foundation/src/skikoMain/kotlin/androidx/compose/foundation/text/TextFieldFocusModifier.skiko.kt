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

package androidx.compose.foundation.text

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager

// TODO: [1.4 Update] implement it properly for platforms

/**
 * TextField consumes the D-pad keys, due to which we can't move focus once a TextField is focused.
 * To prevent this, this modifier can be used to intercept D-pad key events before they are sent to
 * the TextField. It intercepts and handles the directional (Up, Down, Left, Right & Center) D-pad
 * key presses, to move the focus between TextField and other focusable items on the screen.
 *
 * TODO: To be implemented if there's any specific handling required for desktop platform
 */
internal actual fun Modifier.interceptDPadAndMoveFocus(
    state: TextFieldState,
    focusManager: FocusManager
): Modifier = this