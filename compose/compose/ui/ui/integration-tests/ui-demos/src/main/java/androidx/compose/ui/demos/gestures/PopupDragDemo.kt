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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup

@Composable
fun PopupDragDemo() {
    // TODO fix this demo in RTL (check when draggable handles RTL)
    val offset = remember { mutableStateOf(Offset.Zero) }

    Column {
        Text("That is a pop up with a dragGestureFilter on it.  You can drag it around!")
        Popup(
            alignment = Alignment.TopStart,
            offset = offset.value.round()
        ) {
            Box {
                Box(
                    Modifier
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                offset.value = offset.value + dragAmount
                            }
                        }
                        .size(70.dp)
                        .background(Color.Green, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "This is a popup!",
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
