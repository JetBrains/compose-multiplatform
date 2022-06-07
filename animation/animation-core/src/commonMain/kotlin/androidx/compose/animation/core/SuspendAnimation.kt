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

package androidx.compose.animation.core

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException

/**
 * Target based animation that animates from the given [initialValue] towards the [targetValue],
 * with an optional [initialVelocity]. The [initialVelocity] defaults to 0f. By default, a [spring]
 * will be used for the animation. An alternative [animationSpec] can be provided to replace the
 * default [spring]. On each frame, the [block] will be invoked with up-to-date value and velocity.
 *
 * This is a convenient method for Float animation. If there's a need to access more info related to
 * the animation such as start time, target, etc, consider using [AnimationState.animateTo].
 * To animate non-[Float] data types, consider the [animate] overload/variant for generic types.
 *
 * @sample androidx.compose.animation.core.samples.suspendAnimateFloatVariant
 * @see [AnimationState.animateTo]
 */
suspend fun animate(
    initialValue: Float,
    targetValue: Float,
    initialVelocity: Float = 0f,
    animationSpec: AnimationSpec<Float> = spring(),
    block: (value: Float, velocity: Float) -> Unit
) {
    animate(
        Float.VectorConverter,
        initialValue,
        targetValue,
        initialVelocity,
        animationSpec,
        block
    )
}

/**
 * Decay animation that slows down from the given [initialVelocity] starting at [initialValue] until
 * the velocity reaches 0. This is often used after a fling gesture.
 *
 * [animationSpec] defines the decay animation that will be used for this animation. Some options
 * for this [animationSpec] include: [splineBasedDecay][androidx.compose.animation
 * .splineBasedDecay] and [exponentialDecay]. [block] will be invoked on each animation frame
 * with up-to-date value and velocity.
 *
 * This is a convenient method for decay animation. If there's a need to access more info related to
 * the animation such as start time, target, etc, consider using [AnimationState<Float,
 * AnimationVector1D>.animateDecay].
 *
 * @see [AnimationState<Float, AnimationVector1D>.animateDecay]
 */
suspend fun animateDecay(
    initialValue: Float,
    initialVelocity: Float,
    animationSpec: FloatDecayAnimationSpec,
    block: (value: Float, velocity: Float) -> Unit
) {
    val anim = DecayAnimation(animationSpec, initialValue, initialVelocity)
    AnimationState(initialValue, initialVelocity).animate(anim) {
        block(value, velocityVector.value)
    }
}

/**
 * Target based animation for animating any data type [T], so long as [T] can be converted to an
 * [AnimationVector] using [typeConverter]. The animation will start from the [initialValue] and
 * animate to the [targetValue] value. The [initialVelocity] will be derived from an all-0
 * [AnimationVector] unless specified. [animationSpec] can be provided to create a specific look and
 * feel for the animation. By default, a [spring] will be used.
 *
 * This is a convenient method for target-based animation. If there's a need to access more info
 * related to the animation such as start time, target, etc, consider using
 * [AnimationState.animateTo].
 *
 * @see [AnimationState.animateTo]
 */
suspend fun <T, V : AnimationVector> animate(
    typeConverter: TwoWayConverter<T, V>,
    initialValue: T,
    targetValue: T,
    initialVelocity: T? = null,
    animationSpec: AnimationSpec<T> = spring(),
    block: (value: T, velocity: T) -> Unit
) {
    val initialVelocityVector = initialVelocity?.let { typeConverter.convertToVector(it) }
        ?: typeConverter.convertToVector(initialValue).newInstance()
    val anim = TargetBasedAnimation(
        animationSpec = animationSpec,
        initialValue = initialValue,
        targetValue = targetValue,
        typeConverter = typeConverter,
        initialVelocityVector = initialVelocityVector
    )
    AnimationState(typeConverter, initialValue, initialVelocityVector).animate(anim) {
        block(value, typeConverter.convertFromVector(velocityVector))
    }
}

/**
 * Target based animation that takes the value and velocity from the [AnimationState] as the
 * starting condition, and animate to the [targetValue], using the [animationSpec]. During the
 * animation, the given [AnimationState] will be updated with the up-to-date value/velocity,
 * frame time, etc.
 *
 * [sequentialAnimation] indicates whether the animation should use the
 * [AnimationState.lastFrameTimeNanos] as the starting time (if true), or start in a new frame. By
 * default, [sequentialAnimation] is false, to start the animation in a few frame. In cases where
 * an on-going animation is interrupted and a new animation is started to carry over the
 * momentum, using the interruption time (captured in [AnimationState.lastFrameTimeNanos] creates
 * a smoother animation.
 *
 * [block] will be invoked on every frame, and the [AnimationScope] will be checked against
 * cancellation before the animation continues. To cancel the animation from the [block], simply
 * call [AnimationScope.cancelAnimation].  After [AnimationScope.cancelAnimation] is called, [block]
 * will not be invoked again. The animation loop will exit after the [block] returns. All the
 * animation related info can be accessed via [AnimationScope].
 *
 * @sample androidx.compose.animation.core.samples.animateToOnAnimationState
 */
