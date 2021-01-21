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

package androidx.compose.foundation.animation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Scrollable
import androidx.compose.runtime.dispatch.withFrameMillis

/**
 * Smooth scroll by [value] pixels.
 *
 * Cancels the currently running scroll, if any, and suspends until the cancellation is
 * complete.
 *
 * @param value number of pixels to scroll by
 * @param spec [AnimationSpec] to be used for this smooth scrolling
 *
 * @return the amount of scroll consumed
 */
suspend fun Scrollable.smoothScrollBy(
    value: Float,
    spec: AnimationSpec<Float> = spring()
): Float {
    val animSpec = spec.vectorize(Float.VectorConverter)
    val conv = Float.VectorConverter
    val zeroVector = conv.convertToVector(0f)
    val targetVector = conv.convertToVector(value)
    var previousValue = 0f

    scroll {
        val startTimeMillis = withFrameMillis { it }
        do {
            val finished = withFrameMillis { frameTimeMillis ->
                val newValue = conv.convertFromVector(
                    animSpec.getValue(
                        playTime = frameTimeMillis - startTimeMillis,
                        start = zeroVector,
                        end = targetVector,
                        // TODO: figure out if/how we should incorporate existing velocity
                        startVelocity = zeroVector
                    )
                )
                val delta = newValue - previousValue
                val consumed = scrollBy(delta)

                if (consumed != delta) {
                    previousValue += consumed
                    true
                } else {
                    previousValue = newValue
                    previousValue == value
                }
            }
        } while (!finished)
    }
    return previousValue
}

/**
 * Jump instantly by [value] pixels.
 *
 * Cancels the currently running scroll, if any, and suspends until the cancellation is
 * complete.
 *
 * @see smoothScrollBy for an animated version
 *
 * @param value number of pixels to scroll by
 * @return the amount of scroll consumed
 */
suspend fun Scrollable.scrollBy(
    value: Float
): Float {
    var consumed = 0f
    scroll {
        consumed = scrollBy(value)
    }
    return consumed
}