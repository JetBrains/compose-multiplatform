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

package androidx.compose.animation.demos.statetransition

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview

private enum class LikedStates {
    Initial,
    Liked,
    Disappeared
}

@Preview
@Composable
fun DoubleTapToLikeDemo() {

    // Creates a transition state that starts in [Disappeared] State
    var transitionState by remember {
        mutableStateOf(MutableTransitionState(LikedStates.Disappeared))
    }

    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    // This creates a new `MutableTransitionState` object. When a new
                    // `MutableTransitionState` object gets passed to `updateTransition`, a new
                    // transition will be created. All existing values, velocities will be lost as a
                    // result. Hence, in most cases, this is not recommended. The exception is
                    // when it's more important to respond immediately to user interaction than
                    // preserving continuity.
                    transitionState = MutableTransitionState(LikedStates.Initial)
                }
            )
        }
    ) {
        // This ensures sequential states: Initial -> Liked -> Disappeared
        if (transitionState.currentState == LikedStates.Initial) {
            transitionState.targetState = LikedStates.Liked
        } else if (transitionState.currentState == LikedStates.Liked) {
            // currentState will be updated to targetState when the transition is finished, so it
            // can be used as a signal to start the next transition.
            transitionState.targetState = LikedStates.Disappeared
        }

        val transition = updateTransition(transitionState)
        val alpha by transition.animateFloat(
            transitionSpec = {
                when {
                    LikedStates.Initial isTransitioningTo LikedStates.Liked ->
                        keyframes {
                            durationMillis = 500
                            0f at 0 // optional
                            0.5f at 100
                            1f at 225 // optional
                        }
                    LikedStates.Liked isTransitioningTo LikedStates.Disappeared ->
                        tween(durationMillis = 200)
                    else -> snap()
                }
            }
        ) {
            if (it == LikedStates.Liked) 1f else 0f
        }

        val scale by transition.animateFloat(
            transitionSpec = {
                when {
                    LikedStates.Initial isTransitioningTo LikedStates.Liked ->
                        spring(dampingRatio = Spring.DampingRatioHighBouncy)
                    LikedStates.Liked isTransitioningTo LikedStates.Disappeared ->
                        tween(200)
                    else -> snap()
                }
            }
        ) {
            when (it) {
                LikedStates.Initial -> 0f
                LikedStates.Liked -> 4f
                LikedStates.Disappeared -> 2f
            }
        }

        Icon(
            Icons.Filled.Favorite,
            "Like",
            Modifier.align(Alignment.Center)
                .graphicsLayer(
                    alpha = alpha,
                    scaleX = scale,
                    scaleY = scale
                ),
            tint = Color.Red
        )
    }
}
