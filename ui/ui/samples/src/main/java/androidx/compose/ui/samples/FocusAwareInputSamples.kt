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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.KeyEventType.Companion.Unknown
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
@Sampled
@Composable
fun KeyEventSample() {
    // When the inner Box is focused, and the user presses a key, the key goes down the hierarchy
    // and then back up to the parent. At any stage you can stop the propagation by returning
    // true to indicate that you consumed the event.
    Box(
        Modifier
            .onPreviewKeyEvent { keyEvent1 -> false }
            .onKeyEvent { keyEvent4 -> false }
    ) {
        Box(
            Modifier
                .onPreviewKeyEvent { keyEvent2 -> false }
                .onKeyEvent { keyEvent3 -> false }
                .focusable()
        )
    }
}

@Sampled
@Composable
fun KeyEventTypeSample() {
    Box(
        Modifier
            .onKeyEvent {
                when (it.type) {
                    KeyUp -> println(" KeyUp Pressed")
                    KeyDown -> println(" KeyUp Pressed")
                    Unknown -> println("Unknown key type")
                    else -> println("New KeyTpe (For Future Use)")
                }
                false
            }
            .focusable()
    )
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun KeyEventIsAltPressedSample() {
    Box(
        Modifier
            .onKeyEvent {
                if (it.isAltPressed && it.key == Key.A) {
                    println("Alt + A is pressed")
                    true
                } else {
                    false
                }
            }
            .focusable()
    )
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun KeyEventIsCtrlPressedSample() {
    Box(
        Modifier
            .onKeyEvent {
                if (it.isCtrlPressed && it.key == Key.A) {
                    println("Ctrl + A is pressed")
                    true
                } else {
                    false
                }
            }
            .focusable()
    )
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun KeyEventIsMetaPressedSample() {
    Box(
        Modifier
            .onKeyEvent {
                if (it.isMetaPressed && it.key == Key.A) {
                    println("Meta + A is pressed")
                    true
                } else {
                    false
                }
            }
            .focusable()
    )
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun KeyEventIsShiftPressedSample() {
    Box(
        Modifier
            .onKeyEvent {
                if (it.isShiftPressed && it.key == Key.A) {
                    println("Shift + A is pressed")
                    true
                } else {
                    false
                }
            }
            .focusable()
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun RotaryEventSample() {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .onRotaryScrollEvent {
                coroutineScope.launch {
                    scrollState.scrollTo((scrollState.value +
                        it.verticalScrollPixels).roundToInt())
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        repeat(100) {
            Text(
                text = "item $it",
                modifier = Modifier.align(CenterHorizontally),
                color = White,
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun PreRotaryEventSample() {
    MaterialTheme(colors = darkColors()) {
        val rowScrollState = rememberScrollState()
        val columnScrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        val focusRequester = remember { FocusRequester() }
        var interceptScroll by remember { mutableStateOf(false) }
        Column(
            Modifier
                .onPreRotaryScrollEvent {
                    // You can intercept an event before it is sent to the child.
                    if (interceptScroll) {
                        coroutineScope.launch {
                            rowScrollState.scrollBy(it.horizontalScrollPixels)
                        }
                        // return true to consume this event.
                        true
                    } else {
                        // return false to ignore this event and continue propagation to the child.
                        false
                    }
                }
                .onRotaryScrollEvent {
                    // If the child does not use the scroll, we get notified here.
                    coroutineScope.launch {
                        rowScrollState.scrollBy(it.horizontalScrollPixels)
                    }
                    true
                }
        ) {
            Row(
                modifier = Modifier.align(CenterHorizontally),
                verticalAlignment = CenterVertically
            ) {
                Text(
                    modifier = Modifier.width(70.dp),
                    text = if (interceptScroll) "Row" else "Column",
                    style = TextStyle(color = White)
                )
                Switch(
                    checked = interceptScroll,
                    onCheckedChange = { interceptScroll = it },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rowScrollState)
            ) {
                repeat(100) {
                    Text(
                        text = "row item $it ",
                        modifier = Modifier.align(CenterVertically),
                        color = White,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(columnScrollState)
                    .onRotaryScrollEvent {
                        coroutineScope.launch {
                            columnScrollState.scrollBy(it.verticalScrollPixels)
                        }
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable(),
            ) {
                repeat(100) {
                    Text(
                        text = "column item $it",
                        modifier = Modifier.align(CenterHorizontally),
                        color = White,
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}
