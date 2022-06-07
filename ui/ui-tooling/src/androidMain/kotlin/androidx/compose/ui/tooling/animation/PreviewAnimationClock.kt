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

package androidx.compose.ui.tooling.animation

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.RepeatableSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.VectorizedDurationBasedAnimationSpec
import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.ComposeAnimationType
import androidx.compose.animation.tooling.TransitionInfo
import java.util.concurrent.TimeUnit

/**
 * Used to keep track and control animations in the context of Compose Previews. This class is
 * expected to be controlled by the Animation Preview in Android Studio, and most of its methods
 * will be called via reflection, either directly from Android Studio or through
 * `ComposeViewAdapter`.
 *
 * Users will be able to select specific frames of tracked animations when inspecting them in
 * Android Studio through [setClockTime], and read the values of their animated properties by
 * calling [getAnimatedProperties].
 *
 * @suppress
 */
internal open class PreviewAnimationClock(private val setAnimationsTimeCallback: () -> Unit = {}) {

    private val TAG = "PreviewAnimationClock"

    private val DEBUG = false

    /**
     * Set of tracked [TransitionComposeAnimation]s, each one having a [Transition] object that
     * is used in [setClockTime], where we call
     * [Transition.setPlaytimeAfterInitialAndTargetStateEstablished],
     * and in [getAnimatedProperties], where we get the animation values.
     */
    @VisibleForTesting
    internal val trackedTransitions = hashSetOf<TransitionComposeAnimation>()

    /**
     * Set of tracked [AnimatedVisibilityComposeAnimation]s, each one having a [Transition] object
     * representing the parent and used in [setClockTime], where we call
     * [Transition.setPlaytimeAfterInitialAndTargetStateEstablished]. Each
     * [AnimatedVisibilityComposeAnimation] also has another [Transition] object representing the
     * child transition used in [getAnimatedProperties], where we get the animation values.
     */
    @VisibleForTesting
    internal val trackedAnimatedVisibility = hashSetOf<AnimatedVisibilityComposeAnimation>()

    /**
     * Maps [Transition]s to their corresponding cached [TransitionState], which we use to seek
     * the animations when updating the clock time.
     */
    @VisibleForTesting
    internal val transitionStates = hashMapOf<Transition<Any>, TransitionState>()
    private val transitionStatesLock = Any()

    /**
     * Maps [Transition]s to their corresponding cached [AnimatedVisibilityState], which we use
     * to seek the animations when updating the clock time.
     */
    @VisibleForTesting
    internal val animatedVisibilityStates = hashMapOf<Transition<Any>, AnimatedVisibilityState>()
    private val animatedVisibilityStatesLock = Any()

    fun trackTransition(transition: Transition<Any>) {
        synchronized(transitionStatesLock) {
            if (transitionStates.containsKey(transition)) {
                if (DEBUG) {
                    Log.d(TAG, "Transition $transition is already being tracked")
                }
                return@trackTransition
            }
            transitionStates[transition] =
                TransitionState(transition.currentState, transition.targetState)
        }

        if (DEBUG) {
            Log.d(TAG, "Transition $transition is now tracked")
        }

        val composeAnimation = transition.parse()
        trackedTransitions.add(composeAnimation)
        notifySubscribe(composeAnimation)
    }

    fun trackAnimatedVisibility(parent: Transition<Any>, onSeek: () -> Unit = {}) {
        synchronized(animatedVisibilityStatesLock) {
            if (animatedVisibilityStates.containsKey(parent)) {
                if (DEBUG) {
                    Log.d(TAG, "AnimatedVisibility transition $parent is already being tracked")
                }
                return@trackAnimatedVisibility
            }
            // If the Composable is visible, set Exit as the default animation, otherwise use Enter
            animatedVisibilityStates[parent] =
                if (parent.currentState as Boolean) {
                    AnimatedVisibilityState.Exit
                } else {
                    AnimatedVisibilityState.Enter
                }
        }

        if (DEBUG) {
            Log.d(TAG, "AnimatedVisibility transition $parent is now tracked")
        }

        val composeAnimation = parent.parseAnimatedVisibility()
        // Call seek on the first frame to get the correct duration
        val (current, target) = animatedVisibilityStates[parent]!!.toCurrentTargetPair()
        parent.setPlaytimeAfterInitialAndTargetStateEstablished(
            initialState = current,
            targetState = target,
            0
        )
        onSeek()
        trackedAnimatedVisibility.add(composeAnimation)
        notifySubscribe(composeAnimation)
    }

    @VisibleForTesting
    protected open fun notifySubscribe(animation: ComposeAnimation) {
        // This method is expected to be no-op. It is intercepted in Android Studio using bytecode
        // manipulation, in order for the tools to be aware that the animation is now tracked.
    }

    @VisibleForTesting
    protected open fun notifyUnsubscribe(animation: ComposeAnimation) {
        // This method is expected to be no-op. It is intercepted in Android Studio using bytecode
        // manipulation, in order for the tools to be aware that the animation is no longer
        // tracked.
    }

