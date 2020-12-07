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
import androidx.compose.animation.core.AnimationEndReason.Interrupted
import androidx.compose.animation.core.AnimationEndReason.TargetReached

/**
 * This is the base class for [AnimatedValue]. It contains all the functionality of AnimatedValue.
 * It is intended to be used as a base class for the other classes (such as [AnimatedFloat] to build
 * on top of.
 *
 * Animations in this class allow and anticipate the animation target to change frequently. When
 * the target changes as the animation is in-flight, the animation is expected to make a continuous
 * transition to the new target.
 *
 * @param typeConverter A two way type converter that converts from value to [AnimationVector1D],
 *                      [AnimationVector2D], [AnimationVector3D], or [AnimationVector4D], and vice
 *                      versa.
 * @param clock An animation clock observable controlling the progression of the animated value
 * @param visibilityThreshold Visibility threshold of the animation specifies the end condition:
 *      for t > duration, value < visibilityThreshold. Null value defaults to [SpringSpec]
 *      default.
 */
sealed class BaseAnimatedValue<T, V : AnimationVector>(
    internal val typeConverter: TwoWayConverter<T, V>,
    private val clock: AnimationClockObservable,
    internal val visibilityThreshold: T?
) {

    /**
     * Current value of the animation.
     */
    abstract var value: T
        protected set

    /**
     * Indicates whether the animation is running.
     */
    var isRunning: Boolean = false
        internal set

    /**
     * The target of the current animation. This target will not be the same as the value of the
     * animation, until the animation finishes un-interrupted.
     */
    var targetValue: T
        get() {
            if (_targetBackingField != null) {
                return _targetBackingField!!
            } else {
                return value
            }
        }
        internal set(newTarget) {
            _targetBackingField = newTarget
        }

    // TODO: remove the backing field when b/148422703 is fixed
    private var _targetBackingField: T? = null
        get() {
            if (field == null) {
                field = value
            }
            return field
        }

    /**
     * Velocity of the animation. The velocity will be of [AnimationVector1D], [AnimationVector2D],
     * [AnimationVector3D], or [AnimationVector4D] type.
     */
    internal var velocityVector: V
        get() = _velocityBackField!!
        set(value) {
            _velocityBackField = value
        }

    // TODO: remove the backing field when b/148422703 is fixed
    private var _velocityBackField: V? = null
        get() {
            if (field == null) {
                field = typeConverter.convertToVector(value).newInstance()
            }
            return field
        }

    internal var onEnd: ((AnimationEndReason, T) -> Unit)? = null
    private lateinit var anim: Animation<T, V>
    private var startTime: Long = Unset
    // last frame time only gets updated during the animation pulse. It will be reset at the
    // end of the animation.
    private var lastFrameTime: Long = Unset

    private var animationClockObserver: AnimationClockObserver =
        object : AnimationClockObserver {
            override fun onAnimationFrame(frameTimeMillis: Long) {
                doAnimationFrame(frameTimeMillis)
            }
        }

    private val defaultSpringSpec: SpringSpec<T> =
        SpringSpec<T>(visibilityThreshold = visibilityThreshold)

    // TODO: Need a test for animateTo(...) being called with the same target value
    /**
     * Sets the target value, which effectively starts an animation to change the value from [value]
     * to the target value. If there is already an animation in flight, this method will interrupt
     * the ongoing animation, invoke [onEnd] that is associated with that animation, and start
     * a new animation from the current value to the new target value.
     *
     * @param targetValue The new value to animate to
     * @param anim The animation that will be used to animate from the current value to the new
     *             target value. If unspecified, a spring animation will be used by default.
     * @param onEnd An optional callback that will be invoked when the animation finished by any
     *              reason.
     */
    fun animateTo(
        targetValue: T,
        anim: AnimationSpec<T> = defaultSpringSpec,
        onEnd: ((AnimationEndReason, T) -> Unit)? = null
    ) {
        if (isRunning) {
            notifyEnded(Interrupted, value)
        }

        this.targetValue = targetValue
        val animationWrapper = TargetBasedAnimation(
            anim, value, targetValue, typeConverter, velocityVector
        )

        this.onEnd = onEnd
        startAnimation(animationWrapper)
    }

    /**
     * Sets the current value to the target value immediately, without any animation.
     *
     * @param targetValue The new target value to set [value] to.
     */
    open fun snapTo(targetValue: T) {
        stop()
        value = targetValue
        this.targetValue = targetValue
    }

    /**
     * Stops any on-going animation. No op if no animation is running. Note that this method does
     * not skip the animation value to its target value. Rather the animation will be stopped in its
     * track.
     */
    fun stop() {
        if (isRunning) {
            endAnimation(Interrupted)
        }
    }

    internal fun notifyEnded(endReason: AnimationEndReason, endValue: T) {
        val onEnd = this.onEnd
        this.onEnd = null
        onEnd?.invoke(endReason, endValue)
    }

    private fun doAnimationFrame(timeMillis: Long) {
        val playtime: Long
        if (startTime == Unset) {
            startTime = timeMillis
            playtime = 0
        } else {
            playtime = timeMillis - startTime
        }

        lastFrameTime = timeMillis
        velocityVector = anim.getVelocityVector(playtime)
        value = anim.getValue(playtime)

        checkFinished(playtime)
    }

    protected open fun checkFinished(playtime: Long) {
        val animationFinished = anim.isFinished(playtime)
        if (animationFinished) endAnimation()
    }

    internal fun startAnimation(anim: Animation<T, V>) {
        this.anim = anim
        // Quick check before officially starting
        if (anim.isFinished(0)) {
            // If the animation value & velocity is already meeting the finished condition before
            // the animation even starts, end it now.
            endAnimation()
            return
        }

        if (isRunning) {
            startTime = lastFrameTime
        } else {
            startTime = Unset
            isRunning = true
            clock.subscribe(animationClockObserver)
        }
    }

    internal fun endAnimation(endReason: AnimationEndReason = TargetReached) {
        clock.unsubscribe(animationClockObserver)
        isRunning = false
        startTime = Unset
        lastFrameTime = Unset
        notifyEnded(endReason, value)
        // reset velocity after notifyFinish as we might need to return it in onFinished callback
        // depending on whether or not velocity was involved in the animation
        velocityVector.reset()
    }
}

