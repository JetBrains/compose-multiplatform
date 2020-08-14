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

package androidx.compose.animation.core.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.InterruptionHandling
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween

enum class ButtonState {
    Released,
    Pressed,
    Disabled
}

@Sampled
fun TransitionSpecWith3Properties() {

    val Radius = FloatPropKey()
    val Alpha = FloatPropKey()
    val Background = FloatPropKey()

    transitionDefinition<ButtonState> {
        // This defines animations for 3 properties: Radius, Alpha, and Background.
        transition(fromState = ButtonState.Released, toState = ButtonState.Pressed) {
            Radius using tween(
                easing = LinearEasing,
                durationMillis = 75
            )
            Alpha using keyframes {
                durationMillis = 375
                0f at 0 // ms  // Optional
                0.4f at 75 // ms
                0.4f at 225 // ms
                0f at 375 // ms  // Optional
            }
            Background using spring(
                dampingRatio = 1.0f
            )
            interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
        }
    }
}

@Sampled
fun TransitionSpecWithPairs() {

    transitionDefinition<ButtonState> {
        transition(
            ButtonState.Released to ButtonState.Pressed,
            ButtonState.Disabled to ButtonState.Pressed
        ) {
            // ...
        }
    }
}
