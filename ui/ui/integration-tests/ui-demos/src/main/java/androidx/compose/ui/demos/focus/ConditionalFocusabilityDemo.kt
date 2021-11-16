/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.demos.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConditionalFocusabilityDemo() {
    val localInputModeManager = LocalInputModeManager.current
    val (item1, item2, item3, item4) = remember { FocusRequester.createRefs() }
    Column {
        Text(
            """
             The items here are focusable. Use the
             keyboard or DPad to move focus among them.

             The 1st item is focusable in all modes.
             Notice that when you touch the screen it
             does not lose focus like the other items.

             The 2nd item's focusability can be
             controlled by using the button next to it.

             The 3rd item is not focusable in touch mode.

             The 4th item is not focusable in touch mode,
             but clicking on it will request the system
             to switch to keyboard mode, and then call
             request focus.
             """.trimIndent()
        )
        Text(
            text = "Focusable in all modes",
            modifier = Modifier
                .focusAwareBackground()
                .focusRequester(item1)
                .pointerInput(item1) { detectTapGestures { item1.requestFocus() } }
                .focusable()
        )
        Row {
            var item2active by remember { mutableStateOf(false) }
            Text(
                text = "focusable item that is " +
                    "${if (item2active) "activated" else "deactivated"}",
                modifier = Modifier
                    .focusAwareBackground()
                    .focusRequester(item2)
                    .pointerInput(item2) { detectTapGestures { item2.requestFocus() } }
                    .focusProperties { canFocus = item2active }
                    .focusable()
            )
            Button(onClick = { item2active = !item2active }) {
                Text("${if (item2active) "deactivate" else "activate"} item 2")
            }
        }
        Text(
            text = "Focusable in keyboard mode",
            modifier = Modifier
                .focusAwareBackground()
                .focusRequester(item3)
                .pointerInput(item3) { detectTapGestures { item3.requestFocus() } }
                .focusProperties { canFocus = localInputModeManager.inputMode == Keyboard }
                .focusable()
        )
        Text(
            text = "Request focus by touch",
            modifier = Modifier
                .focusAwareBackground()
                .focusRequester(item4)
                .pointerInput(item4) {
                    detectTapGestures {
                        if (localInputModeManager.requestInputMode(Keyboard)) {
                            item4.requestFocus()
                        }
                    }
                }
                .focusProperties { canFocus = localInputModeManager.inputMode == Keyboard }
                .focusable()
        )
    }
}

private fun Modifier.focusAwareBackground() = composed {
    var color by remember { mutableStateOf(Gray) }
    Modifier
        .padding(10.dp)
        .size(150.dp, 50.dp)
        .background(color)
        .onFocusChanged { color = if (it.isFocused) Red else Gray }
}