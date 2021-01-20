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

@file:OptIn(InternalAnimationApi::class)

package androidx.compose.animation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.dispatch.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Bounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Uptime
import kotlin.math.max

/**
 * This sets up a [Transition], and updates it with the target provided by [targetState]. When
 * [targetState] changes, [Transition] will run all of its child animations towards their
 * target values specified for the new [targetState]. Child animations can be dynamically added
 * using [Transition.animateFloat], [animateColor][ androidx.compose.animation.animateColor],
 * [Transition.animateValue], etc.
 *
 * [label] is used to differentiate different transitions in Android Studio.
 *
 * @sample androidx.compose.animation.core.samples.GestureAnimationSample
 *
 * @return a [Transition] object, to which animations can be added.
 * @see Transition
 * @see animateFloat
 * @see animateValue
 * @see androidx.compose.animation.animateColor
 */
@Composable
fun <T> updateTransition(
    targetState: T,
    label: String? = null
): Transition<T> {
    val transition = remember { Transition(targetState, label) }
    transition.updateTarget(targetState)
    return transition
}

/**
 * [Transition] manages all the child animations on a state level. Child animations
 * can be created in a declarative way using [animateFloat], [animateValue],
 * [animateColor][androidx.compose.animation.animateColor] etc. When the [targetState] changes,
 * [Transition] will automatically start or adjust course for all its child animations to animate
 * to the new target values defined for each animation.
 *
 * After arriving at [targetState], [Transition] will be triggered to run if any child animation
 * changes its target value (due to their dynamic target calculation logic, such as theme-dependent
 * values).
 *
 * @sample androidx.compose.animation.core.samples.GestureAnimationSample
 *
 * @return a [Transition] object, to which animations can be added.
 * @see updateTransition
 * @see animateFloat
 * @see animateValue
 * @see androidx.compose.animation.animateColor
 */
