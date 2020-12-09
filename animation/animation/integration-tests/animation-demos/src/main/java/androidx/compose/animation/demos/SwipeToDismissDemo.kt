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

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.animation.AndroidFlingDecaySpec
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.gesture.util.VelocityTracker
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SwipeToDismissDemo() {
    Column {
        var index by remember { mutableStateOf(0) }
        val dismissState = remember { DismissState() }
        Box(Modifier.height(300.dp).fillMaxWidth()) {
            Box(
                Modifier.swipeToDismiss(dismissState).align(Alignment.BottomCenter).size(150.dp)
                    .background(pastelColors[index])
            )
        }
        Text(
            "Swipe up to dismiss",
            fontSize = 30.sp,
            modifier = Modifier.padding(40.dp).align(Alignment.CenterHorizontally)
        )
        Button(
            onClick = {
                index = (index + 1) % pastelColors.size
                dismissState.alpha = 1f
                dismissState.offset = 0f
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("New Card")
        }
    }
}

private fun Modifier.swipeToDismiss(dismissState: DismissState): Modifier = composed {
    val mutatorMutex = remember { MutatorMutex() }

    this.pointerInput {
        fun updateOffset(value: Float) {
            dismissState.offset = value
            dismissState.alpha = 1f - abs(dismissState.offset / size.height)
        }
        coroutineScope {
            while (true) {
                val pointerId = awaitPointerEventScope {
                    awaitFirstDown().id
                }
                val velocityTracker = VelocityTracker()
                // Set a high priority on the mutatorMutex for gestures
                mutatorMutex.mutate(MutatePriority.UserInput) {
                    awaitPointerEventScope {
                        verticalDrag(pointerId) {
                            updateOffset(dismissState.offset + it.positionChange().y)
                            velocityTracker.addPosition(
                                it.current.uptime,
                                it.current.position
                            )
                        }
                    }
                }
                val velocity = velocityTracker.calculateVelocity().pixelsPerSecond.y
                launch {
                    // Use mutatorMutex to make sure drag gesture would cancel any on-going
                    // animation job.
                    mutatorMutex.mutate {
                        // Either fling out of the sight, or snap back
                        val animationState = AnimationState(dismissState.offset, velocity)
                        val decay = AndroidFlingDecaySpec(this@pointerInput)
                        if (decay.getTarget(dismissState.offset, velocity) >= -size.height) {
                            // Not enough velocity to be dismissed
                            animationState.animateTo(0f) {
                                updateOffset(value)
                            }
                        } else {
                            animationState.animateDecay(decay) {
                                // End animation early if it reaches the bounds
                                if (value <= -size.height) {
                                    cancelAnimation()
                                    updateOffset(-size.height.toFloat())
                                } else {
                                    updateOffset(value)
                                }
                            }
                        }
                    }
                }
            }
        }
    }.offset(y = { dismissState.offset }).graphicsLayer(alpha = dismissState.alpha)
}

private class DismissState {
    var alpha by mutableStateOf(1f)
    var offset by mutableStateOf(0f)
}

internal val pastelColors = listOf(
    Color(0xFFffd7d7),
    Color(0xFFffe9d6),
    Color(0xFFfffbd0),
    Color(0xFFe3ffd9),
    Color(0xFFd0fff8)
)
