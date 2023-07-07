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
import androidx.compose.foundation.gestures.detectTransformGestures
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
 * Simple [detectTransformGestures] demo with scale only.
 */
@Composable
fun ScaleGestureFilterDemo() {
    val size = remember { mutableStateOf(192.dp) }

    Column {
        Text("Demonstrates the scale gesture detector!")
        Text("This is only scaling, not translating.")
        Box(
            Modifier.fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .size(size.value)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ -> size.value *= zoom }
                }
                .background(Color(0xFF9e9e9e.toInt()))
        )
    }
}
