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

package androidx.compose.animation.core

import androidx.compose.animation.core.AnimationEndReason.BoundReached
import androidx.compose.animation.core.AnimationEndReason.Finished
import androidx.compose.animation.core.AnimationEndReason.Interrupted
import androidx.compose.runtime.AtomicReference
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Uptime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope

/**
 * [Animatable] is a value holder that automatically animates its value when the value is
 * changed via [animateTo]. If [animateTo] is invoked during an ongoing value change animation,
 * a new animation will transition [Animatable] from its current value (i.e. value at the point of
 * interruption) to the new [targetValue]. This ensures that the value change is __always__
 * continuous using [animateTo]. If a [spring] animation (e.g. default animation) is used with
 * [animateTo], the velocity change will guarantee to be continuous as well.
 *
 * Unlike [AnimationState], [Animatable] ensures *mutual exclusiveness* on its animations. To
 * achieve this, when a new animation is started via [animateTo] (or [animateDecay]), any ongoing
 * animation will be canceled.
 *
 * @sample androidx.compose.animation.core.samples.AnimatableAnimateToGenericsType
 *
 * @param initialValue initial value of the animatable value holder
 * @param typeConverter A two-way converter that converts the given type [T] from and to
 *                      [AnimationVector]
 * @param visibilityThreshold Threshold at which the animation may round off to its target value.
 */