    /**
     * Updates the [TransitionState] corresponding to the given [ComposeAnimation] in the
     * [transitionStates] map, creating a [TransitionState] with the given `from` and `to` states.
     */
    fun updateFromAndToStates(composeAnimation: ComposeAnimation, fromState: Any, toState: Any) {
        if (composeAnimation.type != ComposeAnimationType.TRANSITION_ANIMATION) return

        if (trackedTransitions.contains(composeAnimation)) {
            val transitionComposeAnimation = composeAnimation as TransitionComposeAnimation
            synchronized(transitionStatesLock) {
                transitionStates[transitionComposeAnimation.animationObject] =
                    TransitionState(fromState, toState)
            }
        }
    }

    /**
     * Updates the given [AnimatedVisibilityComposeAnimation]'s cached [AnimatedVisibilityState]
     * with the given state.
     */
    fun updateAnimatedVisibilityState(
        composeAnimation: AnimatedVisibilityComposeAnimation,
        state: Any
    ) {
        if (trackedAnimatedVisibility.contains(composeAnimation)) {
            synchronized(animatedVisibilityStatesLock) {
                animatedVisibilityStates[composeAnimation.animationObject] =
                    state as AnimatedVisibilityState
            }
        }
    }

    /**
     * Returns the cached [AnimatedVisibilityState] corresponding to the given
     * [AnimatedVisibilityComposeAnimation] object. Falls back to [AnimatedVisibilityState.Enter]
     * if there is no state currently mapped to the [AnimatedVisibilityComposeAnimation].
     */
    fun getAnimatedVisibilityState(
        composeAnimation: AnimatedVisibilityComposeAnimation
    ): AnimatedVisibilityState {
        return animatedVisibilityStates[composeAnimation.animationObject]
            // Fallback to Enter by default
            ?: AnimatedVisibilityState.Enter
    }

    /**
     * Returns the duration (ms) of the longest animation being tracked.
     */
    fun getMaxDuration(): Long {
        // TODO(b/160126628): support other animation types, e.g. AnimatedValue
        val transitionsDuration = trackedTransitions.map { composeAnimation ->
            nanosToMillis(composeAnimation.animationObject.totalDurationNanos)
        }.maxOrNull() ?: -1

        val animatedVisibilityDuration = trackedAnimatedVisibility.map { composeAnimation ->
            nanosToMillis(composeAnimation.childTransition?.totalDurationNanos ?: return@map -1)
        }.maxOrNull() ?: -1

        return maxOf(transitionsDuration, animatedVisibilityDuration)
    }

    /**
     * Returns the longest duration (ms) per iteration among the animations being tracked. This
     * can be different from [getMaxDuration], for instance, when there is one or more repeatable
     * animations with multiple iterations.
     *
     * TODO(b/177895209): re-add support repeatable/infinite animations.
     */
    fun getMaxDurationPerIteration(): Long {
        // TODO(b/160126628): support other animation types, e.g. AnimatedValue
        val transitionsDuration = trackedTransitions.map { composeAnimation ->
            nanosToMillis(composeAnimation.animationObject.totalDurationNanos)
        }.maxOrNull() ?: -1

        val animatedVisibilityDuration = trackedAnimatedVisibility.map { composeAnimation ->
            nanosToMillis(composeAnimation.childTransition?.totalDurationNanos ?: return@map -1)
        }.maxOrNull() ?: -1

        return maxOf(transitionsDuration, animatedVisibilityDuration)
    }

    /**
     *  Returns a list of the given [Transition]'s animated properties. The properties are
     *  wrapped into a [ComposeAnimatedProperty] object containing the property label and the
     *  corresponding value at the current time.
     */
    fun getAnimatedProperties(animation: ComposeAnimation): List<ComposeAnimatedProperty> {
        if (trackedTransitions.contains(animation)) {
            val transition = (animation as TransitionComposeAnimation).animationObject
            // In case the transition have child transitions, make sure to return their
            // descendant animations as well.
            return transition.allAnimations().mapNotNull {
                ComposeAnimatedProperty(it.label, it.value ?: return@mapNotNull null)
            }
        } else if (trackedAnimatedVisibility.contains(animation)) {
            (animation as AnimatedVisibilityComposeAnimation).childTransition?.let { child ->
                return child.allAnimations().mapNotNull {
                    ComposeAnimatedProperty(it.label, it.value ?: return@mapNotNull null)
                }
            }
        }
        return emptyList()
    }

    /**
     * Returns a list of the given [Transition]'s animated properties. The properties animation is
     * wrapped into a [TransitionInfo] object containing the property label, start and time
     * of animation and values of the animation.
     */
    fun getTransitions(animation: ComposeAnimation, stepMillis: Long): List<TransitionInfo> {
        if (trackedTransitions.contains(animation)) {
            val transition = (animation as TransitionComposeAnimation).animationObject
            return transition.allAnimations().map {
                it.createTransitionInfo(stepMillis)
            }
        } else if (trackedAnimatedVisibility.contains(animation)) {
            (animation as AnimatedVisibilityComposeAnimation).childTransition?.let { child ->
                return child.allAnimations().map {
                    it.createTransitionInfo(stepMillis)
                }
            }
        }
        return emptyList()
    }

