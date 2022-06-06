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
import kotlin.math.min
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * [VectorizedAnimationSpec]s are stateless vector based animation specifications. They do
 * not assume any starting/ending conditions. Nor do they manage a lifecycle. All it stores is the
 * configuration that is particular to the type of the animation. easing and duration for
 * [VectorizedTweenSpec]s, or spring constants for [VectorizedSpringSpec]s. Its stateless nature
 * allows the same [VectorizedAnimationSpec] to be reused by a few different running animations
 * with different starting and ending values. More importantly, it allows the system to reuse the
 * same animation spec when the animation target changes in-flight.
 *
 * Since [VectorizedAnimationSpec]s are stateless, it requires starting value/velocity and ending
 * value to be passed in, along with playtime, to calculate the value or velocity at that time. Play
 * time here is the progress of the animation in terms of milliseconds, where 0 means the start
 * of the animation and [getDurationNanos] returns the play time for the end of the animation.
 *
 * __Note__: For use cases where the starting values/velocity and ending values aren't expected
 * to change, it is recommended to use [Animation] that caches these static values and hence
 * does not require them to be supplied in the value/velocity calculation.
 *
 * @see Animation
 */
@JvmDefaultWithCompatibility
interface VectorizedAnimationSpec<V : AnimationVector> {
    /**
     * Whether or not the [VectorizedAnimationSpec] specifies an infinite animation. That is, one
     * that will not finish by itself, one that needs an external action to stop. For examples, an
     * indeterminate progress bar, which will only stop when it is removed from the composition.
     */
    val isInfinite: Boolean

    /**
     * Calculates the value of the animation at given the playtime, with the provided start/end
     * values, and start velocity.
     *
     * @param playTimeNanos time since the start of the animation
     * @param initialValue start value of the animation
     * @param targetValue end value of the animation
     * @param initialVelocity start velocity of the animation
     */
    fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V

    /**
     * Calculates the velocity of the animation at given the playtime, with the provided start/end
     * values, and start velocity.
     *
     * @param playTimeNanos time since the start of the animation
     * @param initialValue start value of the animation
     * @param targetValue end value of the animation
     * @param initialVelocity start velocity of the animation
     */
    fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V

    /**
     * Calculates the duration of an animation. For duration-based animations, this will return the
     * pre-defined duration. For physics-based animations, the duration will be estimated based on
     * the physics configuration (such as spring stiffness, damping ratio, visibility threshold)
     * as well as the [initialValue], [targetValue] values, and [initialVelocity].
     *
     * @param initialValue start value of the animation
     * @param targetValue end value of the animation
     * @param initialVelocity start velocity of the animation
     */
    @Suppress("MethodNameUnits")
    fun getDurationNanos(
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): Long

