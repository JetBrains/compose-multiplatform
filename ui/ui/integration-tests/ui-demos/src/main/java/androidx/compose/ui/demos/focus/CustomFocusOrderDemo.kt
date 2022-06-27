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

package androidx.compose.ui.demos.focus

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement.SpaceEvenly
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CustomFocusOrderDemo() {
    Column {
        Row {
            Text(
                "Use the arrow keys to move focus left/right/up/down."
            )
        }
        Column(Modifier.fillMaxSize(), SpaceEvenly) {
            val (item1, item2, item3, item4) = remember { FocusRequester.createRefs() }
            var wrapAround by remember { mutableStateOf(false) }
            Row {
                Text("Wrap around focus search")
                Switch(checked = wrapAround, onCheckedChange = { wrapAround = !wrapAround })
            }
            Row(Modifier.fillMaxWidth(), SpaceEvenly) {
                    FocusableText(
                        text = "1",
                        modifier = Modifier
                            .focusRequester(item1)
                            .focusProperties {
                                left = if (wrapAround) item2 else Default
                                up = if (wrapAround) item3 else Default
                            }
                    )
                        FocusableText(
                        text = "2",
                        modifier = Modifier
                            .focusRequester(item2)
                            .focusProperties {
                                right = if (wrapAround) item1 else Default
                                up = if (wrapAround) item4 else Default
                            }
                    )
                }
                Row(Modifier.fillMaxWidth(), SpaceEvenly) {
                    FocusableText(
                        text = "3",
                        modifier = Modifier
                            .focusRequester(item3)
                            .focusProperties {
                                left = if (wrapAround) item4 else Default
                                down = if (wrapAround) item1 else Default
                            }
                    )
                    FocusableText(
                        text = "4",
                        modifier = Modifier
                            .focusRequester(item4)
                            .focusProperties {
                                right = if (wrapAround) item3 else Default
                                down = if (wrapAround) item2 else Default
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
