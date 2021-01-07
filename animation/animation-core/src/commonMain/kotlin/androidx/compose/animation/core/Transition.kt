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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.dispatch.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Bounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Uptime

/**
 * This sets up a [Transition], and updates it with the target provided by [targetState]. When
 * [targetState] changes, [Transition] will run all of its child animations towards their
 * target values specified for the new [targetState]. Child animations can be dynamically added
 * using [Transition.animateFloat], [animateColor][ androidx.compose.animation.animateColor],
 * [Transition.animateValue], etc.
 *
 * When all the animations in the transition have finished running, the provided [onFinished] will
 * be invoked.
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
    onFinished: (T) -> Unit = {}
): Transition<T> {
    val listener = rememberUpdatedState(onFinished)
    val transition = remember { Transition(targetState, listener) }
    // This is needed because child animations rely on this target state and the state pair to
    // update their animation specs
    transition.updateTarget(targetState)
    SideEffect {
        transition.animateTo(targetState)
    }
    if (transition.isRunning || transition.startRequested) {
        LaunchedEffect(transition) {
            while (true) {
                withFrameNanos {
                    transition.onFrame(it)
                }
            }
        }
    }
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
    private val onFinished: State<(S) -> Unit>
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
     * [transitionStates] contains the initial state and the target state of the currently on-going
     * transition.
     */
    var transitionStates: States<S> by mutableStateOf(States(initialState, initialState))
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
    /*@VisibleForTesting*/
    internal var playTimeNanos by mutableStateOf(0L)
    internal var startRequested: Boolean by mutableStateOf(false)
    private var startTime = Uptime.Unspecified
    private val animations = mutableVectorOf<TransitionAnimationState<*, *>>()

    // Target state that is currently being animated to
    private var currentTargetState: S = initialState

    internal fun onFrame(frameTimeNanos: Long) {
        if (startTime == Uptime.Unspecified) {
            startTime = Uptime(frameTimeNanos)
        }
        startRequested = false

        // Update play time
        playTimeNanos = frameTimeNanos - startTime.nanoseconds
        var allFinished = true
        // Pulse new playtime
        animations.forEach {
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
            onFinished.value(targetState)
        }
    }

    @PublishedApi
    internal fun addAnimation(animation: TransitionAnimationState<*, *>) =
        animations.add(animation)

    @PublishedApi
    internal fun removeAnimation(animation: TransitionAnimationState<*, *>) {
        animations.remove(animation)
    }

    // This target state should only be used to modify "mutableState"s, as it could potentially
    // roll back. The
    internal fun updateTarget(targetState: S) {
        if (transitionStates.targetState != targetState) {
            if (currentState == targetState) {
                // Going backwards
                transitionStates = States(this.targetState, targetState)
            } else {
                transitionStates = States(currentState, targetState)
            }
        }
        this.targetState = targetState
    }

    internal fun animateTo(targetState: S) {
        if (targetState != currentTargetState) {
            if (isRunning) {
                startTime = Uptime(startTime.nanoseconds + playTimeNanos)
                playTimeNanos = 0
            } else {
                startRequested = true
            }
            currentTargetState = targetState
            // If target state is changed, reset all the animations to be re-created in the
            // next frame w/ their new target value. Child animations target values are updated in
            // the side effect that may not have happened when this function in invoked.
            animations.forEach { it.resetAnimation() }
        }
    }

    // Called from children to start an animation
    private fun requestStart() {
        startRequested = true
    }

    // TODO: Consider making this public
    @PublishedApi
    internal inner class TransitionAnimationState<T, V : AnimationVector> @PublishedApi internal
    constructor(
        initialValue: T,
        initialVelocityVector: V,
        val typeConverter: TwoWayConverter<T, V>
    ) : State<T> {

        override var value by mutableStateOf(initialValue)
            internal set

        var targetValue: T = initialValue
            internal set
        var velocityVector: V = initialVelocityVector
            internal set
        var isFinished: Boolean by mutableStateOf(true)
            private set
        private var animation: Animation<T, V>? = null

        @PublishedApi
        internal var animationSpec: FiniteAnimationSpec<T> = spring()
        private var offsetTimeNanos = 0L

        internal fun onPlayTimeChanged(playTimeNanos: Long) {
            val anim = animation ?: TargetBasedAnimation<T, V>(
                animationSpec,
                typeConverter,
                value,
                targetValue,
                velocityVector
            ).also { animation = it }
            val playTimeMillis = (playTimeNanos - offsetTimeNanos) / 1_000_000L
            value = anim.getValue(playTimeMillis)
            velocityVector = anim.getVelocityVector(playTimeMillis)
            if (anim.isFinished(playTimeMillis)) {
                isFinished = true
                offsetTimeNanos = 0
            }
        }

        internal fun resetAnimation() {
            animation = null
            offsetTimeNanos = 0
            isFinished = false
        }

        @PublishedApi
        // This gets called from a side effect.
        internal fun updateTargetValue(targetValue: T) {
            if (this.targetValue != targetValue) {
                this.targetValue = targetValue
                isFinished = false
                animation = null
                offsetTimeNanos = playTimeNanos
                requestStart()
            }
        }
    }

    /**
     * [States] holds [initialState] and [targetState], which are the beginning and end of a
     * transition. These states will be used to obtain the animation spec that will be used for this
     * transition from the child animations.
     */
    class States<S>(val initialState: S, val targetState: S)
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
 * @return A [State] object, the value of which is updated by animation
 * @see updateTransition
 * @see animateFloat
 * @see androidx.compose.animation.animateColor
 */