suspend fun <T, V : AnimationVector> AnimationState<T, V>.animateTo(
    targetValue: T,
    animationSpec: AnimationSpec<T> = spring(),
    // Indicates whether the animation should start from last frame
    sequentialAnimation: Boolean = false,
    block: AnimationScope<T, V>.() -> Unit = {}
) {
    val anim = TargetBasedAnimation(
        animationSpec = animationSpec,
        initialValue = value,
        targetValue = targetValue,
        typeConverter = typeConverter,
        initialVelocityVector = velocityVector
    )
    animate(
        anim,
        if (sequentialAnimation) lastFrameTimeNanos else AnimationConstants.UnspecifiedTime,
        block
    )
}

/**
 * Decay animation that slows down from the current velocity and value captured in [AnimationState]
 * until the velocity reaches 0. During the animation, the given [AnimationState] will be updated
 * with the up-to-date value/velocity, frame time, etc. This is often used to animate the result
 * of a fling gesture.
 *
 * [animationSpec] defines the decay animation that will be used for this animation. Some options
 * for [animationSpec] include: [splineBasedDecay][androidx.compose.animation.splineBasedDecay]
 * and [exponentialDecay].
 *
 * During the animation, [block] will be invoked on every frame, and the [AnimationScope] will be
 * checked against cancellation before the animation continues. To cancel the animation from the
 * [block], simply call [AnimationScope.cancelAnimation].  After [AnimationScope.cancelAnimation] is
 * called, [block] will not be invoked again. The animation loop will exit after the [block]
 * returns. All the animation related info can be accessed via [AnimationScope].
 *
 * [sequentialAnimation] indicates whether the animation should use the
 * [AnimationState.lastFrameTimeNanos] as the starting time (if true), or start in a new frame. By
 * default, [sequentialAnimation] is false, to start the animation in a few frame. In cases where
 * an on-going animation is interrupted and a new animation is started to carry over the
 * momentum, using the interruption time (captured in [AnimationState.lastFrameTimeNanos] creates
 * a smoother animation.
 */
suspend fun <T, V : AnimationVector> AnimationState<T, V>.animateDecay(
    animationSpec: DecayAnimationSpec<T>,
    // Indicates whether the animation should start from last frame
    sequentialAnimation: Boolean = false,
    block: AnimationScope<T, V>.() -> Unit = {}
) {
    val anim = DecayAnimation<T, V>(
        animationSpec = animationSpec,
        initialValue = value,
        initialVelocityVector = velocityVector,
        typeConverter = typeConverter
    )
    animate(
        anim,
        if (sequentialAnimation) lastFrameTimeNanos else AnimationConstants.UnspecifiedTime,
        block
    )
}

/**
 * This animation function runs the animation defined in the given [animation] from start to
 * finish. During the animation, the [AnimationState] will be updated with the up-to-date
 * value/velocity, frame time, etc.
 *
 * If [startTimeNanos] is provided, it will be used as the time that the animation was started. By
 * default, [startTimeNanos] is [AnimationConstants.UnspecifiedTime], meaning the animation will start in the next frame.
 *
 * For [Animation]s that use [AnimationSpec], consider using these more convenient APIs:
 * [animate], [AnimationState.animateTo], [animateDecay],
 * [AnimationState<Float, AnimationVector1D>.animateDecay]
 *
 * [block] will be invoked on every frame, and the [AnimationScope] will be checked against
 * cancellation before the animation continues. To cancel the animation from the [block], simply
 * call [AnimationScope.cancelAnimation].  After [AnimationScope.cancelAnimation] is called, [block]
 * will not be invoked again. The animation loop will exit after the [block] returns. All the
 * animation related info can be accessed via [AnimationScope].
 */
