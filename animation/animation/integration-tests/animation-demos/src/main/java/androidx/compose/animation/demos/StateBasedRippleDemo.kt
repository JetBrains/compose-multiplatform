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

import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.pressIndicatorGestureFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StateBasedRippleDemo() {
    Box(Modifier.fillMaxSize()) {
        RippleRect()
    }
}

@Composable
private fun RippleRect() {
    var down by remember { mutableStateOf(Offset(0f, 0f)) }
    var toState by remember { mutableStateOf(ButtonStatus.Initial) }
    val onPress: (Offset) -> Unit = { position ->
        down = position
        toState = ButtonStatus.Pressed
    }

    val onRelease: () -> Unit = {
        toState = ButtonStatus.Released
    }
    val transition = updateTransition(toState)
    RippleRectFromState(
        Modifier.pressIndicatorGestureFilter(onStart = onPress, onStop = onRelease),
        center = down,
        transition = transition
    )
}

@Composable
private fun RippleRectFromState(
    modifier: Modifier = Modifier,
    center: Offset,
    transition: Transition<ButtonStatus>
) {
    // TODO: Initial -> Pressed: Uninterruptible
    // TODO: Pressed -> Released: Uninterruptible
    // TODO: Auto transition to Initial
    val alpha by transition.animateFloat(
        transitionSpec = {
            if (ButtonStatus.Initial isTransitioningTo ButtonStatus.Pressed) {
                keyframes {
                    durationMillis = 225
                    0f at 0 // optional
                    0.2f at 75
                    0.2f at 225 // optional
                }
            } else if (ButtonStatus.Pressed isTransitioningTo ButtonStatus.Released) {
                tween(durationMillis = 220)
            } else {
                snap()
            }
        }
    ) {
        if (it == ButtonStatus.Pressed) 0.2f else 0f
    }

    val radius by transition.animateDp(
        transitionSpec = {
            if (ButtonStatus.Initial isTransitioningTo ButtonStatus.Pressed) {
                tween(225)
            } else {
                snap()
            }
        }
    ) {
        if (it == ButtonStatus.Initial) TargetRadius * 0.3f else TargetRadius + 15.dp
    }

    Canvas(modifier.fillMaxSize()) {
        drawCircle(
            Color(
                alpha = (alpha * 255).toInt(),
                red = 0,
                green = 235,
                blue = 224
            ),
            center = center,
            radius = radius.toPx()
        )
    }
}

private enum class ButtonStatus {
    Initial,
    Pressed,
    Released
}

private val TargetRadius = 200.dp