    /**
     * Calculates the end velocity of the animation with the provided start/end values, and start
     * velocity. For duration-based animations, end velocity will be the velocity of the
     * animation at the duration time. This is also the default assumption. However, for
     * physics-based animations, end velocity is an [AnimationVector] of 0s.
     *
     * @param initialValue start value of the animation
     * @param targetValue end value of the animation
     * @param initialVelocity start velocity of the animation
     */
    fun getEndVelocity(
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V = getVelocityFromNanos(
        getDurationNanos(initialValue, targetValue, initialVelocity),
        initialValue,
        targetValue,
        initialVelocity
    )
}

/**
 * Calculates the duration of an animation. For duration-based animations, this will return the
 * pre-defined duration. For physics-based animations, the duration will be estimated based on
 * the physics configuration (such as spring stiffness, damping ratio, visibility threshold)
 * as well as the [initialValue], [targetValue] values, and [initialVelocity].
 *
 * @param initialValue start value of the animation
 * @param targetValue end value of the animation
 * @param initialVelocity start velocity of the animation
 */
internal fun <V : AnimationVector> VectorizedAnimationSpec<V>.getDurationMillis(
    initialValue: V,
    targetValue: V,
    initialVelocity: V
): Long = getDurationNanos(initialValue, targetValue, initialVelocity) / MillisToNanos

/**
 * Calculates the value of the animation at given the playtime, with the provided start/end
 * values, and start velocity.
 *
 * @param playTimeMillis time since the start of the animation
 * @param start start value of the animation
 * @param end end value of the animation
 * @param startVelocity start velocity of the animation
 */
// TODO: Move tests off this API
internal fun <V : AnimationVector> VectorizedAnimationSpec<V>.getValueFromMillis(
    playTimeMillis: Long,
    start: V,
    end: V,
    startVelocity: V
): V = getValueFromNanos(playTimeMillis * MillisToNanos, start, end, startVelocity)

/**
 * All the finite [VectorizedAnimationSpec]s implement this interface, including:
 * [VectorizedKeyframesSpec], [VectorizedTweenSpec], [VectorizedRepeatableSpec],
 * [VectorizedSnapSpec], [VectorizedSpringSpec], etc. The [VectorizedAnimationSpec] that does
 * __not__ implement this is: [InfiniteRepeatableSpec].
 */
@JvmDefaultWithCompatibility
interface VectorizedFiniteAnimationSpec<V : AnimationVector> : VectorizedAnimationSpec<V> {
    override val isInfinite: Boolean get() = false
}

/**
 * Base class for [VectorizedAnimationSpec]s that are based on a fixed [durationMillis].
 */
@JvmDefaultWithCompatibility
interface VectorizedDurationBasedAnimationSpec<V : AnimationVector> :
    VectorizedFiniteAnimationSpec<V> {
    /**
     * duration is the amount of time while animation is not yet finished.
     */
    val durationMillis: Int

    /**
     * delay defines the amount of time that animation can be delayed.
     */
    val delayMillis: Int

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long =
        (delayMillis + durationMillis) * MillisToNanos
}

/**
 * Clamps the input [playTime] to the duration range of the given
 * [VectorizedDurationBasedAnimationSpec].
 */
private fun VectorizedDurationBasedAnimationSpec<*>.clampPlayTime(playTime: Long): Long {
    return (playTime - delayMillis).coerceIn(0, durationMillis.toLong())
}

/**
 * [VectorizedKeyframesSpec] class manages the animation based on the values defined at different
 * timestamps in the duration of the animation (i.e. different keyframes). Each keyframe can be
 * provided via [keyframes] parameter. [VectorizedKeyframesSpec] allows very specific animation
 * definitions with a precision to millisecond.
 *
 * Here's an example of creating a [VectorizedKeyframesSpec] animation: ([keyframes] and
 * [KeyframesSpec.KeyframesSpecConfig] could make defining key frames much more readable.)
 *
 *     val delay = 120
 *     val startValue = AnimationVector3D(100f, 200f, 300f)
 *     val endValue = AnimationVector3D(200f, 100f, 0f)
 *     val keyframes = VectorizedKeyframesSpec<AnimationVector3D>(
 *          keyframes = mutableMapOf (
 *               0 to (startValue to LinearEasing),
 *               100 to (startValue to FastOutLinearInEasing)
 *          ),
 *          durationMillis = 200,
 *          delayMillis = delay
 *     )
 *
 * @param keyframes a map from time to a value/easing function pair. The value in each entry
 *                  defines the animation value at that time, and the easing curve is used in the
 *                  interval starting from that time.
 * @param durationMillis total duration of the animation
 * @param delayMillis the amount of the time the animation should wait before it starts. Defaults to
 *                    0.
 *
 * @see [KeyframesSpec]
 */
class VectorizedKeyframesSpec<V : AnimationVector>(
    private val keyframes: Map<Int, Pair<V, Easing>>,
    override val durationMillis: Int,
    override val delayMillis: Int = 0
) : VectorizedDurationBasedAnimationSpec<V> {

    private lateinit var valueVector: V
    private lateinit var velocityVector: V

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        val playTimeMillis = playTimeNanos / MillisToNanos
        val clampedPlayTime = clampPlayTime(playTimeMillis).toInt()
        // If there is a key frame defined with the given time stamp, return that value
        if (keyframes.containsKey(clampedPlayTime)) {
            return keyframes.getValue(clampedPlayTime).first
        }

        if (clampedPlayTime >= durationMillis) {
            return targetValue
        } else if (clampedPlayTime <= 0) return initialValue

        var startTime = 0
        var startVal = initialValue
        var endVal = targetValue
        var endTime: Int = durationMillis
        var easing: Easing = LinearEasing
        for ((timestamp, value) in keyframes) {
            if (clampedPlayTime > timestamp && timestamp >= startTime) {
                startTime = timestamp
                startVal = value.first
                easing = value.second
            } else if (clampedPlayTime < timestamp && timestamp <= endTime) {
                endTime = timestamp
                endVal = value.first
            }
        }

        // Now interpolate
        val fraction = easing.transform(
            (clampedPlayTime - startTime) / (endTime - startTime).toFloat()
        )
        init(initialValue)
        for (i in 0 until startVal.size) {
            valueVector[i] = lerp(startVal[i], endVal[i], fraction)
        }
        return valueVector
    }

