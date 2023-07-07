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

package androidx.compose.ui.demos.focus

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

private const val size = 200f

private enum class CurrentShape { Circle, Square }

@Composable
fun ReuseFocusRequesterDemo() {
    Column(
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            "Click to Focus on the shape. The focused shape disappears, and is replaced by " +
                "another shape. The same focus requester is used for both shapes."
        )

        // Shared focus requester.
        val focusRequester = remember { FocusRequester() }

        var shape by remember { mutableStateOf(CurrentShape.Square) }
        when (shape) {
            CurrentShape.Circle -> Circle(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .pointerInput(Unit) { detectTapGestures { focusRequester.requestFocus() } },
                nextShape = { shape = CurrentShape.Square }
            )
            CurrentShape.Square -> Square(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .pointerInput(Unit) { detectTapGestures { focusRequester.requestFocus() } },
                nextShape = { shape = CurrentShape.Circle }
            )
        }
    }
}

@Composable
private fun Circle(modifier: Modifier = Modifier, nextShape: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 0f else 1f, TweenSpec(2000)) {
        if (it == 0f) {
            nextShape()
        }
    }
    val radius = size / 2
    Canvas(
        modifier
            .onFocusChanged { isFocused = it.isFocused }
            .fillMaxSize()
            .focusTarget()
    ) {
        drawCircle(
            color = if (isFocused) Color.Red else Color.Blue,
            radius = radius * scale,
            center = center
        )
    }
}

@Composable
private fun Square(modifier: Modifier = Modifier, nextShape: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 0f else 1f, TweenSpec(2000)) {
        if (it == 0f) {
            nextShape()
        }
    }
    val side = size
    Canvas(
        modifier
            .onFocusChanged { isFocused = it.isFocused }
            .fillMaxSize()
            .focusTarget()
    ) {
        drawRect(
            color = if (isFocused) Color.Red else Color.Blue,
            topLeft = Offset(
                center.x - side / 2 * scale, center.y - side / 2 * scale
            ),
            size = Size(side * scale, side * scale)
        )
    }
}
