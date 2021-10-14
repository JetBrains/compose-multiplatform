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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun DoubleTapInTapDemo() {
    val defaultColor = Grey

    val innerColor = remember { mutableStateOf(defaultColor) }
    val outerColor = remember { mutableStateOf(defaultColor) }

    val onTap: (Offset) -> Unit = {
        outerColor.value = outerColor.value.next()
    }

    val onDoubleTap: (Offset) -> Unit = { _ ->
        innerColor.value = innerColor.value.prev()
    }

    Column {
        Text(
            "Demonstrates interaction between DoubleTapGestureFilter and TapGestureFilter in an " +
                "edge case that is nevertheless supported (normally regions will be separated" +
                " by a pressIndicatorGestureFilter, but here they are not)."
        )
        Text(
            "Double tap the inner box to change the inner box color. Tap anywhere in the outer " +
                "box once (including the inner box) to change the outer box background " +
                "color. Tap rapidly with one or more fingers anywhere and the colors should" +
                "change as one would expect."
        )
        Box(
            Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .size(192.dp)
                .pointerInput(Unit) { detectTapGestures(onTap = onTap) }
                .border(2.dp, BorderColor)
                .background(color = outerColor.value)
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .size(96.dp)
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = onDoubleTap) }
                .border(2.dp, BorderColor)
                .background(color = innerColor.value, shape = RectangleShape)
        )
    }
}