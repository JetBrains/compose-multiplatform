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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun RepeatedRotationDemo() {
    val state = remember { mutableStateOf(RotationStates.Original) }
    Column(
        Modifier.fillMaxSize()
            .wrapContentSize(Alignment.Center),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            { state.value = RotationStates.Rotated }
        ) {
            Text(text = "Rotate 10 times")
        }
        Spacer(Modifier.requiredHeight(10.dp))
        Button(
            { state.value = RotationStates.Original }
        ) {
            Text(text = "Reset")
        }
        Spacer(Modifier.requiredHeight(10.dp))
        val transition = updateTransition(state.value)
        @Suppress("UnusedTransitionTargetStateParameter")
        val rotation by transition.animateFloat(
            transitionSpec = {
                if (initialState == RotationStates.Original) {
                    repeatable(
                        iterations = 10,
                        animation = keyframes {
                            durationMillis = 1000
                            0f at 0 with LinearEasing
                            360f at 1000
                        }
                    )
                } else {
                    tween(durationMillis = 300)
                }
            }
        ) {
            0f
        }
        Canvas(Modifier.size(100.dp)) {
            rotate(rotation, Offset.Zero) {
                drawRect(Color(0xFF00FF00))
            }
        }
    }
}

private enum class RotationStates {
    Original,
    Rotated
}