@Composable
inline fun <S, T, V : AnimationVector> Transition<S>.animateValue(
    typeConverter: TwoWayConverter<T, V>,
    noinline transitionSpec: @Composable (Transition.States<S>) -> FiniteAnimationSpec<T> =
        { spring() },
    targetValueByState: @Composable (state: S) -> T
): State<T> {
    val targetValue = targetValueByState(targetState)
    val transitionAnimation = remember {
        TransitionAnimationState(
            targetValue,
            typeConverter.createZeroVectorFrom(targetValue),
            typeConverter
        )
    }
    val spec = transitionSpec(transitionStates)

    SideEffect {
        transitionAnimation.animationSpec = spec
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
 * @return A [State] object, the value of which is updated by animation
 * @see updateTransition
 * @see animateValue
 * @see androidx.compose.animation.animateColor
 */
@Composable
inline fun <S> Transition<S>.animateFloat(
    noinline transitionSpec:
        @Composable (Transition.States<S>) -> FiniteAnimationSpec<Float> = { spring() },
    targetValueByState: @Composable (state: S) -> Float
): State<Float> =
    animateValue(Float.VectorConverter, transitionSpec, targetValueByState)

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
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateDp(
    noinline transitionSpec: @Composable (Transition.States<S>) -> FiniteAnimationSpec<Dp> = {
        spring(visibilityThreshold = Dp.VisibilityThreshold)
    },
    targetValueByState: @Composable (state: S) -> Dp
): State<Dp> =
    animateValue(Dp.VectorConverter, transitionSpec, targetValueByState)

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
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateOffset(
    noinline transitionSpec: @Composable (Transition.States<S>) -> FiniteAnimationSpec<Offset> = {
        spring(visibilityThreshold = Offset.VisibilityThreshold)
    },
    targetValueByState: @Composable (state: S) -> Offset
): State<Offset> =
    animateValue(Offset.VectorConverter, transitionSpec, targetValueByState)

/**
 * Creates a [DpOffset] animation as a part of the given [Transition]. This means the states
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
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animatePosition(
    noinline transitionSpec: @Composable (Transition.States<S>) -> FiniteAnimationSpec<DpOffset> = {
        spring(visibilityThreshold = DpOffset.VisibilityThreshold)
    },
    targetValueByState: @Composable (state: S) -> DpOffset
): State<DpOffset> =
    animateValue(DpOffset.VectorConverter, transitionSpec, targetValueByState)

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
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateSize(
    noinline transitionSpec: @Composable (Transition.States<S>) -> FiniteAnimationSpec<Size> = {
        spring(visibilityThreshold = Size.VisibilityThreshold)
    },
    targetValueByState: @Composable (state: S) -> Size
): State<Size> =
    animateValue(Size.VectorConverter, transitionSpec, targetValueByState)

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
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateIntOffset(
    noinline transitionSpec: @Composable (Transition.States<S>) -> FiniteAnimationSpec<IntOffset> =
        { spring(visibilityThreshold = IntOffset(1, 1)) },
    targetValueByState: @Composable (state: S) -> IntOffset
): State<IntOffset> =
    animateValue(IntOffset.VectorConverter, transitionSpec, targetValueByState)

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
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateInt(
    noinline transitionSpec: @Composable (Transition.States<S>) -> FiniteAnimationSpec<Int> = {
        spring(visibilityThreshold = 1)
    },
    targetValueByState: @Composable (state: S) -> Int
): State<Int> =
    animateValue(Int.VectorConverter, transitionSpec, targetValueByState)

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
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateIntSize(
    noinline transitionSpec: @Composable (Transition.States<S>) -> FiniteAnimationSpec<IntSize> = {
        spring(visibilityThreshold = IntSize(1, 1))
    },
    targetValueByState: @Composable (state: S) -> IntSize
): State<IntSize> =
    animateValue(IntSize.VectorConverter, transitionSpec, targetValueByState)

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
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateBounds(
    noinline transitionSpec: @Composable (Transition.States<S>) -> FiniteAnimationSpec<Bounds> = {
        spring(visibilityThreshold = Bounds.VisibilityThreshold)
    },
    targetValueByState: @Composable (state: S) -> Bounds
): State<Bounds> =
    animateValue(Bounds.VectorConverter, transitionSpec, targetValueByState)

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
 * @return A [State] object, the value of which is updated by animation
 */
@Composable
inline fun <S> Transition<S>.animateRect(
    noinline transitionSpec: @Composable (Transition.States<S>) -> FiniteAnimationSpec<Rect> =
        { spring(visibilityThreshold = Rect.VisibilityThreshold) },
    targetValueByState: @Composable (state: S) -> Rect
): State<Rect> =
    animateValue(Rect.VectorConverter, transitionSpec, targetValueByState)
