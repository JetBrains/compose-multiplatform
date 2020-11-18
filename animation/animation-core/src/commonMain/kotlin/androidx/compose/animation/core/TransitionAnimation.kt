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

import androidx.compose.animation.core.InterruptionHandling.UNINTERRUPTIBLE

/**
 * [TransitionAnimation] is the underlying animation used in
 * [androidx.compose.animation.transition] for animating from one set of property values (i.e.
 * one [TransitionState]) to another. In compose, it is recommended to create such an animation
 * using [androidx.compose.animation.transition], instead of instantiating [TransitionAnimation]
 * directly.
 *
 * [TransitionAnimation] reads the property values out of the start and end state,  as well as the
 * animations defined for each state pair for each property, and run these animations until all
 * properties have reached their pre-defined values in the new state. When no animation is specified
 * for a property, a default [FloatSpringSpec] animation will be used.
 *
 * [TransitionAnimation] may be interrupted while the animation is on-going by a request to go
 * to another state. [TransitionAnimation] ensures that all the animating properties preserve their
 * current value and velocity as they createAnimation to the new state.
 *
 * Once a [TransitionDefinition] is instantiated, a [TransitionAnimation] can be created via
 * [TransitionDefinition.createAnimation].
 *
 * @param def Transition definition that defines states and transitions
 * @param clock Optional animation clock that pulses animations when time changes. By default,
 *              the system uses a choreographer based clock read from the [AnimationClockAmbient].
 *              A custom implementation of the [AnimationClockObservable] (such as a
 *              [androidx.compose.animation.core.ManualAnimationClock]) can be supplied here if
 *              there’s a need to manually control the clock (for example in tests).
 * @param initState Optional initial state for the transition. When undefined, the initial state
 *                  will be set to the first [toState] seen in the transition.
 * @param label Optional label for distinguishing different transitions in Android Studio.
 *
 * @see [androidx.compose.animation.transition]
 */