// TODO: Support creating Transition outside of composition and support imperative use of Transition
class Transition<S> internal constructor(
    initialState: S,
    val label: String? = null
) {

    /**
     * Current state of the transition. This will always be the initialState of the transition
     * until the transition is finished. Once the transition is finished, [currentState] will be
     * set to [targetState].
     */
    var currentState: S by mutableStateOf(initialState)
        internal set

    /**
     * Target state of the transition. This will be read by all child animations to determine their
     * most up-to-date target values.
     */
    var targetState: S by mutableStateOf(initialState)
        internal set

    /**
     * [segment] contains the initial state and the target state of the currently on-going
     * transition.
     */
    var segment: Segment<S> by mutableStateOf(Segment(initialState, initialState))
        private set

    /**
     * Indicates whether there is any animation running in the transition.
     */
    val isRunning: Boolean
        get() = startTime != Uptime.Unspecified

    /**
     * Play time in nano-seconds. [playTimeNanos] is always non-negative. It starts from 0L at the
     * beginning of the transition and increment until all child animations have finished.
     */
    internal var playTimeNanos by mutableStateOf(0L)

    // This gets calculated every time child is updated/added
    internal var updateChildrenNeeded: Boolean by mutableStateOf(true)
    private var startTime = Uptime.Unspecified

    /** @suppress **/
    @InternalAnimationApi
    val animations: List<TransitionAnimationState<*, *>>
        get() = _animations

    private val _animations = ArrayList<TransitionAnimationState<*, *>>(4)

    // Seeking related
    @PublishedApi
    internal var isSeeking: Boolean by mutableStateOf(false)
    private var lastSeekedTimeNanos: Long = 0L

    /** @suppress **/
    @InternalAnimationApi
    var totalDurationNanos: Long by mutableStateOf(0L)
        private set

    // Target state that is currently being animated to
    private var currentTargetState: S = initialState

    internal fun onFrame(frameTimeNanos: Long) {
        if (startTime == Uptime.Unspecified) {
            startTime = Uptime(frameTimeNanos)
        }
        updateChildrenNeeded = false

        // Update play time
        playTimeNanos = frameTimeNanos - startTime.nanoseconds
        var allFinished = true
        // Pulse new playtime
        _animations.forEach {
            if (!it.isFinished) {
                it.onPlayTimeChanged(playTimeNanos)
            }
            // Check isFinished flag again after the animation pulse
            if (!it.isFinished) {
                allFinished = false
            }
        }
        if (allFinished) {
            startTime = Uptime.Unspecified
            currentState = targetState
            playTimeNanos = 0
        }
    }

    /**
     * This allows tools to set the transition (between initial and target state) to any time.
     * @suppress
     */
    @InternalAnimationApi
    fun seek(initialState: S, targetState: S, playTimeNanos: Long) {
        // Reset running state
        startTime = Uptime.Unspecified
        if (!isSeeking || this.currentState != initialState || this.targetState != targetState) {
            // Reset all child animations
            this.currentState = initialState
            this.targetState = targetState
            isSeeking = true
            segment = Segment(initialState, targetState)
        }

        if (playTimeNanos != lastSeekedTimeNanos) {
            // Only pulse all children when the play time or any child has changed.
            _animations.forEach {
                it.seekTo(playTimeNanos)
            }
            lastSeekedTimeNanos = playTimeNanos
        }
    }

    @PublishedApi
    internal fun addAnimation(animation: TransitionAnimationState<*, *>) =
        _animations.add(animation)

    @PublishedApi
    internal fun removeAnimation(animation: TransitionAnimationState<*, *>) {
        _animations.remove(animation)
    }

    // This target state should only be used to modify "mutableState"s, as it could potentially
    // roll back. The
    @Suppress("ComposableNaming")
    @Composable
    internal fun updateTarget(targetState: S) {
        if (!isSeeking) {
            // This is needed because child animations rely on this target state and the state pair to
            // update their animation specs
            if (this.targetState != targetState) {
                // Starting state should be the "next" state when waypoints are impl'ed
                segment = Segment(this.targetState, targetState)
                currentState = this.targetState
                this.targetState = targetState
            }
            SideEffect {
                animateTo(targetState)
            }

            // target != currentState adds LaunchedEffect into the tree in the same frame as
            // target change.
            if (targetState != currentState || isRunning || updateChildrenNeeded) {
                LaunchedEffect(this) {
                    while (true) {
                        withFrameNanos {
                            onFrame(it)
                        }
                    }
                }
            }
        }
    }

    internal fun animateTo(targetState: S) {
        if (targetState != currentTargetState) {
            if (isRunning) {
                startTime = Uptime(startTime.nanoseconds + playTimeNanos)
                playTimeNanos = 0
            } else {
                updateChildrenNeeded = true
            }
            currentTargetState = targetState
            // If target state is changed, reset all the animations to be re-created in the
            // next frame w/ their new target value. Child animations target values are updated in
            // the side effect that may not have happened when this function in invoked.
            _animations.forEach { it.resetAnimation() }
        }
    }

    private fun onChildAnimationUpdated() {
        updateChildrenNeeded = true
        if (isSeeking) {
            // Update total duration
            var maxDurationNanos = 0L
            _animations.forEach {
                maxDurationNanos = max(maxDurationNanos, it.durationNanos)
                it.seekTo(lastSeekedTimeNanos)
            }
            totalDurationNanos = maxDurationNanos
            // TODO: Is update duration the only thing that needs to be done during seeking to
            //  accommodate update children?
            updateChildrenNeeded = false
        }
    }

    // TODO: Consider making this public
    /** Suppress **/
    @InternalAnimationApi
    inner class TransitionAnimationState<T, V : AnimationVector> @PublishedApi internal
    constructor(
        initialValue: T,
        initialVelocityVector: V,
        val typeConverter: TwoWayConverter<T, V>,
        val label: String
    ) : State<T> {

        // Changed during composition, may rollback
        @PublishedApi
        internal var targetValue: T by mutableStateOf(initialValue)
            internal set

        @PublishedApi
        internal var animationSpec: FiniteAnimationSpec<T> by mutableStateOf(spring())
        private var animation: TargetBasedAnimation<T, V> by mutableStateOf(
            TargetBasedAnimation(
                animationSpec, typeConverter, initialValue, targetValue,
                initialVelocityVector
            )
        )
        internal var isFinished: Boolean by mutableStateOf(true)
        private var offsetTimeNanos by mutableStateOf(0L)

        // Changed during animation, no concerns of rolling back
        override var value by mutableStateOf(initialValue)
            internal set
        internal var velocityVector: V = initialVelocityVector
        internal val durationNanos
            get() = animation.durationMillis.times(1_000_000L)

        internal fun onPlayTimeChanged(playTimeNanos: Long) {
            val playTimeMillis = (playTimeNanos - offsetTimeNanos) / 1_000_000L
            value = animation.getValue(playTimeMillis)
            velocityVector = animation.getVelocityVector(playTimeMillis)
            if (animation.isFinished(playTimeMillis)) {
                isFinished = true
                offsetTimeNanos = 0
            }
        }

        internal fun seekTo(playTimeNanos: Long) {
            // TODO: unlikely but need to double check that animation returns the correct values
            // when play time is way past their durations.
            val playTimeMillis = playTimeNanos / 1_000_000L
            value = animation.getValue(playTimeMillis)
            velocityVector = animation.getVelocityVector(playTimeMillis)
        }

        private fun updateAnimation(initialValue: T = value) {
            animation = TargetBasedAnimation(
                animationSpec,
                typeConverter,
                initialValue,
                targetValue,
                velocityVector
            )
            onChildAnimationUpdated()
        }

        internal fun resetAnimation() {
            offsetTimeNanos = 0
            isFinished = false
            updateAnimation()
        }

        @PublishedApi
        // This gets called *during* composition
        internal fun updateTargetValue(targetValue: T) {
            if (this.targetValue != targetValue) {
                this.targetValue = targetValue
                isFinished = false
                updateAnimation()
                // This is needed because the target change could happen during a transition
                offsetTimeNanos = playTimeNanos
            }
        }

        @PublishedApi
        // This gets called *during* composition
        internal fun updateInitialAndTargetValue(initialValue: T, targetValue: T) {
            this.targetValue = targetValue
            if (animation.initialValue == initialValue && animation.targetValue == targetValue) {
                return
            }
            updateAnimation(initialValue)
        }
    }

    /**
     * [Segment] holds [initialState] and [targetState], which are the beginning and end of a
     * transition. These states will be used to obtain the animation spec that will be used for this
     * transition from the child animations.
     */
    class Segment<S>(val initialState: S, val targetState: S) {
        @Deprecated("transitionSpec no longer takes a parameter", ReplaceWith("this"))
        val it = this

        /**
         * Returns whether the provided state matches the [initialState] && the provided
         * [targetState] matches [Segment.targetState].
         */
        infix fun S.isTransitioningTo(targetState: S): Boolean {
            return this == initialState && targetState == this@Segment.targetState
        }
    }
}

