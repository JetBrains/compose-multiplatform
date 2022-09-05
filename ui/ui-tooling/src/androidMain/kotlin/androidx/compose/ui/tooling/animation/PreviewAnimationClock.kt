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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimation
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.Transition
import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.TransitionInfo
import androidx.compose.ui.tooling.animation.clock.AnimatedVisibilityClock
import androidx.compose.ui.tooling.animation.clock.ComposeAnimationClock
import androidx.compose.ui.tooling.animation.clock.TransitionClock
import androidx.compose.ui.tooling.animation.clock.millisToNanos
import androidx.compose.ui.tooling.animation.states.AnimatedVisibilityState
import androidx.compose.ui.tooling.animation.states.TargetState

/**
 * Used to keep track and control animations in the context of Compose Previews. This class is
 * expected to be controlled by the Animation Preview in Android Studio, and most of its methods
 * will be called via reflection, either directly from Android Studio or through
 * `ComposeViewAdapter`.
 *
 * Methods to be intercepted in Android Studio:
 * * [notifySubscribe]
 * * [notifyUnsubscribe]
 *
 * Methods to be called from Android Studio:
 * * [updateFromAndToStates]
 * * [updateAnimatedVisibilityState]
 * * [getAnimatedVisibilityState]
 * * [getMaxDuration]
 * * [getMaxDurationPerIteration]
 * * [getAnimatedProperties]
 * * [getTransitions]
 * * [setClockTime]
 * * [setClockTimes]
 */
internal open class PreviewAnimationClock(private val setAnimationsTimeCallback: () -> Unit = {}) {

    private val TAG = "PreviewAnimationClock"

    private val DEBUG = false

    /** Map of subscribed [TransitionComposeAnimation]s and corresponding [TransitionClock]s. */
    @VisibleForTesting
    internal val transitionClocks =
        mutableMapOf<TransitionComposeAnimation<*>, TransitionClock<*>>()

    /**
     * Map of subscribed [AnimatedVisibilityComposeAnimation]s and corresponding [AnimatedVisibilityClock].
     */
    @VisibleForTesting
    internal val animatedVisibilityClocks =
        mutableMapOf<AnimatedVisibilityComposeAnimation, AnimatedVisibilityClock>()

    /** All subscribed animations clocks. */
    private val allClocks: List<ComposeAnimationClock<*, *>>
        get() = transitionClocks.values + animatedVisibilityClocks.values

    private fun findClock(animation: ComposeAnimation): ComposeAnimationClock<*, *>? {
        return transitionClocks[animation] ?: animatedVisibilityClocks[animation]
    }

