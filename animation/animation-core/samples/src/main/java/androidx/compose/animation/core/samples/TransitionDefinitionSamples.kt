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

package androidx.compose.animation.core.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.InterruptionHandling
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.transitionDefinition

private val radius = FloatPropKey()
private val alpha = FloatPropKey()

@Sampled
@Suppress("UNUSED_VARIABLE")
fun TransitionDefSample() {
    val definition = transitionDefinition<ButtonState> {
        state(ButtonState.Pressed) {
            this[alpha] = 0f
            this[radius] = 200f
        }
        state(ButtonState.Released) {
            this[alpha] = 0f
            this[radius] = 60f
        }

        // Optional configuration for transition from Pressed to Released. If no transition is
        // defined, the default physics-based transition will be used.
        transition(fromState = ButtonState.Released, toState = ButtonState.Pressed) {
            radius using spring(
                dampingRatio = 1.0f
            )
            alpha using keyframes {
                durationMillis = 375
                0f at 0 // ms  // Optional
                0.4f at 75 // ms
                0.4f at 225 // ms
                0f at 375 // ms  // Optional
            }
            interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
        }
    }
}