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
 * more lifecycle aware animations. In contrast, [Animatable] and [TransitionAnimation] are
 * stateful and manage their own lifecycles, and subscribe/unsubscribe from an
 * [AnimationClockObservable] as needed.
 *
 * @see [Animatable]
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
    val typeConverter: TwoWayConverter<T, V>

    @Deprecated("Renamed to typeConverter", ReplaceWith("typeConverter"))
    val converter: TwoWayConverter<T, V>
        get() = typeConverter

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
fun <T, V : AnimationVector> Animation<T, V>.getVelocity(playTime: Long): T =
    typeConverter.convertFromVector(getVelocityVector(playTime))

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
/*@VisibleForTesting(otherwise = PACKAGE_PRIVATE)*/
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
        typeConverter = TwoWayConverter({ it }, { it })
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
 * interruption handling is the default behavior for both [Animatable] and
 * [TransitionAnimation]. Consider using those APIs for the interruption handling, as well as
 * built-in animation lifecycle management.
 *
 * @param animationSpec the [AnimationSpec] that will be used to calculate value/velocity
 * @param initialValue the start value of the animation
 * @param targetValue the end value of the animation
 * @param initialVelocity the start velocity (of type [T] of the animation
 * @param typeConverter the [TwoWayConverter] that is used to convert animation type [T] from/to [V]
 */
fun <T, V : AnimationVector> TargetBasedAnimation(
    animationSpec: AnimationSpec<T>,
    typeConverter: TwoWayConverter<T, V>,
    initialValue: T,
    targetValue: T,
    initialVelocity: T
) = TargetBasedAnimation(
    animationSpec,
    typeConverter,
    initialValue,
    targetValue,
    typeConverter.convertToVector(initialVelocity)
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
 * interruption handling is the default behavior for both [Animatable] and
 * [TransitionAnimation]. Consider using those APIs for the interruption handling, as well as
 * built-in animation lifecycle management.
 *
 * @param animationSpec the [VectorizedAnimationSpec] that will be used to calculate value/velocity
 * @param initialValue the start value of the animation
 * @param targetValue the end value of the animation
 * @param typeConverter the [TwoWayConverter] that is used to convert animation type [T] from/to [V]
 * @param initialVelocityVector the start velocity of the animation in the form of [AnimationVector]
 *
 * @see [TransitionAnimation]
 * @see [androidx.compose.animation.transition]
 * @see [Animatable]
 */
class TargetBasedAnimation<T, V : AnimationVector> internal constructor(
    internal val animationSpec: VectorizedAnimationSpec<V>,
    override val typeConverter: TwoWayConverter<T, V>,
    initialValue: T,
    override val targetValue: T,
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
     * interruption handling is the default behavior for both [Animatable] and
     * [TransitionAnimation]. Consider using those APIs for the interruption handling, as well as
     * built-in animation lifecycle management.
     *
     * @param animationSpec the [AnimationSpec] that will be used to calculate value/velocity
     * @param typeConverter the [TwoWayConverter] that is used to convert animation type [T] from/to [V]
     * @param initialValue the start value of the animation
     * @param targetValue the end value of the animation
     * @param initialVelocityVector the start velocity vector, null by default (meaning 0 velocity).
     */
    constructor(
        animationSpec: AnimationSpec<T>,
        typeConverter: TwoWayConverter<T, V>,
        initialValue: T,
        targetValue: T,
        initialVelocityVector: V? = null
    ) : this(
        animationSpec.vectorize(typeConverter),
        typeConverter,
        initialValue,
        targetValue,
        initialVelocityVector
    )

    private val initialValueVector = typeConverter.convertToVector(initialValue)
    private val targetValueVector = typeConverter.convertToVector(targetValue)
    private val initialVelocityVector =
        initialVelocityVector?.copy() ?: typeConverter.convertToVector(initialValue)
            .newInstance()

    override fun getValue(playTime: Long): T {
        return if (playTime < durationMillis) {
            typeConverter.convertFromVector(
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
 * [DecayAnimation] is an animation that slows down from [initialVelocityVector] as
 * time goes on. [DecayAnimation] is stateless, and it does not have any concept of lifecycle. It
 * serves as an animation calculation engine that supports convenient query of value/velocity
 * given a play time. To achieve that, [DecayAnimation] stores all the animation related
 * information: [initialValue], [initialVelocityVector], decay animation spec, [typeConverter].
 *
 * __Note__: Unless there's a need to control the timing manually, it's
 * generally recommended to use higher level animation APIs that build on top [DecayAnimation],
 * such as [Animatable.animateDecay], [AnimationState.animateDecay], etc.
 *
 * @see Animatable.animateDecay
 * @see AnimationState.animateDecay
 */
class DecayAnimation<T, V : AnimationVector> /*@VisibleForTesting*/ constructor(
    private val animationSpec: VectorizedDecayAnimationSpec<V>,
    override val typeConverter: TwoWayConverter<T, V>,
    val initialValue: T,
    initialVelocityVector: V
) : Animation<T, V> {
    private val initialValueVector: V = typeConverter.convertToVector(initialValue)
    val initialVelocityVector: V = initialVelocityVector.copy()
    private val endVelocity: V

    override val targetValue: T = typeConverter.convertFromVector(
        animationSpec.getTarget(initialValueVector, initialVelocityVector)
    )
    override val durationMillis: Long

    /**
     * [DecayAnimation] is an animation that slows down from [initialVelocityVector] as time goes
     * on. [DecayAnimation] is stateless, and it does not have any concept of lifecycle. It
     * serves as an animation calculation engine that supports convenient query of value/velocity
     * given a play time. To achieve that, [DecayAnimation] stores all the animation related
     * information: [initialValue], [initialVelocityVector], decay animation spec, [typeConverter].
     *
     * __Note__: Unless there's a need to control the timing manually, it's
     * generally recommended to use higher level animation APIs that build on top [DecayAnimation],
     * such as [Animatable.animateDecay], [AnimationState.animateDecay], etc.
     *
     * @param animationSpec Decay animation spec that defines the slow-down curve of the animation
     * @param typeConverter Type converter to convert the type [T] from and to [AnimationVector]
     * @param initialValue The starting value of the animation
     * @param initialVelocityVector The starting velocity of the animation in [AnimationVector] form
     *
     * @see Animatable.animateDecay
     * @see AnimationState.animateDecay
     */
    constructor(
        animationSpec: DecayAnimationSpec<T>,
        typeConverter: TwoWayConverter<T, V>,
        initialValue: T,
        initialVelocityVector: V
    ) : this(
        animationSpec.vectorize(typeConverter),
        typeConverter,
        initialValue,
        initialVelocityVector
    )

    /**
     * [DecayAnimation] is an animation that slows down from [initialVelocity] as time goes on.
     * [DecayAnimation] is stateless, and it does not have any concept of lifecycle. It
     * serves as an animation calculation engine that supports convenient query of value/velocity
     * given a play time. To achieve that, [DecayAnimation] stores all the animation related
     * information: [initialValue], [initialVelocity], [animationSpec], [typeConverter].
     *
     * __Note__: Unless there's a need to control the timing manually, it's
     * generally recommended to use higher level animation APIs that build on top [DecayAnimation],
     * such as [Animatable.animateDecay], [AnimationState.animateDecay], etc.
     *
     * @param animationSpec Decay animation spec that defines the slow-down curve of the animation
     * @param typeConverter Type converter to convert the type [T] from and to [AnimationVector]
     * @param initialValue The starting value of the animation
     * @param initialVelocity The starting velocity of the animation
     *
     * @see Animatable.animateDecay
     * @see AnimationState.animateDecay
     */
    constructor(
        animationSpec: DecayAnimationSpec<T>,
        typeConverter: TwoWayConverter<T, V>,
        initialValue: T,
        initialVelocity: T
    ) : this(
        animationSpec.vectorize(typeConverter),
        typeConverter,
        initialValue,
        typeConverter.convertToVector(initialVelocity)
    )

    init {
        durationMillis = animationSpec.getDurationMillis(
            initialValueVector, initialVelocityVector
        )
        endVelocity = animationSpec.getVelocity(
            durationMillis,
            initialValueVector,
            initialVelocityVector
        ).copy()
        for (i in 0 until endVelocity.size) {
            endVelocity[i] = endVelocity[i].coerceIn(
                -animationSpec.absVelocityThreshold,
                animationSpec.absVelocityThreshold
            )
        }
    }

    override fun getValue(playTime: Long): T {
        if (!isFinished(playTime)) {
            return typeConverter.convertFromVector(
                animationSpec.getValue(playTime, initialValueVector, initialVelocityVector)
            )
        } else {
            return targetValue
        }
    }

    override fun getVelocityVector(playTime: Long): V {
        if (!isFinished(playTime)) {
            return animationSpec.getVelocity(playTime, initialValueVector, initialVelocityVector)
        } else {
            return endVelocity
        }
    }
}

/**
 * [DecayAnimation] is an animation that slows down from [initialVelocity] as
 * time goes on. [DecayAnimation] is stateless, and it does not have any concept of lifecycle. It
 * serves as an animation calculation engine that supports convenient query of value/velocity
 * given a play time. To achieve that, [DecayAnimation] stores all the animation related
 * information: [initialValue], [initialVelocity], decay animation spec.
 *
 * __Note__: Unless there's a need to control the timing manually, it's
 * generally recommended to use higher level animation APIs that build on top [DecayAnimation],
 * such as [Animatable.animateDecay], [animateDecay], etc.
 *
 * @param animationSpec decay animation that will be used
 * @param initialValue starting value that will be passed to the decay animation
 * @param initialVelocity starting velocity for the decay animation, 0f by default
 */
fun DecayAnimation(
    animationSpec: FloatDecayAnimationSpec,
    initialValue: Float,
    initialVelocity: Float = 0f
) = DecayAnimation(
    animationSpec.generateDecayAnimationSpec(),
    Float.VectorConverter,
    initialValue,
    AnimationVector(initialVelocity)
)
