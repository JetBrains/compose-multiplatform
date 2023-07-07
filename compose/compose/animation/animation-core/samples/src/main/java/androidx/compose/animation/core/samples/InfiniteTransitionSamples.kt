/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun InfiniteTransitionSample() {
    @Composable
    fun InfinitelyPulsingHeart() {
        // Creates an [InfiniteTransition] instance for managing child animations.
        val infiniteTransition = rememberInfiniteTransition()

        // Creates a child animation of float type as a part of the [InfiniteTransition].
        val scale by infiniteTransition.animateFloat(
            initialValue = 3f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                // Infinitely repeating a 1000ms tween animation using default easing curve.
                animation = tween(1000),
                // After each iteration of the animation (i.e. every 1000ms), the animation will
                // start again from the [initialValue] defined above.
                // This is the default [RepeatMode]. See [RepeatMode.Reverse] below for an
                // alternative.
                repeatMode = RepeatMode.Restart
            )
        )

        // Creates a Color animation as a part of the [InfiniteTransition].
        val color by infiniteTransition.animateColor(
            initialValue = Color.Red,
            targetValue = Color(0xff800000), // Dark Red
            animationSpec = infiniteRepeatable(
                // Linearly interpolate between initialValue and targetValue every 1000ms.
                animation = tween(1000, easing = LinearEasing),
                // Once [TargetValue] is reached, starts the next iteration in reverse (i.e. from
                // TargetValue to InitialValue). Then again from InitialValue to TargetValue. This
                // [RepeatMode] ensures that the animation value is *always continuous*.
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(Modifier.fillMaxSize()) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
                    ),
                tint = color
            )
        }
    }
}

@Sampled
@Composable
fun InfiniteTransitionAnimateValueSample() {
    // Creates an [InfiniteTransition] instance to run child animations.
    val infiniteTransition = rememberInfiniteTransition()
    // Infinitely animate a Dp offset from 0.dp to 100.dp
    val offsetX by infiniteTransition.animateValue(
        initialValue = 0.dp,
        targetValue = 100.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 500
                0.dp at 200 // ms
                80.dp at 300 with FastOutLinearInEasing
            }
            // Use the default RepeatMode.Restart to start from 0.dp after each iteration
        )
    )

    Box(Modifier.offset(x = offsetX)) {
        // Content goes here
    }
}