// TODO: This method uses AnimationState and Animation at the same time, it's potentially confusing
// as to which is the source of truth for initial value/velocity. Consider letting [Animation] have
// some suspend fun differently.
internal suspend fun <T, V : AnimationVector> AnimationState<T, V>.animate(
    animation: Animation<T, V>,
    startTimeNanos: Long = AnimationConstants.UnspecifiedTime,
    block: AnimationScope<T, V>.() -> Unit = {}
) {
    val initialValue = animation.getValueFromNanos(0)
    val initialVelocityVector = animation.getVelocityVectorFromNanos(0)
    var lateInitScope: AnimationScope<T, V>? = null
    try {
        if (startTimeNanos == AnimationConstants.UnspecifiedTime) {
            val durationScale = coroutineContext.durationScale
            animation.callWithFrameNanos {
                lateInitScope = AnimationScope(
                    initialValue = initialValue,
                    typeConverter = animation.typeConverter,
                    initialVelocityVector = initialVelocityVector,
                    lastFrameTimeNanos = it,
                    targetValue = animation.targetValue,
                    startTimeNanos = it,
                    isRunning = true,
                    onCancel = { isRunning = false }
                ).apply {
                    // First frame
                    doAnimationFrameWithScale(it, durationScale, animation, this@animate, block)
                }
            }
        } else {
            lateInitScope = AnimationScope(
                initialValue = initialValue,
                typeConverter = animation.typeConverter,
                initialVelocityVector = initialVelocityVector,
                lastFrameTimeNanos = startTimeNanos,
                targetValue = animation.targetValue,
                startTimeNanos = startTimeNanos,
                isRunning = true,
                onCancel = { isRunning = false }
            ).apply {
                // First frame
                doAnimationFrameWithScale(
                    startTimeNanos,
                    coroutineContext.durationScale,
                    animation,
                    this@animate,
                    block
                )
            }
        }
        // Subsequent frames
        while (lateInitScope!!.isRunning) {
            val durationScale = coroutineContext.durationScale
            animation.callWithFrameNanos {
                lateInitScope!!.doAnimationFrameWithScale(it, durationScale, animation, this, block)
            }
        }
        // End of animation
    } catch (e: CancellationException) {
        lateInitScope?.isRunning = false
        if (lateInitScope?.lastFrameTimeNanos == lastFrameTimeNanos) {
            // There hasn't been another animation.
            isRunning = false
        }
        throw e
    }
}

/**
 * Calls the [finite][withFrameNanos] or [infinite][withInfiniteAnimationFrameNanos]
 * variant of `withFrameNanos`, depending on the value of [Animation.isInfinite].
 */
private suspend fun <R, T, V : AnimationVector> Animation<T, V>.callWithFrameNanos(
    onFrame: (frameTimeNanos: Long) -> R
): R {
    return if (isInfinite) {
        withInfiniteAnimationFrameNanos(onFrame)
    } else {
        withFrameNanos {
            onFrame.invoke(it / AnimationDebugDurationScale)
        }
    }
}

internal val CoroutineContext.durationScale: Float
    get() {
        val scale = this[MotionDurationScale]?.scaleFactor ?: 1f
        check(scale >= 0f)
        return scale
    }

internal fun <T, V : AnimationVector> AnimationScope<T, V>.updateState(
    state: AnimationState<T, V>
) {
    state.value = value
    state.velocityVector.copyFrom(velocityVector)
    state.finishedTimeNanos = finishedTimeNanos
    state.lastFrameTimeNanos = lastFrameTimeNanos
    state.isRunning = isRunning
}

private fun <T, V : AnimationVector> AnimationScope<T, V>.doAnimationFrameWithScale(
    frameTimeNanos: Long,
    durationScale: Float,
    anim: Animation<T, V>,
    state: AnimationState<T, V>,
    block: AnimationScope<T, V>.() -> Unit
) {
    val playTimeNanos =
        if (durationScale == 0f) {
            anim.durationNanos
        } else {
            ((frameTimeNanos - startTimeNanos) / durationScale).toLong()
        }
    doAnimationFrame(frameTimeNanos, playTimeNanos, anim, state, block)
}

// Impl detail, invoked every frame.
private fun <T, V : AnimationVector> AnimationScope<T, V>.doAnimationFrame(
    frameTimeNanos: Long,
    playTimeNanos: Long,
    anim: Animation<T, V>,
    state: AnimationState<T, V>,
    block: AnimationScope<T, V>.() -> Unit
) {
    lastFrameTimeNanos = frameTimeNanos
    value = anim.getValueFromNanos(playTimeNanos)
    velocityVector = anim.getVelocityVectorFromNanos(playTimeNanos)
    val isLastFrame = anim.isFinishedFromNanos(playTimeNanos)
    if (isLastFrame) {
        // TODO: This could probably be a little more granular
        // TODO: end time isn't necessarily last frame time
        finishedTimeNanos = lastFrameTimeNanos
        isRunning = false
    }
    updateState(state)
    block()
}
