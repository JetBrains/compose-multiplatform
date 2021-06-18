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
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
