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

@file:Suppress("DEPRECATION")

package androidx.compose.animation.core

import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastFirstOrNull

/**
 * Static specification for the transition from one state to another.
 *
 * Each property involved in the states that the transition is from and to can have an animation
 * associated with it. When such an animation is defined, the animation system will be using it
 * instead of the default [FloatSpringSpec] animation to animate the value change for that
 * property.
 *
 **/
@Deprecated("Please use updateTransition or rememberInfiniteTransition instead.")
class TransitionSpec<S> internal constructor(private val fromToPairs: Array<out Pair<S?, S?>>) {

    /**
     * Optional state where should we start switching after this transition finishing.
     */
    var nextState: S? = null

    /**
     * The interruption handling mechanism. The default interruption handling is
     * [InterruptionHandling.PHYSICS]. Meaning both value and velocity of the property will be
     * preserved as the target state (and therefore target animation value) changes.
     * [InterruptionHandling.TWEEN], which only ensures the continuity of current animation value.
     * [InterruptionHandling.UNINTERRUPTIBLE] defines a scenario where an animation is so important
     * that it cannot be interrupted, so the new state request has to be queued.
     * [InterruptionHandling.SNAP_TO_END] can be used for cases where higher priority events (such
     * as user gesture) come in and the on-going animation needs to finish immediately to give way
     * to the user events.
     */
    var interruptionHandling: InterruptionHandling = InterruptionHandling.PHYSICS

    /**
     * The default animation to use when it wasn't explicitly provided for a property
     */
    internal enum class DefaultAnimation {
        Spring,
        Snap
    }

    internal var defaultAnimation: DefaultAnimation = DefaultAnimation.Spring

    private val propAnimation: MutableMap<PropKey<*, *>, VectorizedAnimationSpec<*>> =
        mutableMapOf()

    internal fun <T, V : AnimationVector> getAnimationForProp(
        prop: PropKey<T, V>
    ): VectorizedAnimationSpec<V> {
        @Suppress("UNCHECKED_CAST")
        return (
            propAnimation.getOrPut(
                prop,
                { createSpec<V>(defaultAnimation) }
            )
            ) as VectorizedAnimationSpec<V>
    }

    private fun <V : AnimationVector> createSpec(
        anim: DefaultAnimation
    ): VectorizedAnimationSpec<V> =
        when (anim) {
            DefaultAnimation.Spring -> VectorizedSpringSpec()
            DefaultAnimation.Snap -> VectorizedSnapSpec()
        }

    internal fun defines(from: S?, to: S?) =
        fromToPairs.any { it.first == from && it.second == to }

    /**
     * Associates a property with an [AnimationSpec]
     *
     * @param animationSpec: [AnimationSpec] for animating [this] property value changes
     */
    infix fun <T, V : AnimationVector> PropKey<T, V>.using(animationSpec: AnimationSpec<T>) {
        propAnimation[this] =
            animationSpec.vectorize(this.typeConverter) as VectorizedAnimationSpec<*>
    }
}

/**
 * Creates a [TweenSpec] configured with the given duration, delay and easing curve.
 *
 * @param durationMillis duration of the animation spec
 * @param delayMillis the amount of time in milliseconds that animation waits before starting
 * @param easing the easing curve that will be used to interpolate between start and end
 */
@Stable
fun <T> tween(
    durationMillis: Int = DefaultDurationMillis,
    delayMillis: Int = 0,
    easing: Easing = FastOutSlowInEasing
): TweenSpec<T> = TweenSpec(durationMillis, delayMillis, easing)

/**
 * Creates a [SpringSpec] that uses the given spring constants (i.e. [dampingRatio] and
 * [stiffness]. The optional [visibilityThreshold] defines when the animation
 * should be considered to be visually close enough to round off to its target.
 *
 * @param dampingRatio damping ratio of the spring. [Spring.DampingRatioNoBouncy] by default.
 * @param stiffness stiffness of the spring. [Spring.StiffnessMedium] by default.
 * @param visibilityThreshold optionally specifies the visibility threshold.
 */
@Stable
fun <T> spring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    visibilityThreshold: T? = null
): SpringSpec<T> =
    SpringSpec(dampingRatio, stiffness, visibilityThreshold)

/**
 * Creates a [KeyframesSpec] animation, initialized with [init]. For example:
 *
 * @param init Initialization function for the [KeyframesSpec] animation
 * @See KeyframesSpec.KeyframesSpecConfig
 */
@Stable
fun <T> keyframes(
    init: KeyframesSpec.KeyframesSpecConfig<T>.() -> Unit
): KeyframesSpec<T> {
    return KeyframesSpec(KeyframesSpec.KeyframesSpecConfig<T>().apply(init))
}