@Suppress("NotCloseable")
class Animatable<T, V : AnimationVector>(
    initialValue: T,
    val typeConverter: TwoWayConverter<T, V>,
    private val visibilityThreshold: T? = null
) {

    internal val internalState = AnimationState(
        typeConverter = typeConverter,
        initialValue = initialValue
    )

    /**
     * Current value of the animation.
     */
    val value: T
        get() = internalState.value

    /**
     * Velocity vector of the animation (in the form of [AnimationVector].
     */
    val velocityVector: V
        get() = internalState.velocityVector

    /**
     * Returns the velocity, converted from [velocityVector].
     */
    val velocity: T
        get() = typeConverter.convertFromVector(velocityVector)

    /**
     * Indicates whether the animation is running.
     */
    val isRunning: Boolean
        get() = currentJob.get() != null

    /**
     * The target of the current animation. If the animation finishes un-interrupted, it will
     * reach this target value.
     */
    var targetValue: T by mutableStateOf(initialValue)
        private set

    /**
     * Lower bound of the animation. Defaults to null, which means no lower bound. Bounds can be
     * changed using [updateBounds].
     *
     * Animation will stop as soon as *any* dimension specified in [lowerBound] is reached. For
     * example: For an Animatable<Offset> with an [lowerBound] set to Offset(100f, 200f), when
     * the [value].x drops below 100f *or* [value].y drops below 200f, the animation will stop.
     */
    var lowerBound: T? = null
        private set

    /**
     * Upper bound of the animation. Defaults to null, which means no upper bound. Bounds can be
     * changed using [updateBounds].
     *
     * Animation will stop as soon as *any* dimension specified in [upperBound] is reached. For
     * example: For an Animatable<Offset> with an [upperBound] set to Offset(100f, 200f), when
     * the [value].x exceeds 100f *or* [value].y exceeds 200f, the animation will stop.
     */
    var upperBound: T? = null
        private set

    private var currentJob = AtomicReference<Job?>(null)
    internal val defaultSpringSpec: SpringSpec<T> =
        SpringSpec(visibilityThreshold = visibilityThreshold)

    private val negativeInfinityBounds = createVector(Float.NEGATIVE_INFINITY)
    private val positiveInfinityBounds = createVector(Float.POSITIVE_INFINITY)

    private var lowerBoundVector: V = negativeInfinityBounds
    private var upperBoundVector: V = positiveInfinityBounds

    private fun createVector(value: Float): V {
        val newVector = typeConverter.convertToVector(this.value)
        for (i in 0 until newVector.size) {
            newVector[i] = value
        }
        return newVector
    }

    /**
     * Updates either [lowerBound] or [upperBound], or both. This will update
     * [Animatable.lowerBound] and/or [Animatable.upperBound] accordingly after a check to ensure
     * the provided [lowerBound] is no greater than [upperBound] in any dimension.
     *
     * Setting the bounds will immediate clamp the [value], only if the animation isn't running.
     * For the on-going animation, the value at the next frame update will be checked against the
     * bounds. If the value reaches the bound, then the animation will end with [BoundReached]
     * end reason.
     *
     * @param lowerBound lower bound of the animation. Defaults to the [Animatable.lowerBound]
     *                   that is currently set.
     * @param upperBound upper bound of the animation. Defaults to the [Animatable.upperBound]
     *                   that is currently set.
     * @throws [IllegalStateException] if the [lowerBound] is greater than [upperBound] in any
     *                                 dimension.
     */
    fun updateBounds(lowerBound: T? = this.lowerBound, upperBound: T? = this.upperBound) {
        val lowerBoundVector = lowerBound?.run { typeConverter.convertToVector(this) }
            ?: negativeInfinityBounds

        val upperBoundVector = upperBound?.run { typeConverter.convertToVector(this) }
            ?: positiveInfinityBounds

        for (i in 0 until lowerBoundVector.size) {
            // TODO: is this check too aggressive?
            check(lowerBoundVector[i] <= upperBoundVector[i]) {
                "Lower bound must be no greater than upper bound on *all* dimensions. The " +
                    "provided lower bound: $lowerBoundVector is greater than upper bound " +
                    "$upperBoundVector on index $i"
            }
        }
        // After the correctness check:
        this.lowerBoundVector = lowerBoundVector
        this.upperBoundVector = upperBoundVector

        this.upperBound = upperBound
        this.lowerBound = lowerBound
        if (!isRunning) {
            val clampedValue = clampToBounds(value)
            if (clampedValue != value) {
                this.internalState.value = value
            }
        }
    }

    /**
     * Sets the target value, which effectively starts an animation to change the value from [value]
     * to the [targetValue]. If there is already an animation in-flight, this method will cancel
     * the ongoing animation and start a new animation continuing the current [value] and
     * [velocity]. It's recommended to set the optional [initialVelocity] only when [animateTo] is
     * used immediately after a fling. In most of the other cases, altering velocity would result
     * in visual discontinuity.
     *
     * The animation will use the provided [animationSpec] to animate the value towards the
     * [targetValue]. When no [animationSpec] is specified, a [spring] will be used.  [block] will
     * be invoked on each animation frame.
     *
     * Returns an [AnimationResult] object. It contains: 1) the reason for ending the animation,
     * and 2) an end state of the animation. The reason for ending the animation can be any of the
     * following three:
     * -  [Finished], when the animation finishes successfully without any interruption,
     * -  [Interrupted], if/when the animation gets interrupted by 1) another call to start an
     *    animation (i.e. [animateTo]/[animateDecay]), 2) [Animatable.stop], or 3)
     *    [Animatable.snapTo].
     * -  [BoundReached] If the animation reaches the either [lowerBound] or [upperBound] in any
     *    dimension, the animation will end with [BoundReached] being the end reason.
     *
     * __Note__: once the animation ends, its velocity will be reset to 0. The animation state at
     * the point of interruption/reaching bound is captured in the returned [AnimationResult].
     * If there's a need to continue the momentum that the animation had before it was interrupted
     * or reached the bound, it's recommended to use the velocity in the returned
     * [AnimationResult.endState] to start another animation.
     *
     * @sample androidx.compose.animation.core.samples.AnimatableAnimateToGenericsType
     * @sample androidx.compose.animation.core.samples.AnimatableFadeIn
     */
    suspend fun animateTo(
        targetValue: T,
        animationSpec: AnimationSpec<T> = defaultSpringSpec,
        initialVelocity: T = velocity,
        block: (Animatable<T, V>.() -> Unit)? = null
    ): AnimationResult<T, V> {
        internalState.velocityVector = typeConverter.convertToVector(initialVelocity)
        val anim = TargetBasedAnimation(
            animationSpec = animationSpec,
            initialValue = value,
            targetValue = targetValue,
            typeConverter = typeConverter,
            initialVelocity = initialVelocity
        )
        return runAnimation(anim, block)
    }

    /**
     * Starts an animation that slows down from the given [initialVelocity] starting at
     * current [Animatable.value] until the velocity reaches 0. If there's already an ongoing
     * animation, the animation in-flight will be immediately cancelled. Decay animation is often
     * used after a fling gesture.
     *
     * [animationSpec] defines the decay animation that will be used for this animation. Some
     * options for this [animationSpec] include: [androidFlingDecay][androidx.compose
     * .foundation.animation.androidFlingDecay] and [exponentialDecay]. [block] will be
     * invoked on each animation frame.
     *
     * Returns an [AnimationResult] object, that contains the [reason][AnimationEndReason] for
     * ending the animation, and an end state of the animation. The reason for ending the animation
     * will be [Finished], when the animation finishes successfully without any interruption,
     * If/when the animation gets interrupted by 1) another call to start an animation
     * (i.e. [animateTo]/[animateDecay]), 2) [stop], or 3) [snapTo]
     * [Interrupted] will be returned. If the animation reaches the either [lowerBound] or
     * [upperBound] in any dimension, the animation will end with [BoundReached] being the
     * end reason.
     *
     * Note, once the animation ends, its velocity will be reset to 0. If there's a need to
     * continue the momentum before the animation gets interrupted or reaches the bound, it's
     * recommended to use the velocity in the returned [AnimationResult.endState] to start
     * another animation.
     *
     * @sample androidx.compose.animation.core.samples.AnimatableDecayAndAnimateToSample
     */
    suspend fun animateDecay(
        initialVelocity: T,
        animationSpec: DecayAnimationSpec<T>,
        block: (Animatable<T, V>.() -> Unit)? = null
    ): AnimationResult<T, V> {
        internalState.velocityVector = typeConverter.convertToVector(initialVelocity)
        val anim = DecayAnimation(
            animationSpec = animationSpec,
            initialValue = value,
            initialVelocityVector = velocityVector.copy(),
            typeConverter = typeConverter
        )
        return runAnimation(anim, block)
    }

    // All the different types of animation code paths eventually converge to this method.
    private suspend fun runAnimation(
        animation: Animation<T, V>,
        block: (Animatable<T, V>.() -> Unit)?
    ): AnimationResult<T, V> {
        targetValue = animation.targetValue
        return coroutineScope {
            // Update current job, and cancel old job (i.e. existing animation)
            val oldJob = currentJob.getAndSet(coroutineContext[Job])
            oldJob?.also { it.cancelAnimation() } != null

            val startState = internalState.copy(finishedTime = Uptime.Unspecified)
            val endReason = try {
                var clampingNeeded = false
                startState.animate(
                    animation,
                    internalState.lastFrameTime
                ) {
                    if (currentJob.get() == coroutineContext[Job]) {
                        updateState(internalState)
                        val clamped = clampToBounds(value)
                        if (clamped != value) {
                            internalState.value = clamped
                            startState.value = clamped
                            block?.invoke(this@Animatable)
                            cancelAnimation()
                            clampingNeeded = true
                        } else {
                            block?.invoke(this@Animatable)
                        }
                    } else {
                        // Cancelled by another job *initiated by* Animatable
                        cancelAnimation()
                    }
                }
                if (startState.isFinished) {
                    Finished
                } else {
                    if (clampingNeeded) BoundReached else Interrupted
                }
            } catch (e: CancellationException) {
                if (e is AnimationCancellationException) {
                    Interrupted
                } else {
                    // External cancellation. Clean up internal states first, then throw.
                    if (currentJob.compareAndSet(coroutineContext[Job], null)) {
                        endAnimation()
                    }
                    throw e
                }
            }

            // Reset the animation if it wasn't interrupted
            if (currentJob.compareAndSet(coroutineContext[Job], null)) {
                endAnimation()
            }

            AnimationResult(startState, endReason)
        }
    }

    private fun clampToBounds(value: T): T {
        if (
            lowerBoundVector == negativeInfinityBounds &&
            upperBoundVector == negativeInfinityBounds
        ) {
            // Expect this to be the most common use case
            return value
        }
        val valueVector = typeConverter.convertToVector(value)
        var clamped = false
        for (i in 0 until valueVector.size) {
            if (valueVector[i] < lowerBoundVector[i] || valueVector[i] > upperBoundVector[i]) {
                clamped = true
                valueVector[i] =
                    valueVector[i].coerceIn(lowerBoundVector[i], upperBoundVector[i])
            }
        }
        if (clamped) {
            return typeConverter.convertFromVector(valueVector)
        } else {
            return value
        }
    }

    private fun endAnimation() {
        // Reset velocity
        internalState.apply {
            velocityVector.reset()
            lastFrameTime = Uptime.Unspecified
        }
    }

    /**
     * Sets the current value to the target value immediately, without any animation. This will
     * also cancel any on-going animation
     *
     * @param targetValue The new target value to set [value] to.
     */
    fun snapTo(targetValue: T) {
        stop()
        internalState.value = targetValue
        this.targetValue = targetValue
    }

    /**
     * Stops any on-going animation. No op if no animation is running. Note that this method does
     * not skip the animation value to its target value. Rather the animation will be stopped in its
     * track.
     */
    fun stop() {
        currentJob.getAndSet(null)?.cancelAnimation()
        endAnimation()
    }

    /**
     * Returns a [State] representing the current [value] of this animation. This allows
     * hoisting the animation's current value without causing unnecessary recompositions
     * when the value changes.
     */
    fun asState(): State<T> = internalState

    private fun Job.cancelAnimation() {
        cancel(AnimationCancellationException())
    }

    private class AnimationCancellationException : CancellationException(
        "Interrupted by another animation, or stopped."
    )
}