    private fun init(value: V) {
        if (!::valueVector.isInitialized) {
            valueVector = value.newInstance()
            velocityVector = value.newInstance()
        }
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        val playTimeMillis = playTimeNanos / MillisToNanos
        val clampedPlayTime = clampPlayTime(playTimeMillis)
        if (clampedPlayTime <= 0L) {
            return initialVelocity
        }
        val startNum = getValueFromMillis(
            clampedPlayTime - 1,
            initialValue,
            targetValue,
            initialVelocity
        )
        val endNum = getValueFromMillis(
            clampedPlayTime,
            initialValue,
            targetValue,
            initialVelocity
        )

        init(initialValue)
        for (i in 0 until startNum.size) {
            velocityVector[i] = (startNum[i] - endNum[i]) * 1000f
        }
        return velocityVector
    }
}

/**
 * [VectorizedSnapSpec] immediately snaps the animating value to the end value.
 *
 * @param delayMillis the amount of time (in milliseconds) that the animation should wait before it
 *              starts. Defaults to 0.
 */
class VectorizedSnapSpec<V : AnimationVector>(
    override val delayMillis: Int = 0
) : VectorizedDurationBasedAnimationSpec<V> {

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        if (playTimeNanos < delayMillis * MillisToNanos) {
            return initialValue
        } else {
            return targetValue
        }
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return initialVelocity
    }

    override val durationMillis: Int
        get() = 0
}

private const val InfiniteIterations: Int = Int.MAX_VALUE

/**
 * This animation takes another [VectorizedDurationBasedAnimationSpec] and plays it
 * __infinite__ times.
 *
 * initialStartOffset can be used to either delay the start of the animation or to fast forward
 * the animation to a given play time. This start offset will **not** be repeated, whereas the delay
 * in the [animation] (if any) will be repeated. By default, the amount of offset is 0.
 *
 * @param animation the [VectorizedAnimationSpec] describing each repetition iteration.
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *                  [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @param initialStartOffset offsets the start of the animation
 */
class VectorizedInfiniteRepeatableSpec<V : AnimationVector>(
    private val animation: VectorizedDurationBasedAnimationSpec<V>,
    private val repeatMode: RepeatMode = RepeatMode.Restart,
    initialStartOffset: StartOffset = StartOffset(0)
) : VectorizedAnimationSpec<V> {
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "This method has been deprecated in favor of the constructor that" +
            " accepts start offset."
    )
    constructor(
        animation: VectorizedDurationBasedAnimationSpec<V>,
        repeatMode: RepeatMode = RepeatMode.Restart
    ) : this(animation, repeatMode, StartOffset(0))

    override val isInfinite: Boolean get() = true

    /**
     * Single iteration duration
     */
    internal val durationNanos: Long =
        (animation.delayMillis + animation.durationMillis) * MillisToNanos

    private val initialOffsetNanos = initialStartOffset.value * MillisToNanos

    private fun repetitionPlayTimeNanos(playTimeNanos: Long): Long {
        if (playTimeNanos + initialOffsetNanos <= 0) {
            return 0
        } else {
            val postOffsetPlayTimeNanos = playTimeNanos + initialOffsetNanos
            val repeatsCount = postOffsetPlayTimeNanos / durationNanos
            if (repeatMode == RepeatMode.Restart || repeatsCount % 2 == 0L) {
                return postOffsetPlayTimeNanos - repeatsCount * durationNanos
            } else {
                return (repeatsCount + 1) * durationNanos - postOffsetPlayTimeNanos
            }
        }
    }

    private fun repetitionStartVelocity(
        playTimeNanos: Long,
        start: V,
        startVelocity: V,
        end: V
    ): V = if (playTimeNanos + initialOffsetNanos > durationNanos) {
        // Start velocity of the 2nd and subsequent iteration will be the velocity at the end
        // of the first iteration, instead of the initial velocity.
        getVelocityFromNanos(durationNanos - initialOffsetNanos, start, startVelocity, end)
    } else {
        startVelocity
    }

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return animation.getValueFromNanos(
            repetitionPlayTimeNanos(playTimeNanos),
            initialValue,
            targetValue,
            repetitionStartVelocity(playTimeNanos, initialValue, initialVelocity, targetValue)
        )
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return animation.getVelocityFromNanos(
            repetitionPlayTimeNanos(playTimeNanos),
            initialValue,
            targetValue,
            repetitionStartVelocity(playTimeNanos, initialValue, initialVelocity, targetValue)
        )
    }

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long =
        Long.MAX_VALUE
}

