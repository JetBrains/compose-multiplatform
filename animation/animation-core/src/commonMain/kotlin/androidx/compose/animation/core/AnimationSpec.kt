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

import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.KeyframesSpec.KeyframesSpecConfig
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntOffset

object AnimationConstants {
    /**
     * The default duration used in [VectorizedAnimationSpec]s and [AnimationSpec].
     */
    const val DefaultDurationMillis: Int = 300

    /**
     * Used as a iterations count for [VectorizedRepeatableSpec] to create an infinity repeating
     * animation.
     */
    @Deprecated(
        "Using Infinite to specify repeatable animation iterations has been " +
            "deprecated. Please use [InfiniteRepeatableSpec] or [infiniteRepeatable] instead."
    )
    const val Infinite: Int = Int.MAX_VALUE
}

/**
 * [AnimationSpec] stores the specification of an animation, including 1) the data type to be
 * animated, and 2) the animation configuration (i.e. [VectorizedAnimationSpec]) that will be used
 * once the data (of type [T]) has been converted to [AnimationVector].
 *
 * Any type [T] can be animated by the system as long as a [TwoWayConverter] is supplied to convert
 * the data type [T] from and to an [AnimationVector]. There are a number of converters
 * available out of the box. For example, to animate [androidx.compose.ui.unit.IntOffset] the system
 * uses [IntOffset.VectorConverter][IntOffset.Companion.VectorConverter] to convert the object to
 * [AnimationVector2D], so that both x and y dimensions are animated independently with separate
 * velocity tracking. This enables multidimensional objects to be animated in a true
 * multi-dimensional way. It is particularly useful for smoothly handling animation interruptions
 * (such as when the target changes during the animation).
 */
interface AnimationSpec<T> {
    /**
     * Creates a [VectorizedAnimationSpec] with the given [TwoWayConverter].
     *
     * The underlying animation system operates on [AnimationVector]s. [T] will be converted to
     * [AnimationVector] to animate. [VectorizedAnimationSpec] describes how the
     * converted [AnimationVector] should be animated. E.g. The animation could simply
     * interpolate between the start and end values (i.e.[TweenSpec]), or apply spring physics
     * to produce the motion (i.e. [SpringSpec]), etc)
     *
     * @param converter converts the type [T] from and to [AnimationVector] type
     */
    fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedAnimationSpec<V>
}

/**
 * [FiniteAnimationSpec] is the interface that all non-infinite [AnimationSpec]s implement,
 * including: [TweenSpec], [SpringSpec], [KeyframesSpec], [RepeatableSpec], [SnapSpec], etc. By
 * definition, [InfiniteRepeatableSpec] __does not__ implement this interface.
 *
 * @see [InfiniteRepeatableSpec]
 */
interface FiniteAnimationSpec<T> : AnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedFiniteAnimationSpec<V>
}

/**
 * Creates a TweenSpec configured with the given duration, delay, and easing curve.
 *
 * @param durationMillis duration of the [VectorizedTweenSpec] animation.
 * @param delay the number of milliseconds the animation waits before starting, 0 by default.
 * @param easing the easing curve used by the animation. [FastOutSlowInEasing] by default.
 */
@Immutable
class TweenSpec<T>(
    val durationMillis: Int = DefaultDurationMillis,
    val delay: Int = 0,
    val easing: Easing = FastOutSlowInEasing
) : DurationBasedAnimationSpec<T> {

    override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<T, V>) =
        VectorizedTweenSpec<V>(durationMillis, delay, easing)

    override fun equals(other: Any?): Boolean =
        if (other is TweenSpec<*>) {
            other.durationMillis == this.durationMillis &&
                other.delay == this.delay &&
                other.easing == this.easing
        } else {
            false
        }

    override fun hashCode(): Int {
        return (durationMillis * 31 + easing.hashCode()) * 31 + delay
    }
}

/**
 *  This describes [AnimationSpec]s that are based on a fixed duration, such as [KeyframesSpec],
 *  [TweenSpec], and [SnapSpec]. These duration based specs can repeated when put into a
 *  [RepeatableSpec].
 */
interface DurationBasedAnimationSpec<T> : FiniteAnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<T, V>):
        VectorizedDurationBasedAnimationSpec<V>
}

