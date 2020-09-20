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

import androidx.compose.animation.ColorPropKey
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.transition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.gesture.pressIndicatorGestureFilter
import androidx.compose.ui.graphics.Color

private const val halfSize = 200f

private enum class ComponentState { Pressed, Released }

private val scale = FloatPropKey()
private val color = ColorPropKey()

private val definition = transitionDefinition<ComponentState> {
    state(ComponentState.Released) {
        this[scale] = 1f
        this[color] = Color(red = 0, green = 200, blue = 0, alpha = 255)
    }
    state(ComponentState.Pressed) {
        this[scale] = 3f
        this[color] = Color(red = 0, green = 100, blue = 0, alpha = 255)
    }
    transition {
        scale using spring(
            stiffness = 50f
        )
        color using spring(
            stiffness = 50f
        )
    }
}

@Composable
fun GestureBasedAnimationDemo() {
    val toState = remember { mutableStateOf(ComponentState.Released) }
    val pressIndicator =
        Modifier.pressIndicatorGestureFilter(
            onStart = { toState.value = ComponentState.Pressed },
            onStop = { toState.value = ComponentState.Released },
            onCancel = { toState.value = ComponentState.Released }
        )

    val state = transition(definition = definition, toState = toState.value)
    ScaledColorRect(pressIndicator, scale = state[scale], color = state[color])
}

@Composable
private fun ScaledColorRect(modifier: Modifier = Modifier, scale: Float, color: Color) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            color,
            topLeft = Offset(center.x - halfSize * scale, center.y - halfSize * scale),
            size = Size(halfSize * 2 * scale, halfSize * 2 * scale)
        )
    }
}
