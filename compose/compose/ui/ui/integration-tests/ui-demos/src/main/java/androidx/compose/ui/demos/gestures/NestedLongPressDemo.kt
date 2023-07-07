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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Demonstration of how various press/tap gesture interact together in a nested fashion.
 */
@Composable
fun NestedLongPressDemo() {
    Column {
        Text(
            "Demonstrates interaction between nested pointerInput modifiers in an " +
                "edge case that is nevertheless supported (normally regions will be separated" +
                " by a pressIndicatorGestureFilter, but here they are not)."
        )
        Text(
            "This just demonstrates the interaction between directly nested " +
                "pointerInputs with detectTapGestures."
        )
        LongPressableContainer(Modifier.fillMaxSize()) {
            LongPressableContainer(Modifier.padding(48.dp).fillMaxSize()) {
                LongPressableContainer(Modifier.padding(48.dp).fillMaxSize()) {}
            }
        }
    }
}

@Composable
private fun LongPressableContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val defaultColor = DefaultBackgroundColor
    val pressedColor = PressedColor

    val currentColor = remember { mutableStateOf(defaultColor) }
    val pressed = remember { mutableStateOf(false) }

    val onLongPress: (Offset) -> Unit = {
        currentColor.value = currentColor.value.next()
    }

    val color = if (pressed.value) {
        pressedColor.compositeOver(currentColor.value)
    } else {
        currentColor.value
    }

    Box(
        modifier
            .pointerInput(Unit) { detectTapGestures(onLongPress = onLongPress) }
            .background(color)
            .border(BorderStroke(2.dp, BorderColor))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}