/**
 * This animation takes another [VectorizedDurationBasedAnimationSpec] and plays it
 * [iterations] times. For infinitely repeating animation spec, [VectorizedInfiniteRepeatableSpec]
 * is recommended.
 *
 * __Note__: When repeating in the [RepeatMode.Reverse] mode, it's highly recommended to have an
 * __odd__ number of iterations. Otherwise, the animation may jump to the end value when it finishes
 * the last iteration.
 *
 * initialStartOffset can be used to either delay the start of the animation or to fast forward
 * the animation to a given play time. This start offset will **not** be repeated, whereas the delay
 * in the [animation] (if any) will be repeated. By default, the amount of offset is 0.
 *
 *
 * @param iterations the count of iterations. Should be at least 1.
 * @param animation the [VectorizedAnimationSpec] describing each repetition iteration.
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *                  [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @param initialStartOffset offsets the start of the animation
 */
class VectorizedRepeatableSpec<V : AnimationVector>(
    private val iterations: Int,
    private val animation: VectorizedDurationBasedAnimationSpec<V>,
    private val repeatMode: RepeatMode = RepeatMode.Restart,
    initialStartOffset: StartOffset = StartOffset(0)
) : VectorizedFiniteAnimationSpec<V> {
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "This method has been deprecated in favor of the constructor that accepts" +
            " start offset."
    )
    constructor(
        iterations: Int,
        animation: VectorizedDurationBasedAnimationSpec<V>,
        repeatMode: RepeatMode = RepeatMode.Restart
    ) : this(iterations, animation, repeatMode, StartOffset(0))

    init {
        if (iterations < 1) {
            throw IllegalArgumentException("Iterations count can't be less than 1")
        }
    }

    // Per-iteration duration
    internal val durationNanos: Long =
        (animation.delayMillis + animation.durationMillis) * MillisToNanos

    // Fast forward amount. Delay type => negative offset
    private val initialOffsetNanos = initialStartOffset.value * MillisToNanos

    private fun repetitionPlayTimeNanos(playTimeNanos: Long): Long {
        if (playTimeNanos + initialOffsetNanos <= 0) {
            return 0
        } else {
            val postOffsetPlayTimeNanos = playTimeNanos + initialOffsetNanos
            val repeatsCount = min(postOffsetPlayTimeNanos / durationNanos, iterations - 1L)
            if (repeatMode == RepeatMode.Restart || repeatsCount % 2 == 0L) {
                return postOffsetPlayTimeNanos - repeatsCount * durationNanos
            } else {
                return (repeatsCount + 1) * durationNanos - postOffsetPlayTimeNanos
            }
        }
    }

    private fun repetitionStartVelocity(
        playTimeNanos: Long,
        start: V,
        startVelocity: V,
        end: V
    ): V = if (playTimeNanos + initialOffsetNanos > durationNanos) {
        // Start velocity of the 2nd and subsequent iteration will be the velocity at the end
        // of the first iteration, instead of the initial velocity.
        getVelocityFromNanos(durationNanos - initialOffsetNanos, start, startVelocity, end)
    } else
        startVelocity

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return animation.getValueFromNanos(
            repetitionPlayTimeNanos(playTimeNanos),
            initialValue,
            targetValue,
            repetitionStartVelocity(playTimeNanos, initialValue, initialVelocity, targetValue)
        )
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return animation.getVelocityFromNanos(
            repetitionPlayTimeNanos(playTimeNanos),
            initialValue,
            targetValue,
            repetitionStartVelocity(playTimeNanos, initialValue, initialVelocity, targetValue)
        )
    }

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long {
        return iterations * durationNanos - initialOffsetNanos
    }
}