/**
 * Creates an animation of type [T] as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition]. [typeConverter] will be used to convert
 * between type [T] and [AnimationVector] so that the animation system knows how to animate it.
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, ambient, themes, etc. If the targetValue changes outside
 * of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 * @see updateTransition
 * @see animateFloat
 * @see androidx.compose.animation.animateColor
 */
@Composable
inline fun <S, T, V : AnimationVector> Transition<S>.animateValue(
    typeConverter: TwoWayConverter<T, V>,
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<T> =
        { spring() },
    label: String = "ValueAnimation",
    targetValueByState: @Composable (state: S) -> T
): State<T> {

    val targetValue = targetValueByState(targetState)
    // TODO: need a better way to store initial value.
    val initNeeded = remember(this) { mutableStateOf(true) }
    val initValue = if (initNeeded.value) targetValueByState(currentState) else targetValue
    val transitionAnimation = remember(this) {
        // Initialize the animation state to initialState value, so if it's added during a
        // transition run, it'll participate in the animation.
        // This is preferred because it's easy to opt out - Simply adding new animation once
        // currentState == targetState would opt out.
        TransitionAnimationState(
            initValue,
            typeConverter.createZeroVectorFrom(targetValue),
            typeConverter,
            label
        )
    }
    transitionAnimation.animationSpec = transitionSpec(segment)

    if (initNeeded.value) {
        SideEffect {
            initNeeded.value = false
        }
    }

    if (isSeeking) {
        // In the case of seeking, we also need to update initial value as needed
        val initialValue = targetValueByState(segment.initialState)
        transitionAnimation.updateInitialAndTargetValue(initialValue, targetValue)
    } else {
        transitionAnimation.updateTargetValue(targetValue)
    }

    DisposableEffect(transitionAnimation) {
        addAnimation(transitionAnimation)
        onDispose {
            removeAnimation(transitionAnimation)
        }
    }
    return transitionAnimation
}

