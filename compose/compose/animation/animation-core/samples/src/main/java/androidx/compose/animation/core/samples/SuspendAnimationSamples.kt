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
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.isFinished
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

@Sampled
fun animateToOnAnimationState() {
    @Composable
    fun simpleAnimate(
        target: Float,
    ): Float {
        // Create an AnimationState to be updated by the animation.
        val animationState = remember { AnimationState(target) }

        // Launch the suspend animation into the composition's CoroutineContext, and pass
        // `target` to LaunchedEffect so that when`target` changes the old animation job is
        // canceled, and a new animation is created with a new target.
        LaunchedEffect(target) {
            // This starts an animation that updates the animationState on each frame
            animationState.animateTo(
                targetValue = target,
                // Use a low stiffness spring. This can be replaced with any type of `AnimationSpec`
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                // If the previous animation was interrupted (i.e. not finished), configure the
                // animation as a sequential animation to continue from the time the animation was
                // interrupted.
                sequentialAnimation = !animationState.isFinished
            )
            // When the function above returns, the animation has finished.
        }
        // Return the value updated by the animation.
        return animationState.value
    }
}

@Sampled
fun suspendAnimateFloatVariant() {
    @Composable
    fun InfiniteAnimationDemo() {
        // Create a mutable state for alpha, and update it in the animation.
        val alpha = remember { mutableStateOf(1f) }
        LaunchedEffect(Unit) {
            // Animate from 1f to 0f using an infinitely repeating animation
            animate(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                )
            ) { value, /* velocity */ _ ->
                // Update alpha mutable state with the current animation value
                alpha.value = value
            }
        }
        Box(Modifier.fillMaxSize()) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = 3.0f,
                        scaleY = 3.0f,
                        alpha = alpha.value
                    ),
                tint = Color.Red
            )
        }
    }
}