/**
 * Physics class contains a number of recommended configurations for physics animations.
 */
object Spring {
    /**
     * Stiffness constant for extremely stiff spring
     */
    const val StiffnessHigh = 10_000f

    /**
     * Stiffness constant for medium stiff spring. This is the default stiffness for spring
     * force.
     */
    const val StiffnessMedium = 1500f

    /**
     * Stiffness constant for medium-low stiff spring. This is the default stiffness for springs
     * used in enter/exit transitions.
     */
    const val StiffnessMediumLow = 400f

    /**
     * Stiffness constant for a spring with low stiffness.
     */
    const val StiffnessLow = 200f

    /**
     * Stiffness constant for a spring with very low stiffness.
     */
    const val StiffnessVeryLow = 50f

    /**
     * Damping ratio for a very bouncy spring. Note for under-damped springs
     * (i.e. damping ratio < 1), the lower the damping ratio, the more bouncy the spring.
     */
    const val DampingRatioHighBouncy = 0.2f

    /**
     * Damping ratio for a medium bouncy spring. This is also the default damping ratio for
     * spring force. Note for under-damped springs (i.e. damping ratio < 1), the lower the
     * damping ratio, the more bouncy the spring.
     */
    const val DampingRatioMediumBouncy = 0.5f

    /**
     * Damping ratio for a spring with low bounciness. Note for under-damped springs
     * (i.e. damping ratio < 1), the lower the damping ratio, the higher the bounciness.
     */
    const val DampingRatioLowBouncy = 0.75f

    /**
     * Damping ratio for a spring with no bounciness. This damping ratio will create a
     * critically damped spring that returns to equilibrium within the shortest amount of time
     * without oscillating.
     */
    const val DampingRatioNoBouncy = 1f

    /**
     * Default cutoff for rounding off physics based animations
     */
    const val DefaultDisplacementThreshold = 0.01f
}

/**
 * Internal data structure for storing different FloatAnimations for different dimensions.
 */
internal interface Animations {
    operator fun get(index: Int): FloatAnimationSpec
}

/**
 * [VectorizedSpringSpec] uses spring animations to animate (each dimension of) [AnimationVector]s.
 */
class VectorizedSpringSpec<V : AnimationVector> private constructor(
    val dampingRatio: Float,
    val stiffness: Float,
    anims: Animations
) : VectorizedFiniteAnimationSpec<V> by VectorizedFloatAnimationSpec<V>(anims) {

    /**
     * Creates a [VectorizedSpringSpec] that uses the same spring constants (i.e. [dampingRatio] and
     * [stiffness] on all dimensions. The optional [visibilityThreshold] defines when the animation
     * should be considered to be visually close enough to target to stop. By default,
     * [Spring.DefaultDisplacementThreshold] is used on all dimensions of the
     * [AnimationVector].
     *
     * @param dampingRatio damping ratio of the spring. [Spring.DampingRatioNoBouncy] by default.
     * @param stiffness stiffness of the spring. [Spring.StiffnessMedium] by default.
     * @param visibilityThreshold specifies the visibility threshold for each dimension.
     */
    constructor(
        dampingRatio: Float = Spring.DampingRatioNoBouncy,
        stiffness: Float = Spring.StiffnessMedium,
        visibilityThreshold: V? = null
    ) : this(
        dampingRatio, stiffness,
        createSpringAnimations(visibilityThreshold, dampingRatio, stiffness)
    )
}

