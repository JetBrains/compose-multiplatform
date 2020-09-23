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
 * @suppress
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
 * @param startValue the value that the animation will start from
 * @param endValue the value that the animation will end at
 * @param startVelocity the initial velocity to start the animation at
 * @suppress
 */
@VisibleForTesting(otherwise = 3 /*PACKAGE_PRIVATE*/)
fun <V : AnimationVector> VectorizedAnimationSpec<V>.createAnimation(
    startValue: V,
    endValue: V,
    startVelocity: V
): TargetBasedAnimation<V, V> =
    TargetBasedAnimation(
        animationSpec = this,
        startValue = startValue,
        endValue = endValue,
        startVelocityVector = startVelocity,
        converter = TwoWayConverter({ it }, { it })
    )

/**
 * Creates a [TargetBasedAnimation] from a given [VectorizedAnimationSpec] of [AnimationVector] type.
 *
 * @param startValue the value that the animation will start from
 * @param endValue the value that the animation will end at
 * @param startVelocityVector the initial velocity (in the form of [AnimationVector]) to start the
 *                            animation at.
 * @param converter a [TwoWayConverter] that converts the from [AnimationVector] to the animation
 *                  data type [T], and vice versa.
 *
 * @see TargetBasedAnimation
 */
internal fun <T, V : AnimationVector> VectorizedAnimationSpec<V>.createAnimation(
    startValue: T,
    endValue: T,
    startVelocityVector: V,
    converter: TwoWayConverter<T, V>
) = TargetBasedAnimation<T, V>(
    animationSpec = this,
    startValue = startValue,
    endValue = endValue,
    startVelocityVector = startVelocityVector,
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
 * @param startValue the start value of the animation
 * @param endValue the end value of the animation
 * @param startVelocity the start velocity (of type [T] of the animation
 * @param converter the [TwoWayConverter] that is used to convert animation type [T] from/to [V]
 */
fun <T, V : AnimationVector> TargetBasedAnimation(
    animationSpec: AnimationSpec<T>,
    startValue: T,
    endValue: T,
    startVelocity: T,
    converter: TwoWayConverter<T, V>
) = TargetBasedAnimation(
    animationSpec,
    startValue,
    endValue,
    converter,
    converter.convertToVector(startVelocity)
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
 * @param startValue the start value of the animation
 * @param endValue the end value of the animation
 * @param converter the [TwoWayConverter] that is used to convert animation type [T] from/to [V]
 * @param startVelocityVector the start velocity of the animation in the form of [AnimationVector]
 *
 * @see [TransitionAnimation]
 * @see [androidx.compose.animation.transition]
 * @see [AnimatedValue]
 */
class TargetBasedAnimation<T, V : AnimationVector> internal constructor(
    internal val animationSpec: VectorizedAnimationSpec<V>,
    startValue: T,
    val endValue: T,
    override val converter: TwoWayConverter<T, V>,
    startVelocityVector: V? = null
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
     * @param startValue the start value of the animation
     * @param endValue the end value of the animation
     * @param converter the [TwoWayConverter] that is used to convert animation type [T] from/to [V]
     * @param startVelocityVector the start velocity vector, null by default (meaning 0 velocity).
     */
    constructor(
        animationSpec: AnimationSpec<T>,
        startValue: T,
        endValue: T,
        converter: TwoWayConverter<T, V>,
        startVelocityVector: V? = null
    ) : this(
        animationSpec.vectorize(converter),
        startValue,
        endValue,
        converter,
        startVelocityVector
    )

    private val startValueVector = converter.convertToVector.invoke(startValue)
    private val endValueVector = converter.convertToVector.invoke(endValue)
    private val startVelocityVector =
        startVelocityVector ?: converter.convertToVector.invoke(startValue).newInstance()

    override fun getValue(playTime: Long): T {
        return if (playTime < durationMillis) {
            converter.convertFromVector.invoke(
                animationSpec.getValue(
                    playTime, startValueVector,
                    endValueVector, startVelocityVector
                )
            )
        } else {
            endValue
        }
    }

    override val durationMillis: Long = animationSpec.getDurationMillis(
        start = startValueVector,
        end = endValueVector,
        startVelocity = this.startVelocityVector
    )

    private val endVelocity = animationSpec.getEndVelocity(
        startValueVector,
        endValueVector,
        this.startVelocityVector
    )

    override fun getVelocityVector(playTime: Long): V {
        return if (playTime < durationMillis) {
            animationSpec.getVelocity(
                playTime,
                startValueVector,
                endValueVector,
                startVelocityVector
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
 * @param startValue starting value that will be passed to the decay animation
 * @param startVelocity starting velocity for the decay animation
 */
class DecayAnimation(
    private val anim: FloatDecayAnimationSpec,
    private val startValue: Float,
    private val startVelocity: Float = 0f
) : Animation<Float, AnimationVector1D> {
    private val target: Float = anim.getTarget(startValue, startVelocity)
    private val velocityVector: AnimationVector1D = AnimationVector1D(0f)
    override val converter: TwoWayConverter<Float, AnimationVector1D>
        get() = Float.VectorConverter

    // TODO: Remove the MissingNullability suppression when b/134803955 is fixed.
    @Suppress("AutoBoxing", "MissingNullability")
    override fun getValue(playTime: Long): Float {
        if (!isFinished(playTime)) {
            return anim.getValue(playTime, startValue, startVelocity)
        } else {
            return target
        }
    }

    override fun getVelocityVector(playTime: Long): AnimationVector1D {
        if (!isFinished(playTime)) {
            velocityVector.value = anim.getVelocity(playTime, startValue, startVelocity)
        } else {
            velocityVector.value = anim.absVelocityThreshold * sign(startVelocity)
        }
        return velocityVector
    }

    override val durationMillis: Long = anim.getDurationMillis(startValue, startVelocity)
}