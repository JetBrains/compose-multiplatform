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

import androidx.compose.runtime.dispatch.DefaultMonotonicFrameClock
import androidx.compose.runtime.dispatch.MonotonicFrameClock
import androidx.compose.runtime.dispatch.withFrameMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A [MonotonicFrameAnimationClock] is an [AnimationClockObservable] that is built on top of the
 * [MonotonicFrameClock] found in the given [scope]. Use this when you want to use APIs that
 * require the old [AnimationClockObservable], but your clock is a coroutine based
 * [MonotonicFrameClock]. If the scope doesn't contain a frame clock, the
 * [DefaultMonotonicFrameClock] is used.
 *
 * Since a frame clock is a [coroutine element][kotlin.coroutines.CoroutineContext.Element], you
 * can add it to an existing scope using composition:
 * ```
 * withContext(ManualFrameClock(0L) + CoroutineName("Scope with a manually driven clock")) {
 *     withFrameMillis {
 *         // ...
 *     }
 * }
 * ```
 * Note that some coroutine contexts define a default frame clock, so make sure you add your
 * frame clock to a context (`context + clock`) instead of the other way around (`clock + context`).
 */
class MonotonicFrameAnimationClock(
    private val scope: CoroutineScope
) : AnimationClockObservable {

    private val observers = mutableMapOf<AnimationClockObserver, Job>()

    val hasObservers: Boolean
        @Suppress("DEPRECATION_ERROR")
        get() = synchronized(observers) {
            observers.isNotEmpty()
        }

    override fun subscribe(observer: AnimationClockObserver) {
        // Launch a new coroutine that keeps awaiting frames on the monotonic clock,
        // until unsubscribe for that observer is called.
        @Suppress("DEPRECATION_ERROR")
        synchronized(observers) {
            observers[observer] = scope.launch {
                val clock = coroutineContext[MonotonicFrameClock] ?: DefaultMonotonicFrameClock
                // ManualAnimationClock might send the current time when a subscriber subscribes.
                // ManualFrameClock doesn't, because there's no concept of subscription. Fix this
                // in the glue between MonotonicFrameClock and AnimationClockObservable.
                if (clock is ManualFrameClock && clock.dispatchOnSubscribe) {
                    observer.onAnimationFrame(clock.currentTime / 1_000_000)
                }
                while (true) {
                    clock.withFrameMillis { millis ->
                        observer.onAnimationFrame(millis)
                    }
                }
            }
        }
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        @Suppress("DEPRECATION_ERROR")
        synchronized(observers) {
            observers.remove(observer)?.cancel()
        }
    }
}
