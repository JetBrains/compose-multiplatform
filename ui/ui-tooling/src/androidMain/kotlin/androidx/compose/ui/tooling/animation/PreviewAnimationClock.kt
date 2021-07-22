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
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.ComposeAnimationType
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
@OptIn(InternalAnimationApi::class)
internal open class PreviewAnimationClock(private val setAnimationsTimeCallback: () -> Unit = {}) {

    private val TAG = "PreviewAnimationClock"

    private val DEBUG = false

    /**
     * Set of tracked [TransitionComposeAnimation]s, each one having a [Transition] object that
     * is used [setClockTime], where we call [Transition.seek], and in [getAnimatedProperties],
     * where we get the animation values.
     */
    @VisibleForTesting
    internal val trackedTransitions = hashSetOf<TransitionComposeAnimation>()

    /**
     * Maps [Transition]s to their corresponding cached [TransitionState], which we use to seek
     * the animations when updating the clock time.
     */
    @VisibleForTesting
    internal val transitionStates = hashMapOf<Transition<Any>, TransitionState>()
    private val transitionStatesLock = Any()

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
     * Returns the duration (ms) of the longest animation being tracked.
     */
    fun getMaxDuration(): Long {
        // TODO(b/160126628): support other animation types, e.g. AnimatedValue
        return trackedTransitions.map { composeAnimation ->
            nanosToMillis(composeAnimation.animationObject.totalDurationNanos)
        }.maxOrNull() ?: -1
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
        return trackedTransitions.map { composeAnimation ->
            nanosToMillis(composeAnimation.animationObject.totalDurationNanos)
        }.maxOrNull() ?: -1
    }

    /**
     *  Returns a list of the given [Transition]'s animated properties. The properties are
     *  wrapped into a [Pair] of property label and the corresponding value at the current time.
     */
    fun getAnimatedProperties(animation: ComposeAnimation): List<ComposeAnimatedProperty> {
        if (animation.type != ComposeAnimationType.TRANSITION_ANIMATION) return emptyList()
        if (trackedTransitions.contains(animation)) {
            val transition = (animation as TransitionComposeAnimation).animationObject
            return transition.animations.mapNotNull {
                ComposeAnimatedProperty(it.label, it.value ?: return@mapNotNull null)
            }
        }
        return emptyList()
    }

    /**
     * Seeks each animation being tracked to the given [animationTimeMs]. Expected to be called
     * via reflection from Android Studio.
     */
    fun setClockTime(animationTimeMs: Long) {
        val timeNs = TimeUnit.MILLISECONDS.toNanos(animationTimeMs)
        trackedTransitions.forEach { composeAnimation ->
            composeAnimation.animationObject.let {
                val states = transitionStates[it] ?: return@let
                it.seek(states.current, states.target, timeNs)
            }
        }
        setAnimationsTimeCallback.invoke()
    }

    /**
     * Unsubscribes the currently tracked animations and clears all the caches.
     */
    fun dispose() {
        trackedTransitions.forEach { notifyUnsubscribe(it) }
        trackedTransitions.clear()
        transitionStates.clear()
    }

    @VisibleForTesting
    internal data class TransitionState(val current: Any, val target: Any)

    /**
     * Converts the given time in nanoseconds to milliseconds, rounding up when needed.
     */
    private fun nanosToMillis(timeNs: Long) = (timeNs + 999_999) / 1_000_000
}
