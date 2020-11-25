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

import androidx.compose.ui.util.annotation.VisibleForTesting
import kotlin.math.sign

/**
 * This interface provides a convenient way to query from an [VectorizedAnimationSpec] or
 * [FloatDecayAnimationSpec]: It spares the need to pass the starting conditions and in some cases
 * ending condition for each value or velocity query, and instead only requires the play time to be
 * passed for such queries.
 *
 * The implementation of this interface should cache the starting conditions and ending
 * conditions of animations as needed.
 *
 * __Note__: [Animation] does not track the lifecycle of an animation. It merely reacts to play time
 * change and returns the new value/velocity as a result. It can be used as a building block for
 * more lifecycle aware animations. In contrast, [AnimatedValue] and [TransitionAnimation] are
 * stateful and manage their own lifecycles, and subscribe/unsubscribe from an
 * [AnimationClockObservable] as needed.
 *
 * @see [AnimatedValue]
 * @see [androidx.compose.animation.transition]
 */
interface Animation<T, V : AnimationVector> {
    /**
     * This amount of time in milliseconds that the animation will run before it finishes
     */
    val durationMillis: Long

    /**
     * The [TwoWayConverter] that will be used to convert value/velocity from any arbitrary data
     * type to [AnimationVector]. This makes it possible to animate different dimensions of the
     * data object independently (e.g. x/y dimensions of the position data).
     */
    val converter: TwoWayConverter<T, V>

    /**
     * This is the value that the [Animation] will reach when it finishes uninterrupted.
     */
    val targetValue: T

    /**
     * Returns the value of the animation at the given play time.
     *
     * @param playTime the play time that is used to determine the value of the animation.
     */
    fun getValue(playTime: Long): T

    /**
     * Returns the velocity (in [AnimationVector] form) of the animation at the given play time.
     *
     * @param playTime the play time that is used to calculate the velocity of the animation.
     */
    fun getVelocityVector(playTime: Long): V

    /**
     * Returns whether the animation is finished at the given play time.
     *
     * @param playTime the play time used to determine whether the animation is finished.
     */
    fun isFinished(playTime: Long): Boolean {
        return playTime >= durationMillis
    }
}

/**
 * Returns the velocity of the animation at the given play time.
 *
 * @param playTime the play time that is used to calculate the velocity of the animation.
 */
internal fun <T, V : AnimationVector> Animation<T, V>.getVelocity(playTime: Long): T =
    converter.convertFromVector(getVelocityVector(playTime))

/**
 * Creates a [TargetBasedAnimation] from a given [VectorizedAnimationSpec] of [AnimationVector] type. This
 * convenient method is intended for when the value being animated (i.e. start value, end value,
 * etc) is of [AnimationVector] type.
 *
 * @param initialValue the value that the animation will start from
 * @param targetValue the value that the animation will end at
 * @param initialVelocity the initial velocity to start the animation at
 * @suppress
 */
@VisibleForTesting(otherwise = 3 /*PACKAGE_PRIVATE*/)
fun <V : AnimationVector> VectorizedAnimationSpec<V>.createAnimation(
    initialValue: V,
    targetValue: V,
    initialVelocity: V
): TargetBasedAnimation<V, V> =
    TargetBasedAnimation(
        animationSpec = this,
        initialValue = initialValue,
        targetValue = targetValue,
        initialVelocityVector = initialVelocity,
        converter = TwoWayConverter({ it }, { it })
    )

/**
 * Creates a [TargetBasedAnimation] from a given [VectorizedAnimationSpec] of [AnimationVector] type.
 *
 * @param initialValue the value that the animation will start from
 * @param targetValue the value that the animation will end at
 * @param initialVelocityVector the initial velocity (in the form of [AnimationVector]) to start the
 *                            animation at.
 * @param converter a [TwoWayConverter] that converts the from [AnimationVector] to the animation
 *                  data type [T], and vice versa.
 *
 * @see TargetBasedAnimation
 */
internal fun <T, V : AnimationVector> VectorizedAnimationSpec<V>.createAnimation(
    initialValue: T,
    targetValue: T,
    initialVelocityVector: V,
    converter: TwoWayConverter<T, V>
) = TargetBasedAnimation<T, V>(
    animationSpec = this,
    initialValue = initialValue,
    targetValue = targetValue,
    initialVelocityVector = initialVelocityVector,
    converter = converter
)

/**
 * Creates a [TargetBasedAnimation] with the given start/end conditions of the animation, and
 * the provided [animationSpec].
 *
 * The resulting [Animation] assumes that the start value and velocity, as well as end value do
 * not change throughout the animation, and cache these values. This caching enables much more
 * convenient query for animation value and velocity (where only playtime needs to be passed
 * into the methods).
 *
 * __Note__: When interruptions happen to the [TargetBasedAnimation], a new instance should
 * be created that use the current value and velocity as the starting conditions. This type of
 * interruption handling is the default behavior for both [AnimatedValue] and
 * [TransitionAnimation]. Consider using those APIs for the interruption handling, as well as
 * built-in animation lifecycle management.
 *
 * @param animationSpec the [AnimationSpec] that will be used to calculate value/velocity
 * @param initialValue the start value of the animation
 * @param targetValue the end value of the animation
 * @param initialVelocity the start velocity (of type [T] of the animation
 * @param converter the [TwoWayConverter] that is used to convert animation type [T] from/to [V]
 */
fun <T, V : AnimationVector> TargetBasedAnimation(
    animationSpec: AnimationSpec<T>,
    initialValue: T,
    targetValue: T,
    initialVelocity: T,
    converter: TwoWayConverter<T, V>
) = TargetBasedAnimation(
    animationSpec,
    initialValue,
    targetValue,
    converter,
    converter.convertToVector(initialVelocity)
)

