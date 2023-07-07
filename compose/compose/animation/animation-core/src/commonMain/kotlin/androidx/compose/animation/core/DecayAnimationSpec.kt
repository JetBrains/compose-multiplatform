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

import androidx.compose.ui.unit.IntOffset

/**
 * [DecayAnimationSpec] stores the specification of an animation, including 1) the data type to be
 * animated, and 2) the animation configuration (i.e. [VectorizedDecayAnimationSpec]) that will be
 * used once the data (of type [T]) has been converted to [AnimationVector].
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
interface DecayAnimationSpec<T> {

    /**
     * Creates a [VectorizedDecayAnimationSpec] with the given [TwoWayConverter].
     *
     * The underlying animation system operates on [AnimationVector]s. [T] will be converted to
     * [AnimationVector] to animate. [VectorizedDecayAnimationSpec] describes how the
     * converted [AnimationVector] should be animated.
     *
     * @param typeConverter converts the type [T] from and to [AnimationVector] type
     */
    fun <V : AnimationVector> vectorize(
        typeConverter: TwoWayConverter<T, V>
    ): VectorizedDecayAnimationSpec<V>
}

/**
 * Calculates the target value of a decay animation based on the [initialValue] and
 * [initialVelocity], and the [typeConverter] that converts the given type [T] to [AnimationVector].
 *
 * @return target value where the animation will come to a natural stop
 */
fun <T, V : AnimationVector> DecayAnimationSpec<T>.calculateTargetValue(
    typeConverter: TwoWayConverter<T, V>,
    initialValue: T,
    initialVelocity: T
): T {
    val vectorizedSpec = vectorize(typeConverter)
    val targetVector = vectorizedSpec.getTargetValue(
        typeConverter.convertToVector(initialValue),
        typeConverter.convertToVector(initialVelocity)
    )
    return typeConverter.convertFromVector(targetVector)
}

/**
 * Calculates the target value of a Float decay animation based on the [initialValue] and
 * [initialVelocity].
 *
 * @return target value where the animation will come to a natural stop
 */
fun DecayAnimationSpec<Float>.calculateTargetValue(
    initialValue: Float,
    initialVelocity: Float
): Float {
    val vectorizedSpec = vectorize(Float.VectorConverter)
    val targetVector = vectorizedSpec.getTargetValue(
        AnimationVector(initialValue),
        AnimationVector(initialVelocity)
    )
    return targetVector.value
}

/**
 * Creates a decay animation spec where the friction/deceleration is always proportional to the
 * velocity. As a result, the velocity goes under an exponential decay. The constructor parameter,
 * [frictionMultiplier], can be tuned to adjust the amount of friction applied in the decay. The
 * higher the multiplier, the higher the friction, the sooner the animation will stop, and the
 * shorter distance the animation will travel with the same starting condition.
 * [absVelocityThreshold] describes the absolute value of a velocity threshold, below which the
 * animation is considered finished.
 *
 * @param frictionMultiplier The decay friction multiplier. This must be greater than `0`.
 * @param absVelocityThreshold The minimum speed, below which the animation is considered finished.
 * Must be greater than `0`.
 */
fun <T> exponentialDecay(
    /*@FloatRange(
        from = 0.0,
        fromInclusive = false
    )*/
    frictionMultiplier: Float = 1f,
    /*@FloatRange(
        from = 0.0,
        fromInclusive = false
    )*/
    absVelocityThreshold: Float = 0.1f
): DecayAnimationSpec<T> =
    FloatExponentialDecaySpec(frictionMultiplier, absVelocityThreshold).generateDecayAnimationSpec()

/**
 * Creates a [DecayAnimationSpec] from a [FloatDecayAnimationSpec] by applying the given
 * [FloatDecayAnimationSpec] on every dimension of the [AnimationVector] that [T] converts to.
 */
fun <T> FloatDecayAnimationSpec.generateDecayAnimationSpec(): DecayAnimationSpec<T> {
    return DecayAnimationSpecImpl(this)
}

private class DecayAnimationSpecImpl<T>(
    private val floatDecaySpec: FloatDecayAnimationSpec
) : DecayAnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        typeConverter: TwoWayConverter<T, V>
    ): VectorizedDecayAnimationSpec<V> = VectorizedFloatDecaySpec(floatDecaySpec)
}

private class VectorizedFloatDecaySpec<V : AnimationVector>(
    val floatDecaySpec: FloatDecayAnimationSpec
) : VectorizedDecayAnimationSpec<V> {
    private lateinit var valueVector: V
    private lateinit var velocityVector: V
    private lateinit var targetVector: V
    override val absVelocityThreshold: Float = floatDecaySpec.absVelocityThreshold

    override fun getValueFromNanos(playTimeNanos: Long, initialValue: V, initialVelocity: V): V {
        if (!::valueVector.isInitialized) {
            valueVector = initialValue.newInstance()
        }
        for (i in 0 until valueVector.size) {
            valueVector[i] = floatDecaySpec.getValueFromNanos(
                playTimeNanos,
                initialValue[i],
                initialVelocity[i]
            )
        }
        return valueVector
    }

    override fun getDurationNanos(initialValue: V, initialVelocity: V): Long {
        var maxDuration = 0L
        if (!::velocityVector.isInitialized) {
            velocityVector = initialValue.newInstance()
        }
        for (i in 0 until velocityVector.size) {
            maxDuration = maxOf(
                maxDuration,
                floatDecaySpec.getDurationNanos(initialValue[i], initialVelocity[i])
            )
        }
        return maxDuration
    }

    override fun getVelocityFromNanos(playTimeNanos: Long, initialValue: V, initialVelocity: V): V {
        if (!::velocityVector.isInitialized) {
            velocityVector = initialValue.newInstance()
        }
        for (i in 0 until velocityVector.size) {
            velocityVector[i] = floatDecaySpec.getVelocityFromNanos(
                playTimeNanos,
                initialValue[i],
                initialVelocity[i]
            )
        }
        return velocityVector
    }

    override fun getTargetValue(initialValue: V, initialVelocity: V): V {
        if (!::targetVector.isInitialized) {
            targetVector = initialValue.newInstance()
        }
        for (i in 0 until targetVector.size) {
            targetVector[i] = floatDecaySpec.getTargetValue(
                initialValue[i],
                initialVelocity[i]
            )
        }
        return targetVector
    }
}