/**
 * This [Animatable] function creates a float value holder that automatically
 * animates its value when the value is changed via [animateTo]. [Animatable] supports value
 * change during an ongoing value change animation. When that happens, a new animation will
 * transition [Animatable] from its current value (i.e. value at the point of interruption) to the
 * new target. This ensures that the value change is *always* continuous using [animateTo]. If
 * [spring] animation (i.e. default animation) is used with [animateTo], the velocity change will
 * be guaranteed to be continuous as well.
 *
 * Unlike [AnimationState], [Animatable] ensures mutual exclusiveness on its animation. To
 * do so, when a new animation is started via [animateTo] (or [animateDecay]), any ongoing
 * animation job will be cancelled.
 *
 * @sample androidx.compose.animation.core.samples.AnimatableDecayAndAnimateToSample
 *
 * @param initialValue initial value of the animatable value holder
 * @param visibilityThreshold Threshold at which the animation may round off to its target value.
 *                            [Spring.DefaultDisplacementThreshold] by default.
 */
fun Animatable(
    initialValue: Float,
    visibilityThreshold: Float = Spring.DefaultDisplacementThreshold
) = Animatable(
    initialValue,
    Float.VectorConverter,
    visibilityThreshold
)

// TODO: Consider some version of @Composable fun<T, V: AnimationVector> Animatable<T, V>.animateTo
/**
 * AnimationResult contains information about an animation at the end of the animation. [endState]
 * captures the value/velocity/frame time, etc of the animation at its last frame. It can be
 * useful for starting another animation to continue the velocity from the previously interrupted
 * animation. [endReason] describes why the animation ended, it could be one of the following three:
 * -  [Finished], when the animation finishes successfully without any interruption
 * -  [Interrupted], if/when the animation gets interrupted by 1) another call to start an
 *    animation (i.e. [animateTo]/[animateDecay]), 2) [Animatable.stop], or 3)
 *    [Animatable.snapTo].
 * -  [BoundReached] If the animation reaches the either [lowerBound][Animatable.lowerBound] or
 *    [upperBound][Animatable.upperBound] in any dimension, the animation will end with
 *    [BoundReached] being the end reason.
 *
 * @sample androidx.compose.animation.core.samples.AnimatableAnimationResultSample
 */
class AnimationResult<T, V : AnimationVector>(
    val endState: AnimationState<T, V>,
    val endReason: AnimationEndReason
)