private fun <V : AnimationVector> createSpringAnimations(
    visibilityThreshold: V?,
    dampingRatio: Float,
    stiffness: Float
): Animations {
    if (visibilityThreshold != null) {
        return object : Animations {
            private val anims = (0 until visibilityThreshold.size).map { index ->
                FloatSpringSpec(dampingRatio, stiffness, visibilityThreshold[index])
            }

            override fun get(index: Int): FloatSpringSpec = anims[index]
        }
    } else {
        return object : Animations {
            private val anim = FloatSpringSpec(dampingRatio, stiffness)
            override fun get(index: Int): FloatSpringSpec = anim
        }
    }
}

/**
 * [VectorizedTweenSpec] animates a [AnimationVector] value by interpolating the start and end
 * value, in the given [durationMillis] using the given [easing] curve.
 *
 * @param durationMillis duration of the [VectorizedTweenSpec] animation. Defaults to
 *                       [DefaultDurationMillis].
 * @param delayMillis the amount of time the animation should wait before it starts running,
 *                    0 by default.
 * @param easing the easing curve used by the animation. [FastOutSlowInEasing] by default.
 */
// TODO: Support different tween on different dimens
class VectorizedTweenSpec<V : AnimationVector>(
    override val durationMillis: Int = DefaultDurationMillis,
    override val delayMillis: Int = 0,
    val easing: Easing = FastOutSlowInEasing
) : VectorizedDurationBasedAnimationSpec<V> {

    private val anim = VectorizedFloatAnimationSpec<V>(
        FloatTweenSpec(durationMillis, delayMillis, easing)
    )

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return anim.getValueFromNanos(playTimeNanos, initialValue, targetValue, initialVelocity)
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return anim.getVelocityFromNanos(playTimeNanos, initialValue, targetValue, initialVelocity)
    }
}

/**
 * A convenient implementation of [VectorizedFloatAnimationSpec] that turns a [FloatAnimationSpec]
 * into a multi-dimensional [VectorizedFloatAnimationSpec], by using the same [FloatAnimationSpec]
 * on each dimension of the [AnimationVector] that is being animated.
 */
class VectorizedFloatAnimationSpec<V : AnimationVector> internal constructor(
    private val anims: Animations
) : VectorizedFiniteAnimationSpec<V> {
    private lateinit var valueVector: V
    private lateinit var velocityVector: V
    private lateinit var endVelocityVector: V

    /**
     * Creates a [VectorizedAnimationSpec] from a [FloatAnimationSpec]. The given
     * [FloatAnimationSpec] will be used to animate every dimension of the [AnimationVector].
     *
     * @param anim the animation spec for animating each dimension of the [AnimationVector]
     */
    constructor(anim: FloatAnimationSpec) : this(object : Animations {
        override fun get(index: Int): FloatAnimationSpec {
            return anim
        }
    })

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        if (!::valueVector.isInitialized) {
            valueVector = initialValue.newInstance()
        }
        for (i in 0 until valueVector.size) {
            valueVector[i] = anims[i].getValueFromNanos(
                playTimeNanos,
                initialValue[i],
                targetValue[i],
                initialVelocity[i]
            )
        }
        return valueVector
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        if (!::velocityVector.isInitialized) {
            velocityVector = initialVelocity.newInstance()
        }
        for (i in 0 until velocityVector.size) {
            velocityVector[i] =
                anims[i].getVelocityFromNanos(
                    playTimeNanos,
                    initialValue[i],
                    targetValue[i],
                    initialVelocity[i]
                )
        }
        return velocityVector
    }

    override fun getEndVelocity(initialValue: V, targetValue: V, initialVelocity: V): V {
        if (!::endVelocityVector.isInitialized) {
            endVelocityVector = initialVelocity.newInstance()
        }
        for (i in 0 until endVelocityVector.size) {
            endVelocityVector[i] =
                anims[i].getEndVelocity(initialValue[i], targetValue[i], initialVelocity[i])
        }
        return endVelocityVector
    }

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long {
        var maxDuration = 0L
        (0 until initialValue.size).forEach {
            maxDuration = maxOf(
                maxDuration,
                anims[it].getDurationNanos(initialValue[it], targetValue[it], initialVelocity[it])
            )
        }
        return maxDuration
    }
}