/**
 * AnimatedValue is an animatable value holder. It can hold any type of value, and automatically
 * animate the value change when the value is changed via [animateTo]. AnimatedValue supports value
 * change during an ongoing value change animation. When that happens, a new animation will
 * transition AnimatedValue from its current value (i.e. value at the point of interruption) to the
 * new target. This ensures that the value change is always continuous.
 *
 * @param typeConverter Converter for converting value type [T] to [AnimationVector], and vice versa
 * @param clock The animation clock used to drive the animation.
 * @param visibilityThreshold Threshold at which the animation may round off to its target value.
 */
abstract class AnimatedValue<T, V : AnimationVector>(
    typeConverter: TwoWayConverter<T, V>,
    clock: AnimationClockObservable,
    visibilityThreshold: T? = null
) : BaseAnimatedValue<T, V>(typeConverter, clock, visibilityThreshold) {
    val velocity: V
        get() = velocityVector
}

/**
 * This class inherits most of the functionality from BaseAnimatedValue. In addition, it tracks
 * velocity and supports the definition of bounds. Once bounds are defined using [setBounds], the
 * animation will consider itself finished when it reaches the upper or lower bound, even when the
 * velocity is non-zero.
 *
 * @param clock An animation clock observable controlling the progression of the animated value
 * @param visibilityThreshold Threshold at which the animation may round off to its target value.
 */