/**
 * Creates a [SpringSpec] that uses the given spring constants (i.e. [dampingRatio] and
 * [stiffness]. The optional [visibilityThreshold] defines when the animation
 * should be considered to be visually close enough to round off to its target.
 *
 * @param dampingRatio damping ratio of the spring. [Spring.DampingRatioNoBouncy] by default.
 * @param stiffness stiffness of the spring. [Spring.StiffnessMedium] by default.
 * @param visibilityThreshold specifies the visibility threshold
 */
// TODO: annotate damping/stiffness with FloatRange
@Immutable
class SpringSpec<T>(
    val dampingRatio: Float = Spring.DampingRatioNoBouncy,
    val stiffness: Float = Spring.StiffnessMedium,
    val visibilityThreshold: T? = null
) : FiniteAnimationSpec<T> {

    override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<T, V>) =
        VectorizedSpringSpec(dampingRatio, stiffness, converter.convert(visibilityThreshold))

    override fun equals(other: Any?): Boolean =
        if (other is SpringSpec<*>) {
            other.dampingRatio == this.dampingRatio &&
                other.stiffness == this.stiffness &&
                other.visibilityThreshold == this.visibilityThreshold
        } else {
            false
        }

    override fun hashCode(): Int =
        (visibilityThreshold.hashCode() * 31 + dampingRatio.hashCode()) * 31 + stiffness.hashCode()
}

private fun <T, V : AnimationVector> TwoWayConverter<T, V>.convert(data: T?): V? {
    if (data == null) {
        return null
    } else {
        return convertToVector(data)
    }
}

/**
 * [RepeatableSpec] takes another [DurationBasedAnimationSpec] and plays it [iterations] times. For
 * creating infinitely repeating animation spec, consider using [InfiniteRepeatableSpec].
 *
 * __Note__: When repeating in the [RepeatMode.Reverse] mode, it's highly recommended to have an
 * __odd__ number of iterations. Otherwise, the animation may jump to the end value when it finishes
 * the last iteration.
 *
 * @see repeatable
 * @see InfiniteRepeatableSpec
 * @see infiniteRepeatable
 *
 * @param iterations the count of iterations. Should be at least 1.
 * @param animation the [AnimationSpec] to be repeated
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *                  [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 */
@Immutable
class RepeatableSpec<T>(
    val iterations: Int,
    val animation: DurationBasedAnimationSpec<T>,
    val repeatMode: RepeatMode = RepeatMode.Restart
) : FiniteAnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedFiniteAnimationSpec<V> {
        return VectorizedRepeatableSpec(iterations, animation.vectorize(converter), repeatMode)
    }

    override fun equals(other: Any?): Boolean =
        if (other is RepeatableSpec<*>) {
            other.iterations == this.iterations &&
                other.animation == this.animation &&
                other.repeatMode == this.repeatMode
        } else {
            false
        }

    override fun hashCode(): Int {
        return (iterations * 31 + animation.hashCode()) * 31 + repeatMode.hashCode()
    }
}

/**
 * [InfiniteRepeatableSpec] repeats the provided [animation] infinite amount of times. It will
 * never naturally finish. This means the animation will only be stopped via some form of manual
 * cancellation. When used with transition or other animation composables, the infinite animations
 * will stop when the composable is removed from the compose tree.
 *
 * For non-infinite repeating animations, consider [RepeatableSpec].
 *
 * @param animation the [AnimationSpec] to be repeated
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *                  [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @see infiniteRepeatable
 */
// TODO: Consider supporting repeating spring specs
class InfiniteRepeatableSpec<T>(
    val animation: DurationBasedAnimationSpec<T>,
    val repeatMode: RepeatMode = RepeatMode.Restart
) : AnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedAnimationSpec<V> {
        return VectorizedInfiniteRepeatableSpec(animation.vectorize(converter), repeatMode)
    }

    override fun equals(other: Any?): Boolean =
        if (other is InfiniteRepeatableSpec<*>) {
            other.animation == this.animation && other.repeatMode == this.repeatMode
        } else {
            false
        }

    override fun hashCode(): Int {
        return animation.hashCode() * 31 + repeatMode.hashCode()
    }
}

/**
 * Repeat mode for [RepeatableSpec] and [VectorizedRepeatableSpec].
 */
enum class RepeatMode {
    /**
     * [Restart] will restart the animation and animate from the start value to the end value.
     */
    Restart,

    /**
     * [Reverse] will reverse the last iteration as the animation repeats.
     */
    Reverse
}

/**
 * [SnapSpec] describes a jump-cut type of animation. It immediately snaps the animating value to
 * the end value.
 *
 * @param delay the amount of time (in milliseconds) that the animation should wait before it
 *              starts. Defaults to 0.
 */
