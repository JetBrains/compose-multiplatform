/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Simple demonstration of subscribing to pressure changes.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DetectTapPressureGesturesDemo() {

    var pressure by remember { mutableStateOf("Press for value") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text("The box displays pressure. Tap or use stylus to see different pressure values.")
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
                .wrapContentSize(Alignment.Center)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach {
                                pressure = "${it.pressure}"
                                it.consume()
                            }
                        }
                    }
                }
                .clipToBounds()
                .background(Color.Green)
                .border(BorderStroke(2.dp, BorderColor))
        ) {
            Text(
                modifier = Modifier.fillMaxSize(),
                textAlign = TextAlign.Center,
                text = "Pressure: $pressure"
            )
        }
    }
}