abstract class AnimatedFloat(
    clock: AnimationClockObservable,
    visibilityThreshold: Float = Spring.DefaultDisplacementThreshold
) : BaseAnimatedValue<Float, AnimationVector1D>(
    Float.VectorConverter, clock, visibilityThreshold
) {

    /**
     * Lower bound of the animation value. When animations reach this lower bound, it will
     * automatically stop with [AnimationEndReason] being [AnimationEndReason.BoundReached].
     * This bound is [Float.NEGATIVE_INFINITY] by default. It can be adjusted via [setBounds].
     */
    var min: Float = Float.NEGATIVE_INFINITY
        private set
    /**
     * Upper bound of the animation value. When animations reach this upper bound, it will
     * automatically stop with [AnimationEndReason] being [AnimationEndReason.BoundReached].
     * This bound is [Float.POSITIVE_INFINITY] by default. It can be adjusted via [setBounds].
     */
    var max: Float = Float.POSITIVE_INFINITY
        private set

    val velocity: Float
        get() = velocityVector.value

    /**
     * Sets up the bounds that the animation should be constrained to. When the animation
     * reaches the bounds it will stop right away, even when there is remaining velocity. Setting
     * a range will immediately clamp the current value to the new range. Therefore it is not
     * recommended to change bounds in a way that immediately changes current value **during** an
     * animation, as it would result in a discontinuous animation.
     *
     * @param min Lower bound of the animation value. Defaults to [Float.NEGATIVE_INFINITY]
     * @param max Upper bound of the animation value. Defaults to [Float.POSITIVE_INFINITY]
     */
    fun setBounds(min: Float = Float.NEGATIVE_INFINITY, max: Float = Float.POSITIVE_INFINITY) {
        if (max < min) {
            // throw exception?
        }
        this.min = min
        this.max = max
        // Clamp to the range right away.
        val clamped = value.coerceIn(min, max)
        if (clamped != value) {
            value = clamped
            if (isRunning && clamped == targetValue.coerceIn(min, max) && clamped != targetValue) {
                // Target is outside of bounds, animation value is snapped to the bound closer to
                // the target.
                endAnimation(BoundReached)
            }
        }
    }

    override fun snapTo(targetValue: Float) {
        super.snapTo(targetValue.coerceIn(min, max))
    }

    override fun checkFinished(playtime: Long) {
        if (value < min && targetValue <= min) {
            value = min
            endAnimation(BoundReached)
        } else if (value > max && targetValue >= max) {
            value = max
            endAnimation(BoundReached)
        } else {
            value = value.coerceIn(min, max)
            super.checkFinished(playtime)
        }
    }
}

/**
 * Typealias for lambda that will be invoked when fling animation ends.
 * Unlike [AnimatedValue.animateTo] onEnd, this lambda includes 3rd param remainingVelocity,
 * that represents velocity that wasn't consumed after fling finishes.
 */
// TODO: Consolidate onAnimationEnd and onEnd
typealias OnAnimationEnd =
    (endReason: AnimationEndReason, endValue: Float, remainingVelocity: Float) -> Unit

/**
 * Starts a fling animation with the specified starting velocity.
 *
 * @param startVelocity Starting velocity of the fling animation
 * @param decay The decay animation used for slowing down the animation from the starting
 *              velocity
 * @param onEnd An optional callback that will be invoked when this fling animation is
 *                   finished.
 */
// TODO: Figure out an API for customizing the type of decay & the friction
fun AnimatedFloat.fling(
    startVelocity: Float,
    decay: FloatDecayAnimationSpec = ExponentialDecay(),
    onEnd: OnAnimationEnd? = null
) {
    if (isRunning) {
        notifyEnded(Interrupted, value)
    }

    this.onEnd = { endReason, endValue ->
        onEnd?.invoke(endReason, endValue, velocity)
    }

    // start from current value with the given velocity
    targetValue = decay.getTarget(value, startVelocity)
    startAnimation(DecayAnimation(decay, value, startVelocity))
}

// TODO: Devs may want to change the target animation based on how close the target is to the
//       snapping position.
/**
 * Starts a fling animation with the specified starting velocity.
 *
 * @param startVelocity Starting velocity of the fling animation
 * @param adjustTarget A lambda that takes in the projected destination based on the decay
 *                     animation, and returns a nullable TargetAnimation object that contains a
 *                     new destination and an animation to animate to the new destination. This
 *                     lambda should return null when the original target is respected.
 * @param decay The decay animation used for slowing down the animation from the starting
 *              velocity
 * @param onEnd An optional callback that will be invoked when the animation
 *              finished by any reason.
 */