@Immutable
class SnapSpec<T>(val delay: Int = 0) : DurationBasedAnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedDurationBasedAnimationSpec<V> = VectorizedSnapSpec(delay)

    override fun equals(other: Any?): Boolean =
        if (other is SnapSpec<*>) {
            other.delay == this.delay
        } else {
            false
        }

    override fun hashCode(): Int {
        return delay
    }
}

/**
 * [KeyframesSpec] creates a [VectorizedKeyframesSpec] animation.
 *
 * [VectorizedKeyframesSpec] animates based on the values defined at different timestamps in
 * the duration of the animation (i.e. different keyframes). Each keyframe can be defined using
 * [KeyframesSpecConfig.at]. [VectorizedKeyframesSpec] allows very specific animation definitions
 * with a precision to millisecond.
 *
 * @sample androidx.compose.animation.core.samples.FloatKeyframesBuilder
 *
 * You can also provide a custom [Easing] for the interval with use of [with] function applied
 * for the interval starting keyframe.
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderWithEasing
 */
@Immutable
class KeyframesSpec<T>(val config: KeyframesSpecConfig<T>) : DurationBasedAnimationSpec<T> {
    /**
     * [KeyframesSpecConfig] stores a mutable configuration of the key frames, including [durationMillis],
     * [delayMillis], and all the key frames. Each key frame defines what the animation value should be
     * at a particular time. Once the key frames are fully configured, the [KeyframesSpecConfig]
     * can be used to create a [KeyframesSpec].
     *
     * @sample androidx.compose.animation.core.samples.FloatKeyframesBuilder
     * @see keyframes
     */
    class KeyframesSpecConfig<T> {
        /**
         * Duration of the animation in milliseconds. The minimum is `0` and defaults to
         * [DefaultDurationMillis]
         */
        /*@IntRange(from = 0)*/
        var durationMillis: Int = DefaultDurationMillis

        /**
         * The amount of time that the animation should be delayed. The minimum is `0` and defaults
         * to 0.
         */
        /*@IntRange(from = 0)*/
        var delayMillis: Int = 0

        internal val keyframes = mutableMapOf<Int, KeyframeEntity<T>>()

        /**
         * Adds a keyframe so that animation value will be [this] at time: [timeStamp]. For example:
         *     0.8f at 150 // ms
         *
         * @param timeStamp The time in the during when animation should reach value: [this], with
         * a minimum value of `0`.
         * @return an [KeyframeEntity] so a custom [Easing] can be added by [with] method.
         */
        // TODO: Need a IntRange equivalent annotation
        infix fun T.at(/*@IntRange(from = 0)*/ timeStamp: Int): KeyframeEntity<T> {
            return KeyframeEntity(this).also {
                keyframes[timeStamp] = it
            }
        }

        /**
         * Adds an [Easing] for the interval started with the just provided timestamp. For example:
         *     0f at 50 with LinearEasing
         *
         * @sample androidx.compose.animation.core.samples.KeyframesBuilderWithEasing
         * @param easing [Easing] to be used for the next interval.
         */
        infix fun KeyframeEntity<T>.with(easing: Easing) {
            this.easing = easing
        }

        override fun equals(other: Any?): Boolean {
            return other is KeyframesSpecConfig<*> && delayMillis == other.delayMillis &&
                durationMillis == other.durationMillis && keyframes == other.keyframes
        }

        override fun hashCode(): Int {
            return (durationMillis * 31 + delayMillis) * 31 + keyframes.hashCode()
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is KeyframesSpec<*> &&
            config == other.config
    }

    override fun hashCode(): Int {
        return config.hashCode()
    }

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedKeyframesSpec<V> {
        return VectorizedKeyframesSpec(
            config.keyframes.mapValues {
                it.value.toPair(converter.convertToVector)
            },
            config.durationMillis, config.delayMillis
        )
    }

    /**
     * Holder class for building a keyframes animation.
     */
    class KeyframeEntity<T> internal constructor(
        internal val value: T,
        internal var easing: Easing = LinearEasing
    ) {
        internal fun <V : AnimationVector> toPair(convertToVector: (T) -> V) =
            convertToVector.invoke(value) to easing

        override fun equals(other: Any?): Boolean {
            return other is KeyframeEntity<*> && other.value == value && other.easing == easing
        }

        override fun hashCode(): Int {
            return value.hashCode() * 31 + easing.hashCode()
        }
    }
}
