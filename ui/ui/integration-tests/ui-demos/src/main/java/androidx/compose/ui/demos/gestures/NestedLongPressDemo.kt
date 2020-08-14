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
import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.longPressGestureFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp

/**
 * Demonstration of how various press/tap gesture interact together in a nested fashion.
 */
@Composable
fun NestedLongPressDemo() {
    Column {
        Text(
            "Demonstrates interaction between nested longPressGestureFitlers  in an " +
                    "edge case that is nevertheless supported (normally regions will be separated" +
                    " by a pressIndicatorGestureFilter, but here they are not)."
        )
        Text(
            "This just demonstrates the interaction between directly nested " +
                    "longPressGestureFilters."
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
    children: @Composable () -> Unit
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
        modifier.longPressGestureFilter(onLongPress),
        backgroundColor = color,
        gravity = ContentGravity.Center,
        border = BorderStroke(2.dp, BorderColor),
        children = children
    )
}