fun AnimatedFloat.fling(
    startVelocity: Float,
    decay: FloatDecayAnimationSpec = ExponentialDecay(),
    adjustTarget: (Float) -> TargetAnimation?,
    onEnd: OnAnimationEnd? = null
) {
    if (isRunning) {
        notifyEnded(Interrupted, value)
    }

    this.onEnd = { endReason, endValue ->
        onEnd?.invoke(endReason, endValue, velocity)
    }

    // start from current value with the given velocity
    targetValue = decay.getTarget(value, startVelocity)
    val targetAnimation = adjustTarget(targetValue)
    if (targetAnimation == null) {
        val animWrapper = decay.createAnimation(value, startVelocity)
        startAnimation(animWrapper)
    } else {
        targetValue = targetAnimation.target
        val animWrapper = TargetBasedAnimation(
            targetAnimation.animation,
            value,
            targetAnimation.target,
            typeConverter,
            AnimationVector1D(startVelocity)
        )
        startAnimation(animWrapper)
    }
}

private const val Unset: Long = -1

/**
 * Factory method for creating an [AnimatedValue] object, and initialize the value field to
 * [initVal].
 *
 * @param initVal Initial value to initialize the animation to.
 * @param typeConverter Converter for converting value type [T] to [AnimationVector], and vice versa
 * @param clock The animation clock used to drive the animation.
 * @param visibilityThreshold Threshold at which the animation may round off to its target value.
 */
fun <T, V : AnimationVector> AnimatedValue(
    initVal: T,
    typeConverter: TwoWayConverter<T, V>,
    clock: AnimationClockObservable,
    visibilityThreshold: T? = null
): AnimatedValue<T, V> =
    AnimatedValueImpl(initVal, typeConverter, clock, visibilityThreshold)

/**
 * Factory method for creating an [AnimatedVector] object, and initialize the value field to
 * [initVal].
 *
 * @param initVal Initial value to initialize the animation to.
 * @param clock The animation clock used to drive the animation.
 * @param visibilityThreshold Threshold at which the animation may round off to its target value.
 */
fun <V : AnimationVector> AnimatedVector(
    initVal: V,
    clock: AnimationClockObservable,
    visibilityThreshold: V = initVal.newInstanceOfValue(Spring.DefaultDisplacementThreshold)
): AnimatedValue<V, V> =
    AnimatedValueImpl(initVal, TwoWayConverter({ it }, { it }), clock, visibilityThreshold)

/**
 * Factory method for creating an [AnimatedFloat] object, and initialize the value field to
 * [initVal].
 *
 * @param initVal Initial value to initialize the animation to.
 * @param clock The animation clock used to drive the animation.
 * @param visibilityThreshold Threshold at which the animation may round off to its target value.
 */
fun AnimatedFloat(
    initVal: Float,
    clock: AnimationClockObservable,
    visibilityThreshold: Float = Spring.DefaultDisplacementThreshold
): AnimatedFloat = AnimatedFloatImpl(initVal, clock, visibilityThreshold)

// Private impl for AnimatedValue
private class AnimatedValueImpl<T, V : AnimationVector>(
    initVal: T,
    typeConverter: TwoWayConverter<T, V>,
    clock: AnimationClockObservable,
    visibilityThreshold: T? = null
) : AnimatedValue<T, V>(typeConverter, clock, visibilityThreshold) {
    override var value: T = initVal
}

// Private impl for AnimatedFloat
private class AnimatedFloatImpl(
    initVal: Float,
    clock: AnimationClockObservable,
    visibilityThreshold: Float
) : AnimatedFloat(clock, visibilityThreshold) {
    override var value: Float = initVal
}

private fun <V : AnimationVector> V.newInstanceOfValue(value: Float): V {
    return newInstance().apply {
        (0 until size).forEach { set(it, value) }
    }
}
