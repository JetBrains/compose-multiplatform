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

package androidx.compose.ui.demos.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TouchModeDemo() {
    val inputManager = LocalInputModeManager.current
    Column(verticalArrangement = Arrangement.SpaceEvenly) {
        Text(
            "Touch anywhere on the screen to put the system in touch mode, and press a key on a " +
                "hardware keyboard to put the system in key mode."
        )
        Text(
            text = "Currently in ${inputManager.inputMode} mode.",
            color = if (inputManager.inputMode == Touch) Blue else Red,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            "If you don't have a physical keyboard, you can click this button to exit" +
                " touch mode programmatically."
        )
        Button(
            onClick = { inputManager.requestInputMode(Keyboard) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Exit touch mode")
        }
    }
}