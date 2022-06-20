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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.desktop.examples.mouseclicks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalFoundationApi::class)
fun main() {
    var isCtrlPressed: Boolean by mutableStateOf(false)

    singleWindowApplication(
        title = "Desktop Mouse Clicks",
        state = WindowState(width = 512.dp, height = 425.dp),
        onPreviewKeyEvent = {
            isCtrlPressed = it.isCtrlPressed
            false
        }
    ) {
        var enabled by remember { mutableStateOf(true) }

        Column {
            Box(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)) {
                Row(modifier = Modifier.align(Alignment.Center)) {
                    Checkbox(enabled, onCheckedChange = {
                        enabled = it
                    })
                    Text("enabled all", modifier = Modifier.padding(top = 16.dp, start = 8.dp))
                }
            }


            Row {
                Column(modifier = Modifier.padding(25.dp)) {
                    Text("onClick")

                    val interactionSource1 = remember { MutableInteractionSource() }

                    Box(modifier = Modifier.size(200.dp).background(Color.LightGray)
                        .onClick(
                            enabled = enabled,
                            interactionSource = interactionSource1,
                            matcher = PointerMatcher.mouse(PointerButton.Secondary),
                            keyboardModifiers = { isAltPressed },
                            onLongClick = {
                                println("Gray: Alt + Long Right Click")
                            }
                        ) {
                            println("Gray: Alt + Right Click")
                        }
                        .onClick(enabled = enabled, interactionSource = interactionSource1) {
                            println("Gray: Left Click")
                        }.indication(interactionSource1, LocalIndication.current)
                    ) {
                        Column {
                            Text("Left Click, Alt + Right Click, Alt + Right LongClick")

                            val interactionSource2 = remember { MutableInteractionSource() }

                            Box(modifier = Modifier.padding(40.dp).size(100.dp).background(Color.Red)
                                .onClick(
                                    enabled = enabled,
                                    interactionSource = interactionSource2,
                                    matcher = PointerMatcher.mouse(PointerButton.Primary),
                                    keyboardModifiers = { this.isShiftPressed },
                                    onDoubleClick = {
                                        println("Red: Shift + Left DoubleClick")
                                    }
                                ) {
                                    println("Red: Shift + Left Click")
                                }
                                .onClick(
                                    enabled = enabled,
                                    interactionSource = interactionSource2,
                                    matcher = PointerMatcher.mouse(PointerButton.Secondary)
                                ) {
                                    println("Red: Right Click")
                                }.indication(interactionSource2, LocalIndication.current)
                            ) {
                                Text("Right Click, Shift + Left Click, Shift + Left DoubleClick")
                            }
                        }

                    }
                }

                var isDragging by remember { mutableStateOf(false) }
                val isDraggingCtrlPressed = derivedStateOf {  isDragging && isCtrlPressed }

                println("Recomposing now")

                Column(modifier = Modifier.padding(25.dp)) {
                    Text("onDrag")
                    var offset1 by remember { mutableStateOf(Offset.Zero) }
                    Box(modifier = Modifier.offset { IntOffset(offset1.x.toInt(), offset1.y.toInt()) }
                        .size(100.dp).background(Color.Blue).pointerInput(Unit) {
                            detectDragGestures(
                                matcher = PointerMatcher.mouse(PointerButton.Secondary),
                                onDragStart = {
                                    isDragging = true
                                    println("Blue: Start, offset=$it")
                                },
                                onDragEnd = {
                                    isDragging = false
                                    println("Blue: End")
                                },
                                onDragCancel = {
                                    isDragging = false
                                }
                            ) {
                                offset1 += it * if (isDraggingCtrlPressed.value) 2.0f else 1.0f
                            }
                        }
                    ) {
                        Text("Use Right Mouse")
                    }

                    var offset2 by remember { mutableStateOf(Offset.Zero) }
                    Box(
                        modifier = Modifier.offset { IntOffset(offset2.x.toInt(), offset2.y.toInt()) }
                            .size(100.dp).background(Color.Gray)
                            .onDrag(
                                enabled = enabled,
                                onDragStart = { o ->
                                    isDragging = true
                                    println("Gray: Start, offset=$o")
                                },
                                onDragEnd = {
                                    isDragging = false
                                    println("Gray: End")
                                },
                                onDragCancel = {
                                    isDragging = false
                                }
                            ) {
                                offset2 += it * if (isDraggingCtrlPressed.value) 2.0f else 1.0f
                            }) {
                        Text("Use Left Mouse")
                    }
                }
            }
        }
    }
}
