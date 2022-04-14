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

package androidx.compose.animation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.first

/**
 * Creates a [InfiniteTransition] that runs infinite child animations. Child animations can be
 * added using [InfiniteTransition.animateColor][androidx.compose.animation.animateColor],
 * [InfiniteTransition.animateFloat], or [InfiniteTransition.animateValue]. Child animations will
 * start running as soon as they enter the composition, and will not stop until they are removed
 * from the composition.
 *
 * @sample androidx.compose.animation.core.samples.InfiniteTransitionSample
 */
@Composable
fun rememberInfiniteTransition(): InfiniteTransition {
    val infiniteTransition = remember { InfiniteTransition() }
    infiniteTransition.run()
    return infiniteTransition
}

/**
 * [InfiniteTransition] is responsible for running child animations. Child animations can be
 * added using [InfiniteTransition.animateColor][androidx.compose.animation.animateColor],
 * [InfiniteTransition.animateFloat], or [InfiniteTransition.animateValue]. Child animations will
 * start running as soon as they enter the composition, and will not stop until they are removed
 * from the composition.
 *
 * @sample androidx.compose.animation.core.samples.InfiniteTransitionSample
 */
class InfiniteTransition internal constructor() {
    internal inner class TransitionAnimationState<T, V : AnimationVector>(
        var initialValue: T,
        var targetValue: T,
        val typeConverter: TwoWayConverter<T, V>,
        var animationSpec: AnimationSpec<T>
    ) : State<T> {
        override var value by mutableStateOf(initialValue)
            internal set
        var animation = TargetBasedAnimation(
            animationSpec,
            typeConverter,
            initialValue,
            targetValue
        )

        // This is used to signal parent for less work in a normal running mode, but in seeking
        // this is ignored since time can go both ways.
        var isFinished = false

        // If animation is refreshed during the run, start the new animation in the next frame
        var startOnTheNextFrame = false

        // When the animation changes, it needs to start from playtime 0 again, offsetting from
        // parent's playtime to achieve that.
        var playTimeNanosOffset = 0L

        // This gets called when the initial/target value changes, which should be a rare case.
        fun updateValues(initialValue: T, targetValue: T, animationSpec: AnimationSpec<T>) {
            this.initialValue = initialValue
            this.targetValue = targetValue
            this.animationSpec = animationSpec
            // Create a new animation if anything (i.e. initial/target) has changed
            // TODO: Consider providing some continuity maybe?
            animation = TargetBasedAnimation(
                animationSpec,
                typeConverter,
                initialValue,
                targetValue
            )
            refreshChildNeeded = true
            isFinished = false
            startOnTheNextFrame = true
        }

        fun onPlayTimeChanged(playTimeNanos: Long) {
            refreshChildNeeded = false
            if (startOnTheNextFrame) {
                startOnTheNextFrame = false
                playTimeNanosOffset = playTimeNanos
            }
            val playTime = playTimeNanos - playTimeNanosOffset
            value = animation.getValueFromNanos(playTime)
            isFinished = animation.isFinishedFromNanos(playTime)
        }

        fun skipToEnd() {
            value = animation.targetValue
            startOnTheNextFrame = true
        }

        fun reset() {
            startOnTheNextFrame = true
        }
    }

    internal val animations = mutableVectorOf<TransitionAnimationState<*, *>>()
    private var refreshChildNeeded by mutableStateOf(false)
    private var startTimeNanos = AnimationConstants.UnspecifiedTime
    private var isRunning by mutableStateOf(true)

    internal fun addAnimation(animation: TransitionAnimationState<*, *>) {
        animations.add(animation)
        refreshChildNeeded = true
    }

    internal fun removeAnimation(animation: TransitionAnimationState<*, *>) {
        animations.remove(animation)
    }

