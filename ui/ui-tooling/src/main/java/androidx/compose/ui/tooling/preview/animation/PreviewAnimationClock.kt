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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.tooling.preview.animation

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.ManualAnimationClock
import androidx.compose.animation.core.SeekableAnimation
import androidx.compose.animation.core.TransitionAnimation
import androidx.compose.animation.core.createSeekableAnimation
import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.ComposeAnimationType

/**
 * [AnimationClockObservable] used to control animations in the context of Compose Previews. This
 * clock is expected to be controlled by the Animation Inspector in Android Studio, and most of
 * its methods will be called via reflection, either directly from Android Studio or through
 * `ComposeViewAdapter`.
 *
 * It uses an underlying [ManualAnimationClock], as users will be able to select specific frames
 * of subscribed animations when inspecting them in Android Studio.
 *
 * @suppress
 */
@OptIn(InternalAnimationApi::class)
internal open class PreviewAnimationClock(
    private val initialTimeMs: Long = 0L,
    private val setClockTimeCallback: () -> Unit = {}
) :
    AnimationClockObservable {

    private val TAG = "PreviewAnimationClock"

    private val DEBUG = false

    /**
     * Maps subscribed [AnimationClockObserver]s to [ComposeAnimation]s. We parse the observers
     * into [ComposeAnimation]s to make them better to handle, but we still need to hold the
     * original objects in order to clean everything up on [unsubscribe].
     */
    @VisibleForTesting
    internal val observersToAnimations = hashMapOf<AnimationClockObserver, ComposeAnimation>()

    /**
     * Maps [ComposeAnimation]s representing [TransitionAnimation]s to their corresponding
     * [SeekableAnimation], which we use to obtain the animated properties from. Since updating
     * the clock will happen way more often than changing the transition states, we cache one
     * [SeekableAnimation] per animation and call `getAnimValuesAt` on it when the clock changes
     * instead of creating a new [SeekableAnimation] each time. Instead, we create it when `from`
     * or `to` states change.
     */
    @VisibleForTesting
    internal val seekableAnimations = hashMapOf<ComposeAnimation, SeekableAnimation<*>>()

    /**
     * [AnimationClockObserver]s should be added to this set while their corresponding animations
     * are having their `from` and `to` states updated. The animation framework unsubscribes and
     * re-subscribes the animation in the process, and we need to keep track of that to ignore
     * unsubscriptions that are caused by the states update process.
     */
    private val pendingObservers = hashSetOf<AnimationClockObserver>()

    private val pendingObserversLock = Any()

    @VisibleForTesting
    internal val clock = ManualAnimationClock(initialTimeMs)

    override fun subscribe(observer: AnimationClockObserver) {
        // Ignore subscriptions of observers already subscribed.
        if (observersToAnimations.containsKey(observer)) return

        if (DEBUG) {
            Log.d(TAG, "AnimationClockObserver $observer subscribed")
        }
        clock.subscribe(observer)
        when (observer) {
            is TransitionAnimation<*>.TransitionAnimationClockObserver -> {
                observer.animation.monotonic = false
                observer.parse()
            }
            // TODO(b/160126628): support other animation types, e.g. AnimatedValue
            else -> null
        }?.let {
            observersToAnimations[observer] = it
            notifySubscribe(it)
        }
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        synchronized(pendingObserversLock) {
            // unsubscribe is expected to be called once per state update. If There is another
            // call, the animation actually is trying to unsubscribe and we need to process it.
            if (pendingObservers.remove(observer)) {
                return
            }
        }

        if (DEBUG) {
            Log.d(TAG, "AnimationClockObserver $observer unsubscribed")
        }
        clock.unsubscribe(observer)
        observersToAnimations.remove(observer)?.let {
            notifyUnsubscribe(it)
            seekableAnimations.remove(it)
        }
    }

    @VisibleForTesting
    protected open fun notifySubscribe(animation: ComposeAnimation) {
        // This method is expected to be no-op. It is intercepted in Android Studio using bytecode
        // manipulation, in order for the tools to be aware that the animation was subscribed.
    }

    @VisibleForTesting
    protected open fun notifyUnsubscribe(animation: ComposeAnimation) {
        // This method is expected to be no-op. It is intercepted in Android Studio using bytecode
        // manipulation, in order for the tools to be aware that the animation was unsubscribed.
    }

    /**
     * Updates the [SeekableAnimation] corresponding to the given [ComposeAnimation], creating it
     * with the given `from` and `to` states/
     */
    fun updateSeekableAnimation(composeAnimation: ComposeAnimation, fromState: Any, toState: Any) {
        if (composeAnimation.type != ComposeAnimationType.TRANSITION_ANIMATION) return
        @Suppress("UNCHECKED_CAST")
        val animation = composeAnimation.animationObject as TransitionAnimation<Any>
        seekableAnimations[composeAnimation] = animation.createSeekableAnimation(fromState, toState)
    }

    /**
     * Updates all the [TransitionAnimation]s `from` and `to` states. Since we're calling the
     * `snapToState` and `toState` APIs, which respectively unsubscribes and subscribes
     * animations, we also reset the [clock] to make sure all the animations are re-subscribed at
     * the initial time. As we would be unsubscribing and re-subscribing the same animations, we
     * add their corresponding observers to [pendingObservers] while updating the animation states.
     */
    fun updateAnimationStates() {
        observersToAnimations.forEach { (observer, composeAnimation) ->
            seekableAnimations[composeAnimation]?.let { seekableAnimation ->
                synchronized(pendingObserversLock) {
                    pendingObservers.add(observer)
                }
                @Suppress("UNCHECKED_CAST")
                val animation = composeAnimation.animationObject as TransitionAnimation<Any>
                animation.snapToState(seekableAnimation.fromState!!)
                animation.toState(seekableAnimation.toState!!)
            }
        }
        synchronized(pendingObserversLock) {
            pendingObservers.clear()
        }
        // Reset the clock time so all the animations have it as the start time.
        clock.clockTimeMillis = initialTimeMs
    }

    /**
     * Returns the duration of the longest animation being tracked.
     */
    fun getMaxDuration(): Long {
        // TODO(b/160126628): support other animation types, e.g. AnimatedValue
        return seekableAnimations.map { it.value.duration }.maxOrNull() ?: -1
    }

    /**
     * Returns the longest duration per iteration among the animations being tracked. This can be
     * different from [getMaxDuration], for instance, when there is one or more repeatable
     * animations with multiple iterations.
     */
    fun getMaxDurationPerIteration(): Long {
        // TODO(b/160126628): support other animation types, e.g. AnimatedValue
        return seekableAnimations.map { it.value.maxDurationPerIteration }.maxOrNull() ?: -1
    }

    /**
     *  Returns a list of the given [TransitionAnimation]'s animated properties. The properties
     *  are wrapped into a [Pair] of property label and the corresponding value at the current time.
     */
    fun getAnimatedProperties(animation: ComposeAnimation): List<ComposeAnimatedProperty> {
        if (animation.type != ComposeAnimationType.TRANSITION_ANIMATION) return emptyList()
        seekableAnimations[animation]?.let { seekableAnimation ->
            val time = clock.clockTimeMillis - initialTimeMs
            return seekableAnimation.getAnimValuesAt(time).entries.map {
                ComposeAnimatedProperty(it.key.label, it.value)
            }
        }
        return emptyList()
    }

    /**
     * Sets [clock] time to the given [animationTimeMs], relative to [initialTimeMs]. Expected to
     * be called via reflection from Android Studio.
     */
    fun setClockTime(animationTimeMs: Long) {
        clock.clockTimeMillis = initialTimeMs + animationTimeMs
        setClockTimeCallback.invoke()
    }

    fun dispose() {
        observersToAnimations.clear()
        seekableAnimations.clear()
    }
}
