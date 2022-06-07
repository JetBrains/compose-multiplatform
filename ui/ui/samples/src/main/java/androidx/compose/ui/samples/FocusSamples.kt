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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun FocusableSample() {
    var color by remember { mutableStateOf(Black) }
    Box(
        Modifier
            .border(2.dp, color)
            // The onFocusChanged should be added BEFORE the focusable that is being observed.
            .onFocusChanged { color = if (it.isFocused) Green else Black }
            .focusable()
    )
}

@Sampled
@Composable
fun FocusableSampleUsingLowerLevelFocusTarget() {
    var color by remember { mutableStateOf(Black) }
    Box(
        Modifier
            .border(2.dp, color)
            // The onFocusChanged should be added BEFORE the focusTarget that is being observed.
            .onFocusChanged { color = if (it.isFocused) Green else Black }
            .focusTarget()
    )
}

@Sampled
@Composable
fun CaptureFocusSample() {
    val focusRequester = remember { FocusRequester() }
    var value by remember { mutableStateOf("apple") }
    var borderColor by remember { mutableStateOf(Transparent) }
    TextField(
        value = value,
        onValueChange = {
            value = it.apply {
                if (length > 5) focusRequester.captureFocus() else focusRequester.freeFocus()
            }
        },
        modifier = Modifier
            .border(2.dp, borderColor)
            .focusRequester(focusRequester)
            .onFocusChanged { borderColor = if (it.isCaptured) Red else Transparent }
    )
}

@Sampled
@Composable
fun RequestFocusSample() {
    val focusRequester = remember { FocusRequester() }
    var color by remember { mutableStateOf(Black) }
    Box(
        Modifier
            .clickable { focusRequester.requestFocus() }
            .border(2.dp, color)
            // The focusRequester should be added BEFORE the focusable.
            .focusRequester(focusRequester)
            // The onFocusChanged should be added BEFORE the focusable that is being observed.
            .onFocusChanged { color = if (it.isFocused) Green else Black }
            .focusable()
    )
}

@Sampled
@Composable
fun ClearFocusSample() {
    val focusManager = LocalFocusManager.current
    Column(Modifier.clickable { focusManager.clearFocus() }) {
        Box(Modifier.focusable().size(100.dp))
        Box(Modifier.focusable().size(100.dp))
        Box(Modifier.focusable().size(100.dp))
    }
}

@Sampled
@Composable
fun MoveFocusSample() {
    val focusManager = LocalFocusManager.current
    Column {
        Row {
            Box(Modifier.focusable())
            Box(Modifier.focusable())
        }
        Row {
            Box(Modifier.focusable())
            Box(Modifier.focusable())
        }
        Button(onClick = { focusManager.moveFocus(FocusDirection.Right) }) { Text("Right") }
        Button(onClick = { focusManager.moveFocus(FocusDirection.Left) }) { Text("Left") }
        Button(onClick = { focusManager.moveFocus(FocusDirection.Up) }) { Text("Up") }
        Button(onClick = { focusManager.moveFocus(FocusDirection.Down) }) { Text("Down") }
    }
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun CreateFocusRequesterRefsSample() {
    val (item1, item2, item3, item4) = remember { FocusRequester.createRefs() }
    Column {
        Box(Modifier.focusRequester(item1).focusable())
        Box(Modifier.focusRequester(item2).focusable())
        Box(Modifier.focusRequester(item3).focusable())
        Box(Modifier.focusRequester(item4).focusable())
    }
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun CustomFocusOrderSample() {
    Column(Modifier.fillMaxSize(), Arrangement.SpaceEvenly) {
        val (item1, item2, item3, item4) = remember { FocusRequester.createRefs() }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            Box(
                Modifier
                    .focusRequester(item1)
                    .focusProperties {
                        next = item2
                        right = item2
                        down = item3
                        previous = item4
                    }
                    .focusable()
            )
            Box(
                Modifier
                    .focusRequester(item2)
                    .focusProperties {
                        next = item3
                        right = item1
                        down = item4
                        previous = item1
                    }
                    .focusable()
            )
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            Box(
                Modifier
                    .focusRequester(item3)
                    .focusProperties {
                        next = item4
                        right = item4
                        up = item1
                        previous = item2
                    }
            )
            Box(
                Modifier
                    .focusRequester(item4)
                    .focusProperties {
                        next = item1
                        left = item3
                        up = item2
                        previous = item3
                    }
            )
        }
    }
}

@Sampled
@Composable
fun FocusPropertiesSample() {
    Column {
        // Always focusable.
        Box(modifier = Modifier
            .focusProperties { canFocus = true }
            .focusTarget()
        )
        // Only focusable in non-touch mode.
        val inputModeManager = LocalInputModeManager.current
        Box(modifier = Modifier
            .focusProperties { canFocus = inputModeManager.inputMode != Touch }
            .focusTarget()
        )
    }
}
