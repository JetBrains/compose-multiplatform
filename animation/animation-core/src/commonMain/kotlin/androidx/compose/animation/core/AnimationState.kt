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

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * [AnimationState] contains the necessary information to indicate the state of an animation.
 * Once an [AnimationState] is constructed, it can only be updated/mutated by animations. If
 * there's a need to mutate some of the fields of an [AnimationState], consider using [copy]
 * functions.
 *
 * @param typeConverter [TwoWayConverter] to convert type [T] from and to [AnimationVector]
 * @param initialValue initial value of the [AnimationState]
 * @param initialVelocityVector initial velocity of the [AnimationState], null (i.e. no velocity)
 *                              by default.
 * @param lastFrameTimeNanos last frame time of the animation, [AnimationConstants.UnspecifiedTime]
 *                           by default
 * @param finishedTimeNanos the time that the animation finished successfully,
 *                          [AnimationConstants.UnspecifiedTime] until then
 *
 * @param isRunning whether the [AnimationState] is currently being updated by an animation.
 *                  False by default
 */
class AnimationState<T, V : AnimationVector>(
    val typeConverter: TwoWayConverter<T, V>,
    initialValue: T,
    initialVelocityVector: V? = null,
    lastFrameTimeNanos: Long = AnimationConstants.UnspecifiedTime,
    finishedTimeNanos: Long = AnimationConstants.UnspecifiedTime,
    isRunning: Boolean = false
) : State<T> {
    /**
     * Current value of the [AnimationState].
     */
    override var value: T by mutableStateOf(initialValue)
        internal set

    /**
     * Current velocity vector of the [AnimationState].
     */
    var velocityVector: V =
        initialVelocityVector?.copy() ?: typeConverter.createZeroVectorFrom(initialValue)
        internal set

    /**
     * Last frame time of the animation.
     *
     * If the animation has never started, this will be [AnimationConstants.UnspecifiedTime], unless
     * specified otherwise in the [AnimationState] constructor. [lastFrameTimeNanos] is the frame
     * time when the animation is last updated, in the [System.nanoTime] timebase. It is also used
     * for starting a sequential animation in [AnimationState.animateTo]. This allows the sequential
     * animation to set its start time to when the previous animation is interrupted or finished.
     */
    @get:Suppress("MethodNameUnits")
    var lastFrameTimeNanos: Long = lastFrameTimeNanos
        internal set

    /**
     * The time when the animation finished successfully in the [System.nanoTime] timebase.
     *
     * If the animation has never finished (i.e. currently running, interrupted, or never started),
     * this will be [AnimationConstants.UnspecifiedTime], unless specified otherwise in
     * [AnimationState] constructor.
     */
    @get:Suppress("MethodNameUnits")
    var finishedTimeNanos: Long = finishedTimeNanos
        internal set

    /**
     * Indicates whether the animation is currently running.
     */
    var isRunning: Boolean = isRunning
        internal set

    /**
     * Velocity of type [T], converted from [velocityVector].
     */
    val velocity: T
        get() = typeConverter.convertFromVector(velocityVector)

    override fun toString(): String {
        return "AnimationState(" +
            "value=$value, " +
            "velocity=$velocity, " +
            "isRunning=$isRunning, " +
            "lastFrameTimeNanos=$lastFrameTimeNanos, " +
            "finishedTimeNanos=$finishedTimeNanos" +
            ")"
    }
}

/**
 * Indicates whether the given [AnimationState] is for an animation that has finished, indicated by
 * [AnimationState.finishedTimeNanos] having a specified value.
 */
val AnimationState<*, *>.isFinished
    get() = finishedTimeNanos != AnimationConstants.UnspecifiedTime

/**
 * [AnimationScope] provides all the animation related info specific to an animation run. An
 * [AnimationScope] will be accessible during an animation.
 *
 * @see [AnimationState.animateTo]
 */
