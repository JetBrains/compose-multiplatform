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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Simple [awaitTouchSlopOrCancellation] demo.
 */
@Composable
fun DragSlopExceededGestureFilterDemo() {

    val verticalColor = Color(0xfff44336)
    val horizontalColor = Color(0xff2196f3)

    val alternativeColor = remember { mutableStateOf(false) }

    val color = if (alternativeColor.value) verticalColor else horizontalColor

    Column {
        Text(
            "Demonstrates functionality of Modifier.dragSlopExceededGestureFilter, which calls " +
                "its callback when touch slop has been exceeded by the average distance" +
                " change of all pointers."
        )
        Text(
            "When red, a drag on the box will turn the box blue only when you drag up or down on" +
                " the screen.  When blue, a drag on the box will turn the box red when you" +
                " drag to the right or left."
        )
        Box(
            Modifier.fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .size(192.dp)
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                alternativeColor.value = !alternativeColor.value
                                change.consume()
                            }
                        }
                    }
                }
                .background(color)
        )
    }
}
