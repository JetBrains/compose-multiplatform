/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Demonstration of how various press/tap gesture interact together in a nested fashion.
 */
@Composable
fun NestedPressingDemo() {
    Column {
        Text(
            "Demonstrates correct behavior of a nested set of regions that each respond with " +
                "press indication, tap, double tap, and long press."
        )
        Text(
            "Press indication is a darker version of the current color.  Tap changes colors in " +
                "one direction.  Double tap changes colors in the opposite direction. Long " +
                "press resets the color to white. Based on the implementations of each " +
                "gesture detector, you should only be able to interact with one box at a time."
        )
        PressableContainer(Modifier.fillMaxSize()) {
            PressableContainer(Modifier.padding(48.dp).fillMaxSize()) {
                PressableContainer(Modifier.padding(48.dp).fillMaxSize())
            }
        }
    }
}

@Composable
private fun PressableContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val defaultColor = DefaultBackgroundColor
    val pressedColor = PressedColor

    val currentColor = remember { mutableStateOf(defaultColor) }
    val pressed = remember { mutableStateOf(false) }

    val onStart: (Any) -> Unit = {
        pressed.value = true
    }

    val onStop: () -> Unit = {
        pressed.value = false
    }

    val onLongPress = { _: Offset ->
        pressed.value = false
        currentColor.value = defaultColor
    }

    val onTap: (Offset) -> Unit = {
        currentColor.value = currentColor.value.next()
    }

    val onDoubleTap = { _: Offset ->
        currentColor.value = currentColor.value.prev()
    }

    val color = if (pressed.value) {
        pressedColor.compositeOver(currentColor.value)
    } else {
        currentColor.value
    }

    val gestureDetectors =
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onStart.invoke(it)
                        val success = tryAwaitRelease()
                        if (success) onStop.invoke() else onStop.invoke()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = onTap,
                    onDoubleTap = onDoubleTap,
                    onLongPress = onLongPress
                )
            }

    Box(
        modifier
            .then(gestureDetectors)
            .background(color)
            .border(BorderStroke(2.dp, BorderColor))
            .padding(2.dp)
    ) { content() }
}