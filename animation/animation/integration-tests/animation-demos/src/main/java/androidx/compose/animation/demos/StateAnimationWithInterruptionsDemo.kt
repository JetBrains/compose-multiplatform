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

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.ColorPropKey
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.TransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
fun StateAnimationWithInterruptionsDemo() {
    Box(Modifier.fillMaxSize()) {
        ColorRect()
    }
}

private val background = ColorPropKey()
private val y = FloatPropKey()

private enum class OverlayState {
    Open,
    Closed
}

private val definition = transitionDefinition<OverlayState> {
    state(OverlayState.Open) {
        this[background] = Color(red = 128, green = 128, blue = 128, alpha = 255)
        this[y] = 1f // percentage
    }
    state(OverlayState.Closed) {
        this[background] = Color(red = 188, green = 222, blue = 145, alpha = 255)
        this[y] = 0f // percentage
    }
    // Apply this transition to all state changes (i.e. Open -> Closed and Closed -> Open)
    transition {
        background using tween(
            durationMillis = 800
        )
        y using spring(
            // Extremely low stiffness
            stiffness = 40f
        )
    }
}

private val handler = Handler(Looper.getMainLooper())

@Composable
private fun ColorRect() {
    var toState by mutableStateOf(OverlayState.Closed)
    handler.postDelayed(
        object : Runnable {
            override fun run() {
                if ((0..1).random() == 0) {
                    toState = OverlayState.Open
                } else {
                    toState = OverlayState.Closed
                }
            }
        },
        (200..800).random().toLong()
    )
    val state = transition(definition = definition, toState = toState)
    ColorRectState(state = state)
}

@Composable
private fun ColorRectState(state: TransitionState) {
    val color = state[background]
    val scaleY = state[y]
    Canvas(Modifier.fillMaxSize().background(color = color)) {
        drawRect(
            Color(alpha = 255, red = 255, green = 255, blue = 255),
            topLeft = Offset(100f, 0f),
            size = Size(size.width - 200f, scaleY * size.height)
        )
    }
}