    @Suppress("ComposableNaming")
    @Composable
    internal fun run() {
        if (isRunning || refreshChildNeeded) {
            LaunchedEffect(this) {
                var durationScale = 1f
                // Restart every time duration scale changes
                while (true) {
                    withInfiniteAnimationFrameNanos {
                        if (startTimeNanos == AnimationConstants.UnspecifiedTime ||
                            durationScale != coroutineContext.durationScale
                        ) {
                            startTimeNanos = it
                            animations.forEach {
                                it.reset()
                            }
                            durationScale = coroutineContext.durationScale
                        }
                        if (durationScale == 0f) {
                            // Finish right away
                            animations.forEach {
                                it.skipToEnd()
                            }
                        } else {
                            val playTimeNanos = ((it - startTimeNanos) / durationScale).toLong()
                            onFrame(playTimeNanos)
                        }
                    }
                    // Suspend until duration scale is non-zero
                    if (durationScale == 0f) {
                        snapshotFlow { coroutineContext.durationScale }.first {
                            it > 0f
                        }
                    }
                }
            }
        }
    }

    private fun onFrame(playTimeNanos: Long) {
        var allFinished = true
        // Pulse new playtime
        animations.forEach {
            if (!it.isFinished) {
                it.onPlayTimeChanged(playTimeNanos)
            }
            // Check isFinished flag again after the animation pulse
            if (!it.isFinished) {
                allFinished = false
            }
        }
        isRunning = !allFinished
    }
}

/**
 * Creates an animation of type [T] that runs infinitely as a part of the given
 * [InfiniteTransition]. Any data type can be animated so long as it can be converted from and to
 * an [AnimationVector]. This conversion needs to be provided as a [typeConverter]. Some examples
 * of such [TwoWayConverter] are: [Int.VectorConverter][Int.Companion.VectorConverter],
 * [Dp.VectorConverter][Dp.Companion.VectorConverter],
 * [Size.VectorConverter][Size.Companion.VectorConverter], etc
 *
 * Once the animation is created, it will run from [initialValue] to [targetValue] and repeat.
 * Depending on the [RepeatMode] of the provided [animationSpec], the animation could either
 * restart after each iteration (i.e. [RepeatMode.Restart]), or reverse after each iteration (i.e
 * . [RepeatMode.Reverse]).
 *
 * If [initialValue] or [targetValue] is changed at any point during the animation, the animation
 * will be restarted with the new [initialValue] and [targetValue]. __Note__: this means
 * continuity will *not* be preserved.
 *
 * @sample androidx.compose.animation.core.samples.InfiniteTransitionAnimateValueSample
 *
 * @see [InfiniteTransition.animateFloat]
 * @see [androidx.compose.animation.animateColor]
 */
@Composable
fun <T, V : AnimationVector> InfiniteTransition.animateValue(
    initialValue: T,
    targetValue: T,
    typeConverter: TwoWayConverter<T, V>,
    animationSpec: InfiniteRepeatableSpec<T>
): State<T> {
    val transitionAnimation =
        remember {
            TransitionAnimationState(
                initialValue, targetValue, typeConverter, animationSpec
            )
        }

    SideEffect {
        if (initialValue != transitionAnimation.initialValue ||
            targetValue != transitionAnimation.targetValue
        ) {
            transitionAnimation.updateValues(
                initialValue = initialValue,
                targetValue = targetValue,
                animationSpec = animationSpec
            )
        }
    }

    DisposableEffect(transitionAnimation) {
        addAnimation(transitionAnimation)
        onDispose {
            removeAnimation(transitionAnimation)
        }
    }
    return transitionAnimation
}

/**
 * Creates an animation of Float type that runs infinitely as a part of the given
 * [InfiniteTransition].
 *
 * Once the animation is created, it will run from [initialValue] to [targetValue] and repeat.
 * Depending on the [RepeatMode] of the provided [animationSpec], the animation could either
 * restart after each iteration (i.e. [RepeatMode.Restart]), or reverse after each iteration (i.e
 * . [RepeatMode.Reverse]).
 *
 * If [initialValue] or [targetValue] is changed at any point during the animation, the animation
 * will be restarted with the new [initialValue] and [targetValue]. __Note__: this means
 * continuity will *not* be preserved.
 *
 * @sample androidx.compose.animation.core.samples.InfiniteTransitionSample
 *
 * @see [InfiniteTransition.animateValue]
 * @see [androidx.compose.animation.animateColor]
 */
@Composable
fun InfiniteTransition.animateFloat(
    initialValue: Float,
    targetValue: Float,
    animationSpec: InfiniteRepeatableSpec<Float>
): State<Float> =
    animateValue(initialValue, targetValue, Float.VectorConverter, animationSpec)
