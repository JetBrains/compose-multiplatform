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

package androidx.compose.animation

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

/**
 * Return a new [AnimationClockObservable] wrapping this one that will auto-unsubscribe all
 * [AnimationClockObserver]s when this call leaves the composition, preventing clock
 * subscriptions from persisting beyond the composition lifecycle.
 *
 * If you are creating an animation object during composition that will subscribe to an
 * [AnimationClockObservable] or otherwise hold a long-lived reference to one to subscribe to later,
 * create that object with a clock returned by this method.
 */
@Composable
fun AnimationClockObservable.asDisposableClock(): DisposableAnimationClock {
    val disposable = remember(this) { DisposableAnimationClock(this) }
    DisposableEffect(disposable) {
        onDispose {
            disposable.dispose()
        }
    }
    return disposable
}

/**
 * Clock that remembers all of its active subscriptions so that it can dispose of upstream
 * subscriptions. Create auto-disposing clocks in composition using [asDisposableClock].
 */
class DisposableAnimationClock(
    private val clock: AnimationClockObservable
) : AnimationClockObservable {

    // TODO switch to atomicfu if this class survives a move to suspending animation API
    private val allSubscriptions = mutableSetOf<AnimationClockObserver>()
    private var disposed = false

    override fun subscribe(observer: AnimationClockObserver) {
        @Suppress("DEPRECATION_ERROR")
        synchronized(allSubscriptions) {
            if (!disposed) {
                allSubscriptions += observer
                clock.subscribe(observer)
            }
        }
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        @Suppress("DEPRECATION_ERROR")
        synchronized(allSubscriptions) {
            if (allSubscriptions.remove(observer)) {
                clock.unsubscribe(observer)
            }
        }
    }

    /**
     * Unsubscribe all current subscriptions to the clock. No new subscriptions are permitted;
     * further calls to [subscribe] will be ignored.
     * After a call to [dispose], [isDisposed] will return `true`.
     */
    fun dispose() {
        @Suppress("DEPRECATION_ERROR")
        synchronized(allSubscriptions) {
            allSubscriptions.forEach { clock.unsubscribe(it) }
            allSubscriptions.clear()
            disposed = true
        }
    }

    /**
     * `true` if [dispose] has been called and no new subscriptions are permitted.
     */
    @Suppress("DEPRECATION_ERROR")
    val isDisposed: Boolean get() = synchronized(allSubscriptions) { disposed == true }
}