@OptIn(InternalAnimationApi::class)
class TransitionAnimation<T>(
    internal val def: TransitionDefinition<T>,
    private val clock: AnimationClockObservable,
    initState: T? = null,
    val label: String? = null
) : TransitionState {

    var onUpdate: (() -> Unit)? = null
    var onStateChangeFinished: ((T) -> Unit)? = null
    var isRunning = false
        private set

    private val UNSET = -1L
    private var fromState: StateImpl<T>
    private var toState: StateImpl<T>
    private val currentState: InternalAnimationState<T>
    private var startTime: Long = UNSET
    private var lastFrameTime: Long = UNSET
    private var pendingState: StateImpl<T>? = null

    // These animation wrappers contains the start/end value and start velocities for each animation
    // run, to make it convenient to query current values/velocities based on play time. They will
    // be thrown away after each animation run, as we expect start/end value and start
    // velocities to be dynamic. The stateless animation that the wrapper wraps around will be
    // re-used as they are stateless.
    private var currentAnimWrappers: MutableMap<
        PropKey<Any, AnimationVector>,
        Animation<Any, AnimationVector>
        > = mutableMapOf()
    private var startVelocityMap: MutableMap<PropKey<Any, AnimationVector>, Any> = mutableMapOf()

    /**
     * Named class for animation clock observer to help with tools' reflection.
     * @suppress
     */
    @InternalAnimationApi
    inner class TransitionAnimationClockObserver : AnimationClockObserver {
        // This API is intended for tools' use only. Hence the @InternalAnimationApi.
        val animation: TransitionAnimation<T> = this@TransitionAnimation

        override fun onAnimationFrame(frameTimeMillis: Long) {
            doAnimationFrame(frameTimeMillis)
        }
    }

    /**
     * This should be private. It's marked as InternalAnimationApi to give ui-tooling access to the
     * observer.
     * @suppress
     */
    @InternalAnimationApi
    val animationClockObserver: AnimationClockObserver = TransitionAnimationClockObserver()

    // TODO("Create a more efficient code path for default only transition def")

    init {
        // If an initial state is specified in the ctor, use that instead of the default state.
        val defaultState: StateImpl<T>
        if (initState == null) {
            defaultState = def.defaultState
        } else {
            defaultState = def.states[initState]!!
        }
        currentState = InternalAnimationState(defaultState, defaultState.name)
        // Need to come up with a better plan to avoid the foot gun of accidentally modifying state
        fromState = defaultState
        toState = defaultState
    }

    // Interpolate current state and the new state
    private fun setState(newState: StateImpl<T>) {
        if (isRunning) {
            val currentSpec = def.getSpec(fromState.name, toState.name)
            if (currentSpec.interruptionHandling == UNINTERRUPTIBLE) {
                pendingState = newState
                return
            }
        }

        val transitionSpec = def.getSpec(toState.name, newState.name)
        val playTime = getPlayTime()
        // TODO: handle the states that have only partial properties defined
        // For now assume all the properties are defined in all states

        // TODO: Support different interruption types
        // For now assume continuing with the same value,  and for floats the same velocity
        for ((prop, _) in newState.props) {
            val currentVelocity = currentAnimWrappers[prop]?.getVelocityVector(playTime)
            currentAnimWrappers[prop] = prop.createAnimationWrapper(
                transitionSpec.getAnimationForProp(prop), currentState[prop], currentVelocity,
                newState[prop]
            )

            // TODO: Will need to track a few timelines if we support partially defined list of
            // props in each state.
        }

        fromState = InternalAnimationState(currentState, toState.name)
        toState = newState

        // Start animation should be called after all the setup has been done
        startAnimation()
    }

    private fun getPlayTime(): Long {
        if (startTime == UNSET) {
            return 0L
        }
        return lastFrameTime - startTime
    }

    /**
     * Starts the animation to go to a new state with the given state name.
     *
     * @param name Name of the [TransitionState] that is defined in the [TransitionDefinition].
     */
    fun toState(name: T) {
        val nextState = def.states[name]
        if (nextState == null) {
            // Throw exception or ignore?
        } else if (pendingState != null && toState.name == name) {
            // just canceling the pending state
            pendingState = null
        } else if ((pendingState ?: toState).name == name) {
            // already targeting this state
        } else {
            setState(nextState)
        }
    }

    /**
     * This indicates whether animation assumes time stamps increase monotonically. If false,
     * animation will anticipate that the time may go backwards, therefore it won't ever finish,
     * until it's set to true again.
     * @suppress
     */
    @InternalAnimationApi
    var monotonic: Boolean = true
        set(value) {
            if (field == value) {
                return
            }
            field = value
            // Changing from false to true
            if (value && isRunning) {
                // Pump in another frame to properly finish
                doAnimationFrame(lastFrameTime)
            }
        }

    /**
     * This immediately and violently snaps the animation to the new state, regardless whether
     * there's an on-going animation. This will also put the animation in an finished state,
     * effectively unsubscribing the animation from the clock.
     *
     * @param toState the state that animation will be snapped to
     *
     * @suppress
     */
    @InternalAnimationApi
    fun snapToState(toState: T) {
        val stateChanged = toState == fromState.name

        // Snap all values to end value
        val newState = def.states[toState]!!
        for (prop in newState.props.keys) {
            currentState[prop] = newState[prop]
        }
        startVelocityMap.clear()

        if (isRunning) {
            endAnimation()
            currentAnimWrappers.clear()
            fromState = newState
            this.toState = newState
            pendingState = null
        }

        if (stateChanged) {
            onStateChangeFinished?.invoke(toState)
        }
    }

    /**
     * Gets the value of a property with a given property key.
     *
     * @param propKey Property key (defined in [TransitionDefinition]) for a specific property
     */
    override operator fun <T, V : AnimationVector> get(propKey: PropKey<T, V>): T {
        return currentState[propKey]
    }

    // Start animation if not running, otherwise reset start time
    private fun startAnimation() {
        if (!isRunning) {
            isRunning = true
            clock.subscribe(animationClockObserver)
        } else {
            startTime = lastFrameTime
        }
    }

    private fun doAnimationFrame(frameTimeMillis: Long) {
        // Remove finished animations
        lastFrameTime = frameTimeMillis
        if (startTime == UNSET) {
            startTime = frameTimeMillis
        }

        val playTime = getPlayTime()
        var finished = true
        for ((prop, animation) in currentAnimWrappers) {
            if (!animation.isFinished(playTime)) {
                currentState[prop] = animation.getValue(playTime)
                finished = false
            } else {
                currentState[prop] = toState[prop]
            }
        }

        onUpdate?.invoke()

        // When all the sub-animations have finished, we'll only end the transition or move on to
        // the pending state when the transition is monotonic, as we know time won't go backward.
        // Otherwise, we'll stay subscribed to the animation clock indefinitely.
        if (finished && monotonic) {
            // All animations have finished. Snap all values to end value
            for (prop in toState.props.keys) {
                currentState[prop] = toState[prop]
            }
            startVelocityMap.clear()

            endAnimation()
            val currentStateName = toState.name
            val spec = def.getSpec(fromState.name, toState.name)
            val nextState = def.states[spec.nextState]
            fromState = toState

            // Uninterruptible transition to the next state takes a priority over the pending state.
            if (nextState != null && spec.interruptionHandling == UNINTERRUPTIBLE) {
                setState(nextState)
            } else if (pendingState != null) {
                setState(pendingState!!)
                pendingState = null
            } else if (nextState != null) {
                setState(nextState)
            }
            onStateChangeFinished?.invoke(currentStateName)
        }
    }

    private fun endAnimation() {
        clock.unsubscribe(animationClockObserver)
        startTime = UNSET
        lastFrameTime = UNSET
        isRunning = false
    }
}

internal fun <T, V : AnimationVector> PropKey<T, V>.createAnimationWrapper(
    anim: VectorizedAnimationSpec<V>,
    start: T,
    startVelocity: V?,
    end: T
): Animation<T, V> =
    TargetBasedAnimation(anim, start, end, typeConverter, startVelocity)

/**
 * Private class allows mutation on the prop values.
 */
private class InternalAnimationState<T>(state: StateImpl<T>, name: T) : StateImpl<T>(name) {

    init {
        for ((prop, value) in state.props) {
            // Make a copy of the new values
            props[prop] = value
        }
    }

    override operator fun <T, V : AnimationVector> set(propKey: PropKey<T, V>, prop: T) {
        @Suppress("UNCHECKED_CAST")
        propKey as PropKey<Any, AnimationVector>
        props[propKey] = prop as Any
    }
}
