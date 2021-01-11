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

/**
 * A custom clock whose frame time can be manually updated via mutating [clockTimeMillis].
 * Observers will be called immediately with the current time when they are subscribed. Use
 * [dispatchOnSubscribe] = false to wait for the next tick instead, which can be useful if the
 * current time might be outdated.
 */
class ManualAnimationClock(
    initTimeMillis: Long,
    private val dispatchOnSubscribe: Boolean = true
) : BaseAnimationClock() {
    /**
     * Clock time in milliseconds. When [clockTimeMillis] is updated, the [ManualAnimationClock]
     * notifies all its observers (i.e. animations) the new clock time. The animations will
     * consequently snap to the new play time.
     */
    var clockTimeMillis: Long = initTimeMillis
        set(value) {
            field = value
            // Notify subscribers when the value is set
            dispatchTime(value)
        }

    /**
     * Whether or not there are [AnimationClockObserver]s observing this clock.
     */
    val hasObservers: Boolean get() = hasObservers()

    override fun subscribe(observer: AnimationClockObserver) {
        super.subscribe(observer)
        if (dispatchOnSubscribe) {
            // Immediately push the current frame time to the new subscriber
            observer.onAnimationFrame(clockTimeMillis)
        }
    }
}

/**
 * Base implementation for the AnimationClockObservable that handles the subscribing and
 * unsubscribing logic that would be common for all custom animation clocks.
 */
@Suppress("DEPRECATION_ERROR") // TODO: b/159875450 use of synchronized is deprecated.
abstract class BaseAnimationClock : AnimationClockObservable {
    // Using LinkedHashSet to increase removal performance
    private val observers: MutableSet<AnimationClockObserver> = LinkedHashSet()

    private val pendingActions: MutableList<Int> = mutableListOf()
    private val pendingObservers: MutableList<AnimationClockObserver> = mutableListOf()

    private fun addToPendingActions(action: Int, observer: AnimationClockObserver) =
        synchronized(pendingActions) {
            pendingActions.add(action) && pendingObservers.add(observer)
        }

    private fun pendingActionsIsNotEmpty(): Boolean =
        synchronized(pendingActions) {
            pendingActions.isNotEmpty()
        }

    private inline fun forEachObserver(crossinline action: (AnimationClockObserver) -> Unit) =
        synchronized(observers) {
            observers.forEach(action)
        }

    /**
     * Subscribes [observer] to this clock. Duplicate subscriptions will be ignored.
     */
    override fun subscribe(observer: AnimationClockObserver) {
        addToPendingActions(AddAction, observer)
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        addToPendingActions(RemoveAction, observer)
    }

    /*@CallSuper*/
    internal open fun dispatchTime(frameTimeMillis: Long) {
        processPendingActions()

        forEachObserver {
            it.onAnimationFrame(frameTimeMillis)
        }

        while (pendingActionsIsNotEmpty()) {
            processPendingActions().forEach {
                it.onAnimationFrame(frameTimeMillis)
            }
        }
    }

    internal fun hasObservers(): Boolean {
        synchronized(observers) {
            // Start with processing pending actions: it might remove the last observers
            processPendingActions()
            return observers.isNotEmpty()
        }
    }

    private fun processPendingActions(): Set<AnimationClockObserver> {
        synchronized(observers) {
            synchronized(pendingActions) {
                if (pendingActions.isEmpty()) {
                    return emptySet()
                }
                val additions = LinkedHashSet<AnimationClockObserver>()
                pendingActions.forEachIndexed { i, action ->
                    when (action) {
                        AddAction -> {
                            // This check ensures that we only have one instance of the observer in
                            // the callbacks at any given time.
                            if (observers.add(pendingObservers[i])) {
                                additions.add(pendingObservers[i])
                            }
                        }
                        RemoveAction -> {
                            observers.remove(pendingObservers[i])
                            additions.remove(pendingObservers[i])
                        }
                    }
                }
                pendingActions.clear()
                pendingObservers.clear()
                return additions
            }
        }
    }

    private companion object {
        private const val AddAction = 1
        private const val RemoveAction = 2
    }
}
