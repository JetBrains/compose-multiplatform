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

import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis

/**
 * [FloatAnimationSpec] interface is similar to [VectorizedAnimationSpec], except it deals exclusively with
 * floats.
 *
 * Like [VectorizedAnimationSpec], [FloatAnimationSpec] is entirely stateless as well. It requires start/end
 * values and start velocity to be passed in for the query of velocity and value of the animation.
 * The [FloatAnimationSpec] itself stores only the animation configuration (such as the
 * delay, duration and easing curve for [FloatTweenSpec], or spring constants for
 * [FloatSpringSpec].
 *
 * A [FloatAnimationSpec] can be converted to an [VectorizedAnimationSpec] using [vectorize].
 *
 * @see [VectorizedAnimationSpec]
 */
interface FloatAnimationSpec : AnimationSpec<Float> {
    /**
     * Calculates the value of the animation at given the playtime, with the provided start/end
     * values, and start velocity.
     *
     * @param playTime time since the start of the animation
     * @param start start value of the animation
     * @param end end value of the animation
     * @param startVelocity start velocity of the animation
     */
    fun getValue(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float

    /**
     * Calculates the velocity of the animation at given the playtime, with the provided start/end
     * values, and start velocity.
     *
     * @param playTime time since the start of the animation
     * @param start start value of the animation
     * @param end end value of the animation
     * @param startVelocity start velocity of the animation
     */
    fun getVelocity(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float

    /**
     * Calculates the end velocity of the animation with the provided start/end values, and start
     * velocity. For duration-based animations, end velocity will be the velocity of the
     * animation at the duration time. This is also the default assumption. However, for
     * spring animations, the transient trailing velocity will be snapped to zero.
     *
     * @param start start value of the animation
     * @param end end value of the animation
     * @param startVelocity start velocity of the animation
     */
    fun getEndVelocity(
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float = getVelocity(getDurationMillis(start, end, startVelocity), start, end, startVelocity)

    /**
     * Calculates the duration of an animation. For duration-based animations, this will return the
     * pre-defined duration. For physics-based animations, the duration will be estimated based on
     * the physics configuration (such as spring stiffness, damping ratio, visibility threshold)
     * as well as the [start], [end] values, and [startVelocity].
     *
     * __Note__: this may be a computation that is expensive - especially with spring based
     * animations
     *
     * @param start start value of the animation
     * @param end end value of the animation
     * @param startVelocity start velocity of the animation
     */
    fun getDurationMillis(
        start: Float,
        end: Float,
        startVelocity: Float
    ): Long

    /**
     * Create an [VectorizedAnimationSpec] that animates [AnimationVector] from a [FloatAnimationSpec]. Every
     * dimension of the [AnimationVector] will be animated using the given [FloatAnimationSpec].
     */
    override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<Float, V>) =
        VectorizedFloatAnimationSpec<V>(this)
}

/**
 * [FloatSpringSpec] animation uses a spring animation to animate a [Float] value. Its
 * configuration can be tuned via adjusting the spring parameters, namely damping ratio and
 * stiffness.
 *
 * @param dampingRatio damping ratio of the spring. Defaults to [Spring.DampingRatioNoBouncy]
 * @param stiffness Stiffness of the spring. Defaults to [Spring.StiffnessMedium]
 * @param visibilityThreshold The value threshold such that the animation is no longer
 *                              significant. e.g. 1px for translation animations. Defaults to
 *                              [Spring.DefaultDisplacementThreshold]
 */
class FloatSpringSpec(
    val dampingRatio: Float = Spring.DampingRatioNoBouncy,
    val stiffness: Float = Spring.StiffnessMedium,
    private val visibilityThreshold: Float = Spring.DefaultDisplacementThreshold
) : FloatAnimationSpec {

    private val spring = SpringSimulation(1f).also {
        it.dampingRatio = dampingRatio
        it.stiffness = stiffness
    }

    override fun getValue(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float {
        spring.finalPosition = end
        val value = spring.updateValues(start, startVelocity, playTime).value
        return value
    }

    override fun getVelocity(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float {
        spring.finalPosition = end
        val velocity = spring.updateValues(start, startVelocity, playTime).velocity
        return velocity
    }

    override fun getEndVelocity(
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float = 0f

    override fun getDurationMillis(start: Float, end: Float, startVelocity: Float): Long =
        estimateAnimationDurationMillis(
            stiffness = spring.stiffness,
            dampingRatio = spring.dampingRatio,
            initialDisplacement = (start - end) / visibilityThreshold,
            initialVelocity = startVelocity / visibilityThreshold,
            delta = 1f
        )
}

/**
 * [FloatTweenSpec] animates a Float value from any start value to any end value using a provided
 * [easing] function. The animation will finish within the [duration] time. Unless a [delay] is
 * specified, the animation will start right away.
 *
 * @param duration the amount of time (in milliseconds) the animation will take to finish.
 *                     Defaults to [DefaultDuration]
 * @param delay the amount of time the animation will wait before it starts running. Defaults to 0.
 * @param easing the easing function that will be used to interoplate between the start and end
 *               value of the animation. Defaults to [FastOutSlowInEasing].
 */
class FloatTweenSpec(
    val duration: Int = DefaultDurationMillis,
    val delay: Int = 0,
    private val easing: Easing = FastOutSlowInEasing
) : FloatAnimationSpec {
    override fun getValue(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float {
        val clampedPlayTime = clampPlayTime(playTime)
        val rawFraction = if (duration == 0) 1f else clampedPlayTime / duration.toFloat()
        val fraction = easing.transform(rawFraction.coerceIn(0f, 1f))
        return lerp(start, end, fraction)
    }

    private fun clampPlayTime(playTime: Long): Long {
        return (playTime - delay).coerceIn(0, duration.toLong())
    }

    override fun getDurationMillis(start: Float, end: Float, startVelocity: Float): Long {
        return delay + duration.toLong()
    }

    // Calculate velocity by difference between the current value and the value 1 ms ago. This is a
    // preliminary way of calculating velocity used by easing curve based animations, and keyframe
    // animations. Physics-based animations give a much more accurate velocity.
    override fun getVelocity(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float {
        val clampedPlayTime = clampPlayTime(playTime)
        if (clampedPlayTime < 0) {
            return 0f
        } else if (clampedPlayTime == 0L) {
            return startVelocity
        }
        val startNum = getValue(clampedPlayTime - 1, start, end, startVelocity)
        val endNum = getValue(clampedPlayTime, start, end, startVelocity)
        return (endNum - startNum) * 1000f
    }
}