/**
 * Creates a [RepeatableSpec] that plays a [DurationBasedAnimationSpec] (e.g.
 * [TweenSpec], [KeyframesSpec]) the amount of iterations specified by [iterations].
 *
 * The iteration count describes the amount of times the animation will run.
 * 1 means no repeat. Recommend [infiniteRepeatable] for creating an infinity repeating animation.
 *
 * __Note__: When repeating in the [RepeatMode.Reverse] mode, it's highly recommended to have an
 * __odd__ number of iterations. Otherwise, the animation may jump to the end value when it finishes
 * the last iteration.
 *
 * @param iterations the total count of iterations, should be greater than 1 to repeat.
 * @param animation animation that will be repeated
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *                  [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 */
@Stable
fun <T> repeatable(
    iterations: Int,
    animation: DurationBasedAnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart
): RepeatableSpec<T> =
    RepeatableSpec(iterations, animation, repeatMode)

/**
 * Creates a [InfiniteRepeatableSpec] that plays a [DurationBasedAnimationSpec] (e.g.
 * [TweenSpec], [KeyframesSpec]) infinite amount of iterations.
 *
 * For non-infinitely repeating animations, consider [repeatable].
 *
 * @param animation animation that will be repeated
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *                  [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 */
@Stable
fun <T> infiniteRepeatable(
    animation: DurationBasedAnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart
): InfiniteRepeatableSpec<T> =
    InfiniteRepeatableSpec(animation, repeatMode)

/**
 * Creates a Snap animation for immediately switching the animating value to the end value.
 *
 * @param delayMillis the number of milliseconds to wait before the animation runs. 0 by default.
 */
@Stable
fun <T> snap(delayMillis: Int = 0) = SnapSpec<T>(delayMillis)

/**
 * [TransitionDefinition] contains all the animation related configurations that will be used in
 * a state-based transition. It holds a set of [TransitionState]s and an optional set of
 * [TransitionSpec]s. It can be used in [androidx.compose.animation.transition] to create a
 * state-based animation in Compose.
 *
 * Each [TransitionState] specifies how the UI should look in terms of values
 * associated with properties that differentiates the UI from one conceptual state to anther. Each
 * [TransitionState] can be considered as a snapshot of the UI in the form of property values.
 *
 * [TransitionSpec] defines how to animate from one state to another with a specific animation for
 * each property defined in the states. [TransitionSpec] can be created using [transition] method
 * inside of a [TransitionDefinition]. Currently the animations supported in a [transition] are:
 * [tween], [keyframes], [spring], [snap], [repeatable]. When no [TransitionSpec] is specified,
 * the default [spring] animation will be used for all properties involved.
 * Similarly, when no animation is provided in a [TransitionSpec] for a particular property,
 * the default physics animation will be used. For each [transition], both the from and the to state
 * can be omitted. Omitting in this case is equivalent to a wildcard on the starting state or ending
 * state. When both are omitted at the same time, it means this transition applies to all the state
 * transitions unless a more specific transition have been defined.
 *
 * To create a [TransitionDefinition], there are generally 3 steps involved:
 *
 * __Step 1__: Create PropKeys. One [PropKey] is required for each property/value that needs to
 * be animated. These should be file level properties, so they are visible to
 * [TransitionDefinition] ( which will be created in step 3).
 *
 *     val radius = FloatPropKey()
 *     val alpha = FloatPropKey()
 *
 * __Step 2__ (optional): Create state names.
 *
 * This is an optional but recommended step to create a reference for different states that the
 * animation should end at. State names can be of type [T], which means they can be string,
 * integer, etc, or any custom object, so long as they are consistent.

 * It is recommended to either reuse the states that you already defined (e.g.
 * TogglableState.On, TogglableState.Off, etc) for animating those state changes, or create
 * an enum class for all the animation states.
 *
 *     enum class ButtonState {
 *         Released, Pressed, Disabled
 *     }
 *
 * __Step 3__: Create a [TransitionDefinition] using the animation DSL.
 *
 * [TransitionDefinition] is conceptually an animation configuration that defines:
 * 1) States, each of which are described as a set of values.  Each value is associated with a
 * PropKey.
 * 2) Optional transitions, for how to animate from one state to another.
 *
 * Once a [TransitionDefinition] is created, [androidx.compose.animation.transition] composable can take
 * it as an input and create a state-based transition in compose.
 *
 * @see [androidx.compose.animation.transition]
 */
@Deprecated("Please use updateTransition or rememberInfiniteTransition instead.")
class TransitionDefinition<T> {
    internal val states: MutableMap<T, StateImpl<T>> = mutableMapOf()
    internal lateinit var defaultState: StateImpl<T>
    private val transitionSpecs: MutableList<TransitionSpec<T>> = mutableListOf()

    // TODO: Consider also having the initial defined at call site for cases where many components
    // share the same transition def
    // TODO: (Optimization) Type param in TransitionSpec requires this defaultTransitionSpec to be
    // re-created at least for each state type T. Consider dropping this T beyond initial sanity
    // check.
    private val defaultTransitionSpec = TransitionSpec<T>(arrayOf(null to null))