class AnimationScope<T, V : AnimationVector> internal constructor(
    initialValue: T,
    /**
     * [TwoWayConverter] to convert type [T] from and to [AnimationVector].
     */
    val typeConverter: TwoWayConverter<T, V>,
    initialVelocityVector: V,
    lastFrameTimeNanos: Long,
    /**
     * Target value of the animation.
     */
    val targetValue: T,
    /**
     * Start time of the animation in the [System.nanoTime] timebase.
     */
    @get:Suppress("MethodNameUnits")
    val startTimeNanos: Long,
    isRunning: Boolean,
    private val onCancel: () -> Unit
) {
    // Externally immutable fields
    /**
     * Current value of the [AnimationScope].
     */
    var value: T by mutableStateOf(initialValue)
        internal set
    /**
     * Current velocity vector of the [AnimationScope].
     */
    var velocityVector: V = initialVelocityVector.copy()
        internal set

    /**
     * Last frame time of the animation.
     *
     * If the animation has never started, this will be [AnimationConstants.UnspecifiedTime], unless
     * specified otherwise in the [AnimationState] constructor. [lastFrameTimeNanos] is the frame
     * time when the animation is last updated, in the [System.nanoTime] timebase. It is also used
     * for starting a sequential animation in [AnimationState.animateTo]. This allows the sequential
     * animation to set its start time to when the previous animation is interrupted or finished.
     */
    @get:Suppress("MethodNameUnits")
    var lastFrameTimeNanos: Long = lastFrameTimeNanos
        internal set

    /**
     * The time when the animation finished successfully in the [System.nanoTime] timebase.
     *
     * If the animation has never finished (i.e. currently running, interrupted, or never started),
     * this will be [AnimationConstants.UnspecifiedTime], unless specified otherwise in
     * [AnimationState] constructor.
     */
    @get:Suppress("MethodNameUnits")
    var finishedTimeNanos: Long = AnimationConstants.UnspecifiedTime
        internal set
    /**
     * Indicates whether the animation is currently running.
     */
    var isRunning: Boolean by mutableStateOf(isRunning)
        internal set

    /**
     * Velocity of type [T], converted from [velocityVector].
     */
    val velocity
        get() = typeConverter.convertFromVector(velocityVector)

    /**
     * Cancels the animation that this [AnimationScope] corresponds to. The scope will not be
     * updated any more after [cancelAnimation] is called.
     */
    fun cancelAnimation() {
        isRunning = false
        onCancel()
    }

    /**
     * Creates an [AnimationState] that populates all the fields in [AnimationState] from
     * [AnimationScope].
     */
    fun toAnimationState() = AnimationState(
        typeConverter, value, velocityVector, lastFrameTimeNanos, finishedTimeNanos, isRunning
    )
}

/**
 * Creates a new [AnimationState] from a given [AnimationState]. This function allows some of the
 * fields to be different in the new [AnimationState].
 *
 * @param value value of the [AnimationState], using the value of the given [AnimationState] by
 *              default
 * @param velocityVector velocity of the [AnimationState], using the velocity of the given
 *                       [AnimationState] by default.
 * @param lastFrameTimeNanos last frame time of the animation, same as the given [AnimationState] by
 *                           default
 * @param finishedTimeNanos the time that the animation finished successfully,
 *                          [AnimationConstants.UnspecifiedTime] until then. Default value is the
 *                          same as the given [AnimationState].
 * @param isRunning whether the [AnimationState] is currently being updated by an animation.
 *                  Same as the given [AnimationState] by default
 *
 * @return A new [AnimationState] instance copied from the given instance, with some fields
 *         optionally altered
 */
fun <T, V : AnimationVector> AnimationState<T, V>.copy(
    value: T = this.value,
    velocityVector: V? = this.velocityVector.copy(),
    lastFrameTimeNanos: Long = this.lastFrameTimeNanos,
    finishedTimeNanos: Long = this.finishedTimeNanos,
    isRunning: Boolean = this.isRunning
): AnimationState<T, V> =
    AnimationState(
        this.typeConverter, value, velocityVector, lastFrameTimeNanos, finishedTimeNanos, isRunning
    )

