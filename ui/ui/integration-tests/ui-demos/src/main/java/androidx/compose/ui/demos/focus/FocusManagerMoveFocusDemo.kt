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

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement.SpaceEvenly
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FocusManagerMoveFocusDemo() {
    val focusManager = LocalFocusManager.current
    Column {
        Text(
            text = "Use the buttons to move focus",
            modifier = Modifier.align(CenterHorizontally).padding(vertical = 10.dp)
        )
        Row(Modifier.fillMaxWidth(), SpaceEvenly) {
            Button(onClick = { focusManager.moveFocus(Up) }) { Text("Up") }
        }
        Row(Modifier.fillMaxWidth(), SpaceEvenly) {
            Button(onClick = { focusManager.moveFocus(Left) }) { Text("Left") }
            Button(onClick = { focusManager.moveFocus(Right) }) { Text("Right") }
        }
        Row(Modifier.fillMaxWidth(), SpaceEvenly) {
            Button(onClick = { focusManager.moveFocus(Down) }) { Text("Down") }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), SpaceEvenly) {
            Button(onClick = { focusManager.moveFocus(Previous) }) { Text("Previous") }
            Button(onClick = { focusManager.moveFocus(Next) }) { Text("Next") }
        }
        Column(Modifier.fillMaxSize(), SpaceEvenly) {
            val (item1, item2, item3, item4) = FocusRequester.createRefs()
            Row(Modifier.fillMaxWidth(), SpaceEvenly) {
                FocusableText(
                    text = "1",
                    modifier = Modifier
                        .focusRequester(item1)
                        .focusProperties {
                            previous = item4
                            next = item2
                            right = item2
                            down = item3
                        }
                )
                FocusableText(
                    text = "2",
                    modifier = Modifier
                        .focusRequester(item2)
                        .focusProperties {
                            previous = item1
                            next = item3
                            left = item1
                            down = item4
                        }
                )
            }
            Row(Modifier.fillMaxWidth(), SpaceEvenly) {
                FocusableText(
                    text = "3",
                    modifier = Modifier
                        .focusRequester(item3)
                        .focusProperties {
                            previous = item2
                            next = item4
                            right = item4
                            up = item1
                        }
                )
                FocusableText(
                    text = "4",
                    modifier = Modifier
                        .focusRequester(item4)
                        .focusProperties {
                            previous = item3
                            next = item1
                            left = item3
                            up = item2
                        }
                )
            }
            DisposableEffect(Unit) {
                item1.requestFocus()
                onDispose { }
            }
        }
    }
}

@Composable
private fun FocusableText(text: String, modifier: Modifier = Modifier) {
    var color by remember { mutableStateOf(Black) }
    val focusRequester = remember { FocusRequester() }
    Text(
        modifier = modifier
            .border(width = 1.dp, color = Black)
            .requiredWidth(50.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { color = if (it.isFocused) Green else Black }
            .focusTarget()
            .pointerInput(Unit) { detectTapGestures { focusRequester.requestFocus() } },
        text = text,
        fontSize = 40.sp,
        textAlign = TextAlign.Center,
        color = color
    )
}
