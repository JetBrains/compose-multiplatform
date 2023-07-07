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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Sampled
@Composable
fun AnimatableAnimateToGenericsType() {
    // Creates an `Animatable` to animate Offset and `remember` it.
    val animatedOffset = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }

    Box(
        Modifier.fillMaxSize().background(Color(0xffb99aff)).pointerInput(Unit) {
            coroutineScope {
                while (true) {
                    val offset = awaitPointerEventScope {
                        awaitFirstDown().position
                    }
                    // Launch a new coroutine for animation so the touch detection thread is not
                    // blocked.
                    launch {
                        // Animates to the pressed position, with the given animation spec.
                        animatedOffset.animateTo(
                            offset,
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        )
                    }
                }
            }
        }
    ) {
        Text("Tap anywhere", Modifier.align(Alignment.Center))
        Box(
            Modifier
                .offset {
                    // Use the animated offset as the offset of the Box.
                    IntOffset(
                        animatedOffset.value.x.roundToInt(),
                        animatedOffset.value.y.roundToInt()
                    )
                }
                .size(40.dp)
                .background(Color(0xff3c1361), CircleShape)
        )
    }
}

@Sampled
fun AnimatableDecayAndAnimateToSample() {
    /**
     * In this example, we create a swipe-to-dismiss modifier that dismisses the child via a
     * vertical swipe-up.
     */
    fun Modifier.swipeToDismiss(): Modifier = composed {
        // Creates a Float type `Animatable` and `remember`s it
        val animatedOffsetY = remember { Animatable(0f) }
        this.pointerInput(Unit) {
            coroutineScope {
                while (true) {
                    val pointerId = awaitPointerEventScope {
                        awaitFirstDown().id
                    }
                    val velocityTracker = VelocityTracker()
                    awaitPointerEventScope {
                        verticalDrag(pointerId) {
                            // Snaps the value by the amount of finger movement
                            launch {
                                animatedOffsetY.snapTo(
                                    animatedOffsetY.value + it.positionChange().y
                                )
                            }
                            velocityTracker.addPosition(
                                it.uptimeMillis,
                                it.position
                            )
                        }
                    }
                    // At this point, drag has finished. Now we obtain the velocity at the end of
                    // the drag, and animate the offset with it as the starting velocity.
                    val velocity = velocityTracker.calculateVelocity().y

                    // The goal for the animation below is to animate the dismissal if the fling
                    // velocity is high enough. Otherwise, spring back.
                    launch {
                        // Checks where the animation will end using decay
                        val decay = splineBasedDecay<Float>(this@pointerInput)

                        // If the animation can naturally end outside of visual bounds, we will
                        // animate with decay.
                        if (decay.calculateTargetValue(
                                animatedOffsetY.value,
                                velocity
                            ) < -size.height
                        ) {
                            // (Optionally) updates lower bounds. This stops the animation as soon
                            // as bounds are reached.
                            animatedOffsetY.updateBounds(
                                lowerBound = -size.height.toFloat()
                            )
                            // Animate with the decay animation spec using the fling velocity
                            animatedOffsetY.animateDecay(velocity, decay)
                        } else {
                            // Not enough velocity to be dismissed, spring back to 0f
                            animatedOffsetY.animateTo(0f, initialVelocity = velocity)
                        }
                    }
                }
            }
        }.offset { IntOffset(0, animatedOffsetY.value.roundToInt()) }
    }
}

@Sampled
fun AnimatableAnimationResultSample() {
    suspend fun CoroutineScope.animateBouncingOffBounds(
        animatable: Animatable<Offset, *>,
        flingVelocity: Offset,
        parentSize: Size
    ) {
        launch {
            var startVelocity = flingVelocity
            // Set bounds for the animation, so that when it reaches bounds it will stop
            // immediately. We can then inspect the returned `AnimationResult` and decide whether
            // we should start another animation.
            animatable.updateBounds(Offset(0f, 0f), Offset(parentSize.width, parentSize.height))
            do {
                val result = animatable.animateDecay(startVelocity, exponentialDecay())
                // Copy out the end velocity of the previous animation.
                startVelocity = result.endState.velocity

                // Negate the velocity for the dimension that hits the bounds, to create a
                // bouncing off the bounds effect.
                with(animatable) {
                    if (value.x == upperBound?.x || value.x == lowerBound?.x) {
                        // x dimension hits bounds
                        startVelocity = startVelocity.copy(x = -startVelocity.x)
                    }
                    if (value.y == upperBound?.y || value.y == lowerBound?.y) {
                        // y dimension hits bounds
                        startVelocity = startVelocity.copy(y = -startVelocity.y)
                    }
                }
                // Repeat the animation until the animation ends for reasons other than hitting
                // bounds, e.g. if `stop()` is called, or preempted by another animation.
            } while (result.endReason == AnimationEndReason.BoundReached)
        }
    }
}

@Sampled
fun AnimatableFadeIn() {
    fun Modifier.fadeIn(): Modifier = composed {
        // Creates an `Animatable` and remembers it.
        val alphaAnimation = remember { Animatable(0f) }
        // Launches a coroutine for the animation when entering the composition.
        // Uses `alphaAnimation` as the subject so the job in `LaunchedEffect` will run only when
        // `alphaAnimation` is created, which happens one time when the modifier enters
        // composition.
        LaunchedEffect(alphaAnimation) {
            // Animates to 1f from 0f for the fade-in, and uses a 500ms tween animation.
            alphaAnimation.animateTo(
                targetValue = 1f,
                // Default animationSpec uses [spring] animation, here we overwrite the default.
                animationSpec = tween(500)
            )
        }
        this.graphicsLayer(alpha = alphaAnimation.value)
    }
}