    /**
     * Seeks each animation being tracked to the given [animationTimeMs]. Expected to be called
     * via reflection from Android Studio.
     */
    fun setClockTime(animationTimeMs: Long) {
        setClockTimes((trackedTransitions + trackedAnimatedVisibility)
            .associateWith { animationTimeMs })
    }

    /**
     * Seeks each animation being tracked to the given [animationTimeMillis]. Expected to be called
     * via reflection from Android Studio.
     */
    fun setClockTimes(animationTimeMillis: Map<ComposeAnimation, Long>) {
        animationTimeMillis.forEach { (composeAnimation, millis) ->
            val timeNs = TimeUnit.MILLISECONDS.toNanos(millis)
            if (trackedTransitions.contains(composeAnimation)) {
                (composeAnimation as TransitionComposeAnimation).animationObject.let {
                    val states = transitionStates[it] ?: return@let
                    it.setPlaytimeAfterInitialAndTargetStateEstablished(
                        states.current, states.target, timeNs)
                }
            } else if (trackedAnimatedVisibility.contains(composeAnimation)) {
                (composeAnimation as AnimatedVisibilityComposeAnimation).animationObject.let {
                    val (current, target) =
                        animatedVisibilityStates[it]?.toCurrentTargetPair() ?: return@let
                    it.setPlaytimeAfterInitialAndTargetStateEstablished(current, target, timeNs)
                }
            }
        }
        setAnimationsTimeCallback.invoke()
    }

    /**
     * Unsubscribes the currently tracked animations and clears all the caches.
     */
    fun dispose() {
        trackedTransitions.forEach { notifyUnsubscribe(it) }
        trackedAnimatedVisibility.forEach { notifyUnsubscribe(it) }

        trackedAnimatedVisibility.clear()
        trackedTransitions.clear()
        animatedVisibilityStates.clear()
        transitionStates.clear()
    }

    @VisibleForTesting
    internal data class TransitionState(val current: Any, val target: Any)

    /**
     * Creates [TransitionInfo] from [Transition.TransitionAnimationState].
     * * [TransitionInfo.startTimeMillis] is an animation delay if it has one.
     * * [TransitionInfo.endTimeMillis] is an animation duration as it's already includes the delay.
     * * [TransitionInfo.specType] is a java class name of the spec.
     * * [TransitionInfo.values] a map of animation values from [TransitionInfo.startTimeMillis]
     * to [TransitionInfo.endTimeMillis] with [stepMs] sampling.
     */
    private fun <T, V : AnimationVector, S>
    Transition<S>.TransitionAnimationState<T, V>.createTransitionInfo(stepMs: Long = 1):
        TransitionInfo {
            val endTimeMs = nanosToMillis(this.animation.durationNanos)
            val startTimeMs: Long by lazy {
                val animationSpec = this.animationSpec
                when (animationSpec) {
                    is TweenSpec<*> -> animationSpec.delay
                    is SnapSpec<*> -> animationSpec.delay
                    is KeyframesSpec<*> -> animationSpec.config.delayMillis
                    is RepeatableSpec<*> -> {
                        if (animationSpec.initialStartOffset.offsetType == StartOffsetType.Delay)
                            animationSpec.initialStartOffset.offsetMillis
                        else 0L
                    }
                    is InfiniteRepeatableSpec<*> -> {
                        if (animationSpec.initialStartOffset.offsetType == StartOffsetType.Delay)
                            animationSpec.initialStartOffset.offsetMillis
                        else 0L
                    }
                    is VectorizedDurationBasedAnimationSpec<*> -> animationSpec.delayMillis
                    else -> 0L
                }.toLong()
            }
            val values: Map<Long, T> by lazy {
                val values: MutableMap<Long, T> = mutableMapOf()
                // Always add start and end points.
                values[startTimeMs] = this.animation.getValueFromNanos(
                    millisToNanos(startTimeMs)
                )
                values[endTimeMs] = this.animation.getValueFromNanos(millisToNanos(endTimeMs))

                for (millis in startTimeMs..endTimeMs step stepMs) {
                    values[millis] = this.animation.getValueFromNanos(millisToNanos(millis))
                }
                values
            }
            return TransitionInfo(
                this.label, this.animationSpec.javaClass.name,
                startTimeMs, endTimeMs, values
            )
        }

    /**
     * Converts the given time in nanoseconds to milliseconds, rounding up when needed.
     */
    private fun nanosToMillis(timeNs: Long) = (timeNs + 999_999) / 1_000_000

    /**
     * Converts the given time in milliseconds to nanoseconds.
     */
    private fun millisToNanos(timeMs: Long) = timeMs * 1_000_000L

    private fun AnimatedVisibilityState.toCurrentTargetPair() =
        if (this == AnimatedVisibilityState.Enter) false to true else true to false

    /**
     * Return all the animations of a [Transition], as well as all the animations of its every
     * descendant [Transition]s.
     */
    private fun Transition<*>.allAnimations(): List<Transition<*>.TransitionAnimationState<*, *>> {
        val descendantAnimations = transitions.flatMap { it.allAnimations() }
        return animations + descendantAnimations
    }
}
