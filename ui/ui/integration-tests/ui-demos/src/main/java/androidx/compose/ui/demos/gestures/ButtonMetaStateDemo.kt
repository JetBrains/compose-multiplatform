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

package androidx.compose.ui.demos.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.isCapsLockOn
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isForwardPressed
import androidx.compose.ui.input.pointer.isFunctionPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isNumLockOn
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isScrollLockOn
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isSymPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Demo to show the state of buttons and meta keys
 */
@Composable
fun ButtonMetaStateDemo() {
    var control by remember { mutableStateOf(false) }
    var alt by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(false) }
    var meta by remember { mutableStateOf(false) }
    var sym by remember { mutableStateOf(false) }
    var function by remember { mutableStateOf(false) }
    var numLock by remember { mutableStateOf(false) }
    var scrollLock by remember { mutableStateOf(false) }
    var capsLock by remember { mutableStateOf(false) }

    var primary by remember { mutableStateOf(false) }
    var secondary by remember { mutableStateOf(false) }
    var tertiary by remember { mutableStateOf(false) }
    var back by remember { mutableStateOf(false) }
    var forward by remember { mutableStateOf(false) }

    fun trippedModifier(isActive: Boolean, color: Color) =
        Modifier.background(if (isActive) color else Color.Transparent).fillMaxWidth()

    Column(
        Modifier.pointerInput(Unit) {
            forEachGesture {
                awaitPointerEventScope {
                    do {
                        val event = awaitPointerEvent()
                        val metaState = event.keyboardModifiers
                        control = metaState.isCtrlPressed
                        alt = metaState.isAltPressed
                        shift = metaState.isShiftPressed
                        meta = metaState.isMetaPressed
                        sym = metaState.isSymPressed
                        function = metaState.isFunctionPressed
                        numLock = metaState.isNumLockOn
                        scrollLock = metaState.isScrollLockOn
                        capsLock = metaState.isCapsLockOn

                        val buttons = event.buttons
                        primary = buttons.isPrimaryPressed
                        secondary = buttons.isSecondaryPressed
                        tertiary = buttons.isTertiaryPressed
                        back = buttons.isBackPressed
                        forward = buttons.isForwardPressed
                    } while (event.changes.any { it.pressed })
                    // In the future, hover events should work also, but it isn't
                    // implemented yet.
                    control = false
                    alt = false
                    shift = false
                    meta = false
                    sym = false
                    function = false
                    numLock = false
                    scrollLock = false
                    capsLock = false
                    primary = false
                    secondary = false
                    tertiary = false
                    back = false
                    forward = false
                }
            }
        }
    ) {
        Text("Demonstrates which buttons and meta keys are active during pointer input")
        Row {
            Column(Modifier.weight(1f)) {
                Text("Shift", trippedModifier(shift, Color.Blue))
                Text("Control", trippedModifier(control, Color.Green))
                Text("Alt", trippedModifier(alt, Color.Yellow))
                Text("Meta", trippedModifier(meta, Color.Red))
                Text("Sym", trippedModifier(sym, Color.Cyan))
                Text("Function", trippedModifier(function, Color.Magenta))
                Text("Num Lock", trippedModifier(numLock, Color.DarkGray))
                Text("Scroll Lock", trippedModifier(scrollLock, Color.Gray))
                Text("Caps Lock", trippedModifier(capsLock, Color.LightGray))
            }
            Column(Modifier.weight(1f)) {
                Text("Left", trippedModifier(primary, Color.Red))
                Text("Right", trippedModifier(secondary, Color.Yellow))
                Text("Middle", trippedModifier(tertiary, Color.Green))
                Text("Back", trippedModifier(back, Color.Blue))
                Text("Forward", trippedModifier(forward, Color.Magenta))
            }
        }
    }
}