/**
 * Creates a new [AnimationState] of Float [value] type from a given [AnimationState] of the same
 * type. This function allows some of the fields to be different in the new [AnimationState].
 *
 * @param value value of the [AnimationState], using the value of the given [AnimationState] by
 *              default
 * @param velocity velocity of the [AnimationState], using the velocity of the given
 *                 [AnimationState] by default.
 * @param lastFrameTimeNanos last frame time of the animation, same as the given [AnimationState] by
 *                      default
 * @param finishedTimeNanos the time that the animation finished successfully, same as the given
 *                     [AnimationState] by default.
 * @param isRunning whether the [AnimationState] is currently being updated by an animation.
 *                  Same as the given [AnimationState] by default
 *
 * @return A new [AnimationState] instance copied from the given instance, with some fields
 *         optionally altered
 */
fun AnimationState<Float, AnimationVector1D>.copy(
    value: Float = this.value,
    velocity: Float = this.velocityVector.value,
    lastFrameTimeNanos: Long = this.lastFrameTimeNanos,
    finishedTimeNanos: Long = this.finishedTimeNanos,
    isRunning: Boolean = this.isRunning
): AnimationState<Float, AnimationVector1D> =
    AnimationState(
        this.typeConverter,
        value,
        AnimationVector(velocity),
        lastFrameTimeNanos,
        finishedTimeNanos,
        isRunning
    )

/**
 * Factory method for creating an [AnimationState] for Float [initialValue].
 *
 * @param initialValue initial value of the [AnimationState]
 * @param initialVelocity initial velocity of the [AnimationState], 0 (i.e. no velocity) by default
 * @param lastFrameTimeNanos last frame time of the animation, [AnimationConstants.UnspecifiedTime]
 *                           by default
 * @param finishedTimeNanos the time that the animation finished successfully,
 *                          [AnimationConstants.UnspecifiedTime] by default.
 * @param isRunning whether the [AnimationState] is currently being updated by an animation.
 *                  False by default
 *
 * @return A new [AnimationState] instance
 */
fun AnimationState(
    initialValue: Float,
    initialVelocity: Float = 0f,
    lastFrameTimeNanos: Long = AnimationConstants.UnspecifiedTime,
    finishedTimeNanos: Long = AnimationConstants.UnspecifiedTime,
    isRunning: Boolean = false
): AnimationState<Float, AnimationVector1D> {
    return AnimationState(
        Float.VectorConverter,
        initialValue,
        AnimationVector(initialVelocity),
        lastFrameTimeNanos,
        finishedTimeNanos,
        isRunning
    )
}

/**
 * Factory method for creating an [AnimationState] with an [initialValue] and an [initialVelocity].
 *
 * @param typeConverter [TwoWayConverter] to convert type [T] from and to [AnimationVector]
 * @param initialValue initial value of the [AnimationState]
 * @param initialVelocity initial velocity of the [AnimationState]
 * @param lastFrameTimeNanos last frame time of the animation, [AnimationConstants.UnspecifiedTime] by default
 * @param finishedTimeNanos the time that the animation finished successfully, [AnimationConstants.UnspecifiedTime] by
 *                     default.
 * @param isRunning whether the [AnimationState] is currently being updated by an animation.
 *                  False by default
 *
 * @return A new [AnimationState] instance
 */
fun <T, V : AnimationVector> AnimationState(
    typeConverter: TwoWayConverter<T, V>,
    initialValue: T,
    initialVelocity: T,
    lastFrameTimeNanos: Long = AnimationConstants.UnspecifiedTime,
    finishedTimeNanos: Long = AnimationConstants.UnspecifiedTime,
    isRunning: Boolean = false
): AnimationState<T, V> {
    return AnimationState(
        typeConverter,
        initialValue,
        typeConverter.convertToVector(initialVelocity),
        lastFrameTimeNanos,
        finishedTimeNanos,
        isRunning
    )
}

/**
 * Creates an AnimationVector with all the values set to 0 using the provided [TwoWayConverter]
 * and the [value].
 *
 * @return a new AnimationVector instance of type [V].
 */
fun <T, V : AnimationVector> TwoWayConverter<T, V>.createZeroVectorFrom(value: T) =
    convertToVector(value).newInstance()