// TODO: Remove noinline when b/174814083 is fixed.
/**
 * Creates a Float animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, ambient, themes, etc. If the targetValue changes outside
 * of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * @sample androidx.compose.animation.core.samples.AnimateFloatSample
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 * @see updateTransition
 * @see animateValue
 * @see androidx.compose.animation.animateColor
 */
@Composable
inline fun <S> Transition<S>.animateFloat(
    noinline transitionSpec:
        @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Float> = { spring() },
    label: String = "FloatAnimation",
    targetValueByState: @Composable (state: S) -> Float
): State<Float> =
    animateValue(Float.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [Dp] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, ambient, themes, etc. If the targetValue changes outside
 * of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateDp(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Dp> = {
        spring(visibilityThreshold = Dp.VisibilityThreshold)
    },
    label: String = "DpAnimation",
    targetValueByState: @Composable (state: S) -> Dp
): State<Dp> =
    animateValue(Dp.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates an [Offset] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, ambient, themes, etc. If the targetValue changes outside
 * of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateOffset(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Offset> = {
        spring(visibilityThreshold = Offset.VisibilityThreshold)
    },
    label: String = "OffsetAnimation",
    targetValueByState: @Composable (state: S) -> Offset
): State<Offset> =
    animateValue(Offset.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [Size] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, ambient, themes, etc. If the targetValue changes outside
 * of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateSize(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Size> = {
        spring(visibilityThreshold = Size.VisibilityThreshold)
    },
    label: String = "SizeAnimation",
    targetValueByState: @Composable (state: S) -> Size
): State<Size> =
    animateValue(Size.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [IntOffset] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, ambient, themes, etc. If the targetValue changes outside
 * of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateIntOffset(
    noinline transitionSpec:
        @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<IntOffset> =
            { spring(visibilityThreshold = IntOffset(1, 1)) },
    label: String = "IntOffsetAnimation",
    targetValueByState: @Composable (state: S) -> IntOffset
): State<IntOffset> =
    animateValue(IntOffset.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [Int] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, ambient, themes, etc. If the targetValue changes outside
 * of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateInt(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Int> = {
        spring(visibilityThreshold = 1)
    },
    label: String = "IntAnimation",
    targetValueByState: @Composable (state: S) -> Int
): State<Int> =
    animateValue(Int.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [IntSize] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, ambient, themes, etc. If the targetValue changes outside
 * of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateIntSize(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<IntSize> =
        { spring(visibilityThreshold = IntSize(1, 1)) },
    label: String = "IntSizeAnimation",
    targetValueByState: @Composable (state: S) -> IntSize
): State<IntSize> =
    animateValue(IntSize.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [Bounds] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, ambient, themes, etc. If the targetValue changes outside
 * of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateBounds(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Bounds> = {
        spring(visibilityThreshold = Bounds.VisibilityThreshold)
    },
    label: String = "BoundsAnimation",
    targetValueByState: @Composable (state: S) -> Bounds
): State<Bounds> =
    animateValue(Bounds.VectorConverter, transitionSpec, label, targetValueByState)

/**
 * Creates a [Rect] animation as a part of the given [Transition]. This means the states
 * of this animation will be managed by the [Transition].
 *
 * [targetValueByState] is used as a mapping from a target state to the target value of this
 * animation. [Transition] will be using this mapping to determine what value to target this
 * animation towards. __Note__ that [targetValueByState] is a composable function. This means the
 * mapping function could access states, ambient, themes, etc. If the targetValue changes outside
 * of a [Transition] run (i.e. when the [Transition] already reached its targetState), the
 * [Transition] will start running again to ensure this animation reaches its new target smoothly.
 *
 * An optional [transitionSpec] can be provided to specify (potentially different) animation for
 * each pair of initialState and targetState. [FiniteAnimationSpec] includes any non-infinite
 * animation, such as [tween], [spring], [keyframes] and even [repeatable], but not
 * [infiniteRepeatable]. By default, [transitionSpec] uses a [spring] animation for all transition
 * destinations.
 *
 * [label] is used to differentiate from other animations in the same transition in Android Studio.
 *
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateRect(
    noinline transitionSpec: @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Rect> =
        { spring(visibilityThreshold = Rect.VisibilityThreshold) },
    label: String = "RectAnimation",
    targetValueByState: @Composable (state: S) -> Rect
): State<Rect> =
    animateValue(Rect.VectorConverter, transitionSpec, label, targetValueByState)
