/*
 * Copyright 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.ui.demos.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ExplicitEnterExitWithCustomFocusEnterExitDemo() {
    val (top, row, item1, item2, item3, bottom) = remember { FocusRequester.createRefs() }
    val inputModeManager = LocalInputModeManager.current
    Column {
        Text("""
            Click on the top button to request focus on the row, which focuses on item 2.
            Entering the row from the top focuses on item 1.
            Entering the row from the bottom focusses on item 3.
            Exiting the row from the left focuses on the top button.
            Exiting the row from right focuses on the bottom button.
            """.trimIndent()
        )
        Button(
            onClick = {
                // If users click on the button instead of using the keyboard
                // to press the button, we manually switch to Keyboard mode,
                // since buttons are not focusable in touch mode.
                if (inputModeManager.inputMode != InputMode.Keyboard) {
                    inputModeManager.requestInputMode(InputMode.Keyboard)
                }
                row.requestFocus()
            },
            modifier = Modifier.focusRequester(top)
        ) {
            Text("Top Button")
        }

        Row(
            Modifier
                .focusRequester(row)
                .focusProperties {
                    enter = {
                        when (it) {
                            Down -> item1
                            Enter -> item2
                            Up -> item3
                            else -> Default
                        }
                    }
                    exit = {
                        when (it) {
                            Left -> top
                            Right -> bottom
                            else -> Default
                        }
                    }
                }
                .focusGroup()
        ) {
            Button({}, Modifier.focusRequester(item1)) { Text("1") }
            Button({}, Modifier.focusRequester(item2)) { Text("2") }
            Button({}, Modifier.focusRequester(item3)) { Text("3") }
        }
        Button({}, Modifier.focusRequester(bottom)) { Text("Bottom Button") }
    }
}
