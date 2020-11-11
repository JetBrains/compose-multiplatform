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

import android.graphics.PointF
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.InterruptionHandling
import androidx.compose.animation.core.TransitionDefinition
import androidx.compose.animation.core.TransitionState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.pressIndicatorGestureFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.unit.dp

@Composable
fun StateBasedRippleDemo() {
    Box(Modifier.fillMaxSize()) {
        RippleRect()
    }
}

@Composable
private fun RippleRect() {
    val radius = with(AmbientDensity.current) { TargetRadius.toPx() }
    val toState = remember { mutableStateOf(ButtonStatus.Initial) }
    val rippleTransDef = remember { createTransDef(radius) }
    val onPress: (Offset) -> Unit = { position ->
        down.x = position.x
        down.y = position.y
        toState.value = ButtonStatus.Pressed
    }

    val onRelease: () -> Unit = {
        toState.value = ButtonStatus.Released
    }
    val state = transition(definition = rippleTransDef, toState = toState.value)
    RippleRectFromState(
        Modifier.pressIndicatorGestureFilter(onStart = onPress, onStop = onRelease), state = state
    )
}

@Composable
private fun RippleRectFromState(modifier: Modifier = Modifier, state: TransitionState) {
    Canvas(modifier.fillMaxSize()) {
        // TODO: file bug for when "down" is not a file level val, it's not memoized correctly
        drawCircle(
            Color(
                alpha = (state[alpha] * 255).toInt(),
                red = 0,
                green = 235,
                blue = 224
            ),
            center = Offset(down.x, down.y),
            radius = state[radius]
        )
    }
}

private enum class ButtonStatus {
    Initial,
    Pressed,
    Released
}

private val TargetRadius = 200.dp

private val down = PointF(0f, 0f)

private val alpha = FloatPropKey()
private val radius = FloatPropKey()

private fun createTransDef(targetRadius: Float): TransitionDefinition<ButtonStatus> {
    return transitionDefinition {
        state(ButtonStatus.Initial) {
            this[alpha] = 0f
            this[radius] = targetRadius * 0.3f
        }
        state(ButtonStatus.Pressed) {
            this[alpha] = 0.2f
            this[radius] = targetRadius + 15f
        }
        state(ButtonStatus.Released) {
            this[alpha] = 0f
            this[radius] = targetRadius + 15f
        }

        // Grow the ripple
        transition(ButtonStatus.Initial to ButtonStatus.Pressed) {
            alpha using keyframes {
                durationMillis = 225
                0f at 0 // optional
                0.2f at 75
                0.2f at 225 // optional
            }
            radius using tween(durationMillis = 225)
            interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
        }

        // Fade out the ripple
        transition(ButtonStatus.Pressed to ButtonStatus.Released) {
            alpha using tween(durationMillis = 200)
            interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
            // switch back to Initial to prepare for the next ripple cycle
            nextState = ButtonStatus.Initial
        }

        // State switch without animation
        snapTransition(ButtonStatus.Released to ButtonStatus.Initial)
    }
}
