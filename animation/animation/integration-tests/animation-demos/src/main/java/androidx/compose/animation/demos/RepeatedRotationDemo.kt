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

package androidx.compose.animation.demos

import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.tapGestureFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RepeatedRotationDemo() {
    val state = remember { mutableStateOf(RotationStates.Original) }
    Column(
        Modifier.fillMaxSize()
            .wrapContentSize(Alignment.Center),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        val textStyle = TextStyle(fontSize = 18.sp)
        Text(
            modifier = Modifier.tapGestureFilter(onTap = { state.value = RotationStates.Rotated }),
            text = "Rotate 10 times",
            style = textStyle
        )
        Text(
            modifier = Modifier.tapGestureFilter(onTap = { state.value = RotationStates.Original }),
            text = "Reset",
            style = textStyle
        )
        val transitionState = transition(
            definition = definition,
            toState = state.value
        )
        Canvas(Modifier.preferredSize(100.dp)) {
            rotate(transitionState[rotation], Offset.Zero) {
                drawRect(Color(0xFF00FF00))
            }
        }
    }
}

private enum class RotationStates {
    Original,
    Rotated
}

private val rotation = FloatPropKey()

private val definition = transitionDefinition<RotationStates> {
    state(RotationStates.Original) {
        this[rotation] = 0f
    }
    state(RotationStates.Rotated) {
        this[rotation] = 360f
    }
    transition(RotationStates.Original to RotationStates.Rotated) {
        rotation using repeatable(
            iterations = 10,
            animation = tween(
                easing = LinearEasing,
                durationMillis = 1000
            )
        )
    }
    transition(RotationStates.Rotated to RotationStates.Original) {
        rotation using tween(
            durationMillis = 300
        )
    }
}