    /**
     * [MutableTransitionState] is used in [TransitionDefinition] for constructing various
     * [TransitionState]s with corresponding properties and their values.
     */
    @Deprecated("Please use updateTransition or rememberInfiniteTransition instead.")
    interface MutableTransitionState {
        operator fun <T, V : AnimationVector> set(propKey: PropKey<T, V>, prop: T)
    }

    /**
     * Defines all the properties and their values associated with the state with the name: [name]
     * The first state defined in the transition definition will be the default state, whose
     * property values will be used as its initial values to createAnimation from.
     *
     * Note that the first [MutableTransitionState] created with [state] in a [TransitionDefinition]
     * will be used as the initial state.
     *
     * @param name The name of the state, which can be used to createAnimation from or to this state
     * @param init Lambda to initialize a state
     */
    fun state(name: T, init: MutableTransitionState.() -> Unit) {
        val newState = StateImpl(name).apply(init)
        states[name] = newState
        if (!::defaultState.isInitialized) {
            defaultState = newState
        }
    }

    /**
     * Defines a transition from state [fromState] to [toState]. When animating from one state to
     * another, [TransitionAnimation] will find the most specific matching transition, and use the
     * animations defined in it for the state transition. Both [fromState] and [toState] are
     * optional. When undefined, it means a wildcard transition going from/to any state.
     *
     * @param fromState The state that the transition will be animated from
     * @param toState The state that the transition will be animated to
     * @param init Lambda to initialize the transition
     */
    fun transition(fromState: T? = null, toState: T? = null, init: TransitionSpec<T>.() -> Unit) {
        transition(fromState to toState, init = init)
    }

    /**
     * Defines a transition from state first value to the second value of the [fromToPairs].
     * When animating from one state to another, [TransitionAnimation] will find the most specific
     * matching transition, and use the animations defined in it for the state transition. Both
     * values in the pair can be null. When they are null, it means a wildcard transition going
     * from/to any state.
     *
     * Sample of usage with [Pair]s infix extension [to]:
     *
     * @param fromToPairs The pairs of from and to states for this transition
     * @param init Lambda to initialize the transition
     */
    fun transition(vararg fromToPairs: Pair<T?, T?>, init: TransitionSpec<T>.() -> Unit) {
        val newSpec = TransitionSpec(fromToPairs).apply(init)
        transitionSpecs.add(newSpec)
    }

    /**
     * With this transition definition we are saying that every time we reach the
     * state 'from' we should immediately snap to 'to' state instead.
     *
     * Sample of usage with [Pair]s infix extension [to]:
     *     snapTransition(State.Released to State.Pressed)
     *
     * @param fromToPairs The pairs of states for this transition
     * @param nextState Optional state where should we start switching after snap
     */
    fun snapTransition(vararg fromToPairs: Pair<T?, T?>, nextState: T? = null) =
        transition(*fromToPairs) {
            this.nextState = nextState
            defaultAnimation = TransitionSpec.DefaultAnimation.Snap
        }

    internal fun getSpec(fromState: T, toState: T): TransitionSpec<T> {
        return transitionSpecs.fastFirstOrNull { it.defines(fromState, toState) }
            ?: transitionSpecs.fastFirstOrNull { it.defines(fromState, null) }
            ?: transitionSpecs.fastFirstOrNull { it.defines(null, toState) }
            ?: transitionSpecs.fastFirstOrNull { it.defines(null, null) }
            ?: defaultTransitionSpec
    }

    /**
     * Returns a state holder for the specific state [name]. Useful for the cases
     * where we don't need actual animation to be happening like in tests.
     */
    fun getStateFor(name: T): TransitionState = states.getValue(name)
}

/**
 * Creates a transition animation using the transition definition and the given clock.
 *
 * @param clock The clock source for animation to get frame time from.
 */
@Deprecated("Please use updateTransition or rememberInfiniteTransition instead.")
fun <T> TransitionDefinition<T>.createAnimation(
    clock: AnimationClockObservable,
    initState: T? = null
) = TransitionAnimation(this, clock, initState)

/**
 * Creates a [TransitionDefinition] using the [init] function to initialize it.
 *
 * @param init Initialization function for the [TransitionDefinition]
 */
@Deprecated("Please use updateTransition or rememberInfiniteTransition instead.")
fun <T> transitionDefinition(init: TransitionDefinition<T>.() -> Unit) =
    TransitionDefinition<T>().apply(init)

@Deprecated("Please use updateTransition or rememberInfiniteTransition instead.")
enum class InterruptionHandling {
    PHYSICS,
    SNAP_TO_END, // Not yet supported
    TWEEN, // Not yet supported
    UNINTERRUPTIBLE
}