    fun trackTransition(animation: Transition<*>) {
        trackAnimation(animation) {
            animation.parse()?.let {
                transitionClocks[it] = TransitionClock(it)
                notifySubscribe(it)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun trackAnimatedVisibility(animation: Transition<*>, onSeek: () -> Unit = {}) {
        // All AnimatedVisibility animations should be Transition<Boolean>.
        // If it's not the case - ignore it.
        if (animation.currentState !is Boolean) return
        trackAnimation(animation) {
            animation as Transition<Boolean>
            val composeAnimation = animation.parseAnimatedVisibility()
            onSeek()
            animatedVisibilityClocks[composeAnimation] =
                AnimatedVisibilityClock(composeAnimation).apply {
                    setClockTime(0L)
                }
            notifySubscribe(composeAnimation)
        }
    }

    fun trackAnimateXAsState(animation: Animatable<*, *>) {
        trackUnsupported(animation, animation.label)
    }

    fun trackAnimateContentSize(animation: Any) {
        trackUnsupported(animation, "animateContentSize")
    }

    fun trackTargetBasedAnimations(animation: TargetBasedAnimation<*, *>) {
        trackUnsupported(animation, "TargetBasedAnimation")
    }

    fun trackDecayAnimations(animation: DecayAnimation<*, *>) {
        trackUnsupported(animation, "DecayAnimation")
    }

    fun trackAnimatedContent(animation: Transition<*>) {
        trackUnsupported(animation, "AnimatedContent")
    }

    fun trackInfiniteTransition(animation: InfiniteTransition) {
        trackUnsupported(animation, "InfiniteTransition")
    }

    @VisibleForTesting
    val trackedUnsupportedAnimations = linkedSetOf<UnsupportedComposeAnimation>()

    private fun trackUnsupported(animation: Any, label: String) {
        trackAnimation(animation) {
            UnsupportedComposeAnimation.create(label)?.let {
                trackedUnsupportedAnimations.add(it)
                notifySubscribe(it)
            }
        }
    }

    /** Tracked animations. */
    private val trackedAnimations = linkedSetOf<Any>()
    private val lock = Any()

    private fun trackAnimation(animation: Any, createClockAndSubscribe: (Any) -> Unit): Boolean {
        synchronized(lock) {
            if (trackedAnimations.contains(animation)) {
                if (DEBUG) {
                    Log.d(TAG, "Animation $animation is already being tracked")
                }
                return false
            }
            trackedAnimations.add(animation)
        }

        createClockAndSubscribe(animation)

        if (DEBUG) {
            Log.d(TAG, "Animation $animation is now tracked")
        }

        return true
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
     * Updates the [TargetState] corresponding to the given [ComposeAnimation].
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun updateFromAndToStates(composeAnimation: ComposeAnimation, fromState: Any, toState: Any) {
        transitionClocks[composeAnimation]?.setStateParameters(fromState, toState)
    }

    /**
     * Updates the given [AnimatedVisibilityClock]'s with the given state.
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun updateAnimatedVisibilityState(composeAnimation: ComposeAnimation, state: Any) {
        animatedVisibilityClocks[composeAnimation]?.setStateParameters(state)
    }

    /**
     * Returns the [AnimatedVisibilityState] corresponding to the given
     * [AnimatedVisibilityClock] object. Falls back to [AnimatedVisibilityState.Enter].
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun getAnimatedVisibilityState(composeAnimation: ComposeAnimation): AnimatedVisibilityState {
        return animatedVisibilityClocks[composeAnimation]?.state ?: AnimatedVisibilityState.Enter
    }

    /**
     * Returns the duration (ms) of the longest animation being tracked.
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun getMaxDuration(): Long {
        return allClocks.maxOfOrNull { it.getMaxDuration() } ?: 0
    }

    /**
     * Returns the longest duration (ms) per iteration among the animations being tracked. This
     * can be different from [getMaxDuration], for instance, when there is one or more repeatable
     * animations with multiple iterations.
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun getMaxDurationPerIteration(): Long {
        return allClocks.maxOfOrNull { it.getMaxDurationPerIteration() } ?: 0
    }

    /**
     *  Returns a list of the given [ComposeAnimation]'s animated properties. The properties are
     *  wrapped into a [ComposeAnimatedProperty] object containing the property label and the
     *  corresponding value at the current time.
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun getAnimatedProperties(animation: ComposeAnimation): List<ComposeAnimatedProperty> {
        return findClock(animation)?.getAnimatedProperties() ?: emptyList()
    }

    /**
     * Returns a list of the given [ComposeAnimation]'s animated properties. The properties are
     * wrapped into a [TransitionInfo] object containing the property label, start and time
     * of animation and values of the animation.
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun getTransitions(animation: ComposeAnimation, stepMillis: Long): List<TransitionInfo> {
        return findClock(animation)?.getTransitions(stepMillis) ?: emptyList()
    }

    /**
     * Seeks each animation being tracked to the given [animationTimeMillis].
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun setClockTime(animationTimeMillis: Long) {
        val timeNanos = millisToNanos(animationTimeMillis)
        allClocks.forEach { it.setClockTime(timeNanos) }
        setAnimationsTimeCallback.invoke()
    }

    /**
     * Seeks each animation being tracked to the given [animationTimeMillis].
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun setClockTimes(animationTimeMillis: Map<ComposeAnimation, Long>) {
        animationTimeMillis.forEach { (composeAnimation, millis) ->
            findClock(composeAnimation)?.setClockTime(millisToNanos(millis))
        }
        setAnimationsTimeCallback.invoke()
    }

    /**
     * Unsubscribes the currently tracked animations and clears all the caches.
     */
    fun dispose() {
        allClocks.forEach { notifyUnsubscribe(it.animation) }
        trackedUnsupportedAnimations.forEach { notifyUnsubscribe(it) }
        trackedUnsupportedAnimations.clear()
        transitionClocks.clear()
        animatedVisibilityClocks.clear()
        trackedAnimations.clear()
    }
}