/**
 * This is a convenient animation wrapper class that works for all target based animations, i.e.
 * animations that has a pre-defined end value, unlike decay.
 *
 * It assumes that the starting value and velocity, as well as ending value do not change throughout
 * the animation, and cache these values. This caching enables much more convenient query for
 * animation value and velocity (where only playtime needs to be passed into the methods).
 *
 * __Note__: When interruptions happen to the [TargetBasedAnimation], a new instance should
 * be created that use the current value and velocity as the starting conditions. This type of
 * interruption handling is the default behavior for both [AnimatedValue] and
 * [TransitionAnimation]. Consider using those APIs for the interruption handling, as well as
 * built-in animation lifecycle management.
 *
 * @param animationSpec the [VectorizedAnimationSpec] that will be used to calculate value/velocity
 * @param initialValue the start value of the animation
 * @param targetValue the end value of the animation
 * @param converter the [TwoWayConverter] that is used to convert animation type [T] from/to [V]
 * @param initialVelocityVector the start velocity of the animation in the form of [AnimationVector]
 *
 * @see [TransitionAnimation]
 * @see [androidx.compose.animation.transition]
 * @see [AnimatedValue]
 */
class TargetBasedAnimation<T, V : AnimationVector> internal constructor(
    internal val animationSpec: VectorizedAnimationSpec<V>,
    initialValue: T,
    override val targetValue: T,
    override val converter: TwoWayConverter<T, V>,
    initialVelocityVector: V? = null
) : Animation<T, V> {

    /**
     * Creates a [TargetBasedAnimation] with the given start/end conditions of the animation, and
     * the provided [animationSpec].
     *
     * The resulting [Animation] assumes that the start value and velocity, as well as end value do
     * not change throughout the animation, and cache these values. This caching enables much more
     * convenient query for animation value and velocity (where only playtime needs to be passed
     * into the methods).
     *
     * __Note__: When interruptions happen to the [TargetBasedAnimation], a new instance should
     * be created that use the current value and velocity as the starting conditions. This type of
     * interruption handling is the default behavior for both [AnimatedValue] and
     * [TransitionAnimation]. Consider using those APIs for the interruption handling, as well as
     * built-in animation lifecycle management.
     *
     * @param animationSpec the [AnimationSpec] that will be used to calculate value/velocity
     * @param initialValue the start value of the animation
     * @param targetValue the end value of the animation
     * @param converter the [TwoWayConverter] that is used to convert animation type [T] from/to [V]
     * @param initialVelocityVector the start velocity vector, null by default (meaning 0 velocity).
     */
    constructor(
        animationSpec: AnimationSpec<T>,
        initialValue: T,
        targetValue: T,
        converter: TwoWayConverter<T, V>,
        initialVelocityVector: V? = null
    ) : this(
        animationSpec.vectorize(converter),
        initialValue,
        targetValue,
        converter,
        initialVelocityVector
    )

    private val initialValueVector = converter.convertToVector.invoke(initialValue)
    private val targetValueVector = converter.convertToVector.invoke(targetValue)
    private val initialVelocityVector =
        initialVelocityVector ?: converter.convertToVector.invoke(initialValue).newInstance()

    override fun getValue(playTime: Long): T {
        return if (playTime < durationMillis) {
            converter.convertFromVector.invoke(
                animationSpec.getValue(
                    playTime, initialValueVector,
                    targetValueVector, initialVelocityVector
                )
            )
        } else {
            targetValue
        }
    }

    override val durationMillis: Long = animationSpec.getDurationMillis(
        start = initialValueVector,
        end = targetValueVector,
        startVelocity = this.initialVelocityVector
    )

    private val endVelocity = animationSpec.getEndVelocity(
        initialValueVector,
        targetValueVector,
        this.initialVelocityVector
    )

    override fun getVelocityVector(playTime: Long): V {
        return if (playTime < durationMillis) {
            animationSpec.getVelocity(
                playTime,
                initialValueVector,
                targetValueVector,
                initialVelocityVector
            )
        } else {
            endVelocity
        }
    }
}

/**
 * Fixed Decay animation wraps around a [FloatDecayAnimationSpec] and assumes its starting value and
 * velocity never change throughout the animation.
 *
 * @param anim decay animation that will be used
 * @param initialValue starting value that will be passed to the decay animation
 * @param initialVelocity starting velocity for the decay animation
 */
class DecayAnimation(
    private val anim: FloatDecayAnimationSpec,
    private val initialValue: Float,
    private val initialVelocity: Float = 0f
) : Animation<Float, AnimationVector1D> {
    override val targetValue: Float = anim.getTarget(initialValue, initialVelocity)
    private val velocityVector: AnimationVector1D = AnimationVector1D(0f)
    override val converter: TwoWayConverter<Float, AnimationVector1D>
        get() = Float.VectorConverter

    // TODO: Remove the MissingNullability suppression when b/134803955 is fixed.
    @Suppress("AutoBoxing", "MissingNullability")
    override fun getValue(playTime: Long): Float {
        if (!isFinished(playTime)) {
            return anim.getValue(playTime, initialValue, initialVelocity)
        } else {
            return targetValue
        }
    }

    override fun getVelocityVector(playTime: Long): AnimationVector1D {
        if (!isFinished(playTime)) {
            velocityVector.value = anim.getVelocity(playTime, initialValue, initialVelocity)
        } else {
            velocityVector.value = anim.absVelocityThreshold * sign(initialVelocity)
        }
        return velocityVector
    }

    override val durationMillis: Long = anim.getDurationMillis(initialValue, initialVelocity)
}